package com.nexasense.domain.engine

import com.nexasense.domain.model.HeadingSource
import com.nexasense.domain.model.SensorKind
import org.junit.Assert.assertEquals
import org.junit.Test

class SourceSelectorTest {

    @Test
    fun `rotation vector wins when present`() {
        val source = SourceSelector.bestSource(
            setOf(SensorKind.ACCELEROMETER, SensorKind.MAGNETIC_FIELD, SensorKind.ROTATION_VECTOR),
        )
        assertEquals(HeadingSource.ROTATION_VECTOR, source)
    }

    @Test
    fun `geomagnetic rotation vector is the second choice`() {
        val source = SourceSelector.bestSource(
            setOf(
                SensorKind.ACCELEROMETER,
                SensorKind.MAGNETIC_FIELD,
                SensorKind.GEOMAGNETIC_ROTATION_VECTOR,
            ),
        )
        assertEquals(HeadingSource.GEOMAGNETIC_ROTATION_VECTOR, source)
    }

    @Test
    fun `accelerometer plus magnetometer is the fallback`() {
        val source = SourceSelector.bestSource(setOf(SensorKind.ACCELEROMETER, SensorKind.MAGNETIC_FIELD))
        assertEquals(HeadingSource.ACCELEROMETER_MAGNETOMETER, source)
    }

    @Test
    fun `magnetometer alone cannot compute a heading`() {
        val source = SourceSelector.bestSource(setOf(SensorKind.MAGNETIC_FIELD))
        assertEquals(HeadingSource.UNAVAILABLE, source)
    }

    @Test
    fun `accelerometer alone cannot compute a heading`() {
        val source = SourceSelector.bestSource(setOf(SensorKind.ACCELEROMETER))
        assertEquals(HeadingSource.UNAVAILABLE, source)
    }

    @Test
    fun `no sensors means unavailable`() {
        assertEquals(HeadingSource.UNAVAILABLE, SourceSelector.bestSource(emptySet()))
    }

    @Test
    fun `vendor-specific sensors do not affect selection`() {
        val source = SourceSelector.bestSource(
            setOf(SensorKind.ACCELEROMETER, SensorKind.MAGNETIC_FIELD, SensorKind.UNKNOWN),
        )
        assertEquals(HeadingSource.ACCELEROMETER_MAGNETOMETER, source)
    }
}
