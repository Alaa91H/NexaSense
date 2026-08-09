package com.nexasense.domain.math

import kotlin.math.sqrt

/**
 * Pure math for magnetometer calibration.
 *
 * Hard-iron errors (constant offsets per axis, e.g. from nearby metal) are
 * estimated from the per-axis min/max of collected samples. Soft-iron errors
 * (direction-dependent gain) are estimated from the per-axis half-ranges and
 * corrected by rescaling each axis onto the largest one.
 *
 * This is the classic min/max ellipsoid fit — documented in
 * docs/calibration.md, including its limits.
 */
object MagneticCalibrationMath {

    /** Tracks the running min/max of collected samples. */
    class Sampler(
        private val minSamplesForCalibration: Int = 60,
        private val coverageThreshold: Float = 0.35f,
    ) {
        private var minX = Float.MAX_VALUE
        private var minY = Float.MAX_VALUE
        private var minZ = Float.MAX_VALUE
        private var maxX = -Float.MAX_VALUE
        private var maxY = -Float.MAX_VALUE
        private var maxZ = -Float.MAX_VALUE
        private var count = 0

        /** Returns true if the sample was accepted (finite and non-zero). */
        fun addSample(v: com.nexasense.domain.model.Vec3): Boolean {
            if (v.isInvalid) return false
            val m = v.magnitude
            if (m <= 0f) return false
            count++
            if (v.x < minX) minX = v.x
            if (v.y < minY) minY = v.y
            if (v.z < minZ) minZ = v.z
            if (v.x > maxX) maxX = v.x
            if (v.y > maxY) maxY = v.y
            if (v.z > maxZ) maxZ = v.z
            return true
        }

        val sampleCount: Int get() = count

        fun reset() {
            minX = Float.MAX_VALUE
            minY = Float.MAX_VALUE
            minZ = Float.MAX_VALUE
            maxX = -Float.MAX_VALUE
            maxY = -Float.MAX_VALUE
            maxZ = -Float.MAX_VALUE
            count = 0
        }

        fun build(): com.nexasense.domain.model.MagnetometerCalibration {
            if (count == 0) return com.nexasense.domain.model.MagnetometerCalibration.NONE

            val halfX = (maxX - minX) / 2f
            val halfY = (maxY - minY) / 2f
            val halfZ = (maxZ - minZ) / 2f
            val maxHalf = maxOf(halfX, halfY, halfZ)
            if (maxHalf <= 0f) return com.nexasense.domain.model.MagnetometerCalibration.NONE

            val scaleX = maxHalf / halfX
            val scaleY = maxHalf / halfY
            val scaleZ = maxHalf / halfZ

            val coverage = (minOf(halfX, halfY, halfZ) / maxHalf).coerceIn(0f, 1f)
            val isCalibrated = count >= minSamplesForCalibration && coverage >= coverageThreshold

            return com.nexasense.domain.model.MagnetometerCalibration(
                offsetX = (minX + maxX) / 2f,
                offsetY = (minY + maxY) / 2f,
                offsetZ = (minZ + maxZ) / 2f,
                scaleX = scaleX,
                scaleY = scaleY,
                scaleZ = scaleZ,
                sampleCount = count,
                coverage = coverage,
                isCalibrated = isCalibrated,
            )
        }
    }

    /** Applies the calibration to a raw reading. */
    fun apply(
        raw: com.nexasense.domain.model.Vec3,
        calibration: com.nexasense.domain.model.MagnetometerCalibration,
    ): com.nexasense.domain.model.Vec3 {
        if (!calibration.isCalibrated) return raw
        return com.nexasense.domain.model.Vec3(
            x = (raw.x - calibration.offsetX) * calibration.scaleX,
            y = (raw.y - calibration.offsetY) * calibration.scaleY,
            z = (raw.z - calibration.offsetZ) * calibration.scaleZ,
        )
    }

    /** RMS residual of the corrected samples around their mean magnitude (fit quality). */
    fun fitQuality(samples: List<com.nexasense.domain.model.Vec3>): Float {
        if (samples.isEmpty()) return 0f
        var sum = 0f
        var sumSq = 0f
        for (s in samples) {
            val m = s.magnitude
            sum += m
            sumSq += m * m
        }
        val mean = sum / samples.size
        val variance = (sumSq / samples.size) - mean * mean
        return if (variance > 0f) sqrt(variance) else 0f
    }
}
