package com.nexasense.domain.model

/**
 * Non-personal device information used by the diagnostic report.
 * No location, accounts, or personal identifiers are ever collected.
 */
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val device: String,
    val product: String,
    val androidVersion: String,
    val sdkInt: Int,
    val buildFingerprint: String,
    val kernelVersion: String,
    val board: String,
    val hardware: String,
    val buildTags: String,
)
