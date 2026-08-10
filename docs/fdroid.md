# F-Droid publishing guide

NexaSense is designed to be F-Droid-ready: Apache-2.0, no anti-features
(no ads, no trackers, no proprietary dependencies, no internet permission
at all), and a deterministic release build. This page documents the pieces
that already exist and what a maintainer needs to submit.

## What is already in place

- **License**: `LICENSE` (Apache-2.0) at the repository root.
- **No anti-features**: the app declares no INTERNET permission, does not
  use Google Play Services, Firebase, ads or analytics. The only permission
  is `ACCESS_COARSE_LOCATION`, requested at runtime purely for the True
  North declination (WMM2025) and Qibla features — this is optional, never
  required, and noted in `PRIVACY.md`.
- **Metadata**: `fastlane/metadata/android/` contains `title.txt`,
  `short_description.txt` and `full_description.txt` for `en-US`, `ar`,
  `fa` and `ur`. Missing locales fall back to `en-US`; add more directories
  to extend.
- **Store listing images**: place screenshots under
  `fastlane/metadata/android/en-US/images/phoneScreenshots/` (F-Droid
  recommends 1–3 screenshots, max 2 MiB each, no text overlays required).
- **Versioning**: `versionCode` and `versionName` live in
  `app/build.gradle.kts`; F-Droid uses `versionCode` to order versions.
  Keep it monotonically increasing (do not reuse codes across builds).

## Build configuration notes for F-Droid

- `minSdk 31`, `targetSdk 36` — within current F-Droid build capacity.
- The release build uses R8 (`minifyEnabled` + `shrinkResources`) with
  `proguard-rules.pro`; no reflection is used, so shrinking is safe.
- Release signing: F-Droid ignores the local signing config and signs with
  its own key. The `release.yml` workflow's keystore secrets
  (`NEXASENSE_KEYSTORE_*`) are used for GitHub Releases; the same APK can
  be rebuilt by F-Droid from source.
- **Reproducible builds** (recommended, optional): to let F-Droid verify
  byte-for-byte that the published APK matches the source, pin the exact
  Gradle/AGP/Kotlin versions (already pinned in `gradle/libs.versions.toml`
  and the wrapper), disable the build cache (`--no-build-cache`) when
  comparing, and attach the built APK to the GitHub Release (the
  `release.yml` workflow already does this). When production keystore
  secrets are set, the GitHub Release APK is signed with the project key —
  F-Droid rebuilds with its own key, so attach the unsigned/`assembleRelease`
  APK for comparison or publish the signing fingerprint.

## Submitting

1. Tag a release (e.g. `v1.0.1`) and push it; CI builds and attaches the
   APK to the GitHub Release.
2. Open a merge request against the
   [F-Droid data repository](https://gitlab.com/fdroid/fdroiddata) adding
   a `metadata/com.nexasense.android.yml` entry, or ask the IzzyOnDroid
   maintainers to pick the app up.
3. A typical minimal `fdroiddata` metadata file:

   ```yaml
   Categories:
     - Science & Education
   License: Apache-2.0
   AuthorName: NexaSense
   SourceCode: https://github.com/Alaa91H/NexaSense
   IssueTracker: https://github.com/Alaa91H/NexaSense/issues
   Summary: Compass, level and sensor diagnostics for AOSP
   Description: |
     Professional compass with WMM2025 true north, Qibla direction,
     bubble level and full sensor diagnostics. No ads, no trackers.
   AutoName: البوصلة
   ```

4. Test the metadata locally with `fdroid build --test` if you have the
   F-Droid server tools installed.
