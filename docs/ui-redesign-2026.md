# NexaSense UI Restructuring — Google 2026 Design Language

> Status: implemented
> Scope: presentation module (theme, components, settings, navigation, compass, level)

## Goal

Make NexaSense look, feel and behave like Google's 2026 apps: unified Material
design system, the modern **Material Symbols** icon set (the official successor
to the deprecated `material-icons`), simplified settings organized the way
Google Settings does it, and one set of reusable components used by every
screen.

## Research basis (2026)

- **Material 3 / Material You** remains Google's design system. Dynamic color
  (Android 12+) is the default look of Google apps — already enabled here.
- **Material 3 Expressive** (May 2025 → Pixel rollout 2025/2026) is the current
  design direction: rounder shapes, softer surfaces, motion. It lands in
  Compose via `material3` 1.4+/1.5 (`MaterialExpressiveTheme`). This app pins
  Compose BOM 2025.06.01 (material3 1.3.2), so Expressive APIs are not yet
  available; the redesign adopts its *principles* (rounded tonal icon
  containers, softer cards) with stable APIs.
- **Icons**: Google deprecated `androidx.compose.material:material-icons-*`.
  The official guidance (developer.android.com, "Icons") is to use **Material
  Symbols** and download the vector drawables from Google Fonts / the official
  `google/material-design-icons` repo. All app icons are now Material Symbols
  (outlined style — the default; the M3 NavigationBar indicator conveys the
  selected state).
- **Settings pattern**: Google's own settings screens use grouped lists where
  every row has a leading icon inside a tonal rounded container, a title, an
  optional supporting text, and a trailing control (switch / chevron / value).
  Single-choice options open a dialog picker instead of an inline list, which
  simplifies the screen and matches Google's UX. Expandable groups keep
  dependent options (e.g. Qibla sub-options) hidden until their master switch
  is on.

## Changes

### 1. Icon system — Material Symbols (Google 2026)

- New vector drawables in `presentation/src/main/res/drawable/` generated from
  the official repo (`tools/fetch_material_symbols.py`):
  `ic_explore`, `ic_straighten`, `ic_settings`, `ic_arrow_back`, `ic_refresh`,
  `ic_restart_alt`, `ic_palette`, `ic_translate`, `ic_north_east`, `ic_mosque`,
  `ic_tune`, `ic_sensors`, `ic_compass_calibration`, `ic_location_on`,
  `ic_vibration`, `ic_volume_up`, `ic_screen_lock_portrait`.
- All `androidx.compose.material.icons.*` usages migrated to
  `painterResource(...)`; the deprecated `material-icons-extended` dependency is
  removed from `presentation/build.gradle.kts`.

### 2. Unified component set (`components/Components.kt`)

- **`SettingsListItem`** — the Google-settings row: leading Material Symbol in
  a 40dp tonal rounded container, title (`titleMedium`), optional supporting
  text (`bodyMedium`, `onSurfaceVariant`), optional trailing slot (switch,
  chevron, value), optional click. Every settings row in the app is now this
  component.
- **`SettingsOptionDialog`** — generic Google-style single-choice dialog
  (radio list) used by Theme, Language, North reference, Smoothing, Sensor rate
  and Compass style.
- **`SettingsSectionHeader`** — small section label above option groups.
- Existing `ScreenScaffold`, `StatusPill`, `GroupCard`, `SectionHeader`,
  `NavigationRow` kept; `NavigationRow` now builds on `SettingsListItem`.

### 3. Settings screen (Google Settings pattern)

- Single-choice options (Theme, Language, North reference, Smoothing, Sensor
  rate) are now compact rows showing the **current value** as trailing text +
  chevron, opening a dialog picker on tap — no more long inline lists
  (Language alone was 26 rows).
- Compass style moved into a dialog row; its toggles stay as switches.
- Qibla stays an expandable group (master switch; sub-options hidden until
  enabled) so the instrumented UI tests keep passing; Location permission and
  Status stay expandable groups.
- Every group/row now carries its Material Symbol in the tonal container;
  section headers and descriptions follow one typographic rhythm.

### 4. Navigation (bottom bar)

- Bottom nav uses Material Symbols `ic_explore` / `ic_straighten` /
  `ic_settings`; selected state is shown by the M3 indicator + primary color.

### 5. Cross-screen consistency

- Compass top-bar refresh and back arrows use Material Symbols.
- All cards share `GroupCard`'s shape/colors; readouts and pills unchanged.

## Compatibility

- No string resources were removed; no settings keys or domain logic changed.
- Instrumented UI tests (`QiblaUiTest`) keep passing: "Qibla Direction" is
  still a clickable header, "Enable Qibla" still a switch, sub-options hidden
  until enabled.

## Verification

- `./gradlew :presentation:compileDebugKotlin` (or `assembleDebug`) on a
  machine with the Android SDK; manual pass on device: nav bar, settings
  dialogs, Qibla flow, compass/level icons.
