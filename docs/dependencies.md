# Dependency versions & upgrade policy

All versions are pinned in `gradle/libs.versions.toml` (Gradle version
catalogs) and the Gradle wrapper. Pinning is deliberate: F-Droid and
reproducible builds need exact, repeatable toolchains, and every bump is
verified end-to-end (unit tests + lint + debug & release builds) before it
lands on `main`.

## Current pins (as of the v1.0.x line)

| Component          | Pinned | Latest noted by lint | Notes |
|--------------------|--------|----------------------|-------|
| Gradle             | 8.13   | 8.14.5               | wrapper |
| AGP                | 8.13.2 | 9.3.1                | major — deferred |
| Kotlin             | 2.2.10 | 2.4.10               | major — deferred |
| Compose BOM        | 2025.06.01 | 2026.06.01        | major — deferred |
| core-ktx           | 1.16.0 | 1.19.0               | major — deferred |
| lifecycle          | 2.9.1  | 2.11.0               | major — deferred |
| activity-compose   | 1.10.1 | 1.13.0               | major — deferred |
| datastore          | 1.1.2  | 1.2.1                | minor — deferred |
| navigation-compose | 2.9.8  | 2.9.8                | current |
| appcompat          | 1.7.1  | 1.7.1                | current |
| coroutines         | 1.11.0 | 1.11.0               | current |
| profileinstaller   | 1.4.1  | —                    | baseline profiles |

Test-only deps are kept fresh: `androidx.test.ext:junit 1.3.0`,
`espresso-core 3.7.0`, `test:runner 1.7.0`, `test:rules 1.7.0`.

## Why the majors are deferred

- **AGP 8 → 9 and Kotlin 2.2 → 2.4** are major toolchain changes (AGP 9
  removes deprecated APIs and changes plugin behavior; Kotlin 2.4 ships a new
  compiler). They deserve a dedicated migration round with a full device test
  pass, not a silent dependency bump.
- **Compose BOM 2025.06 → 2026.06** spans a year of Material/Compose changes
  that can alter rendering, insets and animation behavior.
- **core/lifecycle/activity/datastore** majors can change defaults (e.g.
  edge-to-edge handling, DataStore internals). DataStore stays pinned because
  the corruption-handler path added in this project should be re-verified
  against the new internals before bumping.

## Upgrade process (when a bump is warranted)

1. Edit `gradle/libs.versions.toml` and `gradle/wrapper/gradle-wrapper.properties`.
2. Run `./gradlew clean test lint assembleDebug assembleRelease` and
   `assembleDebugAndroidTest`; fix any failures.
3. Run the instrumented suite on a device: `./gradlew connectedDebugAndroidTest`.
4. Confirm the release APK still shrinks cleanly (R8) and the baseline
   profile is still bundled (`unzip -l app-release.apk | grep baseline`).
5. Commit with a message listing the exact versions changed.

CI (`.github/workflows/ci.yml`) runs the full unit test + lint + APK matrix
on every push, so a green CI run is the minimum bar for landing a bump.
