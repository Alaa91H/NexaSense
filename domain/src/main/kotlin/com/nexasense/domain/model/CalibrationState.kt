package com.nexasense.domain.model

/**
 * Full 3D ellipsoid correction from a least-squares fit on the collected
 * samples: a hard-iron offset plus a 3x3 soft-iron matrix.
 *
 * The matrix is the transpose of the (magnitude-preserving, scaled) Cholesky
 * factor of the fitted ellipsoid matrix, row-major — i.e. the actual
 * whitening transform (for P = L L^T the correction is L^T, since
 * |L^T v|^2 = v^T P v). It corrects soft-iron distortion in any direction —
 * including rotations the axis-aligned min/max model cannot see.
 */
data class EllipsoidCorrection(
    val offsetX: Float,
    val offsetY: Float,
    val offsetZ: Float,
    /** 9 elements, row-major 3x3. */
    val softIron: FloatArray,
)

/**
 * Magnetometer calibration derived from collected samples.
 *
 * Two tiers:
 *  - [ellipsoid] (when present) — the full least-squares 3D fit: hard-iron
 *    offset + soft-iron matrix; supersedes the axis-aligned correction.
 *  - otherwise the axis-aligned model: constant offsets ([offsetX/Y/Z]) and
 *    per-axis scale factors ([scaleX/Y/Z]) that map the sampled ellipsoid
 *    onto a sphere. This tier is also the persisted fallback for older data.
 * See [com.nexasense.domain.math.MagneticCalibrationMath].
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
    /** Full 3D ellipsoid correction when the fit succeeded. */
    val ellipsoid: EllipsoidCorrection? = null,
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
