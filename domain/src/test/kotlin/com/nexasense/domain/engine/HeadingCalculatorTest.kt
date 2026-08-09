package com.nexasense.domain.engine

import com.nexasense.domain.model.Vec3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeadingCalculatorTest {

    private val eps = 1f

    @Test
    fun `flat device with field to magnetic north reads 0`() {
        val heading = HeadingCalculator.fromAccelerometerMagnetometer(
            accel = Vec3(0f, 0f, 9.81f),
            mag = Vec3(0f, 30f, 40f),
        )
        assertEquals(0f, heading!!, eps)
    }

    @Test
    fun `device rotated to east reads 90`() {
        // Top points East, so device +x points South and the field's north
        // component reads along device -x.
        val heading = HeadingCalculator.fromAccelerometerMagnetometer(
            accel = Vec3(0f, 0f, 9.81f),
            mag = Vec3(-30f, 0f, 40f),
        )
        assertEquals(90f, heading!!, eps)
    }

    @Test
    fun `device rotated to south reads 180`() {
        val heading = HeadingCalculator.fromAccelerometerMagnetometer(
            accel = Vec3(0f, 0f, 9.81f),
            mag = Vec3(0f, -30f, 40f),
        )
        assertEquals(180f, heading!!, eps)
    }

    @Test
    fun `device rotated to west reads 270`() {
        // Top points West, so device +x points North.
        val heading = HeadingCalculator.fromAccelerometerMagnetometer(
            accel = Vec3(0f, 0f, 9.81f),
            mag = Vec3(30f, 0f, 40f),
        )
        assertEquals(270f, heading!!, eps)
    }

    @Test
    fun `tilted device pointing north still reads 0`() {
        // Device pitched 30° nose-up, horizontal component of field to the north.
        val heading = HeadingCalculator.fromAccelerometerMagnetometer(
            accel = Vec3(0f, 4.905f, 8.496f),
            mag = Vec3(0f, 1.116f, -0.067f),
        )
        assertEquals(0f, heading!!, eps)
    }

    @Test
    fun `heading from rotation vector for cardinal directions`() {
        // Rotation vectors use the framework world frame: yaw is clockwise
        // (about -Z), so a top facing East is a negative rotation about +Z.
        val root = kotlin.math.sqrt(0.5f)
        assertEquals(0f, HeadingCalculator.fromRotationVector(0f, 0f, 0f), eps)
        assertEquals(90f, HeadingCalculator.fromRotationVector(0f, 0f, -root), eps)
        assertEquals(180f, HeadingCalculator.fromRotationVector(0f, 0f, 1f), eps)
        assertEquals(270f, HeadingCalculator.fromRotationVector(0f, 0f, root), eps)
    }

    @Test
    fun `wrap-around keeps 359_9 near 360`() {
        // 0.1° counterclockwise from north: positive RV about +Z.
        val heading = HeadingCalculator.fromRotationVector(0f, 0f, 0.00087f)
        assertEquals(359.9f, heading, 0.1f)
    }

    @Test
    fun `magnetometer parallel to gravity is degenerate`() {
        val heading = HeadingCalculator.fromAccelerometerMagnetometer(
            accel = Vec3(0f, 0f, 9.81f),
            mag = Vec3(0f, 0f, 50f),
        )
        assertNull(heading)
    }

    @Test
    fun `free fall is degenerate`() {
        val heading = HeadingCalculator.fromAccelerometerMagnetometer(
            accel = Vec3(0f, 0f, 0f),
            mag = Vec3(0f, 30f, 40f),
        )
        assertNull(heading)
    }

    @Test
    fun `NaN values are rejected`() {
        assertNull(
            HeadingCalculator.fromAccelerometerMagnetometer(
                accel = Vec3(Float.NaN, 0f, 9.81f),
                mag = Vec3(0f, 30f, 40f),
            ),
        )
        assertNull(
            HeadingCalculator.fromAccelerometerMagnetometer(
                accel = Vec3(0f, 0f, 9.81f),
                mag = Vec3(Float.POSITIVE_INFINITY, 30f, 40f),
            ),
        )
    }
}
