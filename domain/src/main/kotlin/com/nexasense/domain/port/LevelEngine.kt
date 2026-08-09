package com.nexasense.domain.port

import com.nexasense.domain.model.OrientationAngles
import kotlinx.coroutines.flow.StateFlow

/** The spirit level engine; works from the accelerometer alone. */
interface LevelEngine {
    val orientation: StateFlow<OrientationAngles>

    /** Whether the engine currently has a valid reading. */
    val isAvailable: StateFlow<Boolean>

    fun setActive(active: Boolean)

    /**
     * Sets the display rotation in degrees (0/90/180/270) so pitch and roll
     * are reported in the user's frame of reference.
     */
    fun setDisplayRotation(rotationDegrees: Int)
}
