package com.nexasense.core.diagnostics

import android.os.Build
import com.nexasense.domain.model.DeviceInfo
import com.nexasense.domain.port.DeviceInfoProvider

/** Non-personal device metadata for the diagnostic report. */
class DeviceInfoProviderImpl : DeviceInfoProvider {

    override fun deviceInfo(): DeviceInfo = DeviceInfo(
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        device = Build.DEVICE,
        product = Build.PRODUCT,
        androidVersion = Build.VERSION.RELEASE,
        sdkInt = Build.VERSION.SDK_INT,
        buildFingerprint = Build.FINGERPRINT,
        kernelVersion = System.getProperty("os.version") ?: "unknown",
        board = Build.BOARD,
        hardware = Build.HARDWARE,
        buildTags = Build.TAGS,
    )
}
