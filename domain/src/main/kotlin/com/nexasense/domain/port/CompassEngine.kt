package com.nexasense.domain.port

import com.nexasense.domain.model.Heading
import com.nexasense.domain.model.MagneticFieldState
import com.nexasense.domain.model.MagnetometerCalibration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The compass engine. Produces a continuous [Heading] from the best available
 * source, plus the analyzed [MagneticFieldState].
 *
 * Implementations register sensors only while active ([setActive]) so that
 * nothing is read in the background.
 */
interface CompassEngine {
    val heading: StateFlow<Heading>

    val magneticField: StateFlow<MagneticFieldState>

    /**
     * Starts or stops sensor registration. Call with `true` while the compass
     * screen is in the STARTED lifecycle state and `false` when it stops.
     */
    fun setActive(active: Boolean)

    /** Resets the internal heading smoother (e.g. after a settings change). */
    fun resetSmoothing()

    /**
     * Sets the display rotation in degrees (0/90/180/270) so the reported
     * heading is relative to the screen top, not the device's natural
     * orientation (mirrors [LevelEngine.setDisplayRotation]).
     */
    fun setDisplayRotation(rotationDegrees: Int)
}

/**
 * Combined magnetometer stream consumer for calibration collection and
 * interference analysis. Implementations share the compass engine's
 * magnetometer registration so the sensor is never read twice.
 */
interface MagneticFieldMonitor {
    val state: StateFlow<MagneticFieldState>

    /** Live calibration built from samples collected so far. */
    val liveCalibration: StateFlow<MagnetometerCalibration>

    fun setActive(active: Boolean)

    /** Discards collected samples and persisted calibration. */
    fun resetCalibration()
}
