package com.nexasense.domain.engine

import com.nexasense.domain.math.AngleMath
import com.nexasense.domain.model.Heading
import com.nexasense.domain.model.HeadingMode
import com.nexasense.domain.model.NorthReference
import com.nexasense.domain.model.QiblaAlignment
import com.nexasense.domain.model.QiblaBearing
import com.nexasense.domain.model.QiblaStatus
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Pure Qibla math — no Android, no location services, fully unit-testable.
 *
 * The bearing and distance are computed locally from the fixed Kaaba
 * coordinates; nothing is ever sent to a server.
 *
 * Two accuracy tiers are provided:
 *  - Spherical great-circle ([initialBearing], [greatCircleDistance]) — fast,
 *    closed-form formulas using the mean Earth radius; accurate to ~0.5 %.
 *  - WGS84 ellipsoidal geodesic ([geodesicBearing], [geodesicDistance]) — the
 *    Vincenty inverse formula on the WGS84 ellipsoid, which is the reference
 *    algorithm used by professional geodesy. It removes the spherical error
 *    (up to ~0.2° of bearing and tens of kilometres of distance on the longest
 *    Qibla paths) and falls back to the spherical result when it cannot
 *    converge (nearly antipodal points). [bearingToKaaba] uses this tier.
 */
object QiblaCalculator {

    /** Coordinates of the Holy Kaaba, fixed locally (latitude/longitude). */
    const val KAABA_LATITUDE = 21.422487
    const val KAABA_LONGITUDE = 39.826206

    /** Mean Earth radius in kilometers. */
    private const val EARTH_RADIUS_KM = 6371.0088

    private const val DEGREES_TO_RADIANS = PI / 180.0

    // WGS84 reference ellipsoid (the standard Earth model).
    private const val WGS84_SEMI_MAJOR_A = 6378137.0
    private const val WGS84_FLATTENING_F = 1.0 / 298.257223563
    private const val WGS84_SEMI_MINOR_B = (1.0 - WGS84_FLATTENING_F) * WGS84_SEMI_MAJOR_A

    private const val VINCENTY_MAX_ITERATIONS = 200
    private const val VINCENTY_TOLERANCE = 1e-12

    /**
     * Initial great-circle bearing from [fromLat]/[fromLon] to
     * [toLat]/[toLon], in degrees [0, 360) referenced to true north.
     *
     * Handles any longitude (including ±180° across the International Date
     * Line), high latitudes and the poles. A zero-distance pair returns 0.
     */
    fun initialBearing(
        fromLatitude: Double,
        fromLongitude: Double,
        toLatitude: Double,
        toLongitude: Double,
    ): Float {
        val phi1 = fromLatitude * DEGREES_TO_RADIANS
        val phi2 = toLatitude * DEGREES_TO_RADIANS
        val deltaLambda = (toLongitude - fromLongitude) * DEGREES_TO_RADIANS

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        if (x == 0.0 && y == 0.0) return 0f
        val bearingRadians = atan2(y, x)
        return AngleMath.normalizeDegrees((bearingRadians / DEGREES_TO_RADIANS).toFloat())
    }

    /**
     * Great-circle distance in kilometers (haversine formula).
     * A zero-distance pair returns 0.
     */
    fun greatCircleDistance(
        fromLatitude: Double,
        fromLongitude: Double,
        toLatitude: Double,
        toLongitude: Double,
    ): Double {
        val phi1 = fromLatitude * DEGREES_TO_RADIANS
        val phi2 = toLatitude * DEGREES_TO_RADIANS
        val deltaPhi = (toLatitude - fromLatitude) * DEGREES_TO_RADIANS
        val deltaLambda = (toLongitude - fromLongitude) * DEGREES_TO_RADIANS

        val a = sin(deltaPhi / 2) * sin(deltaPhi / 2) +
            cos(phi1) * cos(phi2) * sin(deltaLambda / 2) * sin(deltaLambda / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Bearing from the user's location to the Kaaba, computed with the
     * WGS84 geodesic (Vincenty inverse) and falling back to the spherical
     * great-circle result when Vincenty cannot converge.
     */
    fun bearingToKaaba(userLatitude: Double, userLongitude: Double): QiblaBearing = QiblaBearing(
        bearingDegrees = geodesicBearing(userLatitude, userLongitude, KAABA_LATITUDE, KAABA_LONGITUDE),
        distanceKm = geodesicDistance(userLatitude, userLongitude, KAABA_LATITUDE, KAABA_LONGITUDE),
        userLatitude = userLatitude,
        userLongitude = userLongitude,
    )

    /**
     * Initial geodesic bearing on the WGS84 ellipsoid (Vincenty inverse), in
     * degrees [0, 360) referenced to true north. Falls back to the spherical
     * great-circle bearing when Vincenty does not converge (nearly antipodal
     * points) or the points coincide.
     */
    fun geodesicBearing(
        fromLatitude: Double,
        fromLongitude: Double,
        toLatitude: Double,
        toLongitude: Double,
    ): Float {
        val inverse = vincentyInverse(fromLatitude, fromLongitude, toLatitude, toLongitude)
            ?: return initialBearing(fromLatitude, fromLongitude, toLatitude, toLongitude)
        return AngleMath.normalizeDegrees((inverse.first / DEGREES_TO_RADIANS).toFloat())
    }

    /**
     * Geodesic distance on the WGS84 ellipsoid (Vincenty inverse) in
     * kilometers. Falls back to the haversine distance when Vincenty does not
     * converge (nearly antipodal points) or the points coincide.
     */
    fun geodesicDistance(
        fromLatitude: Double,
        fromLongitude: Double,
        toLatitude: Double,
        toLongitude: Double,
    ): Double {
        val inverse = vincentyInverse(fromLatitude, fromLongitude, toLatitude, toLongitude)
            ?: return greatCircleDistance(fromLatitude, fromLongitude, toLatitude, toLongitude)
        return inverse.second / 1000.0
    }

    /**
     * Vincenty inverse formula on the WGS84 ellipsoid.
     *
     * @return (initial azimuth in radians, distance in meters) or null when the
     *   points coincide or the iteration does not converge (antipodal case).
     */
    private fun vincentyInverse(
        fromLatitude: Double,
        fromLongitude: Double,
        toLatitude: Double,
        toLongitude: Double,
    ): Pair<Double, Double>? {
        if (fromLatitude == toLatitude && fromLongitude == toLongitude) return null

        val phi1 = fromLatitude * DEGREES_TO_RADIANS
        val phi2 = toLatitude * DEGREES_TO_RADIANS
        val u1 = atan((1.0 - WGS84_FLATTENING_F) * tan(phi1))
        val u2 = atan((1.0 - WGS84_FLATTENING_F) * tan(phi2))
        val l = (toLongitude - fromLongitude) * DEGREES_TO_RADIANS

        var lambda = l
        var sinSigma = 0.0
        var cosSigma = 0.0
        var sigma = 0.0
        var cosSqAlpha = 0.0
        var cos2SigmaM = 0.0
        var converged = false

        for (iteration in 0 until VINCENTY_MAX_ITERATIONS) {
            val sinLambda = sin(lambda)
            val cosLambda = cos(lambda)
            val sinU1 = sin(u1)
            val cosU1 = cos(u1)
            val sinU2 = sin(u2)
            val cosU2 = cos(u2)

            sinSigma = sqrt(
                (cosU2 * sinLambda) * (cosU2 * sinLambda) +
                    (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda) *
                    (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda),
            )
            if (sinSigma == 0.0) return null // coincident points
            cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda
            sigma = atan2(sinSigma, cosSigma)
            val sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma
            cosSqAlpha = 1.0 - sinAlpha * sinAlpha
            // For alpha = 90° (cos² alpha = 0) the standard formulation fixes
            // cos(2 sigma_m) = 0 instead of dividing by zero.
            cos2SigmaM = if (cosSqAlpha == 0.0) 0.0 else cosSigma - 2.0 * sinU1 * sinU2 / cosSqAlpha

            val c = WGS84_FLATTENING_F / 16.0 * cosSqAlpha *
                (4.0 + WGS84_FLATTENING_F * (4.0 - 3.0 * cosSqAlpha))
            val lambdaPrevious = lambda
            lambda = l + (1.0 - c) * WGS84_FLATTENING_F * sinAlpha *
                (sigma + c * sinSigma * (cos2SigmaM + c * cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM)))

            if (abs(lambda - lambdaPrevious) < VINCENTY_TOLERANCE) {
                converged = true
                break
            }
        }
        if (!converged) return null // nearly antipodal — caller falls back

        val uSquared = cosSqAlpha * (WGS84_SEMI_MAJOR_A * WGS84_SEMI_MAJOR_A - WGS84_SEMI_MINOR_B * WGS84_SEMI_MINOR_B) /
            (WGS84_SEMI_MINOR_B * WGS84_SEMI_MINOR_B)
        val a = 1.0 + uSquared / 16384.0 * (4096.0 + uSquared * (-768.0 + uSquared * (320.0 - 175.0 * uSquared)))
        val b = uSquared / 1024.0 * (256.0 + uSquared * (-128.0 + uSquared * (74.0 - 47.0 * uSquared)))
        val deltaSigma = b * sinSigma *
            (cos2SigmaM + b / 4.0 * (cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM) -
                b / 6.0 * cos2SigmaM * (-3.0 + 4.0 * sinSigma * sinSigma) *
                (-3.0 + 4.0 * cos2SigmaM * cos2SigmaM)))
        val distanceMeters = WGS84_SEMI_MINOR_B * a * (sigma - deltaSigma)

        // Initial azimuth at the from-point, using the final lambda.
        val sinU1 = sin(u1)
        val cosU1 = cos(u1)
        val sinU2 = sin(u2)
        val cosU2 = cos(u2)
        val alpha1 = atan2(
            cosU2 * sin(lambda),
            cosU1 * sinU2 - sinU1 * cosU2 * cos(lambda),
        )
        return Pair(alpha1, distanceMeters)
    }

    /**
     * Signed shortest difference between the device heading and the Qibla,
     * in degrees: positive means the user must turn **right**.
     *
     * Device heading and Qibla bearing must be in the same north reference.
     */
    fun relativeQibla(deviceHeadingDegrees: Float, qiblaBearingDegrees: Float): Float =
        AngleMath.shortestAngularDifference(qiblaBearingDegrees, deviceHeadingDegrees)

    /**
     * Alignment from the relative angle with a configurable threshold
     * (default ±2°).
     */
    fun alignment(relativeQiblaDegrees: Float, thresholdDegrees: Float = 2f): QiblaAlignment = when {
        relativeQiblaDegrees > thresholdDegrees -> QiblaAlignment.TURN_RIGHT
        relativeQiblaDegrees < -thresholdDegrees -> QiblaAlignment.TURN_LEFT
        else -> QiblaAlignment.ALIGNED
    }

    /**
     * Converts the true-north Qibla bearing into the device's north reference
     * so both can be compared directly.
     */
    fun qiblaBearingInReference(
        qiblaTrueBearingDegrees: Float,
        declinationDegrees: Float,
        reference: NorthReference,
    ): Float = when (reference) {
        NorthReference.TRUE_NORTH, NorthReference.AUTOMATIC ->
            AngleMath.normalizeDegrees(qiblaTrueBearingDegrees)

        NorthReference.MAGNETIC_NORTH ->
            AngleMath.normalizeDegrees(qiblaTrueBearingDegrees - declinationDegrees)
    }

    /**
     * Converts the device heading into the true-north frame, so it can be
     * compared against the (true) Qibla bearing.
     */
    fun deviceHeadingInTrueReference(heading: Heading, declinationDegrees: Float?): Float? {
        if (!heading.isAvailable) return null
        return when (heading.mode) {
            HeadingMode.TRUE -> heading.degrees
            HeadingMode.MAGNETIC -> {
                val d = declinationDegrees ?: return null
                AngleMath.normalizeDegrees(heading.degrees + d)
            }
        }
    }

    /** Convenience status for an aligned/ready pair (kept testable). */
    fun statusFor(relativeDegrees: Float, thresholdDegrees: Float, hasHeading: Boolean, hasBearing: Boolean): QiblaStatus {
        if (!hasHeading) return QiblaStatus.COMPASS_UNAVAILABLE
        if (!hasBearing) return QiblaStatus.LOCATION_UNAVAILABLE
        return if (alignment(relativeDegrees, thresholdDegrees) == QiblaAlignment.ALIGNED) {
            QiblaStatus.ALIGNED
        } else {
            QiblaStatus.READY
        }
    }
}
