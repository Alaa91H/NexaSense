package com.nexasense.domain.model

/**
 * Magnetometer calibration derived from collected samples.
 *
 * Hard-iron errors are constant offsets ([offsetX/Y/Z]); soft-iron errors are
 * per-axis scale factors ([scaleX/Y/Z]) that map the sampled ellipsoid onto a
 * sphere. See [com.nexasense.domain.math.MagneticCalibrationMath].
 */
data class MagnetometerCalibration(
    val offsetX: Float,
    val offsetY: Float,
    val offsetZ: Float,
    val scaleX: Float,
    val scaleY: Float,
    val scaleZ: Float,
    val sampleCount: Int,
    /** 0..1 estimate of orientation-space coverage of the collected samples. */
    val coverage: Float,
    val isCalibrated: Boolean,
) {
    companion object {
        val NONE: MagnetometerCalibration = MagnetometerCalibration(
            offsetX = 0f,
            offsetY = 0f,
            offsetZ = 0f,
            scaleX = 1f,
            scaleY = 1f,
            scaleZ = 1f,
            sampleCount = 0,
            coverage = 0f,
            isCalibrated = false,
        )
    }
}

/** Zero-point offsets applied to the level readings. */
data class LevelCalibration(
    val pitchOffsetDegrees: Float,
    val rollOffsetDegrees: Float,
    val isSet: Boolean,
) {
    companion object {
        val NONE: LevelCalibration = LevelCalibration(0f, 0f, isSet = false)
    }
}
