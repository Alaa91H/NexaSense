package com.nexasense.domain.engine

import com.nexasense.domain.model.HeadingSource
import com.nexasense.domain.model.Vec3

/**
 * Sensor fusion abstraction.
 *
 * The framework's rotation-vector sensors are themselves fused products, so
 * the suite uses them directly whenever present. [TiltCompensatedFusion]
 * implements the accel+magnetometer fallback. Future algorithms (complementary
 * filter, Kalman, Madgwick, Mahony) can be added as new implementations
 * without touching the compass engine — see docs/architecture.md.
 */
interface SensorFusionEngine {
    /** Heading in degrees [0, 360), or null when the input is degenerate. */
    fun heading(accel: Vec3?, magnetometer: Vec3?, source: HeadingSource): Float?
}

/**
 * Tilt-compensated fusion used when no fused rotation vector exists.
 * The magnetometer reading is expected to be calibrated before it reaches
 * this engine.
 */
class TiltCompensatedFusion : SensorFusionEngine {

    override fun heading(accel: Vec3?, magnetometer: Vec3?, source: HeadingSource): Float? {
        if (source != HeadingSource.ACCELEROMETER_MAGNETOMETER) return null
        val a = accel ?: return null
        val m = magnetometer ?: return null
        return HeadingCalculator.fromAccelerometerMagnetometer(a, m)
    }
}
