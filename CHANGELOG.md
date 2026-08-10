# Changelog

All notable changes to NexaSense are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added

- **Vertical (plumb) level mode** — the Level screen gains a horizontal /
  vertical toggle. In vertical mode the bubble centers when the device is
  held exactly upright (roll 0°, pitch 90°), so walls, edges and posts can be
  checked for plumb on the same screen; the readout shows the deviation from
  vertical and the haptic "level" pulse works in both modes.
- **Kaaba marker on the compass** — when Qibla is enabled, a small Kaaba icon
  (black cube with gold kiswah band) is drawn on the dial at the Qibla
  bearing instead of the plain triangle.

### Changed

- **Bottom navigation bar** — the app is now three tools only: Compass
  (home), Level and Settings, switched via a bottom navigation bar with a
  single instance per tab and preserved state.
- **Removed screens** — the home dashboard, Sensors list, Sensor detail,
  Diagnostics and About screens are gone, together with the status display
  and the developer-mode block; unsupported sensor lists are no longer
  shown anywhere.
- **Settings cleanup** — the About button and the developer-mode toggle were
  removed.

### Fixed

- **Settings accordion** — every settings section now starts collapsed and
  only one section is open at a time: tapping a header expands it and
  collapses the previously open one. The screen stays compact and the reset
  button remains always visible.
- **Instrumented Compose UI tests** — declared `kotlinx-coroutines-test`
  explicitly (androidTest + debug) so its `META-INF/services` exception-
  handler registration is packaged; without it, `runTest` fails with
  "Exception handler was not found via a ServiceLoader" on every device.
- **Critical launch crash (FC) on every release APK** — the app crashed
  instantly on open for two independent reasons that unit tests never caught:
  1. `CompassEngineImpl` read `AppSettings.northReference` in a field
     initializer before the `settings` field was assigned (Kotlin field-order
     NPE), crashing `AppContainerImpl` construction during
     `Application.onCreate`. Field order fixed, and a new construction
     regression test (`CompassEngineConstructionTest`) guarantees it stays
     fixed.
  2. The **night theme** (`values-night/themes.xml`) still descended from
     `android:Theme.Material.NoActionBar`, which throws
     "You need to use a Theme.AppCompat theme" inside `AppCompatActivity` —
     so dark-mode devices crashed at `setContentView` even after fix 1. Both
     day and night themes now descend from `Theme.AppCompat.DayNight.NoActionBar`.
  Verified live on a POCO F5 (marble) via adb: the app now launches, stays
  in the foreground, and produces zero FATAL logcat entries.

## [1.0.2] - 2026-08-10

### Added

- **Local crash history** — uncaught crashes are recorded on-device (the app
  has no INTERNET permission, so they can never leave it) and shown in the
  Diagnostics screen with a clear-history action; the handler is installed
  before the dependency container so even construction failures are captured.
- **Sensors-blocked detection** — when a sensor exists but delivers no data
  (the system "Sensors Off" toggle or a per-app sensor permission on
  AOSP/GrapheneOS), the compass and level screens now explain this clearly
  instead of showing a generic "unavailable".
- **New professional launcher icon** — a Google-2026-style compass mark on a
  modern diagonal blue gradient (sky → vivid → deep indigo), with a white
  ring, white north needle and light-blue south needle, a dedicated
  monochrome silhouette for Android 13+ themed icons, and bilaterally
  symmetric artwork that needs no mirroring for RTL locales.
- **Compass styles & granular customization** — three professional dial
  styles (Classic, Azimuth with a numbered degree ring, Minimal) switchable
  from Settings, plus per-element toggles: cardinal labels, degree ticks,
  degree numbers, the heading readout, the north-reference badge, the
  source/declination details and the accuracy/calibration panel. All 13 new
  settings are localized in all 24 languages and persist via DataStore.
- **Heading accessibility** — the compass dial's live region announces the
  heading (cardinal direction) to TalkBack exactly when it changes, without
  spamming at sensor rate.

### Fixed

- **Startup crash hardening** — sensor discovery, the home screen's
  capability check and the per-app locale call can no longer crash the app at
  launch when a device's sensor HAL is partial or misbehaving (common on
  AOSP/custom ROMs); failures now degrade gracefully (capabilities reported
  unavailable, device locale kept) instead of force-closing.
- **Refresh button accessibility label** — the toolbar refresh action was
  announced as "Cancel" to screen readers; it now says "Refresh" in all 24
  locales.

### Performance

- **Compass dial** — cardinal labels are measured once and reused, instead of
  re-measuring 8 text layouts on every animation frame inside the Canvas
  draw lambda.

### Fixed

- **Edge-to-edge (Android 15/16)** — scrollable screens now reserve the system
  navigation bar inset, so content no longer runs underneath the gesture bar
  with the enforced edge-to-edge from targetSdk 35+.
- **DataStore robustness** — both settings and calibration stores now use a
  `ReplaceFileCorruptionHandler`: a corrupt preferences file is reset instead
  of throwing on every read/write.
- **Lint cleanup** — added explicit Android 12+ data-extraction and legacy
  full-backup rules, and silenced the redundant adaptive-icon folder notice
  (AAPT2 rejects unversioned `mipmap-anydpi` for the API 33 monochrome
  element); lint is now free of app-code findings.

### Added

- **Level haptic on centered** — the bubble level fires one short haptic
  pulse when the bubble enters the centered zone (≤1.5° in both axes), so a
  surface can be leveled without watching the screen; gated by the global
  haptics setting with a 2 s cooldown (mirrors the Qibla alignment haptic).

### Changed

- **Qibla sun position caching** — the solar azimuth/elevation is recomputed
  at most every 30 s instead of on every sensor event (the sun moves
  ~0.004°/s), removing needless trig work from the sensor-rate update path.
- **Dependency refresh (low-risk)** — navigation-compose 2.9.0 → 2.9.8,
  appcompat 1.7.0 → 1.7.1, coroutines 1.10.2 → 1.11.0, and the test-only
  stack (ext-junit 1.3.0, espresso 3.7.0, runner/rules 1.7.0). Major
  toolchain bumps (AGP 9, Kotlin 2.4, Compose BOM 2026) are deliberately
  deferred and documented in the new `docs/dependencies.md`.
- **Security review** — manifest audit (single launcher-exported activity,
  one coarse-location permission, no network/cleartext, explicit backup
  rules) and diagnostic-report review (hardware/capability info only, no
  coordinates or personal data): no findings, no changes required.
- **String-resource audit** — verified zero unescaped apostrophes across all
  24 locales (aapt2 rejects them); the French string from the previous
  round is properly escaped.

### Added

- **Sun-over-Kaaba Qibla verification** — `SolarPositionCalculator` (NOAA
  algorithm, pure Kotlin, ≈1° accuracy) shows the current solar
  azimuth/elevation on the Qibla card and flags the twice-yearly moment when
  the sun aligns with the Qibla bearing, enabling compass-free shadow
  verification. Validated against the 2026-05-28/07-16 transit events
  (Berlin 136.9° vs 136.5°, New York 58.6° vs 58.4°) and a randomized
  end-to-end alignment sweep; the sweep caught and fixed a sign bug in
  western-longitude solar-time arithmetic (Kotlin `%` keeps the dividend's
  sign).
- **Predictive back (Android 13+)** — `enableOnBackInvokedCallback` is
  declared, so the system back-gesture animation is used.
- **System language picker (Android 13+)** — the app now declares
  `android:localeConfig` with all 24 locales, so it appears in the system
  per-app language settings.
- **Property-based invariant tests** — 11 seeded randomized tests covering
  angle normalization/idempotence, angular difference, short-way lerp,
  cardinal sectors, smoother convergence and the 0°/360° seam, WMM2025 field
  plausibility/smoothness, and geodesic distance symmetry + exact meridian/
  equator bearings (independent Python Vincenty cross-check).
- **F-Droid publishing** — `fastlane` store metadata (en-US, ar, fa, ur) and
  a submission guide in `docs/fdroid.md`.
- **Display-rotation compensation** — the compass heading now stays in the
  user's frame of reference with auto-rotate enabled (sensors report in the
  device's natural frame), matching the level engine. The Qibla marker stays
  consistent automatically.
- **Large-screen sizing** — the compass dial and bubble level are capped at
  480 dp so they stay comfortable on tablets and in landscape instead of
  stretching full-width.

### Added

- **WMM2025 declination model** — the True North feature now computes magnetic
  declination with the current official NOAA/BGS World Magnetic Model 2025
  (valid 2025.0–2030.0), implemented in pure Kotlin with the official
  coefficients embedded verbatim and verified against all 100 NOAA test
  points (max error 0.005°). Replaces `GeomagneticField`, which embeds the
  expired WMM2020 model. `GeomagneticField` remains as a defensive fallback.

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

## [1.0.1] - 2026-08-10

### Added

- **WGS84 geodesic Qibla calculation** — the bearing and distance to the Kaaba
  now use the Vincenty inverse formula on the WGS84 ellipsoid (the reference
  geodesy algorithm), removing the spherical approximation error (up to
  ~0.2° bearing / tens of km on the longest paths) with an automatic spherical
  fallback for nearly antipodal points. Validated against independently
  computed reference values for Berlin, London, New York, Tokyo, Sydney,
  Jakarta, Reykjavik, Casablanca, ±179° longitudes and near the North Pole.
- **Release performance**: R8 minification + resource shrinking for the
  release APK (smaller, obfuscated) with readable stack traces kept
  (`SourceFile`/`LineNumberTable`), and a startup **baseline profile**
  (bundled via `androidx.profileinstaller`) that pre-compiles the cold-start
  hot path on Android 13+.

### Fixed

- **Rotated soft-iron calibration applied the wrong matrix**: the least-
  squares ellipsoid fit stored the Cholesky factor `L` (with `P = LLᵀ`), but
  the whitening transform is `Lᵀ` — `|Lᵀv|² = vᵀPv`. The correction now
  stores the transpose, so rotated soft-iron distortion (off-axis coupling)
  is fully removed instead of leaving a large residual spread. The sphere and
  axis-aligned paths were unaffected (there `L` is diagonal, so `Lᵀ = L`);
  the rotated case is now covered by a dedicated test.

### Planned

- Complementary/Kalman/Madgwick/Mahony fusion options, relative altitude via
  pressure, estimated-pressure module (clearly separated from the hardware
  barometer), sensor FIFO batching for future continuous low-rate streams.
