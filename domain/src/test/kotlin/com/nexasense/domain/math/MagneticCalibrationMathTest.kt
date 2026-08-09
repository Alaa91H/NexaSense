package com.nexasense.domain.math

import com.nexasense.domain.model.MagnetometerCalibration
import com.nexasense.domain.model.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MagneticCalibrationMathTest {

    private val eps = 1f

    /** Generates points on a sphere of [radius] centered at [center]. */
    private fun sphereSamples(
        center: Vec3,
        radius: Float,
        count: Int,
    ): List<Vec3> {
        val samples = mutableListOf<Vec3>()
        for (i in 0 until count) {
            val u = i.toFloat() / count
            // Low-discrepancy pair of angles so the full sphere is covered.
            val theta = 2f * PI.toFloat() * ((i * 0.61803398875f) % 1f)
            val phi = 2f * PI.toFloat() * ((i * 0.7548776662f) % 1f)
            samples += Vec3(
                x = center.x + radius * cos(phi) * cos(theta),
                y = center.y + radius * cos(phi) * sin(theta),
                z = center.z + radius * sin(phi),
            )
        }
        return samples
    }

    @Test
    fun `hard iron offsets are recovered from min-max`() {
        val center = Vec3(10f, -20f, 5f)
        val radius = 30f
        val sampler = MagneticCalibrationMath.Sampler()
        sphereSamples(center, radius, 300).forEach { sampler.addSample(it) }

        val calibration = sampler.build()
        assertEquals(10f, calibration.offsetX, 2f)
        assertEquals(-20f, calibration.offsetY, 2f)
        assertEquals(5f, calibration.offsetZ, 2f)
        assertEquals(1f, calibration.scaleX, 0.1f)
        assertEquals(1f, calibration.scaleY, 0.1f)
        assertEquals(1f, calibration.scaleZ, 0.1f)
        assertTrue(calibration.isCalibrated)
        assertEquals(1f, calibration.coverage, 0.05f)
    }

    @Test
    fun `applying calibration re-centers the samples`() {
        val center = Vec3(10f, -20f, 5f)
        val radius = 30f
        val sampler = MagneticCalibrationMath.Sampler()
        val samples = sphereSamples(center, radius, 300)
        samples.forEach { sampler.addSample(it) }
        val calibration = sampler.build()

        for (sample in samples.take(50)) {
            val corrected = MagneticCalibrationMath.apply(sample, calibration)
            assertEquals(radius, corrected.magnitude, 2f)
        }
    }

    @Test
    fun `soft iron scaling normalizes anisotropic ranges`() {
        // Stretched ellipsoid: x range 20, y range 60, z range 40.
        val sampler = MagneticCalibrationMath.Sampler()
        for (i in 0 until 300) {
            val u = i.toFloat() / 300
            val theta = 2f * PI.toFloat() * u
            sampler.addSample(Vec3(10f * cos(theta), 30f * sin(theta), 20f * sin(2f * theta)))
        }
        val calibration = sampler.build()
        assertEquals(3f, calibration.scaleX, 0.05f)
        assertEquals(1f, calibration.scaleY, 0.05f)
        assertEquals(1.5f, calibration.scaleZ, 0.05f)
    }

    @Test
    fun `insufficient samples are not calibrated`() {
        val sampler = MagneticCalibrationMath.Sampler(minSamplesForCalibration = 60)
        sphereSamples(Vec3(0f, 0f, 0f), 30f, 10).forEach { sampler.addSample(it) }
        assertFalse(sampler.build().isCalibrated)
    }

    @Test
    fun `poor coverage is not calibrated`() {
        val sampler = MagneticCalibrationMath.Sampler(coverageThreshold = 0.35f)
        // Samples only along the x axis -> poor coverage.
        for (i in 0 until 100) {
            sampler.addSample(Vec3(-30f + i * 0.6f, 0f, 0f))
        }
        val calibration = sampler.build()
        assertFalse(calibration.isCalibrated)
        assertTrue(calibration.coverage < 0.35f)
    }

    @Test
    fun `empty sampler returns NONE`() {
        val calibration = MagneticCalibrationMath.Sampler().build()
        assertEquals(MagnetometerCalibration.NONE, calibration)
    }

    @Test
    fun `invalid samples are rejected`() {
        val sampler = MagneticCalibrationMath.Sampler()
        assertFalse(sampler.addSample(Vec3(Float.NaN, 0f, 0f)))
        assertFalse(sampler.addSample(Vec3(0f, Float.POSITIVE_INFINITY, 0f)))
        assertFalse(sampler.addSample(Vec3(0f, 0f, 0f)))
        assertEquals(0, sampler.sampleCount)
    }

    @Test
    fun `reset clears collected samples`() {
        val sampler = MagneticCalibrationMath.Sampler()
        sphereSamples(Vec3(0f, 0f, 0f), 30f, 100).forEach { sampler.addSample(it) }
        sampler.reset()
        assertEquals(0, sampler.sampleCount)
        assertEquals(MagnetometerCalibration.NONE, sampler.build())
    }

    @Test
    fun `fit quality is low for a well-centered sphere`() {
        val samples = sphereSamples(Vec3(0f, 0f, 0f), 30f, 200)
        assertEquals(0f, MagneticCalibrationMath.fitQuality(samples), 2f)
    }
}
