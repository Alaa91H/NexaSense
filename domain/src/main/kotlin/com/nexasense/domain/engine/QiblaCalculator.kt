package com.nexasense.domain.engine

import com.nexasense.domain.math.AngleMath
import com.nexasense.domain.model.Heading
import com.nexasense.domain.model.HeadingMode
import com.nexasense.domain.model.NorthReference
import com.nexasense.domain.model.QiblaAlignment
import com.nexasense.domain.model.QiblaBearing
import com.nexasense.domain.model.QiblaStatus
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure Qibla math — no Android, no location services, fully unit-testable.
 *
 * The initial great-circle bearing (spherical formula below) and the haversine
 * distance are computed locally from the fixed Kaaba coordinates; nothing is
 * ever sent to a server.
 */
object QiblaCalculator {

    /** Coordinates of the Holy Kaaba, fixed locally (latitude/longitude). */
    const val KAABA_LATITUDE = 21.422487
    const val KAABA_LONGITUDE = 39.826206

    /** Mean Earth radius in kilometers. */
    private const val EARTH_RADIUS_KM = 6371.0088

    private const val DEGREES_TO_RADIANS = PI / 180.0

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

    /** Bearing from the user's location to the Kaaba. */
    fun bearingToKaaba(userLatitude: Double, userLongitude: Double): QiblaBearing = QiblaBearing(
        bearingDegrees = initialBearing(userLatitude, userLongitude, KAABA_LATITUDE, KAABA_LONGITUDE),
        distanceKm = greatCircleDistance(userLatitude, userLongitude, KAABA_LATITUDE, KAABA_LONGITUDE),
        userLatitude = userLatitude,
        userLongitude = userLongitude,
    )

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
