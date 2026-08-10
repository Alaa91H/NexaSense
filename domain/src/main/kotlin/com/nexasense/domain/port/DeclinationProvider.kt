package com.nexasense.domain.port

/**
 * Magnetic declination (the angle between magnetic and true north) at a
 * location and time, in degrees (east positive). The default implementation
 * uses the pure-Kotlin WMM2025 model (see `com.nexasense.domain.geomag`);
 * the value is a model estimate, not a measurement. Returns null when the
 * model cannot produce a value for the given input.
 */
interface DeclinationProvider {
    fun declinationAt(
        latitudeDegrees: Double,
        longitudeDegrees: Double,
        altitudeMeters: Double,
        timeMillis: Long,
    ): Float?
}
