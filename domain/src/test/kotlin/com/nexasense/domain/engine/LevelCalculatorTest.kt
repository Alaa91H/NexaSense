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

    @Test
    fun `vertical deviation is zero exactly at upright`() {
        assertEquals(0f, LevelCalculator.verticalDeviation(90f), 0f)
        assertEquals(0f, LevelCalculator.verticalDeviation(-90f), 0f)
    }

    @Test
    fun `vertical deviation is negative in the top-up hemisphere and positive in the bottom-up hemisphere`() {
        // Top-up: pitch is above +90° off flat, so the deviation is negative
        // (the plumb needle leans left). Bottom-up: positive (leans right).
        assertEquals(-30f, LevelCalculator.verticalDeviation(60f), eps)
        assertEquals(-10f, LevelCalculator.verticalDeviation(80f), eps)
        assertEquals(30f, LevelCalculator.verticalDeviation(-60f), eps)
        assertEquals(10f, LevelCalculator.verticalDeviation(-80f), eps)
    }

    @Test
    fun `bubble moves right when the left edge is lowered`() {
        // Positive roll (left lowered / right edge raised) drives the
        // indicator toward the raised end: +x (right).
        val (x, y) = LevelCalculator.bubbleFactors(0f, 30f, 45f)
        assertEquals(30f / 45f, x, eps)
        assertEquals(0f, y, 0f)
    }

    @Test
    fun `bubble moves up when the top edge is raised`() {
        // Positive pitch (top raised) drives the indicator up (−y).
        val (x, y) = LevelCalculator.bubbleFactors(30f, 0f, 45f)
        assertEquals(0f, x, 0f)
        assertEquals(-30f / 45f, y, eps)
    }

    @Test
    fun `bubble saturates at the rim beyond the scale`() {
        val (x, y) = LevelCalculator.bubbleFactors(0f, 60f, 45f)
        assertEquals(1f, x, 0f)
        assertEquals(0f, y, 0f)
    }

    @Test
    fun `plumb offset points straight down at zero degrees`() {
        val (dx, dy) = com.nexasense.domain.math.AngleMath.plumbOffset(100f, 0f)
        assertEquals(0f, dx, 0f)
        assertEquals(100f, dy, eps)
    }

    @Test
    fun `plumb offset positive degrees point right and negative left`() {
        // +90° from straight-down = toward the right edge (+x, level y);
        // -90° = toward the left edge (−x).
        val (rightX, rightY) = com.nexasense.domain.math.AngleMath.plumbOffset(100f, 90f)
        assertEquals(100f, rightX, eps)
        assertEquals(0f, rightY, eps)

        val (leftX, leftY) = com.nexasense.domain.math.AngleMath.plumbOffset(100f, -90f)
        assertEquals(-100f, leftX, eps)
        assertEquals(0f, leftY, eps)
    }

    @Test
    fun `plumb offset magnitude equals the deviation angle`() {
        // At 45° the point sits at 45° from straight-down, equidistant in x
        // and y (sin45 = cos45).
        val (dx, dy) = com.nexasense.domain.math.AngleMath.plumbOffset(100f, 45f)
        assertEquals(70.71f, dx, 0.1f)
        assertEquals(70.71f, dy, 0.1f)
    }
}
