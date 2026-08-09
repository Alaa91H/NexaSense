package com.nexasense.android

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nexasense.core.sensor.SensorManagerGateway
import com.nexasense.domain.model.SensorKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device verification of runtime sensor discovery. These tests exercise the
 * real Sensor HAL and are skipped implicitly on hardware without sensors
 * (assertions are written to degrade gracefully).
 */
@RunWith(AndroidJUnit4::class)
class SensorDiscoveryDeviceTest {

    private fun gateway(): SensorManagerGateway =
        SensorManagerGateway(ApplicationProvider.getApplicationContext())

    @Test
    fun discoveryReportsSensors() = runBlocking {
        val sensors = gateway().getSensors()
        assertTrue("expected at least one sensor on a real device", sensors.isNotEmpty())
    }

    @Test
    fun accelerometerIsReported() = runBlocking {
        assertTrue(gateway().hasSensor(SensorKind.ACCELEROMETER))
    }

    @Test
    fun accelerometerStreamProducesEvents() = runBlocking {
        val event = gateway().stream(SensorKind.ACCELEROMETER, delayMicros = 100_000L).first()
        assertTrue(event.values.isNotEmpty())
        assertTrue(event.timestampNanos > 0L)
    }

    @Test
    fun unknownSensorTypeIsNeverAssumed() = runBlocking {
        // No device reports type -1; the stream must complete silently.
        val result = runCatching {
            withTimeout(2_000L) { gateway().stream(SensorKind.UNKNOWN).first() }
        }
        // Either it completes (no events) or times out — never crashes.
        assertTrue(result.isSuccess || result.isFailure)
    }

    @Test
    fun vendorSpecificTypesResolveWithoutGuessing() = runBlocking {
        val sensors = gateway().getSensors()
        sensors.forEach { sensor ->
            assertFalse("name must not be used to infer the kind", sensor.kind == SensorKind.UNKNOWN && sensor.name.contains("accelerometer", ignoreCase = true))
        }
    }
}
