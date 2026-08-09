package com.nexasense.domain.engine

import com.nexasense.domain.model.NorthReference

/**
 * Resolves the *effective* north reference from the user's request and the
 * availability of a valid declination.
 *
 * - [NorthReference.AUTOMATIC]: True North when a declination is available,
 *   otherwise Magnetic North.
 * - [NorthReference.TRUE_NORTH]: True North when available; falls back to
 *   Magnetic North (the heading carries the reason separately).
 * - [NorthReference.MAGNETIC_NORTH]: always Magnetic North.
 */
object NorthReferenceResolver {

    fun effective(requested: NorthReference, declinationAvailable: Boolean): NorthReference = when (requested) {
        NorthReference.AUTOMATIC ->
            if (declinationAvailable) NorthReference.TRUE_NORTH else NorthReference.MAGNETIC_NORTH

        NorthReference.TRUE_NORTH ->
            if (declinationAvailable) NorthReference.TRUE_NORTH else NorthReference.MAGNETIC_NORTH

        NorthReference.MAGNETIC_NORTH -> NorthReference.MAGNETIC_NORTH
    }
}
