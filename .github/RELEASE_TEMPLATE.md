## NexaSense v{{VERSION}}

**Professional compass, level and sensor diagnostics for AOSP and custom ROMs — offline-first, privacy-first, and independent of any vendor framework.**

### Highlights

- 🧭 **Compass** with 360° dial, smooth wrap-around heading, adaptive magnetic-interference detection and in-app magnetometer calibration (hard/soft iron).
- 🕋 **Qibla Direction** — fully local great-circle bearing to the Kaaba, live marker on the dial, turn guidance with a ±2° alignment threshold, optional distance and haptics. Disabled by default; location is requested only while enabled and never leaves the device.
- 🧭 **North Reference** — Automatic / True North / Magnetic North with effective-reference resolution always shown (e.g. *Automatic · True North*).
- 📏 **Level** — bubble level from the accelerometer alone, display-rotation aware, zero-point calibration.
- 📡 **Sensor discovery & diagnostics** — every sensor the HAL exposes, with full metadata, live raw values, measured sampling rate and a shareable diagnostic report (no personal data).

### Features

- Compass sources by priority: Rotation Vector → Geomagnetic Rotation Vector → Accelerometer + Magnetometer → *unavailable* (never faked).
- Magnetic declination via `GeomagneticField`, cached by location/time — never per sensor event.
- Adaptive interference thresholds, no single worldwide constant.
- Settings: theme (System/Light/Dark + dynamic color), language (25 languages), North Reference, Qibla options, smoothing, sensor rate, haptics, keep-screen-on, developer mode.
- Sensors registered only while a screen is visible — no background collection.

### Compatibility

- **Android 12 – 16** (API 31 – 36), compile/target SDK 36, min SDK 31.
- AOSP ROMs: LineageOS, crDroid, Evolution X, Pixel Experience, GrapheneOS and stock MIUI/HyperOS devices.
- Qualcomm Snapdragon and MediaTek, including Xiaomi / POCO / Redmi phones on AOSP-based ROMs.
- No Google Play Services required — fully offline.
- Missing sensors degrade gracefully (e.g. POCO F5 shows `Barometer: Not available`) — never a crash, never a fake value.

### Localization

English, العربية, Deutsch, Français, Español, Português, Italiano, Türkçe, Русский, Українська, Polski, Nederlands, Bahasa Indonesia, Bahasa Melayu, हिन्दी, বাংলা, اردو, فارسی, 简体中文, 繁體中文, 日本語, 한국어, Tiếng Việt, ไทย — with proper RTL support and per-app language selection (System Default follows the device language).

### UI

Material 3 design system (centralized color/shape/typography/dimensions), dynamic color on Android 12+, light/dark/system themes, adaptive icons, TalkBack-friendly content descriptions and 48 dp touch targets.

### Privacy

Offline-first. No internet, analytics, tracking, ads or telemetry. The only runtime permission is coarse location, requested only for True North/Qibla, and it is never sent anywhere. The diagnostic report contains hardware and configuration info only.

### Known limitations

- The first release APK is signed with the **debug key** until a production keystore is configured (see `docs/development.md`); install it by enabling *Install unknown apps* on the device.
- R8/minification is disabled in this release; it will be enabled in a follow-up after full validation.
- True North / Qibla require a location fix; without one they show a clear "Location required" state.
- Instrumented UI tests require a connected device/emulator (`./gradlew connectedDebugAndroidTest`).

### Installation

Download `NexaSense-v{{VERSION}}-release.apk` below, enable *Install unknown apps* for your browser/file manager, and open the file. No permissions are requested at install time; location is requested only when you enable True North or Qibla.

### Links

- Repository: https://github.com/Alaa91H/NexaSense
- Docs: `docs/` in the repository (architecture, sensors, compass, calibration, qibla, compatibility, troubleshooting, development)
- License: Apache 2.0
