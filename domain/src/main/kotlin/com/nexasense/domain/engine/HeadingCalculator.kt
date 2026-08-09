package com.nexasense.domain.engine

import com.nexasense.domain.math.Quaternion
import com.nexasense.domain.math.RotationMatrix
import com.nexasense.domain.model.Vec3

/**
 * Computes a magnetic heading from the best available data:
 * 1. a fused rotation vector (framework already fused accelerometer, gyroscope
 *    and magnetometer);
 * 2. raw accelerometer + magnetometer via tilt compensation.
 */
object HeadingCalculator {

    /** Heading from a rotation-vector reading `(x, y, z)` (w is derived). */
    fun fromRotationVector(x: Float, y: Float, z: Float): Float =
        OrientationEngine.azimuthDegrees(Quaternion.fromRotationVector(x, y, z).toRotationMatrix())

    /** Heading from a quaternion. */
    fun fromQuaternion(quaternion: Quaternion): Float =
        OrientationEngine.azimuthDegrees(quaternion.toRotationMatrix())

    /**
     * Tilt-compensated heading from accelerometer + magnetometer readings.
     *
     * Returns null when the reading is degenerate: the accelerometer magnitude
     * is ~0 (free fall) or the magnetometer is (nearly) parallel to gravity,
     * in which case no azimuth can be resolved. This is the same algorithm as
     * `SensorManager.getRotationMatrix` + `getOrientation`.
     */
    fun fromAccelerometerMagnetometer(accel: Vec3, mag: Vec3): Float? {
        if (accel.isInvalid || mag.isInvalid) return null
        if (accel.magnitude <= 0.1f) return null

        val ax = accel.x
        val ay = accel.y
        val az = accel.z
        val mx = mag.x
        val my = mag.y
        val mz = mag.z

        // H = M x A — the component of the magnetic field perpendicular to gravity.
        var hx = my * az - mz * ay
        var hy = mz * ax - mx * az
        var hz = mx * ay - my * ax
        val normH = kotlin.math.sqrt(hx * hx + hy * hy + hz * hz)
        if (normH < 0.1f) return null
        val invH = 1f / normH
        hx *= invH
        hy *= invH
        hz *= invH

        val invA = 1f / accel.magnitude
        val axn = ax * invA
        val ayn = ay * invA
        val azn = az * invA

        // M' = A x H
        val mx2 = ayn * hz - azn * hy
        val my2 = azn * hx - axn * hz
        val mz2 = axn * hy - ayn * hx

        val matrix = RotationMatrix(
            floatArrayOf(
                hx, hy, hz,
                mx2, my2, mz2,
                axn, ayn, azn,
            ),
        )
        return OrientationEngine.azimuthDegrees(matrix)
    }
}
