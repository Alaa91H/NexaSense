package com.nexasense.domain.engine

import java.time.Instant
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reference values were computed with an independent Python implementation
 * of the NOAA Solar Calculator algorithm (verified against known events:
 * equinox sunrise due east, solstice elevations, and the sun-over-Kaaba
 * event where the solar azimuth equals the Qibla bearing).
 */
class SolarPositionCalculatorTest {

    private val latitude = 52.52 // Berlin
    private val longitude = 13.405

    private fun at(instant: String): Long = Instant.parse(instant).toEpochMilli()

    @Test
    fun `sun over Kaaba aligns with Qibla bearing from Berlin`() {
        // 2026-05-28 ~09:18 UTC: the sun transits the Kaaba; the shadow
        // points exactly away from Qibla — usable without any compass.
        val sun = SolarPositionCalculator.positionAt(latitude, longitude, at("2026-05-28T09:18:00Z"))
        val qibla = QiblaCalculator.geodesicBearing(latitude, longitude, 21.422487, 39.826206).toDouble()
        assertEquals(136.91, sun.azimuthDegrees, 1.5)
        assertTrue("azimuth=$sun qibla=$qibla", abs(sun.azimuthDegrees - qibla) < 1.5)
        assertEquals(52.8, sun.elevationDegrees, 1.0)
    }

    @Test
    fun `sun over Kaaba aligns with Qibla bearing from New York`() {
        val lat = 40.7128
        val lon = -74.006
        val sun = SolarPositionCalculator.positionAt(lat, lon, at("2026-05-28T09:18:00Z"))
        val qibla = QiblaCalculator.geodesicBearing(lat, lon, 21.422487, 39.826206).toDouble()
        assertEquals(58.63, sun.azimuthDegrees, 1.5)
        assertTrue("azimuth=$sun qibla=$qibla", abs(sun.azimuthDegrees - qibla) < 1.5)
    }

    @Test
    fun `july sun over Kaaba also aligns with Qibla from Berlin`() {
        val sun = SolarPositionCalculator.positionAt(latitude, longitude, at("2026-07-16T09:26:00Z"))
        val qibla = QiblaCalculator.geodesicBearing(latitude, longitude, 21.422487, 39.826206).toDouble()
        assertEquals(136.41, sun.azimuthDegrees, 1.5)
        assertTrue("azimuth=$sun qibla=$qibla", abs(sun.azimuthDegrees - qibla) < 1.5)
    }

    @Test
    fun `equinox sunrise is due east`() {
        val sun = SolarPositionCalculator.positionAt(0.0, 0.0, at("2026-03-21T06:00:00Z"))
        assertEquals(90.0, sun.azimuthDegrees, 3.0)
        assertTrue("elevation=${sun.elevationDegrees}", sun.elevationDegrees < 1.0)
    }

    @Test
    fun `june solstice noon at the equator has declination elevation and north azimuth`() {
        val sun = SolarPositionCalculator.positionAt(0.0, 0.0, at("2026-06-21T12:00:00Z"))
        // Solar declination ≈ 23.44° → zenith distance 23.44° → elevation ≈ 66.6°.
        assertEquals(66.5, sun.elevationDegrees, 1.0)
        // Sun is north of the equator in June.
        assertTrue("azimuth=${sun.azimuthDegrees}", sun.azimuthDegrees < 5.0 || sun.azimuthDegrees > 355.0)
    }

    @Test
    fun `december solstice noon at the equator has south azimuth`() {
        val sun = SolarPositionCalculator.positionAt(0.0, 0.0, at("2026-12-21T12:00:00Z"))
        assertEquals(66.5, sun.elevationDegrees, 1.0)
        assertEquals(180.0, sun.azimuthDegrees, 5.0)
    }

    @Test
    fun `sun elevation at the north pole on the june solstice equals declination`() {
        val sun = SolarPositionCalculator.positionAt(90.0, 0.0, at("2026-06-21T12:00:00Z"))
        // At the pole the sun circles at the declination altitude.
        assertEquals(23.4, sun.elevationDegrees, 0.6)
    }

    @Test
    fun `sun azimuth from random locations equals qibla at the kaaba transit`() {
        val random = kotlin.random.Random(20260810L)
        val instant = at("2026-05-28T09:18:00Z")
        repeat(500) {
            val lat = random.nextDouble() * 140.0 - 70.0
            val lon = random.nextDouble() * 360.0 - 180.0
            val sun = SolarPositionCalculator.positionAt(lat, lon, instant)
            if (sun.elevationDegrees < 0.0) return@repeat // sun below horizon — shadow unusable
            val qibla = QiblaCalculator.geodesicBearing(lat, lon, 21.422487, 39.826206).toDouble()
            val diff = abs(((sun.azimuthDegrees - qibla + 540.0) % 360.0) - 180.0)
            assertTrue("lat=$lat lon=$lon sun=${sun.azimuthDegrees} qibla=$qibla diff=$diff", diff < 2.0)
        }
    }
}
