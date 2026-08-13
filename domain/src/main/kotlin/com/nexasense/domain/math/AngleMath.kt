package com.nexasense.domain.math

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Angle helpers shared by every engine. */
object AngleMath {

    const val DEGREES_IN_CIRCLE = 360f
    const val HALF_CIRCLE = 180f

    /** Normalizes an angle in degrees into [0, 360). */
    fun normalizeTo360(degrees: Float): Float {
        if (degrees.isNaN() || degrees.isInfinite()) return 0f
        var result = degrees % DEGREES_IN_CIRCLE
        if (result < 0f) result += DEGREES_IN_CIRCLE
        return result
    }

    /** Normalizes an angle in degrees into (-180, 180]. */
    fun normalizeTo180(degrees: Float): Float {
        if (degrees.isNaN() || degrees.isInfinite()) return 0f
        var result = degrees % DEGREES_IN_CIRCLE
        if (result > HALF_CIRCLE) result -= DEGREES_IN_CIRCLE
        if (result <= -HALF_CIRCLE) result += DEGREES_IN_CIRCLE
        return result
    }

    /** Shortest signed difference `target - current` in (-180, 180] degrees. */
    fun angularDifference(target: Float, current: Float): Float =
        normalizeTo180(target - current)

    /** Linear interpolation between two angles taking the short way around. */
    fun lerpDegrees(start: Float, end: Float, fraction: Float): Float {
        val difference = angularDifference(end, start)
        return normalizeTo360(start + difference * fraction)
    }

    /**
     * Wraps a raw azimuth in radians into [0, 360) degrees.
     * `atan2` returns values in (-π, π].
     */
    fun radiansToDegrees(radians: Float): Float =
        normalizeTo360((radians * 180f / PI.toFloat()))

    /** Radians, with `atan2`-style handling. */
    fun atan2(y: Float, x: Float): Float = atan2(y, x)

    /** Absolute distance between two angles in degrees, taking the short way. */
    fun angularDistance(a: Float, b: Float): Float = abs(angularDifference(b, a))

    // --- Centralized aliases used across the compass and qibla engines ---

    /** Normalizes any angle in degrees into [0, 360). */
    fun normalizeDegrees(degrees: Float): Float = normalizeTo360(degrees)

    /** Normalizes any angle in degrees into (-180, 180]. */
    fun normalizeSignedDegrees(degrees: Float): Float = normalizeTo180(degrees)

    /** Shortest signed difference `target - current` in (-180, 180] degrees. */
    fun shortestAngularDifference(target: Float, current: Float): Float =
        angularDifference(target, current)

    /**
     * Position of a compass point on a circle using the compass-rose
     * convention: [degrees] is measured clockwise from north, so 0° sits at
     * the top, 90° at the right, 180° at the bottom and 270° at the left.
     * Compose's y axis points down, hence the `-cos` y term.
     *
     * The rose lives in physical screen coordinates (not layout direction),
     * so it renders identically in LTR and RTL — a compass must not mirror.
     */
    fun roseOffset(centerX: Float, centerY: Float, radius: Float, degrees: Float): Pair<Float, Float> {
        val radians = Math.toRadians(degrees.toDouble())
        val s = sin(radians)
        val c = cos(radians)
        return Pair(
            centerX + (radius * s).toFloat(),
            centerY - (radius * c).toFloat(),
        )
    }
}
