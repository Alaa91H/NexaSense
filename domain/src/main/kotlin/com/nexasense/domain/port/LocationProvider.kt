package com.nexasense.domain.port

import kotlinx.coroutines.flow.Flow

/** A location fix. Only used for declination and the Qibla bearing. */
data class LocationPoint(
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
    val altitudeMeters: Double,
    val timeMillis: Long,
    /** Estimated horizontal accuracy in meters, when the provider reports it. */
    val accuracyMeters: Float? = null,
)

/**
 * Offline location provider. Returns null / completes silently when no fix is
 * available or permission is missing; it never synthesizes a location.
 */
interface LocationProvider {
    /** The most recent cached fix, if any. */
    suspend fun lastKnownLocation(): LocationPoint?

    /**
     * Requests a fresh fix, waiting up to [timeoutMillis].
     * Returns null when the request times out or is denied.
     */
    suspend fun requestCurrentLocation(timeoutMillis: Long = 10_000L): LocationPoint?

    /**
     * A stream of fixes delivered only when the location changed by at least
     * [minDistanceMeters] (and not more often than [minIntervalMillis]).
     * Completes silently when permission is missing. Used by the Qibla engine
     * so recalculation happens on significant movement, not per callback.
     */
    fun locationUpdates(
        minDistanceMeters: Float = 50f,
        minIntervalMillis: Long = 15_000L,
    ): Flow<LocationPoint>
}
