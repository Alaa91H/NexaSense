package com.nexasense.core.location

import android.hardware.GeomagneticField
import com.nexasense.core.logging.NexaLogger
import com.nexasense.domain.port.DeclinationProvider

/**
 * Declination computed with the platform's `GeomagneticField` model — an
 * estimate from the IGRF/WMM model, never a measurement. Returns null when
 * the model rejects the input.
 */
class GeomagneticFieldDeclinationProvider : DeclinationProvider {

    override fun declinationAt(
        latitudeDegrees: Double,
        longitudeDegrees: Double,
        altitudeMeters: Double,
        timeMillis: Long,
    ): Float? = try {
        GeomagneticField(
            latitudeDegrees.toFloat(),
            longitudeDegrees.toFloat(),
            altitudeMeters.toFloat(),
            timeMillis,
        ).declination
    } catch (t: Throwable) {
        NexaLogger.w("GeomagneticField rejected input: ${t.message}")
        null
    }
}
