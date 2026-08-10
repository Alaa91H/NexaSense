package com.nexasense.data.sensor

import com.nexasense.domain.model.SensorDescriptor
import com.nexasense.domain.model.SensorKind
import com.nexasense.domain.model.SensorReading
import com.nexasense.domain.port.CalibrationStore
import com.nexasense.domain.port.DeclinationProvider
import com.nexasense.domain.port.LocationPoint
import com.nexasense.domain.port.LocationProvider
import com.nexasense.domain.port.SensorDiscovery
import com.nexasense.domain.port.SensorEventStream
import com.nexasense.domain.port.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Regression test for the launch-time force-close shipped in v1.0.0-v1.0.2:
 * `_heading = MutableStateFlow(unavailableHeading())` ran before the
 * `settings` field was initialized, so `unavailableHeading()` read a null
 * `settings` and threw inside the constructor — crashing the app on every
 * launch. Constructing the engine with fakes would have caught it.
 */
class CompassEngineConstructionTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Test
    fun `constructing the compass engine must not throw`() {
        val engine = CompassEngineImpl(
            discovery = FakeDiscovery(),
            streams = FakeStreams(),
            declinationProvider = object : DeclinationProvider {
                override fun declinationAt(
                    latitudeDegrees: Double,
                    longitudeDegrees: Double,
                    altitudeMeters: Double,
                    timeMillis: Long,
                ): Float? = 0f
            },
            locationProvider = FakeLocationProvider(),
            calibrationStore = FakeCalibrationStore(),
            settingsStore = FakeSettingsStore(),
            scope = scope,
            locationPermissionGranted = { false },
        )
        assertNotNull(engine)
        // The initial (pre-activation) heading must already be usable.
        assertNotNull(engine.heading.value)
    }

    private class FakeDiscovery : SensorDiscovery {
        override suspend fun getSensors(): List<SensorDescriptor> = emptyList()
        override suspend fun hasSensor(kind: SensorKind): Boolean = false
        override suspend fun sensorsOf(kind: SensorKind): List<SensorDescriptor> = emptyList()
    }

    private class FakeStreams : SensorEventStream {
        override fun stream(
            kind: SensorKind,
            delayMicros: Long,
            sensorId: Int?,
        ): Flow<SensorReading> = emptyFlow()
    }

    private class FakeLocationProvider : LocationProvider {
        override suspend fun lastKnownLocation(): LocationPoint? = null
        override suspend fun requestCurrentLocation(timeoutMillis: Long): LocationPoint? = null
        override fun locationUpdates(
            minDistanceMeters: Float,
            minIntervalMillis: Long,
        ): Flow<LocationPoint> = emptyFlow()
    }

    private class FakeCalibrationStore : CalibrationStore {
        override val magnetometerCalibration =
            emptyFlow<com.nexasense.domain.model.MagnetometerCalibration>()
        override val levelCalibration = emptyFlow<com.nexasense.domain.model.LevelCalibration>()
        override suspend fun saveMagnetometer(calibration: com.nexasense.domain.model.MagnetometerCalibration) = Unit
        override suspend fun resetMagnetometer() = Unit
        override suspend fun saveLevel(calibration: com.nexasense.domain.model.LevelCalibration) = Unit
        override suspend fun resetLevel() = Unit
    }

    private class FakeSettingsStore : SettingsStore {
        override val settings = emptyFlow<com.nexasense.domain.model.AppSettings>()
        override suspend fun update(transform: (com.nexasense.domain.model.AppSettings) -> com.nexasense.domain.model.AppSettings) = Unit
        override suspend fun reset() = Unit
    }
}
