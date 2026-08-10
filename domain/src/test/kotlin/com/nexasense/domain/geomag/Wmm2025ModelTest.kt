package com.nexasense.domain.geomag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the pure-Kotlin WMM2025 model against the official NOAA reference
 * values published with the model (WMM2025_TestValues.txt). The reference
 * declination/inclination are rounded to 0.01° and the components to nT
 * precision; the implementation reproduces the official C code to < 0.005°
 * and < 0.001 nT, so all tolerances here are dominated by the reference
 * rounding.
 */
class Wmm2025ModelTest {

    private data class TestPoint(
        val year: Double,
        val altitudeKm: Double,
        val latitude: Double,
        val longitude: Double,
        val declination: Double,
        val inclination: Double,
        val h: Double,
        val x: Double,
        val y: Double,
        val z: Double,
    )

    // A representative subset of the 100 official NOAA test points, spanning
    // the model lifetime (2025.0–2029.0), both hemispheres and altitudes.
    private val points = listOf(
        TestPoint(2025.0, 28.0, 89.00, -121.0, -99.77, 88.47, 1504.2981, -255.3887, -1482.4606, 56194.2888),
        TestPoint(2025.0, 48.0, 80.00, -96.0, -29.91, 87.77, 2164.2855, 1875.9823, -1079.2694, 55623.0441),
        TestPoint(2025.0, 54.0, 82.00, 87.0, 54.89, 87.68, 2302.4273, 1324.3369, 1883.4286, 56740.7721),
        TestPoint(2025.0, 65.0, 43.00, 93.0, 0.50, 64.10, 24300.7647, 24299.8528, 210.5171, 50037.9240),
        TestPoint(2025.0, 51.0, -33.00, 109.0, -5.49, -67.50, 21838.0465, 21737.7788, -2090.2741, -52710.0039),
        TestPoint(2025.0, 39.0, -59.00, -8.0, -15.75, -58.55, 14918.1158, 14358.0955, -4049.1075, -24389.0864),
        TestPoint(2025.0, 3.0, -50.00, -103.0, 27.96, -54.89, 22106.0414, 19526.5328, 10362.9910, -31437.5628),
        TestPoint(2025.0, 94.0, -29.00, -110.0, 15.74, -38.25, 24181.9901, 23275.4711, 6559.0465, -19063.6053),
        TestPoint(2025.5, 6.0, -36.00, -137.0, 20.28, -52.11, 25353.2307, 23781.9307, 8786.6989, -32577.5186),
        TestPoint(2025.5, 63.0, 26.00, 81.0, 0.51, 41.07, 34803.9801, 34802.6134, 308.4319, 30332.0570),
        TestPoint(2025.5, 69.0, 38.00, -144.0, 12.93, 56.97, 23096.3370, 22510.8045, 5167.6362, 35525.9903),
        TestPoint(2025.5, 50.0, -70.00, -133.0, 57.21, -71.94, 16656.7094, 9021.8478, 14001.8652, -51084.8383),
        TestPoint(2026.0, 74.0, -57.00, 3.0, -22.51, -58.65, 14362.2066, 13268.1196, -5498.1796, -23576.0629),
        TestPoint(2026.0, 46.0, -24.00, -122.0, 14.01, -34.17, 26638.5001, 25846.1187, 6448.8633, -18080.3994),
        TestPoint(2026.0, 69.0, 23.00, 63.0, 1.17, 35.92, 34565.8585, 34558.6053, 708.0788, 25043.4059),
        TestPoint(2026.0, 33.0, -3.00, -147.0, 9.71, -2.12, 30957.6661, 30514.0864, 5221.8407, -1146.9790),
        TestPoint(2027.0, 37.0, -66.00, -5.0, -17.22, -59.04, 17159.8365, 16390.7713, -5079.6266, -28608.2436),
        TestPoint(2028.0, 49.0, 20.00, 167.0, 5.10, 26.82, 30251.2625, 30131.4361, 2689.8767, 15295.6118),
        TestPoint(2029.0, 95.0, -60.00, -59.0, 8.58, -55.17, 18095.5989, 17893.0189, 2700.1059, -26011.8458),
    )

    @Test
    fun `matches NOAA reference declination and inclination`() {
        for (p in points) {
            val f = Wmm2025.magneticFieldAt(p.year, p.latitude, p.longitude, p.altitudeKm * 1000.0)
            assertEquals("declination at (${p.latitude}, ${p.longitude}, ${p.year})", p.declination, f.declinationDegrees, 0.02)
            assertEquals("inclination at (${p.latitude}, ${p.longitude}, ${p.year})", p.inclination, f.inclinationDegrees, 0.02)
        }
    }

    @Test
    fun `matches NOAA reference field components`() {
        for (p in points) {
            val f = Wmm2025.magneticFieldAt(p.year, p.latitude, p.longitude, p.altitudeKm * 1000.0)
            assertEquals("X at (${p.latitude}, ${p.longitude}, ${p.year})", p.x, f.xNanoTesla, 0.05)
            assertEquals("Y at (${p.latitude}, ${p.longitude}, ${p.year})", p.y, f.yNanoTesla, 0.05)
            assertEquals("Z at (${p.latitude}, ${p.longitude}, ${p.year})", p.z, f.zNanoTesla, 0.05)
            assertEquals("H at (${p.latitude}, ${p.longitude}, ${p.year})", p.h, f.horizontalIntensity, 0.05)
        }
    }

    @Test
    fun `total intensity is consistent with components`() {
        val f = Wmm2025.magneticFieldAt(2026.5, 31.0, 35.0, 0.0)
        val expected = Math.hypot(f.horizontalIntensity, f.zNanoTesla)
        assertEquals(expected, f.totalIntensity, 1e-6)
        assertTrue(f.totalIntensity > 40_000.0) // order-of-magnitude sanity (Israel, ~2026)
    }

    @Test
    fun `year is clamped to the model validity window`() {
        val at2025 = Wmm2025.declinationAt(2025.0, 30.0, 30.0, 0.0)
        val before = Wmm2025.declinationAt(2019.0, 30.0, 30.0, 0.0)
        assertEquals(at2025, before, 1e-9)

        val at2030 = Wmm2025.declinationAt(2030.0, 30.0, 30.0, 0.0)
        val after = Wmm2025.declinationAt(2035.0, 30.0, 30.0, 0.0)
        assertEquals(at2030, after, 1e-9)
    }

    @Test
    fun `longitude wrap does not change the result`() {
        val a = Wmm2025.declinationAt(2026.0, 40.0, 21.0, 100.0)
        val b = Wmm2025.declinationAt(2026.0, 40.0, 381.0, 100.0)
        assertEquals(a, b, 1e-9)
    }

    @Test
    fun `works at extreme latitudes without numeric issues`() {
        for (lat in listOf(-89.999, -90.0, 89.999, 90.0)) {
            val dec = Wmm2025.declinationAt(2027.0, lat, 45.0, 0.0)
            assertTrue("declination finite at lat=$lat", dec.isFinite())
        }
    }

    @Test
    fun `sea level altitude is valid`() {
        val f = Wmm2025.magneticFieldAt(2025.0, 52.52, 13.405, 0.0) // Berlin
        // Berlin 2025: declination ≈ 3.7° E; sanity range, not an exact value.
        assertTrue(Math.abs(f.declinationDegrees) < 10.0)
        assertTrue(f.horizontalIntensity > 10_000.0)
    }

    @Test
    fun `decimal year conversion maps known timestamps`() {
        // 2025-01-01T00:00:00Z and 2030-01-01T00:00:00Z
        val y2025 = Wmm2025.decimalYear(1_735_689_600_000L)
        val y2030 = Wmm2025.decimalYear(1_893_456_000_000L)
        assertEquals(2025.0, y2025, 0.001)
        assertEquals(2030.0, y2030, 0.001)
        // Mid-2026 lands near 2026.5
        assertEquals(2026.5, Wmm2025.decimalYear(1_782_864_000_000L), 0.01)
    }
}
