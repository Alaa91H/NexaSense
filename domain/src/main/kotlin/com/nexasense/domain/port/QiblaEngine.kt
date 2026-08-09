package com.nexasense.domain.port

import com.nexasense.domain.model.QiblaState
import kotlinx.coroutines.flow.StateFlow

/**
 * Computes the Qibla direction from the user's location and the live compass
 * heading. Fully local and offline; location is only requested while the
 * feature is enabled.
 */
interface QiblaEngine {
    val state: StateFlow<QiblaState>

    /**
     * Starts/stops location handling and Qibla recalculation. Call with
     * `true` while the compass screen is active.
     */
    fun setActive(active: Boolean)

    /** Forces a fresh location request (e.g. after permission was granted). */
    fun refreshLocation()
}
