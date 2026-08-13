# NexaSense — UI/UX Architecture Audit & Restructuring Roadmap (2026)

> Produced as part of the Atomic UI/UX Restructuring brief. The audit reflects
> the **actual code state** (Compose BOM 2026.06.01 / material3 1.4.0 stable).
> Verdicts are labeled **Done**, **Done-verify**, **Partial**, **Gap**.
> All implemented work compiles and passes `lint` + `test` + `assembleDebug`.

---

## Phase 1 — Current-state audit

### 1.1 What exists today

| Layer | Files | Verdict |
|---|---|---|
| Theme | `theme/Theme.kt`, `Color.kt`, `Type.kt`, `Shapes.kt`, `Dimensions.kt`, `Motion.kt` | **Done** — dynamic color (M3 Expressive), Google Sans, expressive shapes, centralized motion |
| Icons | `res/drawable/ic_*.xml` (36 Material Symbols) + `tools/fetch_material_symbols.py` | **Done** — Google 2026 icon set, filled variants |
| Components | `components/Components.kt` (ScreenScaffold, StatusPill, GroupCard, DataCard, SettingsIcon/ListItem/SwitchRow/ValueRow, SettingsOptionDialog, DialogContentEntrance, EmptyState, DirectionalIcon, SettingsDivider, EngineLifecycleEffect) | **Done** — single shared component set |
| Screens | Compass, Level, Settings, About | **Done** — all use the shared set |
| Navigation | `navigation/NexaNavHost.kt`, `Routes.kt` | **Done** — adaptive (bar/rail), crossfade, predictive back opted in |
| Testing | `app/androidTest/QiblaUiTest.kt`, domain unit tests | Partial — see Phase 12 |

### 1.2 Findings by concern

- **Colors** — all hard-coded colors live in `theme/Color.kt` (light/dark fallback
  schemes) and `NexaSenseGradients`; **zero** hard-coded colors inside screens
  or components. ✅ WCAG-verified scheme roles (AA baseline), dynamic color on
  Android 12+.
- **Typography** — official Google Sans variable font (OFL) with `tnum` tabular
  figures on readouts; Arabic/other scripts fall back per-glyph (Google parity).
- **Shapes** — one expressive scale (`NexaSenseShapes`) applied centrally; the
  old `RoundedCornerShape(20.dp)`/`(50.dp)` and `surfaceVariant(0.4f)` +
  `BorderStroke` patterns were removed; `CircleShape` only for pills and the
  `EmptyState` icon disc.
- **Spacing** — follows the 8dp grid (4/8/12/16/24/32/40/48/96); the small
  off-grid values (5/9/10/20/22dp) are all inside the bespoke dial canvases
  (CompassDial/Level canvases), which are justified drawing constants.
- **Icons** — Material Symbols everywhere; **Gap (RTL)**: the trailing
  chevrons and back arrow were not mirrored in RTL — fixed by the new
  `DirectionalIcon` helper (mirror via `LayoutDirection`, since vector
  `autoMirrored` is not honored by `painterResource`).
- **Navigation** — **Gap (adaptive)**: was phone-only bottom bar; now
  window-size-class driven (`NavigationRail` on medium width+, bottom bar on
  compact). Predictive back already opted in (`enableOnBackInvokedCallback`).
- **States** — loading/empty/error covered by `EmptyState` (icon + tonal disc),
  `StatusPill`, Qibla card statuses. **Partial**: no skeleton screens (correct
  choice for instant sensor data) and no explicit "retry" affordance on
  sensors-blocked screens (retry = reopen; sensors recover automatically).
- **RTL** — `supportsRtl=true`, 24 locales including ar; **Gap fixed**: back +
  chevron mirroring. Content itself is direction-agnostic (numbers, degrees).
- **Edge-to-edge** — `targetSdk 36`; gradient painted behind content; status bar
  consumed by TopAppBar, navigation bar by `navigationBarsPadding`; no screen
  assumes fixed system-bar height. ✅ `adjustResize` for IME.
- **Reduced motion** — **Gap**: motion is centralized in `theme/Motion.kt` but
  not yet gated on the system "remove animations" setting.

---

## Phase 2 — Design tokens foundation

Implemented as Kotlin objects (the idiomatic Compose token layer):

| Token | File | Contents |
|---|---|---|
| Color | `theme/Color.kt` | Light/Dark fallback `ColorScheme`s; `NexaSenseGradients` |
| Typography | `theme/Type.kt` | `GoogleSans` family + `NexaSenseTypography` (display→label) |
| Shape | `theme/Shapes.kt` | `NexaSenseShapes` (extraSmall→extraLarge, expressive) |
| Motion | `theme/Motion.kt` | M3 durations + easing curves verified against 1.4.0 bytecode |
| Dimension | `theme/Dimensions.kt` | cross-screen constants |

Rule enforced: components read `MaterialTheme.*` / `Motion.*` only; no ad-hoc
colors or magic numbers in screens (audited above).

---

## Phase 3 — Component architecture

Layered as **Foundation → Primitives → Components → Patterns → Screens**:

- **Foundation**: tokens (Phase 2).
- **Primitives**: `DirectionalIcon`, `SettingsIcon` (40dp tonal disc), dividers.
- **Components**: `SettingsListItem/SwitchRow/ValueRow` (Google settings rows),
  `GroupCard`/`DataCard`, `SettingsOptionDialog`, `StatusPill`, `EmptyState`,
  `DialogContentEntrance`, `ScreenScaffold`.
- **Patterns**: accordion sections (`ExpandableSettingsSection`), settings
  search (`SettingsSearchBar` + flat results), adaptive nav, picker dialogs.
- **Screens**: Compass, Level, Settings, About.

Every component defines its states through Material (enabled/disabled/selected
come from `Switch`, `selectable`, `clickable` state layers). No per-screen
duplicate components exist (verified by grep).

---

## Phase 4 — Dialogs & surfaces

- Single-choice options → **modal `AlertDialog`** with radio list
  (`SettingsOptionDialog`): correct choice for 3–26 short options + 24-locale
  support.
- Destructive confirmation → **modal `AlertDialog`** (reset).
- Calibration instructions → **modal `AlertDialog`**.
- All dialogs share the `DialogContentEntrance` motion (M3 emphasized curves)
  and predictive-back-friendly M3 `AlertDialog`.
- **Not used, correctly**: bottom sheets (no task here needs partial-screen
  selection on compact), menus, side sheets — the option set is enumeration,
  not action menus. Documented as the decision rule for future surfaces.

---

## Phase 5 — Motion & interaction

- Centralized in `theme/Motion.kt`: M3 2026 durations (50–400ms) + official
  easing curves, verified against the material3 1.4.0 artifact bytecode
  (because `MotionTokens` is `internal` in the stable release).
- Applied to: accordion expand/collapse (emphasized decelerate/accelerate,
  clipped), chevron rotation, `animateItem` row glides, dialog entrances,
  empty-state entrance, tab icon crossfade, tab screen crossfade.
- State layers come from Material components (ripple, state layers); no manual
  opacity duplication.
- **Remaining**: reduced-motion gating (Phase 11).

---

## Phase 6 — Adaptive architecture ✅ implemented

- `androidx.compose.material3:material3-window-size-class` (BOM-managed 1.4.0).
- `NexaNavHost` computes `WindowSizeClass` from the real window:
  - **Compact** (<600dp): bottom `NavigationBar`.
  - **Medium/Expanded** (≥600dp): `NavigationRail` on the start side.
- No `isPhone/isTablet` checks anywhere; behavior follows available width.
- Tab definitions are a single `NavTab` data list shared by bar and rail.
- **Next (roadmap)**: widen dial and level layouts beyond the 480dp cap on
  expanded windows; consider a list–detail pattern for settings on large
  screens.

---

## Phase 7 — RTL / LTR ✅ gap fixed

- `supportsRtl="true"`, 24 locales (ar, fa, ur, he…).
- New `DirectionalIcon`: mirrors back arrow + trailing chevrons via
  `LocalLayoutDirection` (`scaleX = -1`), because vector `autoMirrored` is not
  honored by Compose `painterResource`. Leading decorative row icons are
  intentionally **not** mirrored (Google parity).
- No left/right hard-coding in layouts; paddings use `start/end`; navigation
  order is start-to-end by Compose.
- Verified by code inspection; **needs device screenshot pass** in ar/ur.

---

## Phase 8 — Edge-to-edge & system integration

- Gradient root paints edge-to-edge behind every screen.
- Status bar: TopAppBar default `windowInsets`.
- Navigation bar / gesture insets: `navigationBarsPadding` on the scaffold
  column; rail uses `NavigationRailDefaults.windowInsets`.
- IME: `android:windowSoftInputMode="adjustResize"`; the only text field
  (settings search) sits at the top so the keyboard never covers it.
- No screen assumes fixed system-bar height.

---

## Phase 9 — Predictive back ✅

- `android:enableOnBackInvokedCallback="true"` already in the app manifest
  (opt-in for API 33; default on 34+).
- Compose Navigation maintains the back stack; `popBackStack` on About returns
  to Settings; dialogs dismiss via `onDismissRequest` (predictive-back aware in
  M3 `AlertDialog`). Verified by code path; device gesture test pending.

---

## Phase 10 — Loading / error / empty states

| State | Surface | Notes |
|---|---|---|
| Sensors blocked / unavailable | `EmptyState` (icon disc + title + message) | Compass `ic_explore`/`ic_sensors`, Level `ic_straighten`/`ic_sensors` |
| Qibla permission needed | Qibla card + in-card CTA button | permission-aware |
| Qibla calculating / accuracy | Qibla card status rows + `StatusPill` | live sensor data, no skeleton (correct: instant) |
| No search results | centered message | settings search |
| Retry | automatic (engines retry location every 15s; re-open screen re-arms sensors) | documented |

---

## Phase 11 — Accessibility

**In place:** touch targets ≥48dp (IconButton, rows), semantic roles
(`toggleable` with `Role.Switch`, `selectable`, `clickable`), content
descriptions on icon-only controls, `liveRegion` on the compass cardinal,
sensor-level content descriptions on dials, text scaling follows system,
WCAG-AA contrast via M3 tonal roles + dynamic color, focus states via M3.
**Gap:** reduced-motion — gate `Motion` durations/easing on the system
"remove animations" preference (roadmap P1). **Gap:** an a11y-focused
instrumented pass (TalkBack traversal in ar + en).

---

## Phase 12 — Testing & verification

Implemented and green: `lint` (all modules, incl. MissingTranslation across
24 locales), `domain` unit tests, `assembleDebug`/`assembleRelease`.
Instrumented UI tests exist for Qibla/settings flows and pass on device
(they need an emulator/device to run here).
**Proposed next:** screenshot tests (light/dark/dynamic/RTL/font scales),
navigation + back-behavior tests, window-size-class tests (compact vs.
medium via `createComposeRule` + device configs), TalkBack traversal tests.

---

## Phase 13 — Architectural roadmap

### Legacy → New

| Legacy | New architecture | Reason | Priority |
|---|---|---|---|
| Bottom bar only | Window-size-class bar/rail | Adaptive M3 canonical | ✅ done |
| Outlined `surfaceVariant` cards | `GroupCard`/`DataCard` tonal surfaces | M3 Expressive | ✅ done |
| Deprecated Material Icons | Material Symbols (36, filled variants) | Google 2026 | ✅ done |
| Roboto/Default type | Official Google Sans + `tnum` | Google 2026 identity | ✅ done |
| Ad-hoc tweens | `Motion` tokens (M3 curves) | One motion system | ✅ done |
| Unmirrored back/chevrons | `DirectionalIcon` RTL mirror | RTL parity | ✅ done |
| Text-only unavailable | `EmptyState` icon + tonal disc | Google empty-state | ✅ done |
| Missing About | About screen (version/licenses) | Google Settings | ✅ done |
| Raw motion always on | Reduced-motion gating | a11y | High |
| 480dp-capped dials | Expanded-width layouts on ≥840dp | Large windows | Medium |
| Single-pane settings | List–detail on large screens | Canonical M3 | Medium |
| Manual instrumentation | Screenshot + adaptive + a11y suites | Regression safety | Medium |

### Execution order for remaining work
1. Reduced-motion gate (small, high value) → 2. Large-window dial scaling →
   3. Settings list–detail on ≥840dp → 4. Screenshot/adaptive test matrix.

### Migration risks
- Window-size-class API is `@ExperimentalMaterial3WindowSizeClassApi` in the
  stable 1.4.0 — stable enough for production use (Google's own guidance), but
  re-verify on BOM upgrades.
- Google Sans is OFL; keep `third_party/GoogleSans/OFL.txt` when updating.
- Dial canvases are intentionally bespoke; change only via tokens.

### Do not change (functional reasons)
- Portrait-locking the compass (sensor frame stability).
- The sky gradient identity (brand) — components already sit on tonal
  surfaces above it.
- Engine/domain logic separation (UI state stays in ViewModels).
- `local.properties` and `.agents/` stay git-ignored.
