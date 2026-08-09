package com.nexasense.domain.engine

import com.nexasense.domain.math.AngleMath
import com.nexasense.domain.math.RotationMatrix
import kotlin.math.asin
import kotlin.math.atan2

/**
 * Extracts orientation angles from a rotation matrix using the Android
 * `SensorManager.getOrientation` convention, matching AOSP exactly:
 * - azimuth = atan2(R[0][1], R[1][1]) — angle of the device's top axis in the
 *   ground plane, 0 at magnetic north and increasing **clockwise** (the docs
 *   call it "rotation about the -z axis");
 * - pitch = asin(-R[2][1]);
 * - roll = atan2(-R[2][0], R[2][2]).
 *
 * Values are in radians; helpers convert azimuth to compass degrees.
 */
object OrientationEngine {

    data class Orientation(
        val azimuthRadians: Float,
        val pitchRadians: Float,
        val rollRadians: Float,
    )

    fun fromRotationMatrix(matrix: RotationMatrix): Orientation {
        val azimuth = atan2(matrix[0, 1], matrix[1, 1])
        val pitch = asin((-matrix[2, 1]).coerceIn(-1f, 1f))
        val roll = atan2(-matrix[2, 0], matrix[2, 2])
        return Orientation(azimuth, pitch, roll)
    }

    /** Azimuth in compass degrees [0, 360). */
    fun azimuthDegrees(matrix: RotationMatrix): Float =
        AngleMath.radiansToDegrees(fromRotationMatrix(matrix).azimuthRadians)
}
