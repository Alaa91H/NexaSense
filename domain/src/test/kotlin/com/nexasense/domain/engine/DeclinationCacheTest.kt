package com.nexasense.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeclinationCacheTest {

    @Test
    fun `first call computes and caches`() {
        val cache = DeclinationCache(maxAgeMillis = 60_000L, minMoveMeters = 1_000.0)
        var calls = 0
        val value = cache.declination(52.52, 13.405, 34.0, 1_000L, nowMillis = 2_000L) { _, _, _, _ ->
            calls++
            3.8f
        }
        assertEquals(3.8f, value!!, 1e-3f)
        assertEquals(1, calls)
    }

    @Test
    fun `fresh location and age reuse the cache`() {
        val cache = DeclinationCache(maxAgeMillis = 60_000L, minMoveMeters = 1_000.0)
        var calls = 0
        val compute: (Double, Double, Double, Long) -> Float? = { _, _, _, _ -> calls++; 3.8f }

        cache.declination(52.52, 13.405, 34.0, 1_000L, nowMillis = 2_000L, compute = compute)
        // Same location, within the max age -> no new model evaluation.
        val second = cache.declination(52.5205, 13.4052, 34.0, 1_100L, nowMillis = 3_000L, compute = compute)
        assertEquals(3.8f, second!!, 1e-3f)
        assertEquals(1, calls)
    }

    @Test
    fun `large movement triggers recomputation`() {
        val cache = DeclinationCache(maxAgeMillis = 60_000L, minMoveMeters = 1_000.0)
        var calls = 0
        val compute: (Double, Double, Double, Long) -> Float? = { _, _, _, _ -> calls++; 3.8f }

        cache.declination(52.52, 13.405, 34.0, 1_000L, nowMillis = 2_000L, compute = compute)
        // ~1000+ km away -> must recompute.
        val second = cache.declination(52.52, 13.405 + 15.0, 34.0, 1_100L, nowMillis = 3_000L, compute = compute)
        assertEquals(3.8f, second!!, 1e-3f)
        assertEquals(2, calls)
    }

    @Test
    fun `stale age triggers recomputation`() {
        val cache = DeclinationCache(maxAgeMillis = 60_000L, minMoveMeters = 1_000.0)
        var calls = 0
        val compute: (Double, Double, Double, Long) -> Float? = { _, _, _, _ -> calls++; 3.8f }

        cache.declination(52.52, 13.405, 34.0, 1_000L, nowMillis = 2_000L, compute = compute)
        // 10 minutes later, same spot -> recompute because the value is stale.
        val second = cache.declination(52.52, 13.405, 34.0, 1_000L, nowMillis = 2_000L + 61_000L, compute = compute)
        assertEquals(3.8f, second!!, 1e-3f)
        assertEquals(2, calls)
    }

    @Test
    fun `null computation result is cached as null`() {
        val cache = DeclinationCache(maxAgeMillis = 60_000L, minMoveMeters = 1_000.0)
        var calls = 0
        val compute: (Double, Double, Double, Long) -> Float? = { _, _, _, _ -> calls++; null }

        val first = cache.declination(52.52, 13.405, 34.0, 1_000L, nowMillis = 2_000L, compute = compute)
        assertNull(first)
        // A null result is not "fresh" (cachedValue == null), so it is retried.
        cache.declination(52.52, 13.405, 34.0, 1_000L, nowMillis = 3_000L, compute = compute)
        assertEquals(2, calls)
    }

    @Test
    fun `clear forces recomputation`() {
        val cache = DeclinationCache(maxAgeMillis = 60_000L, minMoveMeters = 1_000.0)
        var calls = 0
        val compute: (Double, Double, Double, Long) -> Float? = { _, _, _, _ -> calls++; 3.8f }

        cache.declination(52.52, 13.405, 34.0, 1_000L, nowMillis = 2_000L, compute = compute)
        cache.clear()
        cache.declination(52.52, 13.405, 34.0, 1_000L, nowMillis = 3_000L, compute = compute)
        assertEquals(2, calls)
    }
}
