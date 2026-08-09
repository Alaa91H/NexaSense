package com.nexasense.domain.engine

import com.nexasense.domain.model.AccuracyLevel
import com.nexasense.domain.model.MagneticFieldState
import com.nexasense.domain.model.Vec3
import kotlin.math.abs

/**
 * Analyzes magnetometer readings for interference.
 *
 * The approach is deliberately adaptive rather than a single worldwide
 * threshold: a rolling baseline tracks the local field strength and a rolling
 * deviation tracks its stability. A reading is flagged when either
 * - its magnitude deviates from the baseline by more than
 *   `jitterMultiplier × deviation` (turbulence / nearby magnetic objects), or
 * - its magnitude leaves the [sanityMinMicroTesla, sanityMaxMicroTesla] band
 *   (clearly anomalous for the Earth's field anywhere on the planet).
 *
 * All parameters are configurable; NaN/infinite readings are ignored.
 */
class MagneticFieldAnalyzer(
    private val config: Config = Config(),
) {

    data class Config(
        /** EWMA weight for the baseline magnitude. */
        val baselineAlpha: Float = 0.06f,
        /** EWMA weight for the deviation estimate. */
        val deviationAlpha: Float = 0.1f,
        /** Deviation multiplier for the adaptive jitter threshold. */
        val jitterMultiplier: Float = 4f,
        /** Floor of the adaptive threshold in µT. */
        val minJitterThreshold: Float = 1.2f,
        /** Absolute sanity band: below this magnitude the reading is anomalous. */
        val sanityMinMicroTesla: Float = 5f,
        /** Absolute sanity band: above this magnitude the reading is anomalous. */
        val sanityMaxMicroTesla: Float = 120f,
        /** Readings closer than this to a previous reading are still analyzed. */
        val minMagnitude: Float = 0.01f,
    )

    private var baseline = 0f
    private var deviation = 0f
    private var initialized = false
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    /**
     * Feeds one reading and returns the analysis. Returns null for invalid
     * (NaN/infinite/zero) readings.
     */
    fun update(x: Float, y: Float, z: Float, accuracy: AccuracyLevel): MagneticFieldState? {
        if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return null
        val magnitude = Vec3(x, y, z).magnitude
        if (magnitude <= config.minMagnitude) return null

        if (!initialized) {
            initialized = true
            baseline = magnitude
            deviation = 0f
        } else {
            baseline += config.baselineAlpha * (magnitude - baseline)
            deviation += config.deviationAlpha * (abs(magnitude - baseline) - deviation)
        }
        lastX = x
        lastY = y
        lastZ = z

        val adaptiveThreshold = maxOf(config.minJitterThreshold, deviation * config.jitterMultiplier)
        val jitterInterference = abs(magnitude - baseline) > adaptiveThreshold
        val bandViolation = magnitude < config.sanityMinMicroTesla || magnitude > config.sanityMaxMicroTesla

        return MagneticFieldState(
            x = x,
            y = y,
            z = z,
            magnitudeMicroTesla = magnitude,
            accuracy = accuracy,
            interference = jitterInterference || bandViolation,
            bandViolation = bandViolation,
            baselineMicroTesla = baseline,
        )
    }

    fun reset() {
        initialized = false
        baseline = 0f
        deviation = 0f
    }
}
