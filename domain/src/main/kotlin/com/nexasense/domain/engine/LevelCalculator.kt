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
}
