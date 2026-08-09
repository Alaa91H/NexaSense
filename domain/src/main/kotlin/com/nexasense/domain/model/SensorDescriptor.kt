package com.nexasense.domain.model

/**
 * Static metadata describing one sensor reported by the framework.
 *
 * Fields mirror `android.hardware.Sensor`; the mapping happens in the core
 * layer so this model stays free of Android types.
 *
 * @param id the framework sensor handle, unique within the sensor list.
 * @param kind the resolved [SensorKind]; [SensorKind.UNKNOWN] for vendor types.
 * @param name the name reported by the HAL (informational only, never used to
 *   infer the sensor kind).
 * @param vendor the vendor string reported by the HAL.
 * @param version the sensor module version reported by the HAL.
 * @param stringType the sensor's type string (e.g. "android.sensor.accelerometer").
 * @param resolution smallest measurable value increment, in sensor units.
 * @param maxRange maximum measurable range in sensor units.
 * @param powerMilliAmps average current draw in mA.
 * @param minDelayMicros minimum allowed delay between events (µs), 0 for
 *   one-shot sensors.
 * @param maxDelayMicros maximum allowed delay between events (µs).
 * @param isWakeUp whether the sensor is a wake-up sensor.
 * @param isDynamic whether the sensor is a dynamic (plugged-in) sensor.
 * @param reportingMode one of continuous / on-change / one-shot / special.
 * @param maxFifoCount number of events the FIFO can hold, 0 if none.
 */
data class SensorDescriptor(
    val id: Int,
    val kind: SensorKind,
    val name: String,
    val vendor: String,
    val version: Int,
    val stringType: String,
    val resolution: Float,
    val maxRange: Float,
    val powerMilliAmps: Float,
    val minDelayMicros: Int,
    val maxDelayMicros: Int,
    val isWakeUp: Boolean,
    val isDynamic: Boolean,
    val reportingMode: String,
    val maxFifoCount: Int,
) {
    /** True for sensors that deliver continuous streams of vector values. */
    val isContinuous: Boolean
        get() = kind in CONTINUOUS_KINDS

    private companion object {
        val CONTINUOUS_KINDS = setOf(
            SensorKind.ACCELEROMETER,
            SensorKind.MAGNETIC_FIELD,
            SensorKind.GYROSCOPE,
            SensorKind.LIGHT,
            SensorKind.PRESSURE,
            SensorKind.PROXIMITY,
            SensorKind.GRAVITY,
            SensorKind.LINEAR_ACCELERATION,
            SensorKind.ROTATION_VECTOR,
            SensorKind.RELATIVE_HUMIDITY,
            SensorKind.AMBIENT_TEMPERATURE,
            SensorKind.MAGNETIC_FIELD_UNCALIBRATED,
            SensorKind.GAME_ROTATION_VECTOR,
            SensorKind.GYROSCOPE_UNCALIBRATED,
            SensorKind.GEOMAGNETIC_ROTATION_VECTOR,
            SensorKind.ACCELEROMETER_UNCALIBRATED,
            SensorKind.HINGE_ANGLE,
            SensorKind.HEAD_TRACKER,
        )
    }
}
