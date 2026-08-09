package com.nexasense.core.sensor

import android.os.Build

/**
 * Central registry for device-specific workarounds (e.g. a broken HAL on a
 * particular ROM). The suite deliberately ships with **no** workarounds — any
 * future quirk must be registered here, keyed by device, and must never change
 * behavior for other hardware. See docs/compatibility.md.
 */
object DeviceQuirkRegistry {

    /** One documented, isolated workaround. */
    interface Quirk {
        val deviceKey: String

        fun applies(): Boolean
    }

    private val quirks: List<Quirk> = emptyList()

    /** Whether any registered quirk applies to the current device. */
    fun hasActiveQuirk(): Boolean = quirks.any { it.applies() }

    /**
     * Stable key for the current device: `manufacturer:model:device` — used to
     * scope quirks so a workaround never leaks to other hardware.
     */
    fun currentDeviceKey(): String =
        "${Build.MANUFACTURER}:${Build.MODEL}:${Build.DEVICE}"
}
