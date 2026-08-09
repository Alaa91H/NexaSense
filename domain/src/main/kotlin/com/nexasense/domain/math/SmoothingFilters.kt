package com.nexasense.domain.math

import com.nexasense.domain.model.Vec3
import kotlin.math.abs

/**
 * Swappable smoothing filters for sensor streams.
 *
 * All filters are pure and testable; the constants are tuned for degrees-scale
 * readings. See docs/architecture.md for the filtering strategy.
 */
object SmoothingFilters {

    /**
     * Exponential (low-pass) smoothing: `y = α·x + (1−α)·y`.
     * Lower [alpha] means smoother output but more latency.
     */
    class ExponentialSmoothing(val alpha: Float = 0.2f) {
        private var initialized = false
        private var yx = 0f
        private var yy = 0f
        private var yz = 0f

        fun update(v: Vec3): Vec3 {
            if (v.isInvalid) return if (initialized) Vec3(yx, yy, yz) else v
            if (!initialized) {
                initialized = true
                yx = v.x
                yy = v.y
                yz = v.z
                return v
            }
            yx += alpha * (v.x - yx)
            yy += alpha * (v.y - yy)
            yz += alpha * (v.z - yz)
            return Vec3(yx, yy, yz)
        }

        fun reset() {
            initialized = false
        }
    }

    /**
     * Adaptive smoothing: the smoothing strength grows with the observed
     * deviation, so steady readings stay responsive and noisy periods are
     * dampened. `deviationScale` is the typical magnitude of a legitimate
     * single-step change.
     */
    class AdaptiveFilter(
        private val baseAlpha: Float = 0.3f,
        private val deviationScale: Float = 5f,
    ) {
        private var initialized = false
        private var yx = 0f
        private var yy = 0f
        private var yz = 0f

        fun update(v: Vec3): Vec3 {
            if (v.isInvalid) return if (initialized) Vec3(yx, yy, yz) else v
            if (!initialized) {
                initialized = true
                yx = v.x
                yy = v.y
                yz = v.z
                return v
            }
            val deviation = abs(v.x - yx) + abs(v.y - yy) + abs(v.z - yz)
            val alpha = baseAlpha * (deviationScale / (deviationScale + deviation)).coerceIn(0f, 1f)
            yx += alpha * (v.x - yx)
            yy += alpha * (v.y - yy)
            yz += alpha * (v.z - yz)
            return Vec3(yx, yy, yz)
        }

        fun reset() {
            initialized = false
        }
    }

    /**
     * Smoothes a heading in degrees without ever taking the long way around
     * the 0°/360° boundary. `alpha = 1` disables smoothing.
     */
    class AngleSmoother(private val alpha: Float = 0.2f) {
        private var initialized = false
        private var smoothed = 0f

        fun update(degrees: Float): Float {
            if (degrees.isNaN() || degrees.isInfinite()) {
                return if (initialized) smoothed else 0f
            }
            val input = AngleMath.normalizeTo360(degrees)
            if (!initialized) {
                initialized = true
                smoothed = input
                return input
            }
            val difference = AngleMath.angularDifference(input, smoothed)
            smoothed = AngleMath.normalizeTo360(smoothed + difference * alpha.coerceIn(0f, 1f))
            return smoothed
        }

        fun reset() {
            initialized = false
        }
    }
}
