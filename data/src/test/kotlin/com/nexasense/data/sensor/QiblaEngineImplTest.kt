package com.nexasense.data.sensor

import com.nexasense.domain.engine.DeclinationCache
import com.nexasense.domain.model.AccuracyLevel
import com.nexasense.domain.model.AppSettings
import com.nexasense.domain.model.CardinalDirection
import com.nexasense.domain.model.Heading
import com.nexasense.domain.model.HeadingMode
import com.nexasense.domain.model.HeadingSource
import com.nexasense.domain.model.MagneticFieldState
import com.nexasense.domain.model.MagnetometerCalibration
import com.nexasense.domain.model.NorthReference
import com.nexasense.domain.model.QiblaStatus
import com.nexasense.domain.port.CompassEngine
import com.nexasense.domain.port.DeclinationProvider
import com.nexasense.domain.port.LocationPoint
import com.nexasense.domain.port.LocationProvider
import com.nexasense.domain.port.MagneticFieldMonitor
import com.nexasense.domain.port.SettingsStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Qibla engine location-state tests. These pin two behaviors that broke the
 * feature in the field:
 *
 * 1. While a location fix is being requested, the sensor-rate state updates
 *    must report CALCULATING — not LOCATION_UNAVAILABLE. Previously
 *    `updateState()` stomped the CALCULATING status with LOCATION_UNAVAILABLE
 *    the moment a sensor event fired, so the user saw "Qibla unavailable —
 *    Location required" during the entire request even when a fix was on its
 *    way.
 * 2. A failed request must be retried automatically (with a pause) instead of
 *    giving up permanently, so the feature recovers when location becomes
 *    available — no manual refresh needed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QiblaEngineImplTest {

@Test
fun `state stays CALCULATING while the location request is in flight`() = runTest {
        val settings = MutableStateFlow(AppSettings(qiblaEnabled = true))
        val heading = MutableStateFlow(heading(degrees = 0f))
        val magneticField = MutableStateFlow(magneticFieldState(AccuracyLevel.MEDIUM))
        val liveCalibration = MutableStateFlow(calibrated())
        val location = GatedLocationProvider()

        val engine = newEngine(
            settings = settings,
            heading = heading,
            magneticField = magneticField,
            liveCalibration = liveCalibration,
            location = location,
            scope = testScope(),
        )
        engine.setActive(true)
        runCurrent()

        // A sensor event while the request is in flight must not flip the
        // state to LOCATION_UNAVAILABLE.
        heading.value = heading(degrees = 90f)
        runCurrent()
        assertEquals(QiblaStatus.CALCULATING, engine.state.value.status)

        // The fix arrives: the state must become READY.
        location.gate.complete(fix())
        runCurrent()
        assertEquals(QiblaStatus.READY, engine.state.value.status)

        engine.setActive(false)
    }

    @Test
    fun `failed request reports LOCATION_UNAVAILABLE then retries until a fix arrives`() = runTest {
        val settings = MutableStateFlow(AppSettings(qiblaEnabled = true))
        val heading = MutableStateFlow(heading(degrees = 0f))
        val magneticField = MutableStateFlow(magneticFieldState(AccuracyLevel.MEDIUM))
        val liveCalibration = MutableStateFlow(calibrated())
        val location = QueueLocationProvider(ArrayDeque(listOf(null, fix())))

        val engine = newEngine(
            settings = settings,
            heading = heading,
            magneticField = magneticField,
            liveCalibration = liveCalibration,
            location = location,
            scope = testScope(),
        )
        engine.setActive(true)
        runCurrent()

        // First attempt failed: honest "unavailable" during the pause.
        assertEquals(QiblaStatus.LOCATION_UNAVAILABLE, engine.state.value.status)
        assertEquals(1, location.requestCount)

        // After the retry pause the engine tries again and recovers on its own.
        advanceTimeBy(QiblaEngineImpl.LOCATION_RETRY_DELAY_MILLIS)
        runCurrent()
        assertEquals(QiblaStatus.READY, engine.state.value.status)
        assertEquals(2, location.requestCount)

        engine.setActive(false)
    }

    @Test
    fun `disabling the feature cancels an in-flight request`() = runTest {
        val settings = MutableStateFlow(AppSettings(qiblaEnabled = true))
        val heading = MutableStateFlow(heading(degrees = 0f))
        val magneticField = MutableStateFlow(magneticFieldState(AccuracyLevel.MEDIUM))
        val liveCalibration = MutableStateFlow(calibrated())
        val location = GatedLocationProvider()

        val engine = newEngine(
            settings = settings,
            heading = heading,
            magneticField = magneticField,
            liveCalibration = liveCalibration,
            location = location,
            scope = testScope(),
        )
        engine.setActive(true)
        runCurrent()
        assertEquals(QiblaStatus.CALCULATING, engine.state.value.status)

        settings.value = AppSettings(qiblaEnabled = false)
        runCurrent()
        assertEquals(QiblaStatus.QIBLA_DISABLED, engine.state.value.status)

        // The request was cancelled: completing the gate later must not change
        // anything (no fix is consumed, no second request is made).
        location.gate.complete(fix())
        runCurrent()
        assertEquals(1, location.requests.size)
        assertEquals(QiblaStatus.QIBLA_DISABLED, engine.state.value.status)

        engine.setActive(false)
    }

    /** A scope on the runTest virtual scheduler so delays are test-controlled. */
    private fun TestScope.testScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))

    private fun newEngine(
        settings: MutableStateFlow<AppSettings>,
        heading: MutableStateFlow<Heading>,
        magneticField: MutableStateFlow<MagneticFieldState>,
        liveCalibration: MutableStateFlow<MagnetometerCalibration>,
        location: LocationProvider,
        scope: CoroutineScope,
    ): QiblaEngineImpl {
        val compass = FakeCompassEngine(heading, magneticField)
        return QiblaEngineImpl(
            compassEngine = compass,
            magneticMonitor = FakeMagneticMonitor(magneticField, liveCalibration),
            locationProvider = location,
            declinationProvider = object : DeclinationProvider {
                override fun declinationAt(
                    latitudeDegrees: Double,
                    longitudeDegrees: Double,
                    altitudeMeters: Double,
                    timeMillis: Long,
                ): Float? = 0f
            },
            declinationCache = DeclinationCache(),
            settingsStore = FakeSettingsStore(settings),
            scope = scope,
            locationPermissionGranted = { true },
        )
    }

    private fun heading(degrees: Float): Heading = Heading(
        degrees = degrees,
        cardinal = CardinalDirection.fromDegrees(degrees),
        source = HeadingSource.ROTATION_VECTOR,
        mode = HeadingMode.MAGNETIC,
        declinationDegrees = 0f,
        requestedNorthReference = NorthReference.AUTOMATIC,
        effectiveNorthReference = NorthReference.MAGNETIC_NORTH,
    )

    private fun magneticFieldState(accuracy: AccuracyLevel): MagneticFieldState = MagneticFieldState(
        x = 30f,
        y = -15f,
        z = 40f,
        magnitudeMicroTesla = 52f,
        accuracy = accuracy,
        interference = false,
        bandViolation = false,
        baselineMicroTesla = 50f,
    )

    private fun calibrated(): MagnetometerCalibration = MagnetometerCalibration(
        offsetX = 0f,
        offsetY = 0f,
        offsetZ = 0f,
        scaleX = 1f,
        scaleY = 1f,
        scaleZ = 1f,
        sampleCount = 120,
        coverage = 1f,
        isCalibrated = true,
    )

    private fun fix(): LocationPoint = LocationPoint(
        latitudeDegrees = 52.52,
        longitudeDegrees = 13.405,
        altitudeMeters = 34.0,
        timeMillis = 1_000L,
        accuracyMeters = 30f,
    )

    /** Suspends each request until the test completes its gate. */
    private class GatedLocationProvider : LocationProvider {
        val requests = mutableListOf<CompletableDeferred<LocationPoint?>>()
        val gate = CompletableDeferred<LocationPoint?>()

        override suspend fun lastKnownLocation(): LocationPoint? = null

        override suspend fun requestCurrentLocation(timeoutMillis: Long): LocationPoint? {
            requests += gate
            return gate.await()
        }

        override fun locationUpdates(
            minDistanceMeters: Float,
            minIntervalMillis: Long,
        ): Flow<LocationPoint> = emptyFlow()
    }

    /** Returns one canned result per call; extra calls return null. */
    private class QueueLocationProvider(
        private val results: ArrayDeque<LocationPoint?>,
    ) : LocationProvider {
        var requestCount = 0
            private set

        override suspend fun lastKnownLocation(): LocationPoint? = null

        override suspend fun requestCurrentLocation(timeoutMillis: Long): LocationPoint? {
            requestCount++
            return if (results.isEmpty()) null else results.removeFirst()
        }

        override fun locationUpdates(
            minDistanceMeters: Float,
            minIntervalMillis: Long,
        ): Flow<LocationPoint> = emptyFlow()
    }

    private class FakeCompassEngine(
        override val heading: MutableStateFlow<Heading>,
        override val magneticField: MutableStateFlow<MagneticFieldState>,
    ) : CompassEngine {
        override val sensorBlocked = MutableStateFlow(false)
        override fun setActive(active: Boolean) = Unit
        override fun resetSmoothing() = Unit
        override fun setDisplayRotation(rotationDegrees: Int) = Unit
    }

    private class FakeMagneticMonitor(
        override val state: MutableStateFlow<MagneticFieldState>,
        override val liveCalibration: MutableStateFlow<MagnetometerCalibration>,
    ) : MagneticFieldMonitor {
        override fun setActive(active: Boolean) = Unit
        override fun resetCalibration() = Unit
    }

    private class FakeSettingsStore(
        override val settings: MutableStateFlow<AppSettings>,
    ) : SettingsStore {
        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            settings.value = transform(settings.value)
        }

        override suspend fun reset() = Unit
    }
}
