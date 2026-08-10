package com.nexasense.domain.model

/** The great-circle bearing from the user to the Kaaba, plus context. */
data class QiblaBearing(
    /** Initial great-circle bearing in degrees [0, 360), referenced to true north. */
    val bearingDegrees: Float,
    /** Great-circle distance in kilometers. */
    val distanceKm: Double,
    val userLatitude: Double,
    val userLongitude: Double,
)

/** High-level Qibla feature state. */
enum class QiblaStatus {
    QIBLA_DISABLED,
    LOCATION_PERMISSION_REQUIRED,
    LOCATION_UNAVAILABLE,
    LOCATION_ACCURACY_LOW,
    COMPASS_UNAVAILABLE,
    COMPASS_ACCURACY_LOW,
    CALIBRATION_REQUIRED,
    CALCULATING,
    READY,
    ALIGNED,
}

/** Which way the user must turn to face the Qibla. */
enum class QiblaAlignment {
    ALIGNED,
    TURN_LEFT,
    TURN_RIGHT,
    UNAVAILABLE,
}

/** Location fix accuracy classification, separate from compass accuracy. */
enum class LocationAccuracyLevel {
    HIGH,
    MEDIUM,
    LOW;

    companion object {
        fun fromMeters(accuracyMeters: Float?): LocationAccuracyLevel = when {
            accuracyMeters == null || !accuracyMeters.isFinite() -> LOW
            accuracyMeters < 50f -> HIGH
            accuracyMeters < 200f -> MEDIUM
            else -> LOW
        }
    }
}

/**
 * Full Qibla state exposed to the UI. [bearingDegrees] is the true great-circle
 * bearing; [relativeQiblaDegrees] is the signed shortest difference between the
 * device heading and the Qibla in the same north reference (positive = turn
 * right).
 */
data class QiblaState(
    val status: QiblaStatus = QiblaStatus.QIBLA_DISABLED,
    /** True great-circle bearing to the Kaaba, referenced to true north. */
    val bearingDegrees: Float? = null,
    /**
     * The bearing converted into the same north reference as the compass
     * heading currently shown, so the dial marker and heading are comparable.
     */
    val bearingInDeviceReferenceDegrees: Float? = null,
    val relativeQiblaDegrees: Float? = null,
    val alignment: QiblaAlignment = QiblaAlignment.UNAVAILABLE,
    val distanceKm: Double? = null,
    val declinationDegrees: Float? = null,
    val locationAccuracy: LocationAccuracyLevel? = null,
    val compassAccuracy: AccuracyLevel? = null,
    /** Current solar azimuth (clockwise from true north), if a fix exists. */
    val sunAzimuthDegrees: Float? = null,
    /** Current solar elevation above the horizon, if a fix exists. */
    val sunElevationDegrees: Float? = null,
) {
    val isReady: Boolean get() = status == QiblaStatus.READY || status == QiblaStatus.ALIGNED
}
