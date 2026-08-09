# Development

## Toolchain

- JDK 17
- Android SDK: platform 36, build-tools 36
- Gradle 8.13 (wrapper included)

## Quick start

```bash
git clone <repo> && cd NexaSense
# local.properties -> sdk.dir=...  (or export ANDROID_HOME)
./gradlew assembleDebug
```

## Project layout

```
domain/          pure Kotlin: models, math, ports, engines + unit tests
core/            Android adapters (SensorManager, location, permissions, logging)
data/            engine implementations, DataStore stores, report factory
presentation/    Compose UI + ViewModels (Material 3)
app/             MainActivity, Application, DI container, manifest
docs/            this documentation set
.github/         CI workflows
```

## Adding a feature (example: Barometer)

1. **domain**: model (e.g. `PressureReading`) + port (`PressureEngine`) +
   pure logic (`BarometricFormula` — and note the absolute/relative altitude
   distinction and reference-pressure requirement).
2. **core**: nothing unless a new framework API is needed.
3. **data**: `PressureEngineImpl` collecting `SensorKind.PRESSURE` events.
4. **presentation**: screen + ViewModel observing the engine; capability
   detection (if `PRESSURE` not discovered → "Not available").
5. **tests**: pure math in `domain`, then run:

```bash
./gradlew test lint assembleDebug
```

## Conventions

- `domain` must stay Android-free; keep heavy math pure and unit-tested.
- Sensors register only via `setActive()` from the UI lifecycle effect.
- All user-visible strings live in `presentation/src/main/res/values*/`.
- No `TODO`/`FIXME`; no fake values; quirks go through `DeviceQuirkRegistry`.

## Checks

```bash
./gradlew test                      # unit tests
./gradlew lint                      # Android lint (errors fail the build)
./gradlew assembleDebug             # debug APK
./gradlew assembleDebugAndroidTest  # instrumented test APK
./gradlew connectedDebugAndroidTest # run instrumented tests (device required)
```

## Release

1. Bump `versionCode`/`versionName` in `app/build.gradle.kts` (single source
   of truth — the About screen reads them from `BuildConfig`).
2. Update `CHANGELOG.md`.
3. Commit, tag `vX.Y.Z`, push the tag. The `release.yml` workflow validates,
   builds the release APK, creates the GitHub Release and attaches
   `NexaSense-vX.Y.Z-release.apk` automatically.

### Release signing

No keystore or password lives in the repository. The `release` signing
config reads these environment variables (GitHub Actions secrets in CI):

```
NEXASENSE_KEYSTORE_PATH
NEXASENSE_KEYSTORE_PASSWORD
NEXASENSE_KEY_ALIAS
NEXASENSE_KEY_PASSWORD
```

In CI, set `NEXASENSE_KEYSTORE_BASE64` to the base64-encoded keystore; the
workflow decodes it into a temporary file. **Without a configured keystore the
release APK is signed with the debug key** — fine for a first public release,
but not production signing; document this in the release notes until a real
keystore is added.

To build locally with your own keystore:

```bash
export NEXASENSE_KEYSTORE_PATH=/path/to/nexasense-release.jks
NEXASENSE_KEYSTORE_PASSWORD=... NEXASENSE_KEY_ALIAS=... NEXASENSE_KEY_PASSWORD=... \
  ./gradlew assembleRelease
```

## Localization

- Source of truth: `presentation/src/main/res/values/strings.xml` (English).
- One `values-XX/strings.xml` per language; every locale must contain **all**
  keys (Android lint enforces this).
- Adding a language:
  1. Add the enum value + `localeTag` in
     `domain/.../model/AppSettings.kt` (`LanguagePreference`).
  2. Add `settings_language_<name>` to every locale file (native name).
  3. Create the new `values-XX/strings.xml` with a full translation.
  4. Map the locale in `MainActivity.applyLanguage()` (automatic — it uses
     `localeTag`).
- RTL (ar, ur, fa) is handled by the Android resource system; do not
  hardcode layout directions.
