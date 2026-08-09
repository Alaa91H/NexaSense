package com.nexasense.domain.model

/**
 * The sensor kinds the suite knows about, keyed by the numeric `Sensor.TYPE_*`
 * identifiers defined by the Android Sensor Framework.
 *
 * The type id is authoritative: vendor-specific or unknown types fall back to
 * [UNKNOWN] instead of being guessed from the sensor name.
 */
enum class SensorKind(val type: Int) {
    ACCELEROMETER(1),
    MAGNETIC_FIELD(2),
    GYROSCOPE(4),
    LIGHT(5),
    PRESSURE(6),
    PROXIMITY(8),
    GRAVITY(9),
    LINEAR_ACCELERATION(10),
    ROTATION_VECTOR(11),
    RELATIVE_HUMIDITY(12),
    AMBIENT_TEMPERATURE(13),
    MAGNETIC_FIELD_UNCALIBRATED(14),
    GAME_ROTATION_VECTOR(15),
    GYROSCOPE_UNCALIBRATED(16),
    SIGNIFICANT_MOTION(17),
    STEP_DETECTOR(18),
    STEP_COUNTER(19),
    GEOMAGNETIC_ROTATION_VECTOR(20),
    HEART_RATE(21),
    DEVICE_ORIENTATION(27),
    POSE_6DOF(28),
    STATIONARY_DETECT(29),
    MOTION_DETECT(30),
    HEART_BEAT(31),
    LOW_LATENCY_OFFBODY_DETECT(34),
    ACCELEROMETER_UNCALIBRATED(35),
    HINGE_ANGLE(36),
    HEAD_TRACKER(37),
    HIGH_SAMPLING_RATE_SENSOR(38),
    UNKNOWN(-1);

    companion object {
        private val BY_TYPE = entries.associateBy(SensorKind::type)

        /** Resolves a raw framework type id to a [SensorKind], never guessing. */
        fun fromType(type: Int): SensorKind = BY_TYPE[type] ?: UNKNOWN
    }
}
