package com.nexasense.domain.model

/**
 * Sampling statistics measured from event timestamps, never assumed from the
 * requested delay constant.
 */
data class SamplingStats(
    /** The delay requested at registration, in microseconds, if any. */
    val requestedDelayMicros: Long?,
    /** Measured event rate in Hz (EMA over recent intervals). */
    val actualHz: Float,
    /** Number of events observed since the stream started. */
    val sampleCount: Int,
) {
    val requestedLabel: String
        get() = when (requestedDelayMicros) {
            null -> "—"
            else -> "$requestedDelayMicros µs"
        }

    companion object {
        val EMPTY: SamplingStats = SamplingStats(requestedDelayMicros = null, actualHz = 0f, sampleCount = 0)
    }
}
