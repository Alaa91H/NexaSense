package com.nexasense.domain.model

/**
 * How the compass interprets its heading relative to north.
 *
 * - [AUTOMATIC] picks the best available reference: True North when a valid
 *   declination (and therefore a location) is available, otherwise Magnetic
 *   North. The effective choice is always reported to the user.
 * - [TRUE_NORTH] converts the magnetic heading with the geomagnetic
 *   declination; falls back to magnetic when no declination is available.
 * - [MAGNETIC_NORTH] uses the raw magnetic heading without declination.
 */
enum class NorthReference {
    AUTOMATIC,
    TRUE_NORTH,
    MAGNETIC_NORTH,
}
