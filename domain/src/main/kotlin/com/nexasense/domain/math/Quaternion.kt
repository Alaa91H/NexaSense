package com.nexasense.domain.math

import com.nexasense.domain.model.Vec3
import kotlin.math.sqrt

/**
 * Quaternion in the Android rotation-vector convention: the rotation vector
 * elements are `axis * sin(θ/2)` and `w = cos(θ/2)`.
 */
data class Quaternion(
    val w: Float,
    val x: Float,
    val y: Float,
    val z: Float,
) {

    fun normalized(): Quaternion {
        val norm = sqrt(w * w + x * x + y * y + z * z)
        if (norm <= 0f || !norm.isFinite()) return IDENTITY
        val inv = 1f / norm
        return Quaternion(w * inv, x * inv, y * inv, z * inv)
    }

    /**
     * Converts to the row-major rotation matrix used by the Android Sensor
     * Framework (`SensorManager.getRotationMatrixFromVector` convention),
     * which maps device coordinates into world coordinates (x=East, y=North,
     * z=Up).
     */
    fun toRotationMatrix(): RotationMatrix {
        val n = normalized()
        val w = n.w
        val x = n.x
        val y = n.y
        val z = n.z

        val m = FloatArray(9)
        m[0] = 1f - 2f * (y * y + z * z)
        m[1] = 2f * (x * y - w * z)
        m[2] = 2f * (x * z + w * y)

        m[3] = 2f * (x * y + w * z)
        m[4] = 1f - 2f * (x * x + z * z)
        m[5] = 2f * (y * z - w * x)

        m[6] = 2f * (x * z - w * y)
        m[7] = 2f * (y * z + w * x)
        m[8] = 1f - 2f * (x * x + y * y)
        return RotationMatrix(m)
    }

    companion object {
        val IDENTITY: Quaternion = Quaternion(1f, 0f, 0f, 0f)

        /**
         * Builds a quaternion from an Android rotation-vector reading
         * `(x, y, z)` where `w` is derived as `cos(θ/2)`.
         */
        fun fromRotationVector(x: Float, y: Float, z: Float): Quaternion {
            val wSquared = 1f - x * x - y * y - z * z
            val w = if (wSquared > 0f) sqrt(wSquared) else 0f
            return Quaternion(w, x, y, z).normalized()
        }

        /**
         * Quaternion for a device whose top axis points at [degrees] clockwise
         * from magnetic north (a compass heading). Clockwise yaw is rotation
         * about the -Z axis in the framework's world frame.
         */
        fun fromYawDegrees(degrees: Float): Quaternion {
            val half = Math.toRadians(AngleMath.normalizeTo360(degrees).toDouble()) / 2.0
            val s = kotlin.math.sin(half).toFloat()
            val c = kotlin.math.cos(half).toFloat()
            return Quaternion(c, 0f, 0f, -s)
        }
    }
}

/** Row-major 3x3 rotation matrix, indexable as `m[row, col]`. */
data class RotationMatrix(val values: FloatArray) {
    operator fun get(row: Int, col: Int): Float = values[row * 3 + col]

    /** Rotates a vector: `R * v`. */
    fun rotate(v: Vec3): Vec3 = Vec3(
        x = this[0, 0] * v.x + this[0, 1] * v.y + this[0, 2] * v.z,
        y = this[1, 0] * v.x + this[1, 1] * v.y + this[1, 2] * v.z,
        z = this[2, 0] * v.x + this[2, 1] * v.y + this[2, 2] * v.z,
    )

    companion object {
        val IDENTITY: RotationMatrix = RotationMatrix(
            floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f),
        )
    }
}
