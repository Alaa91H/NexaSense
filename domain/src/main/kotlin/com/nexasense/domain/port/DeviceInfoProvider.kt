package com.nexasense.domain.port

import com.nexasense.domain.model.DeviceInfo

/** Non-personal device metadata for the diagnostic report. */
interface DeviceInfoProvider {
    fun deviceInfo(): DeviceInfo
}
