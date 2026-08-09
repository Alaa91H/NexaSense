package com.nexasense.domain.math

import com.nexasense.domain.model.Vec3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmoothingFiltersTest {

    @Test
    fun `exponential smoothing converges to a step`() {
        val filter = SmoothingFilters.ExponentialSmoothing(alpha = 0.5f)
        filter.update(Vec3(0f, 0f, 0f))
        var value = 0f
        repeat(20) {
            value = filter.update(Vec3(10f, 0f, 0f)).x
        }
        assertEquals(10f, value, 0.01f)
    }

    @Test
    fun `exponential smoothing is monotonic towards the target`() {
        val filter = SmoothingFilters.ExponentialSmoothing(alpha = 0.25f)
        filter.update(Vec3(0f, 0f, 0f))
        var previous = 0f
        repeat(10) {
            val current = filter.update(Vec3(10f, 0f, 0f)).x
            assertTrue(current >= previous)
            previous = current
        }
    }

    @Test
    fun `adaptive filter responds to steady input`() {
        val filter = SmoothingFilters.AdaptiveFilter()
        filter.update(Vec3(10f, 0f, 0f))
        repeat(50) { filter.update(Vec3(10f, 0f, 0f)) }
        assertEquals(10f, filter.update(Vec3(10f, 0f, 0f)).x, 0.1f)
    }

    @Test
    fun `angle smoother crosses the wrap boundary the short way`() {
        val smoother = SmoothingFilters.AngleSmoother(alpha = 0.5f)
        smoother.update(355f)
        val after1 = smoother.update(5f)
        val after2 = smoother.update(5f)
        assertTrue("should move toward 0/360, was $after1", after1 < 10f || after1 > 350f)
        assertTrue("should move toward 0/360, was $after2", after2 < 10f || after2 > 350f)
    }

    @Test
    fun `angle smoother never jumps the long way`() {
        val smoother = SmoothingFilters.AngleSmoother(alpha = 0.2f)
        smoother.update(350f)
        var value = 350f
        repeat(30) {
            value = smoother.update(10f)
            // Must stay in the corridor between 350 and 10 through 0.
            assertTrue(
                "value $value left the wrap corridor",
                (value in 340f..360f) || (value in 0f..20f),
            )
        }
        // Converged near 10 having crossed 0, not having gone 350 -> 10 backwards.
        assertEquals(10f, value, 5f)
    }

    @Test
    fun `angle smoother with alpha 1 passes input through`() {
        val smoother = SmoothingFilters.AngleSmoother(alpha = 1f)
        smoother.update(100f)
        assertEquals(270f, smoother.update(270f), 0f)
    }

    @Test
    fun `filters handle NaN inputs gracefully`() {
        val filter = SmoothingFilters.ExponentialSmoothing()
        val result = filter.update(Vec3(Float.NaN, 0f, 0f))
        assertTrue(result.isInvalid)
        // Subsequent valid input still works.
        val valid = filter.update(Vec3(1f, 0f, 0f))
        assertTrue(valid.x.isFinite())
    }
}
