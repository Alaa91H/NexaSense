package com.nexasense.domain.engine

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Solar position (azimuth + elevation) computed with the NOAA Solar
 * Calculator algorithm — pure Kotlin, offline, accurate to about 1°.
 *
 * Azimuth is measured clockwise from true north (0° = north, 90° = east,
 * 180° = south, 270° = west); elevation is degrees above the horizon
 * (refraction-corrected), matching the NOAA calculator.
 *
 * The algorithm was validated in Python against an independent
 * implementation and against the twice-yearly **sun-over-Kaaba** event:
 * when the sun transits the Kaaba (≈28 May and ≈16 July, ~09:18/09:26 UTC),
 * the azimuth to the sun equals the Qibla bearing from any location — e.g.
 * Berlin 136.9° vs 136.5° and New York 58.6° vs 58.4°.
 */
object SolarPositionCalculator {

    /** Azimuth (clockwise from north) and elevation above the horizon. */
    data class SunPosition(
        val azimuthDegrees: Double,
        val elevationDegrees: Double,
    )

    private const val DEGREES_TO_RADIANS = PI / 180.0

    /**
     * Solar position at [latitudeDegrees]/[longitudeDegrees] (east positive)
     * for the instant [timeMillis] (UTC milliseconds since the epoch).
     */
    fun positionAt(
        latitudeDegrees: Double,
        longitudeDegrees: Double,
        timeMillis: Long,
    ): SunPosition {
        val dayOfYear = dayOfYear(timeMillis)
        val secondsIntoDay = ((timeMillis % DAY_MILLIS) + DAY_MILLIS) % DAY_MILLIS
        val utcHours = secondsIntoDay / 3_600_000.0

        // Fractional year (radians) — the NOAA spreadsheet formulation.
        val gamma = 2.0 * PI / 365.0 * (dayOfYear - 1 + (utcHours - 12.0) / 24.0)

        val equationOfTime = 229.18 * (
            0.000075 +
                0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
                0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma)
            )

        val declination = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
            0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) -
            0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)

        // True solar time (minutes), UTC-based; 4 minutes per degree of
        // longitude. Floored modulo: Kotlin/Java `%` keeps the dividend's
        // sign, which would break western longitudes early in the day.
        val trueSolarMinutes = ((utcHours * 60 + equationOfTime + 4 * longitudeDegrees) % 1440.0 + 1440.0) % 1440.0
        val hourAngle = trueSolarMinutes / 4.0 - 180.0 // degrees

        val latRad = latitudeDegrees * DEGREES_TO_RADIANS
        val decRad = declination
        val haRad = hourAngle * DEGREES_TO_RADIANS

        val cosZenith = (
            sin(latRad) * sin(decRad) +
                cos(latRad) * cos(decRad) * cos(haRad)
            ).coerceIn(-1.0, 1.0)
        val zenith = acos(cosZenith) / DEGREES_TO_RADIANS
        var elevation = 90.0 - zenith

        // Atmospheric refraction (NOAA correction) — only above the horizon.
        if (elevation > -0.833) {
            elevation += 0.017 / tan((elevation + 10.26 / (elevation + 5.10)) * DEGREES_TO_RADIANS)
        }

        val cosAzimuth = (
            sin(decRad) - sin(latRad) * cosZenith
            ) / (cos(latRad) * sin(zenith * DEGREES_TO_RADIANS))
        var azimuth = acos(cosAzimuth.coerceIn(-1.0, 1.0)) / DEGREES_TO_RADIANS
        // After solar noon the sun is west of the meridian.
        if (hourAngle > 0) azimuth = 360.0 - azimuth

        return SunPosition(
            azimuthDegrees = ((azimuth % 360.0) + 360.0) % 360.0,
            elevationDegrees = elevation,
        )
    }

    private fun dayOfYear(timeMillis: Long): Int {
        val utc = java.util.TimeZone.getTimeZone("UTC")
        val calendar = java.util.Calendar.getInstance(utc).apply { timeInMillis = timeMillis }
        return calendar.get(java.util.Calendar.DAY_OF_YEAR)
    }

    private const val DAY_MILLIS = 86_400_000L
}
