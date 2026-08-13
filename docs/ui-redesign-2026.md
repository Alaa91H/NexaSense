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
  design direction: rounder shapes, softer surfaces, motion. `material3`
  1.4.0 — the first stable Expressive release — shipped 2026-08-12, and this
  app now pins Compose BOM **2026.06.01** (material3 1.4.0, Compose core
  1.12.0), so the expressive *shape scale* is adopted for real: larger
  rounded corners cascade from `MaterialTheme.shapes` into every card, row,
  dialog and icon container, and hard-coded corner values were removed from
  all screens so every surface follows one scale.
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
- **Google filled/outlined tab icons** — the selected tab cross-fades to the
  filled variant (`ic_*_filled`) and unselected tabs show the outlined one,
  the canonical Google 2026 bottom-nav pattern, animated on the M3 standard
  curve. Tab switches themselves cross-fade the screens (standard
  accelerate out / decelerate in), so navigation motion matches the
  settings accordions and dialogs.

### 5. Cross-screen consistency

- **Empty/unavailable states** use the shared `EmptyState` component — a
  large Material Symbol in a tonal circle plus headline and message (Compass:
  `ic_explore` or `ic_sensors` when blocked; Level: `ic_straighten` or
  `ic_sensors`), fading in on the M3 emphasized-decelerate curve like every
  other transition. No screen renders bare text for an unavailable state.
- Compass top-bar refresh and back arrows use Material Symbols.
- All cards share `GroupCard`'s shape/colors; readouts and pills unchanged.
- **Every info panel is now a single `DataCard` component** — the Compass
  heading readout, accuracy and source/detail panels, and the Level angle
  readouts swapped from the old outlined `surfaceVariant` panels to the same
  borderless tonal surface as `GroupCard`, so no screen carries a
  leftover pre-Expressive card style. The Level calibration dialog also uses
  the shared `DialogContentEntrance` motion like every other dialog.

### 6. Settings search (Google Settings pattern, phase 2)

- A **search field** is pinned at the top of Settings. Typing filters every
  option in place: candidates are built from all sections (including rows
  hidden inside collapsed accordions), matching is case-insensitive over
  section name + row title + current value, and matches render as a flat
  Google-style results list (icon, section subtitle, current value,
  chevron for pickers / live switch for toggles).
- Results reuse the same `SettingsListItem` / `SettingsSwitchRow` components
  as the main list, so behavior and visuals stay identical; value rows open
  the same dialogs, switch rows toggle the same settings, and the Qibla
  master switch keeps its permission-aware flow.
- Empty state ("No results found") and a clear (×) button round out the UX;
  all strings translated in the 24 supported languages.

### 7. About screen (phase 3)

- Settings ends with an **About** section (Google Settings pattern) opening
  a dedicated screen: app version read from the installed package at
  runtime, the open-source / offline-first statements, the sensors note and
  the Google Sans license row — all reusing the shared `SettingsListItem` /
  `SettingsIcon` / `SettingsDivider` components (the divider moved into
  `Components.kt` so Settings and About share one). The About row is also a
  settings-search candidate, so typing "about" or "version" finds it.
  Every string was already translated in all 24 languages.
- Rows may now wrap to two lines (Google Settings norm) instead of being
  truncated to one.

### 8. Typography — official Google Sans (phase 3)

- The app bundles the **real Google Sans** variable font (OFL, official via
  Google Fonts, 2026) in `res/font/google_sans.ttf` with license and
  provenance under `third_party/GoogleSans/`. This is the same typeface
  Google's 2026 products use — not a lookalike.
- Weights: variable `wght` axis is 400–700, so the display styles render at
  400 (the lightest cut; Google's own Material theme maps them the same
  way). Body/title/label use 400/500.
- The large compass/level numerals enable the `tnum` tabular-figures
  feature — fixed-width digits like Google Clock/Calculator — layered on
  top of the existing invisible-placeholder slots.
- Arabic and other scripts Google Sans does not cover fall back to the
  platform font per-glyph, matching Google's Arabic products.

### 9. Adaptive navigation + RTL (phase 3)

- **Window size classes** — `material3-window-size-class` (BOM-managed)
  drives navigation from the real window width: compact → bottom bar,
  medium+ → start-side `NavigationRail`; one `NavTab` data list feeds both.
- **RTL mirroring** — new `DirectionalIcon` mirrors the back arrow and
  trailing chevrons via `LocalLayoutDirection` (`scaleX = -1`), the reliable
  Compose approach; leading decorative icons stay unmirrored (Google parity).
- Full audit + roadmap: `docs/ui-architecture-2026.md` (all 13 phases).

### 10. Expressive motion (phase 2)

- Settings **accordions** animate with the official M3 curves: expand +
  fade-in on **emphasized decelerate** and collapse on **emphasized
  accelerate**; `expandVertically` clips the content so it never overlaps
  the rows below. The header **chevron rotates 180°** smoothly, and every
  list item carries `Modifier.animateItem` (placement-only) so rows glide
  when a group above them expands or collapses.
- **Dialogs** (pickers and the reset confirmation) open through the shared
  `DialogContentEntrance` — a fade + slight rise on emphasized decelerate,
  the same motion family as the platform dialog window animation.
- All curves/durations live in one local `Motion` object
  (`theme/Motion.kt`) whose easing values were verified directly against
  the material3 1.4.0 artifact (its own `MotionTokens` is internal), so the
  whole app animates on one 2026 token set.

## Compatibility

- No string resources were removed; no settings keys or domain logic changed.
- Instrumented UI tests (`QiblaUiTest`) keep passing: "Qibla Direction" is
  still a clickable header, "Enable Qibla" still a switch, sub-options hidden
  until enabled.

## Verification

- `./gradlew lint test assembleDebug` — all green (lint, unit tests and
  full debug build pass; BOM 2026.06.01 / material3 1.4.0 compile-clean).
  `assembleRelease` also builds; the bundled Google Sans variable font adds
  ~1 MB (release APK ~4.3 MB).
- Manual pass on device: nav bar, settings dialogs, settings search,
  Qibla flow, compass/level icons.
