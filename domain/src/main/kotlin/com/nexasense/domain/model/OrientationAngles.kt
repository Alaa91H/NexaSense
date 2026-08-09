package com.nexasense.domain.model

/**
 * Device orientation angles for the level feature, in degrees.
 *
 * Sign conventions (documented in docs/compass.md):
 * - positive pitch: the top edge of the device is raised;
 * - positive roll: the device is rolled clockwise when viewed from the front
 *   (right edge lowered).
 */
data class OrientationAngles(
    val pitch: Float,
    val roll: Float,
) {
    companion object {
        val ZERO: OrientationAngles = OrientationAngles(0f, 0f)
    }
}
