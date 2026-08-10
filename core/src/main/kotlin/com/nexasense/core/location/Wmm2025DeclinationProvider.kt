package com.nexasense.core.location

import com.nexasense.core.logging.NexaLogger
import com.nexasense.domain.geomag.Wmm2025
import com.nexasense.domain.port.DeclinationProvider

/**
 * Declination from the pure-Kotlin [Wmm2025] model — the current official
 * NOAA/BGS geomagnetic reference model (valid 2025.0–2030.0). The platform's
 * `GeomagneticField` embeds the expired WMM2020, whose declination error
 * grows every year past 2025.0; this provider replaces it.
 *
 * The framework model is kept as a defensive fallback: if the local model
 * ever fails for a given input, the platform value is used and the event is
 * logged. See [GeomagneticFieldDeclinationProvider].
 */
class Wmm2025DeclinationProvider(
    private val fallback: DeclinationProvider = GeomagneticFieldDeclinationProvider(),
) : DeclinationProvider {

    override fun declinationAt(
        latitudeDegrees: Double,
        longitudeDegrees: Double,
        altitudeMeters: Double,
        timeMillis: Long,
    ): Float? {
        val year = Wmm2025.decimalYear(timeMillis)
        return try {
            Wmm2025.declinationAt(year, latitudeDegrees, longitudeDegrees, altitudeMeters).toFloat()
        } catch (t: Throwable) {
            NexaLogger.w("WMM2025 model rejected input: ${t.message}; falling back to GeomagneticField")
            fallback.declinationAt(latitudeDegrees, longitudeDegrees, altitudeMeters, timeMillis)
        }
    }
}
