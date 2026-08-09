package com.nexasense.domain.engine

import com.nexasense.domain.model.LevelCalibration
import com.nexasense.domain.model.Vec3
import org.junit.Assert.assertEquals
import org.junit.Test

class LevelCalculatorTest {

    private val eps = 0.5f

    @Test
    fun `flat device reads zero pitch and roll`() {
        val angles = LevelCalculator.fromAccelerometer(Vec3(0f, 0f, 9.81f))
        assertEquals(0f, angles.pitch, eps)
        assertEquals(0f, angles.roll, eps)
    }

    @Test
    fun `positive pitch when the top edge is raised`() {
        // Nose-up 30°: ay = g*sin(30), az = g*cos(30)
        val angles = LevelCalculator.fromAccelerometer(Vec3(0f, 4.905f, 8.496f))
        assertEquals(30f, angles.pitch, eps)
        assertEquals(0f, angles.roll, eps)
    }

    @Test
    fun `negative pitch when the top edge is lowered`() {
        val angles = LevelCalculator.fromAccelerometer(Vec3(0f, -4.905f, 8.496f))
        assertEquals(-30f, angles.pitch, eps)
        assertEquals(0f, angles.roll, eps)
    }

    @Test
    fun `positive roll when the left edge is lowered`() {
        // Left edge lowered -> device x+ points up -> the accelerometer reads
        // the reaction along +x.
        val angles = LevelCalculator.fromAccelerometer(Vec3(4.905f, 0f, 8.496f))
        assertEquals(30f, angles.roll, eps)
        assertEquals(0f, angles.pitch, eps)
    }

    @Test
    fun `negative roll when the right edge is lowered`() {
        val angles = LevelCalculator.fromAccelerometer(Vec3(-4.905f, 0f, 8.496f))
        assertEquals(-30f, angles.roll, eps)
        assertEquals(0f, angles.pitch, eps)
    }

    @Test
    fun `upright device reads 90 pitch`() {
        val angles = LevelCalculator.fromAccelerometer(Vec3(0f, 9.81f, 0f))
        assertEquals(90f, angles.pitch, eps)
    }

    @Test
    fun `invalid readings fall back to zero`() {
        val nan = LevelCalculator.fromAccelerometer(Vec3(Float.NaN, 0f, 0f))
        assertEquals(0f, nan.pitch, 0f)
        val freeFall = LevelCalculator.fromAccelerometer(Vec3(0f, 0f, 0f))
        assertEquals(0f, freeFall.roll, 0f)
    }

    @Test
    fun `display rotation maps angles into the user frame`() {
        val angles = com.nexasense.domain.model.OrientationAngles(30f, 0f)
        val r90 = LevelCalculator.mapToDisplay(angles, 90)
        assertEquals(0f, r90.pitch, eps)
        assertEquals(30f, r90.roll, eps)

        val r180 = LevelCalculator.mapToDisplay(angles, 180)
        assertEquals(-30f, r180.pitch, eps)
        assertEquals(0f, r180.roll, eps)

        val r270 = LevelCalculator.mapToDisplay(angles, 270)
        assertEquals(0f, r270.pitch, eps)
        assertEquals(-30f, r270.roll, eps)
    }

    @Test
    fun `display rotation keeps a level device level`() {
        val zero = com.nexasense.domain.model.OrientationAngles(0f, 0f)
        for (rotation in listOf(0, 90, 180, 270)) {
            val mapped = LevelCalculator.mapToDisplay(zero, rotation)
            assertEquals(0f, mapped.pitch, 0f)
            assertEquals(0f, mapped.roll, 0f)
        }
    }

    @Test
    fun `calibration offsets are subtracted`() {
        val calibration = LevelCalibration(pitchOffsetDegrees = 5f, rollOffsetDegrees = -3f, isSet = true)
        val raw = com.nexasense.domain.model.OrientationAngles(8f, -1f)
        val corrected = LevelCalculator.applyOffsets(raw, calibration)
        assertEquals(3f, corrected.pitch, eps)
        assertEquals(2f, corrected.roll, eps)
    }

    @Test
    fun `unset calibration is a no-op`() {
        val raw = com.nexasense.domain.model.OrientationAngles(8f, -1f)
        val corrected = LevelCalculator.applyOffsets(raw, LevelCalibration.NONE)
        assertEquals(8f, corrected.pitch, eps)
        assertEquals(-1f, corrected.roll, eps)
    }

    @Test
    fun `offsets wrap through 180`() {
        val calibration = LevelCalibration(pitchOffsetDegrees = 5f, rollOffsetDegrees = 0f, isSet = true)
        val corrected = LevelCalculator.applyOffsets(
            com.nexasense.domain.model.OrientationAngles(170f, 0f),
            calibration,
        )
        assertEquals(165f, corrected.pitch, eps)
    }
}
