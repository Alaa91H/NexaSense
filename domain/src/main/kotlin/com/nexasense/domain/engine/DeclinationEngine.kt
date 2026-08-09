package com.nexasense.domain.engine

import com.nexasense.domain.math.AngleMath

/**
 * Converts magnetic headings to true headings using magnetic declination
 * (east positive). The declination itself comes from the platform's
 * `GeomagneticField` model via [com.nexasense.domain.port.DeclinationProvider].
 */
object DeclinationEngine {

    /** True heading from a magnetic heading and declination, both in degrees. */
    fun trueHeading(magneticDegrees: Float, declinationDegrees: Float): Float =
        AngleMath.normalizeTo360(magneticDegrees + declinationDegrees)

    /** Magnetic heading from a true heading (inverse of [trueHeading]). */
    fun magneticHeading(trueDegrees: Float, declinationDegrees: Float): Float =
        AngleMath.normalizeTo360(trueDegrees - declinationDegrees)
}
