package com.nexasense.domain.engine

import com.nexasense.domain.model.HeadingSource
import com.nexasense.domain.model.SensorKind

/**
 * Picks the best heading source given the sensors actually present, in the
 * documented priority order:
 * 1. Rotation Vector (framework-fused, most stable);
 * 2. Geomagnetic Rotation Vector (fusion without gyroscope drift);
 * 3. Accelerometer + Magnetometer (tilt-compensated fallback);
 * 4. nothing — the compass is reported unavailable, never faked.
 */
object SourceSelector {

    fun bestSource(availableKinds: Set<SensorKind>): HeadingSource = when {
        SensorKind.ROTATION_VECTOR in availableKinds -> HeadingSource.ROTATION_VECTOR
        SensorKind.GEOMAGNETIC_ROTATION_VECTOR in availableKinds -> HeadingSource.GEOMAGNETIC_ROTATION_VECTOR
        SensorKind.ACCELEROMETER in availableKinds && SensorKind.MAGNETIC_FIELD in availableKinds ->
            HeadingSource.ACCELEROMETER_MAGNETOMETER
        else -> HeadingSource.UNAVAILABLE
    }
}
