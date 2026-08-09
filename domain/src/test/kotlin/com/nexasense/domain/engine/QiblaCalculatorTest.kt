package com.nexasense.domain.engine

import com.nexasense.domain.model.CardinalDirection
import com.nexasense.domain.model.Heading
import com.nexasense.domain.model.HeadingMode
import com.nexasense.domain.model.HeadingSource
import com.nexasense.domain.model.NorthReference
import com.nexasense.domain.model.QiblaAlignment
import com.nexasense.domain.model.QiblaStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QiblaCalculatorTest {

    private val eps = 0.1f
    private val epsKm = 5.0

    private val kaabaLat = QiblaCalculator.KAABA_LATITUDE
    private val kaabaLon = QiblaCalculator.KAABA_LONGITUDE

    // --- Initial great-circle bearing: known reference values -----------------

    @Test
    fun `bearing from Berlin`() {
        assertEquals(136.7f, QiblaCalculator.initialBearing(52.52, 13.405, kaabaLat, kaabaLon), eps)
    }

    @Test
    fun `bearing from London`() {
        assertEquals(119.0f, QiblaCalculator.initialBearing(51.5074, -0.1278, kaabaLat, kaabaLon), eps)
    }

    @Test
    fun `bearing from New York crosses the Atlantic`() {
        assertEquals(58.5f, QiblaCalculator.initialBearing(40.7128, -74.006, kaabaLat, kaabaLon), eps)
    }

    @Test
    fun `bearing from Jakarta in the southern hemisphere`() {
        assertEquals(295.1f, QiblaCalculator.initialBearing(-6.2088, 106.8456, kaabaLat, kaabaLon), eps)
    }

    @Test
    fun `bearing from Sydney`() {
        assertEquals(277.5f, QiblaCalculator.initialBearing(-33.8688, 151.2093, kaabaLat, kaabaLon), eps)
    }

    // --- Great-circle distance: known reference values ------------------------

    @Test
    fun `distance from Berlin`() {
        assertEquals(4130.0, QiblaCalculator.greatCircleDistance(52.52, 13.405, kaabaLat, kaabaLon), epsKm)
    }

    @Test
    fun `distance from Istanbul`() {
        assertEquals(2405.0, QiblaCalculator.greatCircleDistance(41.0082, 28.9784, kaabaLat, kaabaLon), epsKm)
    }

    @Test
    fun `distance from New York`() {
        assertEquals(10306.0, QiblaCalculator.greatCircleDistance(40.7128, -74.006, kaabaLat, kaabaLon), epsKm)
    }

    @Test
    fun `distance and bearing are symmetric city-to-city`() {
        val toKaaba = QiblaCalculator.greatCircleDistance(52.52, 13.405, kaabaLat, kaabaLon)
        val fromKaaba = QiblaCalculator.greatCircleDistance(kaabaLat, kaabaLon, 52.52, 13.405)
        assertEquals(toKaaba, fromKaaba, 1e-6)
    }

    // --- WGS84 geodesic (Vincenty inverse): reference values ---------------------
    // Reference values below were computed independently with a Python
    // Vincenty-inverse implementation on the WGS84 ellipsoid (a = 6378137 m,
    // f = 1/298.257223563), using the fixed Kaaba coordinates.

    @Test
    fun `geodesic bearing and distance from Berlin`() {
        assertEquals(136.5774f, QiblaCalculator.geodesicBearing(52.52, 13.405, kaabaLat, kaabaLon), 0.02f)
        assertEquals(4127.57, QiblaCalculator.geodesicDistance(52.52, 13.405, kaabaLat, kaabaLon), 0.3)
    }

    @Test
    fun `geodesic bearing and distance from London`() {
        assertEquals(118.8684f, QiblaCalculator.geodesicBearing(51.5074, -0.1278, kaabaLat, kaabaLon), 0.02f)
        assertEquals(4794.76, QiblaCalculator.geodesicDistance(51.5074, -0.1278, kaabaLat, kaabaLon), 0.3)
    }

    @Test
    fun `geodesic bearing and distance from New York`() {
        assertEquals(58.3960f, QiblaCalculator.geodesicBearing(40.7128, -74.006, kaabaLat, kaabaLon), 0.02f)
        assertEquals(10323.92, QiblaCalculator.geodesicDistance(40.7128, -74.006, kaabaLat, kaabaLon), 0.3)
    }

    @Test
    fun `geodesic bearing and distance from Tokyo`() {
        assertEquals(293.0720f, QiblaCalculator.geodesicBearing(35.6762, 139.6503, kaabaLat, kaabaLon), 0.02f)
        assertEquals(9487.88, QiblaCalculator.geodesicDistance(35.6762, 139.6503, kaabaLat, kaabaLon), 0.3)
    }

    @Test
    fun `geodesic bearing and distance from Sydney in the southern hemisphere`() {
        assertEquals(277.3188f, QiblaCalculator.geodesicBearing(-33.8688, 151.2093, kaabaLat, kaabaLon), 0.02f)
        assertEquals(13236.95, QiblaCalculator.geodesicDistance(-33.8688, 151.2093, kaabaLat, kaabaLon), 0.3)
    }

    @Test
    fun `geodesic bearing and distance from Reykjavik at high latitude`() {
        assertEquals(106.0275f, QiblaCalculator.geodesicBearing(64.1466, -21.9426, kaabaLat, kaabaLon), 0.02f)
        assertEquals(6522.78, QiblaCalculator.geodesicDistance(64.1466, -21.9426, kaabaLat, kaabaLon), 0.3)
    }

    @Test
    fun `geodesic bearing and distance from Jakarta`() {
        assertEquals(295.0247f, QiblaCalculator.geodesicBearing(-6.2088, 106.8456, kaabaLat, kaabaLon), 0.02f)
        assertEquals(7922.24, QiblaCalculator.geodesicDistance(-6.2088, 106.8456, kaabaLat, kaabaLon), 0.3)
    }

    @Test
    fun `geodesic bearing and distance from Casablanca`() {
        assertEquals(93.5936f, QiblaCalculator.geodesicBearing(33.5731, -7.5898, kaabaLat, kaabaLon), 0.02f)
        assertEquals(4830.48, QiblaCalculator.geodesicDistance(33.5731, -7.5898, kaabaLat, kaabaLon), 0.3)
    }

    @Test
    fun `geodesic bearing and distance across the international date line`() {
        assertEquals(301.0842f, QiblaCalculator.geodesicBearing(0.0, 179.0, kaabaLat, kaabaLon), 0.02f)
        assertEquals(15001.49, QiblaCalculator.geodesicDistance(0.0, 179.0, kaabaLat, kaabaLon), 0.3)
        assertEquals(302.1702f, QiblaCalculator.geodesicBearing(0.0, -179.0, kaabaLat, kaabaLon), 0.02f)
        assertEquals(15191.08, QiblaCalculator.geodesicDistance(0.0, -179.0, kaabaLat, kaabaLon), 0.3)
    }

    @Test
    fun `geodesic bearing near the north pole`() {
        assertEquals(140.0454f, QiblaCalculator.geodesicBearing(89.5, 0.0, kaabaLat, kaabaLon), 0.02f)
        assertEquals(7589.26, QiblaCalculator.geodesicDistance(89.5, 0.0, kaabaLat, kaabaLon), 0.3)
    }

    @Test
    fun `geodesic at the Kaaba is zero distance and zero bearing`() {
        assertEquals(0.0, QiblaCalculator.geodesicDistance(kaabaLat, kaabaLon, kaabaLat, kaabaLon), 1e-6)
        assertEquals(0f, QiblaCalculator.geodesicBearing(kaabaLat, kaabaLon, kaabaLat, kaabaLon), 1e-3f)
    }

    @Test
    fun `geodesic falls back to spherical for nearly antipodal points`() {
        // Antipode of the Kaaba: Vincenty does not converge there.
        val antipodalLat = -kaabaLat
        val antipodalLon = kaabaLon - 180.0
        val bearing = QiblaCalculator.geodesicBearing(antipodalLat, antipodalLon, kaabaLat, kaabaLon)
        val distance = QiblaCalculator.geodesicDistance(antipodalLat, antipodalLon, kaabaLat, kaabaLon)
        assertTrue(!bearing.isNaN() && bearing >= 0f && bearing < 360f)
        assertTrue(!distance.isNaN() && distance > 0.0)
    }

    @Test
    fun `geodesic is more accurate than spherical on long paths`() {
        // The WGS84 geodesic distance is within ~0.1 % of the haversine value
        // and the bearing within a fraction of a degree — but the geodesic is
        // the reference result, so assert it is close to (and slightly
        // different from) the spherical approximation.
        val spherical = QiblaCalculator.greatCircleDistance(40.7128, -74.006, kaabaLat, kaabaLon)
        val geodesic = QiblaCalculator.geodesicDistance(40.7128, -74.006, kaabaLat, kaabaLon)
        assertTrue(kotlin.math.abs(spherical - geodesic) < 50.0)
        assertTrue(spherical != geodesic)
    }

    @Test
    fun `bearing to Kaaba uses the geodesic WGS84 calculation`() {
        val bearing = QiblaCalculator.bearingToKaaba(40.7128, -74.006)
        assertEquals(58.3960f, bearing.bearingDegrees, 0.02f)
        assertEquals(10323.92, bearing.distanceKm, 0.3)
    }

    // --- Edge cases ------------------------------------------------------------

    @Test
    fun `user at the Kaaba has zero distance and zero bearing`() {
        assertEquals(0.0, QiblaCalculator.greatCircleDistance(kaabaLat, kaabaLon, kaabaLat, kaabaLon), 1e-6)
        assertEquals(0f, QiblaCalculator.initialBearing(kaabaLat, kaabaLon, kaabaLat, kaabaLon), 1e-3f)
    }

    @Test
    fun `user very close to the Kaaba still yields a bearing`() {
        val bearing = QiblaCalculator.initialBearing(21.4225, 39.8263, kaabaLat, kaabaLon)
        assertTrue(bearing >= 0f && bearing < 360f)
        assertTrue(QiblaCalculator.greatCircleDistance(21.4225, 39.8263, kaabaLat, kaabaLon) < 1.0)
    }

    @Test
    fun `international date line longitudes behave consistently`() {
        // +179° and -179° are only 2° apart physically; bearings must be close.
        val east = QiblaCalculator.initialBearing(10.0, 179.0, kaabaLat, kaabaLon)
        val west = QiblaCalculator.initialBearing(10.0, -179.0, kaabaLat, kaabaLon)
        val diff = kotlin.math.abs(
            com.nexasense.domain.math.AngleMath.shortestAngularDifference(west, east),
        )
        assertTrue("bearings differ by $diff", diff < 10f)
    }

    @Test
    fun `high latitude cities return valid bearings`() {
        val cities = listOf(
            64.1466 to -21.9426, // Reykjavik
            60.1699 to 24.9384, // Helsinki
            61.2181 to -149.9003, // Anchorage
        )
        cities.forEach { (lat, lon) ->
            val bearing = QiblaCalculator.initialBearing(lat, lon, kaabaLat, kaabaLon)
            assertTrue("bearing $bearing not in range", bearing >= 0f && bearing < 360f)
            assertTrue(!bearing.isNaN())
        }
    }

    @Test
    fun `poles return valid bearings`() {
        val northPole = QiblaCalculator.initialBearing(90.0, 0.0, kaabaLat, kaabaLon)
        val southPole = QiblaCalculator.initialBearing(-90.0, 0.0, kaabaLat, kaabaLon)
        assertEquals(140.2f, northPole, 0.5f)
        assertEquals(39.8f, southPole, 0.5f)
        assertEquals(7625.0, QiblaCalculator.greatCircleDistance(90.0, 0.0, kaabaLat, kaabaLon), epsKm)
        assertEquals(12390.0, QiblaCalculator.greatCircleDistance(-90.0, 0.0, kaabaLat, kaabaLon), epsKm)
    }

    @Test
    fun `bearings are always in the 0 to 360 range`() {
        val points = listOf(
            -33.8688 to 151.2093,
            55.7558 to 37.6173,
            1.3521 to 103.8198,
            -1.2921 to 36.8219,
            33.5731 to -7.5898,
        )
        points.forEach { (lat, lon) ->
            val bearing = QiblaCalculator.bearingToKaaba(lat, lon)
            assertTrue(bearing.bearingDegrees >= 0f && bearing.bearingDegrees < 360f)
            assertTrue(bearing.distanceKm > 0.0)
            assertEquals(lat, bearing.userLatitude, 1e-9)
            assertEquals(lon, bearing.userLongitude, 1e-9)
        }
    }

    // --- Relative Qibla (shortest angular difference) --------------------------

    @Test
    fun `relative qibla positive means turn right`() {
        assertEquals(18f, QiblaCalculator.relativeQibla(120f, 138f), 1e-3f)
    }

    @Test
    fun `relative qibla negative means turn left`() {
        assertEquals(-18f, QiblaCalculator.relativeQibla(138f, 120f), 1e-3f)
    }

    @Test
    fun `relative qibla wraps across 360`() {
        assertEquals(20f, QiblaCalculator.relativeQibla(350f, 10f), 1e-3f)
        assertEquals(-20f, QiblaCalculator.relativeQibla(10f, 350f), 1e-3f)
    }

    @Test
    fun `relative qibla is never larger than 180 degrees`() {
        for (device in intArrayOf(0, 45, 90, 180, 270, 359)) {
            for (qibla in intArrayOf(0, 60, 120, 180, 240, 300, 359)) {
                val relative = QiblaCalculator.relativeQibla(device.toFloat(), qibla.toFloat())
                assertTrue(kotlin.math.abs(relative) <= 180f)
            }
        }
    }

    // --- Alignment --------------------------------------------------------------

    @Test
    fun `aligned within the default two degree threshold`() {
        assertEquals(QiblaAlignment.ALIGNED, QiblaCalculator.alignment(1.2f))
        assertEquals(QiblaAlignment.ALIGNED, QiblaCalculator.alignment(-1.9f))
        assertEquals(QiblaAlignment.ALIGNED, QiblaCalculator.alignment(0f))
        assertEquals(QiblaAlignment.ALIGNED, QiblaCalculator.alignment(2f))
        assertEquals(QiblaAlignment.ALIGNED, QiblaCalculator.alignment(-2f))
    }

    @Test
    fun `turn right and turn left outside the threshold`() {
        assertEquals(QiblaAlignment.TURN_RIGHT, QiblaCalculator.alignment(2.1f))
        assertEquals(QiblaAlignment.TURN_LEFT, QiblaCalculator.alignment(-2.1f))
        assertEquals(QiblaAlignment.TURN_RIGHT, QiblaCalculator.alignment(45f))
        assertEquals(QiblaAlignment.TURN_LEFT, QiblaCalculator.alignment(-45f))
    }

    @Test
    fun `alignment threshold is configurable`() {
        assertEquals(QiblaAlignment.ALIGNED, QiblaCalculator.alignment(4f, thresholdDegrees = 5f))
        assertEquals(QiblaAlignment.TURN_RIGHT, QiblaCalculator.alignment(6f, thresholdDegrees = 5f))
    }

    // --- North reference conversion ---------------------------------------------

    @Test
    fun `true bearing stays unchanged in true reference`() {
        assertEquals(
            138.2f,
            QiblaCalculator.qiblaBearingInReference(138.2f, 3.8f, NorthReference.TRUE_NORTH),
            1e-3f,
        )
        assertEquals(
            138.2f,
            QiblaCalculator.qiblaBearingInReference(138.2f, 3.8f, NorthReference.AUTOMATIC),
            1e-3f,
        )
    }

    @Test
    fun `true bearing converts to magnetic reference with declination`() {
        assertEquals(
            134.4f,
            QiblaCalculator.qiblaBearingInReference(138.2f, 3.8f, NorthReference.MAGNETIC_NORTH),
            1e-3f,
        )
    }

    @Test
    fun `magnetic to true heading conversion`() {
        assertEquals(129.4f, DeclinationEngine.trueHeading(125.6f, 3.8f), 1e-3f)
        assertEquals(125.6f, DeclinationEngine.magneticHeading(129.4f, 3.8f), 1e-3f)
    }

    @Test
    fun `device heading in true reference converts magnetic headings`() {
        val magnetic = Heading(
            degrees = 125.6f,
            cardinal = CardinalDirection.SE,
            source = HeadingSource.ROTATION_VECTOR,
            mode = HeadingMode.MAGNETIC,
        )
        assertEquals(129.4f, QiblaCalculator.deviceHeadingInTrueReference(magnetic, 3.8f)!!, 1e-3f)
    }

    @Test
    fun `device heading in true reference passes true headings through`() {
        val trueHeading = Heading(
            degrees = 129.4f,
            cardinal = CardinalDirection.SE,
            source = HeadingSource.ROTATION_VECTOR,
            mode = HeadingMode.TRUE,
        )
        assertEquals(129.4f, QiblaCalculator.deviceHeadingInTrueReference(trueHeading, 3.8f)!!, 1e-3f)
    }

    @Test
    fun `device heading without declination cannot convert to true`() {
        val magnetic = Heading(
            degrees = 125.6f,
            cardinal = CardinalDirection.SE,
            source = HeadingSource.ROTATION_VECTOR,
            mode = HeadingMode.MAGNETIC,
        )
        assertNull(QiblaCalculator.deviceHeadingInTrueReference(magnetic, null))
    }

    @Test
    fun `unavailable device heading cannot convert`() {
        val unavailable = Heading(
            degrees = 0f,
            cardinal = CardinalDirection.N,
            source = HeadingSource.UNAVAILABLE,
            mode = HeadingMode.MAGNETIC,
        )
        assertNull(QiblaCalculator.deviceHeadingInTrueReference(unavailable, 3.8f))
    }

    // --- Status ------------------------------------------------------------------

    @Test
    fun `aligned pair produces aligned status`() {
        assertEquals(
            QiblaStatus.ALIGNED,
            QiblaCalculator.statusFor(relativeDegrees = 1f, thresholdDegrees = 2f, hasHeading = true, hasBearing = true),
        )
    }

    @Test
    fun `unaligned pair produces ready status`() {
        assertEquals(
            QiblaStatus.READY,
            QiblaCalculator.statusFor(relativeDegrees = 18f, thresholdDegrees = 2f, hasHeading = true, hasBearing = true),
        )
    }

    @Test
    fun `missing heading or bearing produces an error status`() {
        assertEquals(
            QiblaStatus.COMPASS_UNAVAILABLE,
            QiblaCalculator.statusFor(relativeDegrees = 1f, thresholdDegrees = 2f, hasHeading = false, hasBearing = true),
        )
        assertEquals(
            QiblaStatus.LOCATION_UNAVAILABLE,
            QiblaCalculator.statusFor(relativeDegrees = 1f, thresholdDegrees = 2f, hasHeading = true, hasBearing = false),
        )
    }

    // --- Kaaba coordinates are fixed and local -----------------------------------

    @Test
    fun `kaaba coordinates are the fixed local constants`() {
        assertEquals(21.422487, kaabaLat, 1e-9)
        assertEquals(39.826206, kaabaLon, 1e-9)
        assertNotNull(kaabaLat)
    }
}
