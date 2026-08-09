package com.nexasense.domain.engine

import com.nexasense.domain.model.AccuracyLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MagneticFieldAnalyzerTest {

    private val high = AccuracyLevel.HIGH

    @Test
    fun `stable field is not interference`() {
        val analyzer = MagneticFieldAnalyzer()
        repeat(200) { analyzer.update(0f, 0f, 45f, high) }
        val state = analyzer.update(0f, 0f, 45f, high)
        assertEquals(45f, state!!.magnitudeMicroTesla, 0.5f)
        assertFalse(state.interference)
    }

    @Test
    fun `sudden spike is flagged as interference`() {
        val analyzer = MagneticFieldAnalyzer()
        repeat(200) { analyzer.update(0f, 0f, 45f, high) }
        val state = analyzer.update(0f, 0f, 80f, high)
        assertTrue(state!!.interference)
    }

    @Test
    fun `extreme magnitude violates the sanity band`() {
        val analyzer = MagneticFieldAnalyzer()
        repeat(50) { analyzer.update(0f, 0f, 45f, high) }
        val highState = analyzer.update(0f, 0f, 500f, high)
        assertTrue(highState!!.bandViolation)
        assertTrue(highState.interference)

        val lowState = analyzer.update(0f, 0f, 1f, high)
        assertTrue(lowState!!.bandViolation)
        assertTrue(lowState.interference)
    }

    @Test
    fun `adaptive threshold does not flag gradual drift`() {
        // A very slow drift stays under the adaptive threshold.
        val analyzer = MagneticFieldAnalyzer()
        var value = 40f
        repeat(300) {
            analyzer.update(0f, 0f, value, high)
            value += 0.02f
        }
        val state = analyzer.update(0f, 0f, value, high)
        assertFalse(state!!.interference)
    }

    @Test
    fun `NaN and infinity readings are ignored`() {
        val analyzer = MagneticFieldAnalyzer()
        assertNull(analyzer.update(Float.NaN, 0f, 45f, high))
        assertNull(analyzer.update(Float.POSITIVE_INFINITY, 0f, 45f, high))
        assertNull(analyzer.update(0f, 0f, Float.NEGATIVE_INFINITY, high))
        assertNull(analyzer.update(0f, 0f, 0f, high))
    }

    @Test
    fun `reset restarts the baseline`() {
        val analyzer = MagneticFieldAnalyzer()
        repeat(200) { analyzer.update(0f, 0f, 45f, high) }
        analyzer.reset()
        val state = analyzer.update(0f, 0f, 30f, high)
        // Baseline re-initialized to the first value after reset.
        assertEquals(30f, state!!.baselineMicroTesla, 0.5f)
        assertFalse(state.interference)
    }

    @Test
    fun `analyzer reports raw components`() {
        val analyzer = MagneticFieldAnalyzer()
        analyzer.update(10f, -20f, 30f, AccuracyLevel.LOW)
        val state = analyzer.update(10f, -20f, 30f, AccuracyLevel.MEDIUM)
        assertEquals(10f, state!!.x, 0f)
        assertEquals(-20f, state.y, 0f)
        assertEquals(30f, state.z, 0f)
        assertEquals(AccuracyLevel.MEDIUM, state.accuracy)
    }
}
