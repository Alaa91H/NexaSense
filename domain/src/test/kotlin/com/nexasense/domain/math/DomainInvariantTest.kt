package com.nexasense.domain.math

import com.nexasense.domain.engine.QiblaCalculator
import com.nexasense.domain.geomag.Wmm2025
import com.nexasense.domain.model.CardinalDirection
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Randomized (fixed-seed) invariant tests for the domain math. Every
 * property must hold for *any* input, so a pseudo-random sweep gives broad
 * edge-case coverage at near-zero cost — no external property-test library.
 */
class DomainInvariantTest {

    private val random = Random(20260810L)

    @Test
    fun `normalizeTo360 always returns in 0 to 360 and is idempotent`() {
        repeat(10_000) {
            val angle = random.nextFloat() * 10_000f - 5_000f
            val normalized = AngleMath.normalizeTo360(angle)
            assertTrue("normalized=$normalized", normalized >= 0f && normalized < 360f)
            assertEquals(normalized, AngleMath.normalizeTo360(normalized), 1e-4f)
        }
    }

    @Test
    fun `normalizeTo180 always returns in -180 to 180 and is idempotent`() {
        repeat(10_000) {
            val angle = random.nextFloat() * 10_000f - 5_000f
            val normalized = AngleMath.normalizeTo180(angle)
            assertTrue("normalized=$normalized", normalized > -180f && normalized <= 180f)
            assertEquals(normalized, AngleMath.normalizeTo180(normalized), 1e-4f)
        }
    }

    @Test
    fun `angular difference is bounded and symmetric`() {
        repeat(10_000) {
            val a = random.nextFloat() * 720f - 360f
            val b = random.nextFloat() * 720f - 360f
            val forward = AngleMath.angularDistance(a, b)
            val backward = AngleMath.angularDistance(b, a)
            assertTrue("distance=$forward", forward <= 180f)
            assertEquals(forward, backward, 1e-3f)
        }
    }

    @Test
    fun `lerp takes the short way around the circle`() {
        repeat(5_000) {
            val start = random.nextFloat() * 360f
            val end = random.nextFloat() * 360f
            val fraction = random.nextFloat()
            val result = AngleMath.lerpDegrees(start, end, fraction)
            // The interpolated point must lie on the short arc between start and end.
            val totalArc = AngleMath.angularDistance(start, end)
            val covered = AngleMath.angularDistance(start, result)
            assertTrue("start=$start end=$end frac=$fraction r=$result", covered <= totalArc + 1e-3f)
            assertEquals(start, AngleMath.lerpDegrees(start, end, 0f), 1e-3f)
            assertEquals(end, AngleMath.lerpDegrees(start, end, 1f), 1e-3f)
        }
    }

    @Test
    fun `cardinal direction is rotation invariant`() {
        repeat(5_000) {
            val angle = random.nextFloat() * 360f
            val expected = CardinalDirection.fromDegrees(angle)
            assertEquals(expected, CardinalDirection.fromDegrees(angle + 360f))
            assertEquals(expected, CardinalDirection.fromDegrees(angle - 360f))
        }
    }

    @Test
    fun `cardinal direction matches its 45 degree sector`() {
        CardinalDirection.entries.forEach { cardinal ->
            val center = cardinal.degrees.toFloat()
            repeat(1_000) {
                // Strictly inside the sector (boundaries excluded).
                val offset = random.nextFloat() * 44.9f - 22.45f
                val angle = ((center + offset) % 360f + 360f) % 360f
                assertEquals(cardinal, CardinalDirection.fromDegrees(angle))
            }
        }
    }

    @Test
    fun `angle smoother converges to a constant input and stays bounded`() {
        repeat(200) {
            val smoother = SmoothingFilters.AngleSmoother(0.25f)
            val target = random.nextFloat() * 360f
            var value = smoother.update(random.nextFloat() * 360f)
            repeat(300) {
                value = smoother.update(target)
                assertTrue("value=$value", value >= 0f && value < 360f)
            }
            assertTrue(
                "did not converge to $target (got $value)",
                abs(AngleMath.angularDifference(target, value)) < 1f,
            )
        }
    }

    @Test
    fun `angle smoother takes the short way across the 0-360 seam`() {
        val smoother = SmoothingFilters.AngleSmoother(0.5f)
        var value = smoother.update(359f)
        value = smoother.update(1f)
        // 359 -> 1 should move +2 degrees (not -358): short way, no needle spin.
        assertTrue(
            "value=$value",
            abs(AngleMath.angularDifference(1f, value)) < 2f,
        )
        // And back: 1 -> 359 moves -2 degrees.
        value = smoother.update(359f)
        assertTrue(
            "value=$value",
            abs(AngleMath.angularDifference(359f, value)) < 2f,
        )
    }

    @Test
    fun `wmm2025 field is smooth and physically plausible`() {
        repeat(2_000) {
            val lat = random.nextDouble() * 170.0 - 85.0
            val lon = random.nextDouble() * 360.0 - 180.0
            val field = Wmm2025.magneticFieldAt(2026.5, lat, lon, 0.0)
            // Earth's surface field: roughly 20–70 µT (nT units here).
            assertTrue(
                "intensity=${field.totalIntensity} at ($lat, $lon)",
                field.totalIntensity in 20_000.0..70_000.0,
            )
            assertTrue("declination=${field.declinationDegrees}", abs(field.declinationDegrees) <= 180.0)
            // Nearby points must have close declinations (smooth field) — but
            // only where declination is well-defined: near the magnetic pole
            // the horizontal component vanishes and declination legitimately
            // swings wildly.
            if (field.horizontalIntensity > 5_000.0) {
                val near = Wmm2025.declinationAt(2026.5, lat + 0.5, lon + 0.5, 0.0)
                val far = field.declinationDegrees
                assertTrue(
                    "declination jumps at ($lat, $lon): $far vs $near",
                    abs(AngleMath.normalizeTo180((near - far).toFloat())) < 15.0,
                )
            }
        }
    }

    @Test
    fun `geodesic distances are symmetric on the ellipsoid`() {
        repeat(2_000) {
            val latA = random.nextDouble() * 160.0 - 80.0
            val lonA = random.nextDouble() * 360.0 - 180.0
            val latB = random.nextDouble() * 160.0 - 80.0
            val lonB = random.nextDouble() * 360.0 - 180.0
            val ab = QiblaCalculator.geodesicDistance(latA, lonA, latB, lonB)
            val ba = QiblaCalculator.geodesicDistance(latB, lonB, latA, lonA)
            assertTrue(
                "A($latA,$lonA) B($latB,$lonB): ab=$ab ba=$ba",
                abs(ab - ba) < 1e-6,
            )
        }
    }

    @Test
    fun `geodesic bearings are exact on meridians and the equator`() {
        // Same meridian: due north / due south.
        assertEquals(0f, QiblaCalculator.geodesicBearing(30.0, 10.0, 50.0, 10.0), 1e-3f)
        assertEquals(180f, QiblaCalculator.geodesicBearing(50.0, 10.0, 30.0, 10.0), 1e-3f)
        // Same latitude: due east / due west.
        assertEquals(90f, QiblaCalculator.geodesicBearing(0.0, 10.0, 0.0, 20.0), 1e-3f)
        assertEquals(270f, QiblaCalculator.geodesicBearing(0.0, 20.0, 0.0, 10.0), 1e-3f)
        // Across the date line: the short way around.
        assertEquals(90f, QiblaCalculator.geodesicBearing(0.0, 179.0, 0.0, -179.0), 1e-3f)
        assertEquals(270f, QiblaCalculator.geodesicBearing(0.0, -179.0, 0.0, 179.0), 1e-3f)
    }
}
