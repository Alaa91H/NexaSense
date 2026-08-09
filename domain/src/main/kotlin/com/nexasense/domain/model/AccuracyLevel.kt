package com.nexasense.domain.model

/** Accuracy of a sensor reading, mapped from the framework status codes. */
enum class AccuracyLevel {
    UNRELIABLE,
    LOW,
    MEDIUM,
    HIGH;

    companion object {
        /**
         * Maps the `Sensor.SENSOR_STATUS_*` integer codes (0..3) to
         * [AccuracyLevel]. Unknown codes are treated as [UNRELIABLE].
         */
        fun fromStatus(status: Int): AccuracyLevel = when (status) {
            1 -> LOW
            2 -> MEDIUM
            3 -> HIGH
            else -> UNRELIABLE
        }
    }
}
