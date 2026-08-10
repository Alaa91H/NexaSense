# NexaSense — AOSP Sensor Suite

![Build](https://img.shields.io/github/actions/workflow/status/Alaa91H/NexaSense/ci.yml?branch=main&label=CI)
![Latest release](https://img.shields.io/github/v/release/Alaa91H/NexaSense)
![License](https://img.shields.io/github/license/Alaa91H/NexaSense)
![Android](https://img.shields.io/badge/Android-12%E2%80%9316%20(API%2031%E2%80%9336)-3ddc84)

**Professional compass, level and sensor diagnostics for AOSP and custom ROMs.**

NexaSense is a free, open-source replacement for the Compass & Level tools found
in MIUI / HyperOS. It is built exclusively on the standard Android Sensor
Framework, makes **no assumptions about hardware**, and never displays fake
values. If a sensor is missing, the feature is disabled and reported as
unavailable — nothing is simulated, estimated, or "borrowed" from GPS.

> التطبيق بديل مستقل وعالي الجودة لبوصلة وميزان MIUI/HyperOS، مخصص لأجهزة AOSP
> والرومات المخصصة، ولا يعتمد على أي واجهات برمجية خاصة بـ Xiaomi.

---

## Features

| Feature | Description |
| --- | --- |
| 🧭 **Compass** | 360° dial, magnetic heading, smooth wrap-around transitions, adaptive interference detection, hardware calibration (hard/soft iron). |
| 🧭 **North Reference** | Automatic / True North / Magnetic North. True North uses the current official **WMM2025** geomagnetic model (pure-Kotlin, NOAA-verified to < 0.005°), computed on-device from a location fix. Automatic falls back to Magnetic North when no declination is available — the effective reference is always shown. |
| 🕋 **Qibla Direction** | Offline, local **WGS84 geodesic** bearing to the Kaaba (Vincenty inverse formula, spherical fallback), relative turn guidance (±2° alignment threshold), optional distance, haptics on alignment, fully optional and disabled by default. |
| 📏 **Level** | Bubble level from the accelerometer alone (no gyroscope required), portrait/landscape aware, zero-point calibration. |
| 📡 **Sensor discovery** | Full runtime discovery: accelerometer, gyroscope, magnetometer, rotation vectors, pressure, light, proximity, step counters, humidity, temperature and every other sensor the HAL exposes. |
| 🔬 **Diagnostics** | Capability detection, per-sensor raw values, *measured* sampling rate, accuracy, HAL metadata, shareable diagnostic report (no personal data). |
| ⚙️ **Settings** | Theme (system/light/dark + dynamic color), language (25 languages, RTL-aware), North Reference, Qibla options, smoothing, sensor rate, haptics, keep-screen-on, developer mode. |

## Latest Release

**NexaSense v1.0.0** — the first official release: [GitHub Release](https://github.com/Alaa91H/NexaSense/releases/latest) · [v1.0.0 tag](https://github.com/Alaa91H/NexaSense/tree/v1.0.0)

The release APK is attached to the GitHub Release. The v1.0.0 APK is signed
with the debug key until a production keystore is configured (documented in
the release notes and [docs/development.md](docs/development.md)).

## Screenshots

Screenshots of the Compass, Level, Sensors, Diagnostics, Settings and About
screens will be added here with the first stable release.

## Localization

Fully localized UI with **25 languages** and per-app language selection
(Settings → Language; System Default follows the device language):

English · العربية (RTL) · Deutsch · Français · Español · Português · Italiano ·
Türkçe · Русский · Українська · Polski · Nederlands · Bahasa Indonesia ·
Bahasa Melayu · हिन्दी · বাংলা · اردو (RTL) · فارسی (RTL) · 简体中文 · 繁體中文 ·
日本語 · 한국어 · Tiếng Việt · ไทย

All user-facing strings live in `res/values*/strings.xml` — nothing is
hardcoded. RTL locales (Arabic, Urdu, Persian) get full layout mirroring and
correct text alignment automatically through the Android resource system.

## Theme & UI

- **Material 3** design system: centralized color schemes, shapes, typography
  and spacing in `presentation/.../theme/`.
- **Theme**: System (follows `isSystemInDarkTheme()` live) / Light / Dark.
- **Dynamic color** (Material You) on Android 12+ when available, with
  functional colors (interference, alignment, accuracy) kept distinguishable.
- Adaptive launcher icon with a themed (monochrome) layer on Android 13+.
- Accessibility: TalkBack-friendly content descriptions, 48 dp touch targets,
  font scaling, and status never conveyed by color alone.

## Architecture

```
domain        pure Kotlin — models, math, ports, engines (no Android imports)
core          Android adapters — SensorManager gateway, location, permissions,
              device info, logger
data          DataStore stores + compass/level engine implementations
presentation  Jetpack Compose UI (Material 3), ViewModels, navigation
app           MainActivity, manual dependency container, manifest
```

Dependency inversion keeps the domain layer free of Android framework types;
the sensor source, calibration, declination and fusion decisions are all pure
and unit-tested. See [docs/architecture.md](docs/architecture.md).

## Supported platforms

- **Android 12 – 16 (API 31 – 36)**, compile/target SDK 36, min SDK 31
- **AOSP ROMs**: LineageOS, crDroid, Evolution X, Pixel Experience, etc.
- **Devices**: Qualcomm Snapdragon and MediaTek, including Xiaomi / POCO / Redmi
  phones running AOSP-based ROMs
- **No Google Play Services required** — fully offline

The POCO F5 (and any device without a pressure sensor) shows
`Barometer: Not available` and works normally — a missing sensor never causes a
crash.

## Build

Requirements: JDK 17, Android SDK (platform 36).

```bash
./gradlew assembleDebug          # debug APK
./gradlew test                   # unit tests
./gradlew lint                   # Android lint
./gradlew assembleDebugAndroidTest   # instrumented test APK
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`. Install with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Testing

- **Unit tests** (`./gradlew test`): heading math (0°/90°/180°/270°/wrap),
  quaternion ↔ rotation matrix conversions, tilt-compensated fallback, level
  pitch/roll, calibration math, adaptive interference analysis, filters,
  sampling-rate estimation, source selection, declination, robustness
  (NaN/Infinity/degenerate inputs).
- **Instrumented tests** (`./gradlew connectedDebugAndroidTest`): end-to-end
  UI smoke tests and on-device sensor discovery/streaming tests against the
  real HAL.

## Troubleshooting

- **Compass shows "unavailable"** — the device exposes neither a rotation
  vector nor a magnetometer.
- **True North shows "Location required"** — enable the mode in Settings and
  grant the location permission; declination is only computed with a fix.
- **Qibla shows "Location permission required"** — grant the coarse-location
  permission from the Qibla card; everything is still computed locally.
- **"Magnetic interference detected"** — move away from speakers, magnets,
  metal desks; the thresholds are adaptive per-device.

See [docs/troubleshooting.md](docs/troubleshooting.md) and
[docs/compatibility.md](docs/compatibility.md).

## Privacy

Offline-first. No internet, analytics, tracking, ads, or telemetry. The only
runtime permission is coarse location, requested **only** when True North or
Qibla is enabled, and it is never sent anywhere. Qibla bearings and distances
are computed locally from fixed Kaaba coordinates. The diagnostic report
contains hardware information only — see [PRIVACY.md](PRIVACY.md) and
[docs/qibla.md](docs/qibla.md).

## License

Apache License 2.0 — see [LICENSE](LICENSE).
