package com.nexasense.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SamplingRateEstimatorTest {

    @Test
    fun `constant 50ms interval measures about 20 Hz`() {
        val estimator = SamplingRateEstimator()
        var timestamp = 0L
        repeat(20) {
            estimator.update(timestamp)
            timestamp += 50_000_000L
        }
        val stats = estimator.current(requestedDelayMicros = 20_000L)
        assertEquals(20f, stats.actualHz, 3f)
        assertEquals(20_000L, stats.requestedDelayMicros)
        assertEquals(20, stats.sampleCount)
    }

    @Test
    fun `10ms interval measures about 100 Hz`() {
        val estimator = SamplingRateEstimator()
        var timestamp = 0L
        repeat(30) {
            estimator.update(timestamp)
            timestamp += 10_000_000L
        }
        assertEquals(100f, estimator.current().actualHz, 10f)
    }

    @Test
    fun `gaps reset the estimate instead of skewing it`() {
        val estimator = SamplingRateEstimator()
        var timestamp = 0L
        repeat(10) {
            estimator.update(timestamp)
            timestamp += 50_000_000L
        }
        // 10 second gap: stream paused, estimate must not average the gap in.
        estimator.update(timestamp + 10_000_000_000L)
        val stats = estimator.update(timestamp + 10_000_000_000L + 50_000_000L)
        assertTrue(stats.actualHz > 0f)
        assertEquals(20f, stats.actualHz, 5f)
    }

    @Test
    fun `non-increasing timestamps are ignored`() {
        val estimator = SamplingRateEstimator()
        estimator.update(1_000L)
        estimator.update(1_000L) // same timestamp
        estimator.update(500L) // going backwards
        val stats = estimator.current()
        assertEquals(0f, stats.actualHz, 0f)
        assertTrue(stats.sampleCount >= 3)
    }

    @Test
    fun `negative timestamps do not crash`() {
        val estimator = SamplingRateEstimator()
        estimator.update(-1L)
        estimator.update(100L)
        assertTrue(estimator.current().sampleCount >= 1)
    }

    @Test
    fun `reset clears the state`() {
        val estimator = SamplingRateEstimator()
        var timestamp = 0L
        repeat(10) {
            estimator.update(timestamp)
            timestamp += 50_000_000L
        }
        estimator.reset()
        assertEquals(0, estimator.current().sampleCount)
        assertEquals(0f, estimator.current().actualHz, 0f)
    }
}
