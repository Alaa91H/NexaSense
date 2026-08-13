package com.nexasense.domain.engine

import com.nexasense.domain.math.AngleMath
import com.nexasense.domain.model.LevelCalibration
import com.nexasense.domain.model.OrientationAngles
import com.nexasense.domain.model.Vec3
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Computes pitch and roll from the accelerometer only — the level feature
 * therefore works on any device with an accelerometer, without a gyroscope.
 *
 * Conventions (documented in docs/compass.md):
 * - pitch: + when the top edge is raised (device pitched nose-up), ±90° at
 *   upright/inverted;
 * - roll: + when the device is rolled counterclockwise viewed from the front
 *   (left edge lowered), matching the "leaning left" convention of levels.
 */
object LevelCalculator {

    /**
     * At rest the accelerometer measures the reaction force (opposite to
     * gravity), so a device held with its top edge raised reports `ay > 0`.
     */
    fun fromAccelerometer(accel: Vec3): OrientationAngles {
        if (accel.isInvalid || accel.magnitude <= 0.1f) return OrientationAngles.ZERO
        val pitch = Math.toDegrees(atan2(accel.y.toDouble(), sqrt(accel.x * accel.x + accel.z * accel.z).toDouble())).toFloat()
        val roll = Math.toDegrees(atan2(accel.x.toDouble(), sqrt(accel.y * accel.y + accel.z * accel.z).toDouble())).toFloat()
        return OrientationAngles(pitch, roll)
    }

    /**
     * Rotates pitch/roll into the user's frame of reference for the given
     * display rotation (0/90/180/270 degrees, from `Display.getRotation()`).
     * A level phone reports (0, 0) in every rotation.
     */
    fun mapToDisplay(angles: OrientationAngles, rotationDegrees: Int): OrientationAngles {
        return when ((rotationDegrees % 360 + 360) % 360) {
            0 -> angles
            90 -> OrientationAngles(-angles.roll, angles.pitch)
            180 -> OrientationAngles(-angles.pitch, -angles.roll)
            270 -> OrientationAngles(angles.roll, -angles.pitch)
            else -> angles
        }
    }

    /** Applies the user's zero-point offsets. */
    fun applyOffsets(angles: OrientationAngles, calibration: LevelCalibration): OrientationAngles {
        if (!calibration.isSet) return angles
        return OrientationAngles(
            pitch = AngleMath.normalizeTo180(angles.pitch - calibration.pitchOffsetDegrees),
            roll = AngleMath.normalizeTo180(angles.roll - calibration.rollOffsetDegrees),
        )
    }

    /**
     * Deviation of the device from the upright position, in degrees. Pitch
     * is ±90° when the device is vertical (top up / bottom up), so the
     * deviation is 0 exactly at vertical.
     *
     * Sign convention (hemisphere): with the device top-up the deviation is
     * **negative** (the needle leans left), with the device bottom-up it is
     * **positive** (the needle leans right). Because the level is
     * accelerometer-only, the forward/backward lean from vertical is not
     * distinguishable — only its magnitude and the hemisphere are — so the
     * direction is a fixed, deterministic stylization that always agrees
     * with the signed readout.
     */
    fun verticalDeviation(pitch: Float): Float =
        if (pitch >= 0f) pitch - 90f else pitch + 90f

    /**
     * Normalized bubble displacement for the two bubble surfaces, following
     * the physical rule "the indicator moves toward the raised end":
     * - positive [roll] (left edge lowered / right edge raised) → bubble
     *   moves RIGHT (+x);
     * - positive [pitch] (top edge raised) → bubble moves UP (−y);
     *
     * [scaleDegrees] is the tilt angle that saturates the bubble at the rim
     * (45° for the flat two-axis bubble, 30° for the vertical tube). The
     * result is a pure coordinate mapping in physical screen space — no
     * layout-direction input, so it renders identically in LTR and RTL.
     */
    fun bubbleFactors(pitchDegrees: Float, rollDegrees: Float, scaleDegrees: Float): Pair<Float, Float> = Pair(
        (rollDegrees / scaleDegrees).coerceIn(-1f, 1f),
        (-pitchDegrees / scaleDegrees).coerceIn(-1f, 1f),
    )
}
