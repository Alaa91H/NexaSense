# Changelog

All notable changes to NexaSense are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.0.0] - 2026-08-09

### Added

- **Compass engine** with source priority: Rotation Vector →
  Geomagnetic Rotation Vector → Accelerometer + Magnetometer (tilt-compensated)
  → unavailable. Never faked.
- **True North** via `GeomagneticField` declination; location requested only
  when the user enables the mode.
- **Magnetic interference analysis** with adaptive, per-device thresholds.
- **Magnetometer calibration** (hard/soft iron, min/max ellipsoid fit) with
  live progress, persistence via DataStore and reset.
- **Level** from the accelerometer only (no gyroscope), display-rotation aware,
  zero-point calibration.
- **Sensor discovery** for every sensor the HAL exposes, with full metadata:
  name, vendor, version, type, resolution, range, power, delays, wake-up,
  dynamic, reporting mode, FIFO.
- **Sensor detail / raw viewer** with live values, accuracy, timestamps and
  **measured** sampling rate (never assumed from the requested delay).
- **Diagnostics** with capability detection and a shareable text report
  (hardware info only, no personal data).
- **Settings**: theme (system/light/dark + dynamic color), language
  (25 languages with RTL), North Reference, Qibla options, smoothing, sensor
  rate, haptics, keep-screen-on, developer mode, reset.
- **Architecture**: multi-module Clean Architecture
  (`domain` / `core` / `data` / `presentation` / `app`), dependency inversion,
  Material 3 UI, DataStore, Coroutines/Flow.
- **Testing**: 85+ unit tests (math, engines, calibration, robustness),
  instrumented UI + on-device sensor tests, GitHub Actions CI
  (`test`, `lint`, `assembleDebug`).
- **Documentation**: docs/architecture, sensors, compass, calibration,
  compatibility, troubleshooting, development.

## [1.0.0] - 2026-08-09 (first official release)

### Added

- **Localization**: 21 new languages (fr, es, pt, it, tr, ru, uk, pl, nl, in,
  ms, hi, bn, ur, fa, zh, zh-TW, ja, ko, vi, th) — 25 in total, with
  per-app language selection via AndroidX AppCompat locale APIs and proper
  RTL for Arabic, Urdu and Persian.
- **Centralized Material 3 design system**: `theme/Color.kt`, `Theme.kt`,
  `Type.kt`, `Shapes.kt`, `Dimensions.kt`; dynamic color on Android 12+.
- **About screen**: version name/code and build type from the single
  BuildConfig source, crash-safe GitHub link.
- **Themed launcher icon** (monochrome layer for Android 13+).
- **Release pipeline**: tag-triggered `release.yml` that validates, builds the
  release APK and creates the GitHub Release with the artifact attached;
  optional production signing via GitHub Secrets with a documented debug-key
  fallback for this first release.
- **Release badges and Latest Release section** in the README.

### Fixed

- (First release — nothing to fix yet.)

## [1.1.0] - 2026-08-09

### Added

- **North Reference system** (`AUTOMATIC` default / `TRUE_NORTH` /
  `MAGNETIC_NORTH`) with effective-reference resolution, always shown in the
  compass header (e.g. `Automatic · True North`).
- **Qibla Direction**: local, offline great-circle bearing to the Kaaba
  (fixed coordinates 21.422487° N, 39.826206° E), relative turn guidance with
  a ±2° alignment threshold, optional distance, optional haptics on alignment
  (cooldown-gated single pulse), live marker on the compass dial and a Qibla
  card. Disabled by default; location requested only while enabled.
- **Declination cache** — `GeomagneticField` evaluated only on significant
  location/time change, never per sensor event.
- **Location updates** distance-thresholded (≈ 50 m / 15 s) so Qibla
  recalculation follows significant movement only.
- **Location accuracy separated from compass accuracy** in the Qibla card and
  diagnostic report; user coordinates are never included in the report.
- **New settings**: North Reference (Automatic/True/Magnetic) and Qibla
  (enable, show on compass, show card, show distance, haptics), persisted in
  DataStore with migration from the legacy `compass_mode` key.
- **Testing**: QiblaCalculator (bearings, distances, high latitudes, poles,
  International Date Line, relative angle, alignment, north-reference
  conversion), NorthReferenceResolver, DeclinationCache, angle-alias
  normalization, plus instrumented UI tests for the Qibla/north-reference
  settings flow.
- **Docs**: `docs/qibla.md`, updated architecture/compass/compatibility.

## [Unreleased]

- Planned: complementary/Kalman/Madgwick/Mahony fusion options, relative
  altitude via pressure, estimated-pressure module (clearly separated from the
  hardware barometer).
