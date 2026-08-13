package com.nexasense.data.sensor

import com.nexasense.domain.engine.DeclinationCache
import com.nexasense.domain.engine.QiblaCalculator
import com.nexasense.domain.engine.SolarPositionCalculator
import com.nexasense.domain.model.AccuracyLevel
import com.nexasense.domain.model.AppSettings
import com.nexasense.domain.model.Heading
import com.nexasense.domain.model.LocationAccuracyLevel
import com.nexasense.domain.model.QiblaAlignment
import com.nexasense.domain.model.QiblaBearing
import com.nexasense.domain.model.QiblaState
import com.nexasense.domain.model.QiblaStatus
import com.nexasense.domain.port.CompassEngine
import com.nexasense.domain.port.DeclinationProvider
import com.nexasense.domain.port.LocationPoint
import com.nexasense.domain.port.LocationProvider
import com.nexasense.domain.port.MagneticFieldMonitor
import com.nexasense.domain.port.QiblaEngine
import com.nexasense.domain.port.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Qibla engine.
 *
 * Pipeline (all local, offline):
 * ```
 * user location → QiblaCalculator → true bearing to Kaaba
 * compass heading → north reference conversion → same frame as bearing
 *                        ↓
 *              relative Qibla + alignment
 * ```
 *
 * Location is requested only while the feature is enabled and the screen is
 * active; updates are distance-thresholded (≈50 m) so recalculation happens
 * on significant movement, never per sensor event.
 */
class QiblaEngineImpl(
    compassEngine: CompassEngine,
    private val magneticMonitor: MagneticFieldMonitor,
    private val locationProvider: LocationProvider,
    private val declinationProvider: DeclinationProvider,
    private val declinationCache: DeclinationCache,
    private val settingsStore: SettingsStore,
    private val scope: CoroutineScope,
    private val locationPermissionGranted: () -> Boolean,
    private val alignmentThresholdDegrees: Float = 2f,
) : QiblaEngine {

    private val _state = MutableStateFlow(QiblaState(QiblaStatus.QIBLA_DISABLED))
    override val state: StateFlow<QiblaState> = _state.asStateFlow()

    private val heading: StateFlow<Heading> = compassEngine.heading
    private val magneticField = compassEngine.magneticField
    private val liveCalibration = magneticMonitor.liveCalibration

    @Volatile
    private var settings = AppSettings.DEFAULT

    @Volatile
    private var active = false

    @Volatile
    private var lastFix: LocationPoint? = null

    /**
     * True while a location fix is being requested (cleared during the retry
     * pause — see [requestLocationWithRetry]). Keeps [updateState] from
     * reporting LOCATION_UNAVAILABLE while a request is still in flight:
     * without it, the sensor-rate state updates stomp the CALCULATING status
     * set by [startLocationIfNeeded] and the user sees "Qibla unavailable"
     * during the whole request instead of "calculating".
     */
    @Volatile
    private var requestingLocation = false

    @Volatile
    private var qiblaBearing: QiblaBearing? = null

    @Volatile
    private var declination: Float? = null

    // The sun moves ~0.004°/s, so cache its position and only recompute every
    // 30 s instead of on every sensor event (updates run at sensor rate).
    @Volatile
    private var lastSunComputeMillis = 0L

    @Volatile
    private var sunCache: SolarPositionCalculator.SunPosition? = null

    private val locationJobs = mutableListOf<Job>()
    private val lifecycleJobs = mutableListOf<Job>()

    override fun setActive(active: Boolean) {
        if (this.active == active) return
        this.active = active
        if (active) start() else stop()
    }

    override fun refreshLocation() {
        if (!active) return
        stopLocation()
        startLocationIfNeeded()
    }

    private fun start() {
        stop()
        lifecycleJobs += scope.launch {
            settingsStore.settings.collectLatest { newSettings ->
                settings = newSettings
                if (!newSettings.qiblaEnabled) {
                    stopLocation()
                    lastFix = null
                    qiblaBearing = null
                    declination = null
                    _state.value = QiblaState(QiblaStatus.QIBLA_DISABLED)
                } else {
                    startLocationIfNeeded()
                }
            }
        }
        lifecycleJobs += scope.launch {
            combine(heading, magneticField, liveCalibration) { _, _, _ -> updateState() }
        }
        // Recompute the state immediately with whatever we already have.
        updateState()
        startLocationIfNeeded()
    }

    private fun stop() {
        lifecycleJobs.forEach { it.cancel() }
        lifecycleJobs.clear()
        stopLocation()
        lastFix = null
        qiblaBearing = null
        declination = null
        _state.value = QiblaState(QiblaStatus.QIBLA_DISABLED)
    }

    private fun startLocationIfNeeded() {
        if (!active || !settings.qiblaEnabled) return
        if (locationJobs.isNotEmpty()) return
        if (!locationPermissionGranted()) {
            _state.value = QiblaState(QiblaStatus.LOCATION_PERMISSION_REQUIRED)
            return
        }
        if (lastFix != null) return
        _state.value = QiblaState(QiblaStatus.CALCULATING)
        // Set before launching so [updateState] never reports
        // LOCATION_UNAVAILABLE while the request is in flight.
        requestingLocation = true
        locationJobs += scope.launch {
            val requestJob = coroutineContext[Job]
            try {
                val fix = locationProvider.lastKnownLocation()
                    ?: requestLocationWithRetry()
                if (fix != null) {
                    onFix(fix)
                    locationProvider.locationUpdates(minDistanceMeters = 50f, minIntervalMillis = 15_000L)
                        .collect { newFix -> onFix(newFix) }
                }
            } finally {
                // Only the job that is still driving the request may clear the
                // flag: a stale finally from a cancelled predecessor (rapid
                // screen stop/start) must not mark a newer in-flight request
                // as not-requesting.
                if (requestJob == null || locationJobs.contains(requestJob)) {
                    requestingLocation = false
                }
            }
        }
    }

    /**
     * Requests a fresh fix, retrying until one arrives or the job is
     * cancelled. A single failed attempt is not terminal: GPS often needs
     * more than one try (indoors, cold start) and location services may be
     * switched on while the screen is open, so the feature should recover by
     * itself instead of requiring a manual refresh. Between attempts the
     * state reports LOCATION_UNAVAILABLE; while a request is in flight it is
     * CALCULATING.
     */
    private suspend fun requestLocationWithRetry(): LocationPoint? {
        while (currentCoroutineContext().isActive) {
            val fix = locationProvider.requestCurrentLocation(
                timeoutMillis = LOCATION_REQUEST_TIMEOUT_MILLIS,
            )
            if (fix != null) return fix
            // Honest feedback during the pause, then try again.
            requestingLocation = false
            _state.value = QiblaState(QiblaStatus.LOCATION_UNAVAILABLE)
            delay(LOCATION_RETRY_DELAY_MILLIS)
            requestingLocation = true
        }
        return null
    }

    private fun stopLocation() {
        locationJobs.forEach { it.cancel() }
        locationJobs.clear()
    }

    private fun currentSunPosition(latitude: Double, longitude: Double): SolarPositionCalculator.SunPosition {
        val now = System.currentTimeMillis()
        val cached = sunCache
        if (cached != null && now - lastSunComputeMillis < SUN_RECOMPUTE_INTERVAL_MILLIS) return cached
        val computed = SolarPositionCalculator.positionAt(latitude, longitude, now)
        sunCache = computed
        lastSunComputeMillis = now
        return computed
    }

    private fun onFix(fix: LocationPoint) {
        lastFix = fix
        qiblaBearing = QiblaCalculator.bearingToKaaba(fix.latitudeDegrees, fix.longitudeDegrees)
        declination = declinationCache.declination(
            fix.latitudeDegrees,
            fix.longitudeDegrees,
            fix.altitudeMeters,
            fix.timeMillis,
            compute = declinationProvider::declinationAt,
        )
        updateState()
    }

    private fun updateState() {
        if (!settings.qiblaEnabled) {
            _state.value = QiblaState(QiblaStatus.QIBLA_DISABLED)
            return
        }
        val fix = lastFix
        val bearing = qiblaBearing
        if (!locationPermissionGranted()) {
            _state.value = QiblaState(QiblaStatus.LOCATION_PERMISSION_REQUIRED)
            return
        }
        if (fix == null || bearing == null) {
            _state.value = QiblaState(
                // A request may still be in flight (the first fix is on its
                // way); only report unavailable once a round has finished
                // without a fix.
                if (requestingLocation) QiblaStatus.CALCULATING else QiblaStatus.LOCATION_UNAVAILABLE,
            )
            return
        }

        val locationAccuracy = LocationAccuracyLevel.fromMeters(fix.accuracyMeters)
        val compassAccuracy = magneticField.value.accuracy
        val currentHeading = heading.value

        // Current solar position (pure local math; enables the "sun aligned
        // with Qibla" shadow check without any compass). Cached — see above.
        val sun = currentSunPosition(fix.latitudeDegrees, fix.longitudeDegrees)

        val deviceTrue = QiblaCalculator.deviceHeadingInTrueReference(currentHeading, declination)
        val relative = deviceTrue?.let {
            QiblaCalculator.relativeQibla(it, bearing.bearingDegrees)
        }

        // Bearing in the same north reference as the currently displayed
        // heading, so the dial marker lines up with the heading readout.
        val bearingInDeviceReference = when (currentHeading.mode) {
            com.nexasense.domain.model.HeadingMode.TRUE -> bearing.bearingDegrees
            com.nexasense.domain.model.HeadingMode.MAGNETIC -> declination?.let {
                com.nexasense.domain.math.AngleMath.normalizeDegrees(bearing.bearingDegrees - it)
            }
        }

        val status = statusFor(
            headingAvailable = currentHeading.isAvailable,
            relative = relative,
            locationAccuracy = locationAccuracy,
            compassAccuracy = compassAccuracy,
            calibrationComplete = liveCalibration.value.isCalibrated,
        )

        val alignment = relative?.let { QiblaCalculator.alignment(it, alignmentThresholdDegrees) }
            ?: QiblaAlignment.UNAVAILABLE

        _state.value = QiblaState(
            status = status,
            bearingDegrees = bearing.bearingDegrees,
            bearingInDeviceReferenceDegrees = bearingInDeviceReference,
            relativeQiblaDegrees = relative,
            alignment = alignment,
            distanceKm = bearing.distanceKm,
            declinationDegrees = declination,
            locationAccuracy = locationAccuracy,
            compassAccuracy = compassAccuracy,
            sunAzimuthDegrees = sun.azimuthDegrees.toFloat(),
            sunElevationDegrees = sun.elevationDegrees.toFloat(),
        )
    }

    private fun statusFor(
        headingAvailable: Boolean,
        relative: Float?,
        locationAccuracy: LocationAccuracyLevel,
        compassAccuracy: AccuracyLevel,
        calibrationComplete: Boolean,
    ): QiblaStatus = when {
        !headingAvailable -> QiblaStatus.COMPASS_UNAVAILABLE
        relative == null -> QiblaStatus.CALCULATING
        QiblaCalculator.alignment(relative, alignmentThresholdDegrees) == QiblaAlignment.ALIGNED ->
            QiblaStatus.ALIGNED

        locationAccuracy == LocationAccuracyLevel.LOW -> QiblaStatus.LOCATION_ACCURACY_LOW
        compassAccuracy == AccuracyLevel.LOW || compassAccuracy == AccuracyLevel.UNRELIABLE ->
            QiblaStatus.COMPASS_ACCURACY_LOW

        !calibrationComplete -> QiblaStatus.CALIBRATION_REQUIRED
        else -> QiblaStatus.READY
    }

    companion object {
        const val SUN_RECOMPUTE_INTERVAL_MILLIS = 30_000L

        /** How long a single fresh-fix request may take before it is retried. */
        const val LOCATION_REQUEST_TIMEOUT_MILLIS = 8_000L

        /** Pause between failed location attempts before retrying. */
        const val LOCATION_RETRY_DELAY_MILLIS = 15_000L
    }
}
