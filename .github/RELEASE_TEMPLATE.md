## NexaSense v{{VERSION}}

**Professional compass and level for AOSP and custom ROMs — offline-first, privacy-first, and independent of any vendor framework.**

### Highlights

- ✨ **Google 2026 design language** — every icon is now a **Material Symbol** (Google's official 2026 icon set) instead of the deprecated Material Icons, and the whole app shares one set of Google-settings-style components: compact rows with a colored tonal icon container, a current-value label and a chevron that open a Google-style radio dialog on tap. Options, menus and properties look and behave identically on every screen.
- 🎨 **Material 3 Expressive redesign** — upgraded to the stable M3 Expressive release (Compose BOM 2026.06.01): a fully rounded expressive shape scale, **Google Sans** (the official open-source Google typeface, bundled under SIL OFL) with tabular numerals for readouts, unified M3 motion tokens that animate settings accordions, chevrons, list reordering, dialog entrances and tab crossfades, tonal frameless `DataCard`s and `EmptyState` screens with icons, a **settings search bar** that filters every option live (Google Settings style), an **About screen** with runtime version and font/license attribution, and a Google-style bottom navigation where the active tab shows a **filled** icon that crossfades from the outlined one. Navigation is adaptive via **window size classes**: phones get the bottom bar, wider windows get a **Navigation Rail**, and directional icons mirror correctly in RTL.
- 🧭 **Compass** with a 360° dial, smooth wrap-around heading, adaptive magnetic-interference detection and in-app magnetometer calibration (hard/soft iron). Three professional dial styles (**Classic / Azimuth / Minimal**) switchable from Settings, with granular per-element toggles (cardinal labels, degree ticks, degree numbers, heading readout, north-reference badge, source/declination details, accuracy panel) and an **auto-hide** option that hides the numbers whenever the magnetic accuracy is low. All readouts and bottom details sit in **closed cards with fixed-width numeric slots**, so the digits changing as you move the phone never shift the text.
- 🌙 **Automatic night mode** — follows the device (System theme by default) with a deep-navy **sky gradient** background and a blue-tinted dark palette; light mode gets a soft daylight gradient. The app stays **always portrait**.
- 🕋 **Qibla Direction** — fully local WGS84 geodesic bearing to the Kaaba (Vincenty inverse formula on the WGS84 ellipsoid, spherical fallback), 🕋 marker on the dial (always upright), turn guidance with a ±2° alignment threshold, optional distance and haptics, plus a **sun-over-Kaaba shadow check**: when the sun transits the Kaaba (twice a year) the solar azimuth equals the Qibla bearing, so shadows verify the direction compass-free. Disabled by default; location is requested only while enabled and never leaves the device.
- 🔁 **Qibla recovers on its own** — the location engine no longer reports "Qibla unavailable" while a fix is still on its way (the status stays "calculating" during the request), and a failed fix is **retried automatically every 15 s** until one arrives or the screen is closed. The feature comes back by itself indoors, after a cold start, or when location services are switched on — no manual refresh needed. Covered by new `QiblaEngineImplTest` unit tests.
- 🧭 **North Reference** — Automatic / True North / Magnetic North with effective-reference resolution always shown (e.g. *Automatic · True North*).
- 📏 **Level** — automatically switches by how the device is held: flat shows a two-axis bubble (four directions); held upright shows a water/mercury **tube level** (left-right) with a **plumb gauge** whose needle shows the deviation-from-vertical on a degree scale. Every moving element (mercury bubble, plumb needle, flat bubble) **glides smoothly** instead of snapping, and reaching perfect level lights a **pulsing centered dot** in both modes. Feedback on center: a **graded haptic** that ramps up in strength as you approach level/plumb in **both** modes (proximity bands 8°→0.5°), plus a soft **confirmation chime** with its own settings toggle, independent of the haptics toggle. Zero-point calibration.

### Features

- Compass sources by priority: Rotation Vector → Geomagnetic Rotation Vector → Accelerometer + Magnetometer → *unavailable* (never faked).
- Magnetic declination from the current official **WMM2025** model (pure-Kotlin, NOAA-verified to < 0.005°), cached by location/time — never per sensor event.
- Adaptive interference thresholds, no single worldwide constant.
- Three-screen bottom navigation (**Compass / Level / Settings**) — one instance per tab, state preserved.
- Settings redesigned on the **Google Settings pattern**: single-choice options (Theme, Language, North Reference, Smoothing, Sensor rate, Compass style) are compact rows showing the current value and open a radio dialog on tap instead of long inline lists; every group and row carries a Material Symbol in a tonal rounded container, switch rows toggle on whole-row tap, and rows are separated by subtle dividers. Qibla keeps its master-switch group with sub-options hidden until enabled. Also: 24 languages (system per-app language picker on Android 13+), Qibla options, compass appearance toggles, haptics, level confirmation sound, keep-screen-on, reset.
- Startup is hardened against broken/partial sensor HALs (common on custom ROMs) — discovery and the per-app locale call degrade gracefully instead of crashing, and a stuck/blocked sensor (Sensors Off toggle, per-app sensor permission) is detected and explained.
- Sensors registered only while a screen is visible — no background collection; non-wake-up sensors preferred.
- Heading stays in the user's frame across display rotations (compensation), and the dial/level cap at 480 dp on large screens.
- 182 unit tests (math, geodesy, geomagnetic WMM2025 model, solar position, engines, calibration, robustness), lint-clean; instrumented UI tests run on a device.

### Compatibility

- **Android 12 – 16** (API 31 – 36), compile/target SDK 36, min SDK 31.
- AOSP ROMs: LineageOS, crDroid, Evolution X, Pixel Experience, GrapheneOS and stock MIUI/HyperOS devices.
- Qualcomm Snapdragon and MediaTek, including Xiaomi / POCO / Redmi phones on AOSP-based ROMs.
- No Google Play Services required — fully offline.
- Missing sensors degrade gracefully (e.g. POCO F5 shows `Barometer: Not available`) — never a crash, never a fake value.

### Localization

English, العربية, Deutsch, Français, Español, Português, Italiano, Türkçe, Русский, Українська, Polski, Nederlands, Bahasa Indonesia, Bahasa Melayu, हिन्दी, বাংলা, اردو, فارسی, 简体中文, 繁體中文, 日本語, 한국어, Tiếng Việt, ไทย — 24 languages with proper RTL support, per-app language selection (System Default follows the device language) and the app name localized per language (e.g. البوصلة, Kompass, Brújula, 指南针).

### UI

Material 3 design system (centralized color/shape/typography/dimensions), dynamic color on Android 12+, light/dark/system themes with the night-sky and daylight gradients, **Material Symbols** icon set throughout, a new Google-2026-style launcher icon (diagonal blue gradient, white compass mark, monochrome themed-icon variant for Android 13+), edge-to-edge on Android 15/16, predictive-back support, TalkBack-friendly content descriptions with a heading live region, and 48 dp touch targets. **Per-screen orientation**: the compass stays portrait always, while the level and settings rotate freely with the device.

### Privacy

Offline-first. No internet, analytics, tracking, ads or telemetry. The only runtime permission is coarse location, requested only for True North/Qibla, and it is never sent anywhere.

### Known limitations

- Release APKs are signed with the **debug key** until a production keystore is configured via GitHub Secrets (`NEXASENSE_KEYSTORE_*`; see `docs/development.md`); install by enabling *Install unknown apps* on the device.
- The release APK is minified with R8 (~3 MB) and bundles a startup **baseline profile** for faster cold start on Android 13+.
- True North / Qibla require a location fix; without one they show a clear "Location required" state and retry automatically every 15 s until one arrives.
- Instrumented UI tests require a connected device/emulator (`./gradlew connectedDebugAndroidTest`).

### Installation

Download `NexaSense-v{{VERSION}}-release.apk` below, enable *Install unknown apps* for your browser/file manager, and open the file. No permissions are requested at install time; location is requested only when you enable True North or Qibla.

### Links

- Repository: https://github.com/Alaa91H/NexaSense
- Docs: `docs/` in the repository (architecture, sensors, compass, calibration, qibla, compatibility, troubleshooting, development)
- License: Apache 2.0
