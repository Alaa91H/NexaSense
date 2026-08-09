package com.nexasense.domain.model

/** Which algorithm produced a [Heading]. */
enum class HeadingSource {
    /** Framework fused rotation vector. */
    ROTATION_VECTOR,

    /** Framework fused geomagnetic rotation vector. */
    GEOMAGNETIC_ROTATION_VECTOR,

    /** Tilt-compensated accelerometer + magnetometer fallback. */
    ACCELEROMETER_MAGNETOMETER,

    /** Heading could not be computed with the available hardware. */
    UNAVAILABLE,
}

/** Whether a heading is relative to magnetic or true north. */
enum class HeadingMode {
    MAGNETIC,
    TRUE,
}

/**
 * A computed compass heading.
 *
 * [degrees] is always in the range [0, 360) for a valid heading. A heading with
 * [source] == [HeadingSource.UNAVAILABLE] carries no meaningful [degrees] value.
 *
 * @param requestedNorthReference what the user asked for in Settings.
 * @param effectiveNorthReference what is actually applied — always reported so
 *   the user knows (e.g. Automatic · True North vs Automatic · Magnetic North).
 * @param locationAvailable whether a location fix exists (needed for
 *   declination and therefore True North / Qibla).
 * @param trueNorthUnavailableReason set when true north was requested but no
 *   declination is available; drives the "True North unavailable — Location
 *   required" UI.
 */
data class Heading(
    val degrees: Float,
    val cardinal: CardinalDirection,
    val source: HeadingSource,
    val mode: HeadingMode,
    val declinationDegrees: Float? = null,
    val requestedNorthReference: NorthReference = NorthReference.AUTOMATIC,
    val effectiveNorthReference: NorthReference = NorthReference.AUTOMATIC,
    val locationAvailable: Boolean = false,
    val trueNorthUnavailableReason: TrueNorthUnavailableReason? = null,
) {
    val isAvailable: Boolean get() = source != HeadingSource.UNAVAILABLE

    val declinationAvailable: Boolean get() = declinationDegrees != null
}

/** Why true-north compensation could not be applied. */
enum class TrueNorthUnavailableReason {
    LOCATION_REQUIRED,
    PERMISSION_DENIED,
    LOCATION_UNKNOWN,
}
