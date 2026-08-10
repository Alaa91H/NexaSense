package com.nexasense.data.sensor

import com.nexasense.core.logging.NexaLogger
import com.nexasense.domain.engine.DeclinationCache
import com.nexasense.domain.engine.DeclinationEngine
import com.nexasense.domain.engine.HeadingCalculator
import com.nexasense.domain.engine.MagneticFieldAnalyzer
import com.nexasense.domain.engine.NorthReferenceResolver
import com.nexasense.domain.engine.SourceSelector
import com.nexasense.domain.engine.TiltCompensatedFusion
import com.nexasense.domain.math.AngleMath
import com.nexasense.domain.math.MagneticCalibrationMath
import com.nexasense.domain.math.SmoothingFilters
import com.nexasense.domain.model.AccuracyLevel
import com.nexasense.domain.model.AppSettings
import com.nexasense.domain.model.CardinalDirection
import com.nexasense.domain.model.Heading
import com.nexasense.domain.model.HeadingMode
import com.nexasense.domain.model.HeadingSource
import com.nexasense.domain.model.MagneticFieldState
import com.nexasense.domain.model.MagnetometerCalibration
import com.nexasense.domain.model.NorthReference
import com.nexasense.domain.model.SensorKind
import com.nexasense.domain.model.SmoothingPreference
import com.nexasense.domain.model.TrueNorthUnavailableReason
import com.nexasense.domain.model.Vec3
import com.nexasense.domain.port.CalibrationStore
import com.nexasense.domain.port.CompassEngine
import com.nexasense.domain.port.DeclinationProvider
import com.nexasense.domain.port.LocationProvider
import com.nexasense.domain.port.MagneticFieldMonitor
import com.nexasense.domain.port.SensorDiscovery
import com.nexasense.domain.port.SensorEventStream
import com.nexasense.domain.port.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Compass engine.
 *
 * Source priority (see [SourceSelector]):
 *  1. Rotation Vector
 *  2. Geomagnetic Rotation Vector
 *  3. Accelerometer + Magnetometer (tilt-compensated)
 *  4. Unavailable — never faked.
 *
 * North reference: the requested [NorthReference] (Automatic / True /
 * Magnetic) is resolved to an *effective* reference via
 * [NorthReferenceResolver] based on declination availability; the effective
 * reference is always reported on the [Heading].
 *
 * The magnetometer is registered exactly once and shared by the heading
 * fallback, the interference analyzer and the calibration sampler. Sensors are
 * registered only while [setActive] is true (lifecycle-driven). Declination is
 * computed through [DeclinationCache] — never per sensor event.
 */
class CompassEngineImpl(
    private val discovery: SensorDiscovery,
    private val streams: SensorEventStream,
    private val declinationProvider: DeclinationProvider,
    private val locationProvider: LocationProvider,
    private val calibrationStore: CalibrationStore,
    private val settingsStore: SettingsStore,
    private val scope: CoroutineScope,
    private val locationPermissionGranted: () -> Boolean,
    private val declinationCache: DeclinationCache = DeclinationCache(),
) : CompassEngine, MagneticFieldMonitor {

    // IMPORTANT: this must be declared BEFORE _heading — the _heading
    // initializer calls unavailableHeading(), which reads settings.
    // Kotlin runs instance-field initializers in declaration order, so a
    // settings field declared below would still be null there and crash the
    // constructor (the launch-time FC that shipped in v1.0.0-v1.0.2).
    @Volatile
    private var settings = AppSettings.DEFAULT

    private val _heading = MutableStateFlow(unavailableHeading())
    override val heading: StateFlow<Heading> = _heading.asStateFlow()

    private val _magneticField = MutableStateFlow(MagneticFieldState.NONE)
    override val magneticField: StateFlow<MagneticFieldState> = _magneticField.asStateFlow()
    override val state: StateFlow<MagneticFieldState> = _magneticField.asStateFlow()

    private val _sensorBlocked = MutableStateFlow(false)
    override val sensorBlocked: StateFlow<Boolean> = _sensorBlocked.asStateFlow()

    private val _liveCalibration = MutableStateFlow(MagnetometerCalibration.NONE)
    override val liveCalibration: StateFlow<MagnetometerCalibration> = _liveCalibration.asStateFlow()

    private val magState = MutableStateFlow<Vec3?>(null)
    private val magAccuracy = MutableStateFlow(AccuracyLevel.UNRELIABLE)

    private val analyzer = MagneticFieldAnalyzer()
    private val sampler = MagneticCalibrationMath.Sampler()
    private val fusion = TiltCompensatedFusion()

    @Volatile
    private var smoother = SmoothingFilters.AngleSmoother(SmoothingPreference.MEDIUM.alpha)

    @Volatile
    private var calibration = MagnetometerCalibration.NONE

    @Volatile
    private var declination: Float? = null

    @Volatile
    private var locationAvailable = false

    @Volatile
    private var active = false

    @Volatile
    private var displayRotationDegrees = 0

    private var source = HeadingSource.UNAVAILABLE
    private var lastRawHeading: Float? = null
    private var samplesSinceSave = 0

    private val sensorJobs = mutableListOf<Job>()
    private val lifecycleJobs = mutableListOf<Job>()

    override fun setActive(active: Boolean) {
        if (this.active == active) return
        this.active = active
        _sensorBlocked.value = false
        if (active) startSensors() else stopSensors()
    }

    override fun resetSmoothing() {
        smoother = SmoothingFilters.AngleSmoother(settings.smoothing.alpha)
    }

    override fun setDisplayRotation(rotationDegrees: Int) {
        if (this.displayRotationDegrees == rotationDegrees) return
        this.displayRotationDegrees = rotationDegrees
        lastRawHeading?.let { _heading.value = composeHeading(it, source) }
    }

    override fun resetCalibration() {
        sampler.reset()
        samplesSinceSave = 0
        _liveCalibration.value = MagnetometerCalibration.NONE
        scope.launch { calibrationStore.resetMagnetometer() }
    }

    private fun startSensors() {
        stopSensors()
        lifecycleJobs += scope.launch {
            settingsStore.settings.collectLatest { newSettings ->
                val rateChanged = settings.sensorRate != newSettings.sensorRate
                val smoothingChanged = settings.smoothing != newSettings.smoothing
                val referenceChanged = settings.northReference != newSettings.northReference
                settings = newSettings
                if (smoothingChanged) resetSmoothing()
                if (referenceChanged) refreshDeclination()
                if (rateChanged && active) restartSensors()
            }
        }
        lifecycleJobs += scope.launch {
            calibrationStore.magnetometerCalibration.collect { persisted ->
                calibration = persisted
                // Reflect externally-persisted state (e.g. after a reset).
                if (persisted == MagnetometerCalibration.NONE) {
                    sampler.reset()
                    _liveCalibration.value = persisted
                }
            }
        }
        collectSensors()
        refreshDeclination()
    }

    private fun stopSensors() {
        sensorJobs.forEach { it.cancel() }
        sensorJobs.clear()
    }

    private fun restartSensors() {
        stopSensors()
        collectSensors()
    }

    private fun collectSensors() {
        sensorJobs += scope.launch {
            val kinds = discovery.getSensors().map { it.kind }.toSet()
            source = SourceSelector.bestSource(kinds)
            if (source == HeadingSource.UNAVAILABLE) {
                _heading.value = unavailableHeading()
                _sensorBlocked.value = false
                NexaLogger.w("No heading source available on this device.")
                return@launch
            }
            // If the heading source exists but no heading arrives within the
            // timeout, the stream is open but silent — the system "Sensors
            // Off" toggle or a per-app sensor permission (AOSP ROMs) blocks
            // it. A closed stream (no sensor) is NOT blocked.
            val watchdog = scope.launch {
                delay(SENSOR_BLOCKED_TIMEOUT_MILLIS)
                if (!_heading.value.isAvailable) _sensorBlocked.value = true
            }
            try {
                when (source) {
                    HeadingSource.ROTATION_VECTOR -> collectHeadingStream(
                        SensorKind.ROTATION_VECTOR,
                        HeadingSource.ROTATION_VECTOR,
                    )

                    HeadingSource.GEOMAGNETIC_ROTATION_VECTOR -> collectHeadingStream(
                        SensorKind.GEOMAGNETIC_ROTATION_VECTOR,
                        HeadingSource.GEOMAGNETIC_ROTATION_VECTOR,
                    )

                    HeadingSource.ACCELEROMETER_MAGNETOMETER -> collectAccelMagnetometer()
                    HeadingSource.UNAVAILABLE -> Unit
                }
                collectMagnetometer()
            } finally {
                watchdog.cancel()
            }
        }
    }

    private fun collectHeadingStream(kind: SensorKind, headingSource: HeadingSource) {
        sensorJobs += scope.launch {
            streams.stream(kind, settings.sensorRate.delayMicros).collect { reading ->
                if (reading.values.size >= 3) {
                    val heading = HeadingCalculator.fromRotationVector(reading.x, reading.y, reading.z)
                    publishHeading(heading, headingSource)
                }
            }
        }
    }

    private fun collectAccelMagnetometer() {
        sensorJobs += scope.launch {
            streams.stream(SensorKind.ACCELEROMETER, settings.sensorRate.delayMicros)
                .combine(magState) { accelReading, mag ->
                    accelReading.vector3() to mag
                }
                .collect { (accel, mag) ->
                    val m = mag ?: return@collect
                    val calibrated = MagneticCalibrationMath.apply(m, calibration)
                    val heading = fusion.heading(accel, calibrated, HeadingSource.ACCELEROMETER_MAGNETOMETER)
                    if (heading != null) {
                        publishHeading(heading, HeadingSource.ACCELEROMETER_MAGNETOMETER)
                    }
                }
        }
    }

    private fun collectMagnetometer() {
        sensorJobs += scope.launch {
            streams.stream(SensorKind.MAGNETIC_FIELD, settings.sensorRate.delayMicros).collect { reading ->
                val v = reading.vector3()
                if (v.isInvalid) return@collect
                magState.value = v
                magAccuracy.value = reading.accuracy
                analyzer.update(v.x, v.y, v.z, reading.accuracy)?.let { _magneticField.value = it }
                if (sampler.addSample(v)) {
                    samplesSinceSave++
                    if (samplesSinceSave >= CALIBRATION_SAVE_INTERVAL_SAMPLES) {
                        samplesSinceSave = 0
                        val built = sampler.build()
                        _liveCalibration.value = built
                        scope.launch { calibrationStore.saveMagnetometer(built) }
                    }
                }
            }
        }
    }

    private fun publishHeading(rawDegrees: Float, headingSource: HeadingSource) {
        if (rawDegrees.isNaN() || rawDegrees.isInfinite()) return
        _sensorBlocked.value = false
        lastRawHeading = rawDegrees
        val smoothed = if (settings.smoothing == SmoothingPreference.NONE) {
            AngleMath.normalizeTo360(rawDegrees)
        } else {
            smoother.update(rawDegrees)
        }
        _heading.value = composeHeading(smoothed, headingSource)
    }

    private fun composeHeading(smoothedDegrees: Float, headingSource: HeadingSource): Heading {
        val requested = settings.northReference
        val d = declination
        val effective = NorthReferenceResolver.effective(requested, declinationAvailable = d != null)

        // Sensors report azimuth in the device's natural frame; rotate it into
        // the display frame so the reading matches the screen top (the dial's
        // fixed marker) when the user rotates the device (auto-rotate).
        val displayDegrees = AngleMath.normalizeTo360(smoothedDegrees - displayRotationDegrees)

        val trueHeadingDegrees = d?.let { DeclinationEngine.trueHeading(displayDegrees, it) }

        val mode = when (effective) {
            NorthReference.TRUE_NORTH -> HeadingMode.TRUE
            else -> HeadingMode.MAGNETIC
        }
        val degrees = when (mode) {
            HeadingMode.TRUE -> trueHeadingDegrees ?: displayDegrees
            HeadingMode.MAGNETIC -> displayDegrees
        }
        val normalized = AngleMath.normalizeDegrees(degrees)

        val reason = when {
            requested == NorthReference.TRUE_NORTH && d == null -> {
                if (!locationPermissionGranted()) {
                    TrueNorthUnavailableReason.PERMISSION_DENIED
                } else {
                    TrueNorthUnavailableReason.LOCATION_REQUIRED
                }
            }
            else -> null
        }

        return Heading(
            degrees = normalized,
            cardinal = CardinalDirection.fromDegrees(normalized),
            source = headingSource,
            mode = mode,
            declinationDegrees = d,
            requestedNorthReference = requested,
            effectiveNorthReference = effective,
            locationAvailable = locationAvailable,
            trueNorthUnavailableReason = reason,
        )
    }

    /**
     * Refreshes the declination (cached by location/time). Only runs when the
     * selected reference can use it (Automatic or True North) — Magnetic North
     * never touches the location services.
     */
    private fun refreshDeclination() {
        if (!active || settings.northReference == NorthReference.MAGNETIC_NORTH) return
        if (!locationPermissionGranted()) {
            declination = null
            locationAvailable = false
            _heading.value = composeHeading(lastRawHeading ?: 0f, source)
            return
        }
        lifecycleJobs += scope.launch {
            val point = locationProvider.lastKnownLocation()
                ?: locationProvider.requestCurrentLocation(timeoutMillis = 8_000L)
            if (point != null) {
                locationAvailable = true
                declination = declinationCache.declination(
                    point.latitudeDegrees,
                    point.longitudeDegrees,
                    point.altitudeMeters,
                    point.timeMillis,
                    compute = declinationProvider::declinationAt,
                )
            } else {
                locationAvailable = false
                declination = null
            }
            lastRawHeading?.let { raw ->
                _heading.value = composeHeading(raw, source)
            }
        }
    }

    private fun unavailableHeading(): Heading = Heading(
        degrees = 0f,
        cardinal = CardinalDirection.N,
        source = HeadingSource.UNAVAILABLE,
        mode = HeadingMode.MAGNETIC,
        requestedNorthReference = settings.northReference,
        effectiveNorthReference = NorthReference.MAGNETIC_NORTH,
    )

    /** Reads the currently persisted calibration once (used by diagnostics). */
    suspend fun currentCalibration(): MagnetometerCalibration =
        calibrationStore.magnetometerCalibration.first()

    companion object {
        const val CALIBRATION_SAVE_INTERVAL_SAMPLES = 60

        /** Silence threshold before declaring the sensors blocked (ms). */
        const val SENSOR_BLOCKED_TIMEOUT_MILLIS = 3_000L
    }
}
