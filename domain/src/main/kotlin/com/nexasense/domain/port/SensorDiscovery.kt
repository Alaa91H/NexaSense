package com.nexasense.domain.port

import com.nexasense.domain.model.SensorDescriptor
import com.nexasense.domain.model.SensorKind

/**
 * Runtime sensor discovery. Implementations read from the platform
 * SensorManager; the domain never assumes a sensor exists.
 */
interface SensorDiscovery {
    /** All sensors currently reported by the framework, static and dynamic. */
    suspend fun getSensors(): List<SensorDescriptor>

    /** Whether at least one sensor of [kind] is exposed. */
    suspend fun hasSensor(kind: SensorKind): Boolean

    /** All sensors matching [kind]. */
    suspend fun sensorsOf(kind: SensorKind): List<SensorDescriptor>
}
