package com.nexasense.domain.model

import kotlin.math.sqrt

/** Immutable 3D vector in sensor units. */
data class Vec3(val x: Float, val y: Float, val z: Float) {

    val magnitude: Float get() = sqrt(x * x + y * y + z * z)

    operator fun plus(other: Vec3): Vec3 = Vec3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Vec3): Vec3 = Vec3(x - other.x, y - other.y, z - other.z)

    operator fun times(scalar: Float): Vec3 = Vec3(x * scalar, y * scalar, z * scalar)

    fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vec3): Vec3 = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x,
    )

    fun normalized(): Vec3? {
        val m = magnitude
        if (m <= 0f || !m.isFinite()) return null
        return this * (1f / m)
    }

    /** True if any component is NaN or infinite. */
    val isInvalid: Boolean get() = !x.isFinite() || !y.isFinite() || !z.isFinite()

    companion object {
        val ZERO: Vec3 = Vec3(0f, 0f, 0f)
    }
}
