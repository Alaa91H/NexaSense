package com.nexasense.domain.model

/** Result of a runtime capability check for one feature. */
enum class FeatureStatus {
    AVAILABLE,
    UNAVAILABLE,
}

/** Why a feature is unavailable; the presentation layer maps this to strings. */
enum class AvailabilityReason {
    MISSING_SENSOR,
    LOCATION_REQUIRED,
    PERMISSION_DENIED,
    LOCATION_UNKNOWN,
    SENSOR_ERROR,
}

/** Capability detection result for a feature such as Compass or Barometer. */
data class FeatureAvailability(
    val status: FeatureStatus,
    val reason: AvailabilityReason? = null,
) {
    val isAvailable: Boolean get() = status == FeatureStatus.AVAILABLE

    companion object {
        fun available(): FeatureAvailability = FeatureAvailability(FeatureStatus.AVAILABLE)

        fun unavailable(reason: AvailabilityReason): FeatureAvailability =
            FeatureAvailability(FeatureStatus.UNAVAILABLE, reason)
    }
}
