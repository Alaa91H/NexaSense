package com.nexasense.domain.port

import com.nexasense.domain.model.SensorKind
import com.nexasense.domain.model.SensorReading
import kotlinx.coroutines.flow.Flow

/**
 * Live sensor event streams. Implementations own the framework registration
 * lifecycle and must handle registration failures gracefully (an empty or
 * cancelled flow, never an exception thrown to the collector).
 */
interface SensorEventStream {
    /**
     * Stream of events for [kind]. The stream completes silently when the
     * sensor is unavailable, registration fails, or the collector is cancelled.
     *
     * @param delayMicros requested inter-event delay; 0 requests the fastest
     *   rate. The actual rate is measured downstream.
     * @param sensorId optional handle to stream a specific sensor instance.
     */
    fun stream(
        kind: SensorKind,
        delayMicros: Long = 0L,
        sensorId: Int? = null,
    ): Flow<SensorReading>
}
