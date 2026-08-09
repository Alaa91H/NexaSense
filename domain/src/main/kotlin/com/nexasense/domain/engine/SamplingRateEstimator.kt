package com.nexasense.domain.engine

import com.nexasense.domain.model.SamplingStats

/**
 * Measures the actual sampling rate from event timestamps instead of assuming
 * anything from the requested delay constant.
 *
 * Interval outliers are excluded: a non-positive interval or a gap longer than
 * [gapResetNanos] resets the estimate (the stream likely paused).
 */
class SamplingRateEstimator(
    private val gapResetNanos: Long = 2_000_000_000L,
) {

    private var lastTimestampNanos: Long = -1L
    private var emaIntervalNanos = 0.0
    private var count = 0

    /** Feeds one event timestamp; returns updated statistics. */
    fun update(timestampNanos: Long): SamplingStats {
        if (timestampNanos < 0) return current()
        if (lastTimestampNanos < 0) {
            lastTimestampNanos = timestampNanos
            count = 1
            return current()
        }
        val interval = timestampNanos - lastTimestampNanos
        lastTimestampNanos = timestampNanos
        count++
        if (interval <= 0 || interval > gapResetNanos) {
            emaIntervalNanos = 0.0
            return current()
        }
        emaIntervalNanos = if (emaIntervalNanos <= 0.0) {
            interval.toDouble()
        } else {
            emaIntervalNanos + 0.1 * (interval - emaIntervalNanos)
        }
        return current()
    }

    fun reset() {
        lastTimestampNanos = -1L
        emaIntervalNanos = 0.0
        count = 0
    }

    fun current(requestedDelayMicros: Long? = null): SamplingStats {
        val hz = if (emaIntervalNanos > 0.0) {
            (1_000_000_000.0 / emaIntervalNanos).toFloat()
        } else {
            0f
        }
        return SamplingStats(requestedDelayMicros, hz, count)
    }
}
