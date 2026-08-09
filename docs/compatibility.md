# Compatibility

## Supported devices and ROMs

NexaSense targets any device exposing the standard Android Sensor Framework:

- **SoCs**: Qualcomm Snapdragon, MediaTek, Exynos, Tensor, Unisoc, ...
- **ROMs**: AOSP, LineageOS, crDroid, Evolution X, Pixel Experience, GrapheneOS,
  stock MIUI/HyperOS-based devices, ...
- **Android versions**: 12 – 16 (API 31 – 36). All framework APIs used are
  version-guarded where required.

The POCO F5 (Snapdragon 7+ Gen 2) is the reference validation device; it has no
pressure sensor, so the app must show `Barometer: Not available` and work
normally.

## Design rules that make it compatible

- **No vendor assumptions**: no Xiaomi/MIUI/HyperOS proprietary APIs, no
  assumed sensor names, no assumed HAL implementation, no assumed sensor ids.
- **Runtime discovery everywhere**: capabilities are computed from
  `SensorManager` on every launch; a feature is only "available" if its
  sensors were actually found.
- **Graceful degradation**: missing sensors, failed registrations, inaccurate
  readings and permission denials produce a clear UI state, never a crash.
- **No hardcoded thresholds**: interference analysis is adaptive; calibration
  is derived from the device's own data.

## Differences across ROMs you may observe

| Symptom | Why | NexaSense behavior |
| --- | --- | --- |
| Sensor names differ ("BMI160", "lsm6dso Accelerometer") | different HALs/drivers | shown as-is; type id decides the feature |
| Duplicate sensors of the same type (wake + non-wake) | common on Qualcomm devices | listed; non-wake-up instance preferred for streaming |
| Vendor-only sensors (e.g. `samsung.proximity`) | proprietary extensions | listed as UNKNOWN kind, never used for features |
| Missing rotation vector despite magnetometer present | broken/hidden HAL fusion | falls back to accel+magnetometer tilt compensation |
| Magnetometer requires "figure-eight" on first boot | HAL calibration state | calibration status + accuracy shown; user can calibrate in-app |
| Uncalibrated vs calibrated magnetometer variants | API 18+ | `MAGNETIC_FIELD` (calibrated) is used for headings |

## Qibla & location compatibility

- **No Google Play Services**: location comes from the standard
  `LocationManager` (network provider), so the Qibla and True North features
  work on AOSP ROMs without GMS. If no location provider exists on the ROM,
  the features show "Location required" — they never fall back to a fake
  position.
- **Location is opt-in**: requested only when Qibla or True North is enabled.
  Disabling the feature releases the listener immediately; updates are
  distance-thresholded (≈ 50 m) so there is no continuous GPS drain.
- **Offline**: Qibla bearings and distances are computed locally from fixed
  Kaaba coordinates; no server is ever contacted.

## Device quirks

Any device-specific workaround must be registered in
`DeviceQuirkRegistry` (core) and keyed by
`manufacturer:model:device`. The registry is currently **empty by design** —
no workaround has been proven necessary. This keeps fixes for one device from
changing behavior on all others.

## Reporting a device issue

Open an issue with the **diagnostic report** (Diagnostics → Copy diagnostic
report) plus a short description of the problem. The report includes the
sensor list, vendors, capabilities and calibration status — no personal data.
