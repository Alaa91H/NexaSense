package com.nexasense.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SensorKindTest {

    @Test
    fun `known framework type ids map to their kinds`() {
        assertEquals(SensorKind.ACCELEROMETER, SensorKind.fromType(1))
        assertEquals(SensorKind.MAGNETIC_FIELD, SensorKind.fromType(2))
        assertEquals(SensorKind.GYROSCOPE, SensorKind.fromType(4))
        assertEquals(SensorKind.LIGHT, SensorKind.fromType(5))
        assertEquals(SensorKind.PRESSURE, SensorKind.fromType(6))
        assertEquals(SensorKind.PROXIMITY, SensorKind.fromType(8))
        assertEquals(SensorKind.GRAVITY, SensorKind.fromType(9))
        assertEquals(SensorKind.LINEAR_ACCELERATION, SensorKind.fromType(10))
        assertEquals(SensorKind.ROTATION_VECTOR, SensorKind.fromType(11))
        assertEquals(SensorKind.RELATIVE_HUMIDITY, SensorKind.fromType(12))
        assertEquals(SensorKind.AMBIENT_TEMPERATURE, SensorKind.fromType(13))
        assertEquals(SensorKind.GAME_ROTATION_VECTOR, SensorKind.fromType(15))
        assertEquals(SensorKind.STEP_COUNTER, SensorKind.fromType(19))
        assertEquals(SensorKind.GEOMAGNETIC_ROTATION_VECTOR, SensorKind.fromType(20))
    }

    @Test
    fun `unknown type ids map to UNKNOWN instead of guessing`() {
        assertEquals(SensorKind.UNKNOWN, SensorKind.fromType(9999))
        assertEquals(SensorKind.UNKNOWN, SensorKind.fromType(-7))
    }

    @Test
    fun `duplicate types resolve deterministically`() {
        // Two sensors of the same framework type both resolve to the same kind.
        assertEquals(
            SensorKind.fromType(2),
            SensorKind.fromType(2),
        )
    }

    @Test
    fun `vendor-specific ids never collide with known kinds`() {
        // AOSP reserves small ids; any vendor extension falls back to UNKNOWN
        // and must never be misread as a standard sensor.
        assertEquals(SensorKind.UNKNOWN, SensorKind.fromType(100))
        assertEquals(SensorKind.UNKNOWN, SensorKind.fromType(0x80))
    }
}
