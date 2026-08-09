# Architecture

NexaSense is a multi-module Android project with strict dependency inversion:
the **domain** layer knows nothing about Android.

## Modules

```
┌────────────────────────────────────────────────────────┐
│ app            MainActivity, Application, DI container │
├────────────────────────────────────────────────────────┤
│ presentation   Compose UI, ViewModels, navigation      │
├────────────────────────────────────────────────────────┤
│ data           engine impls, DataStore stores, reports │
├────────────────────────────────────────────────────────┤
│ core           SensorManager gateway, location, logger │
├────────────────────────────────────────────────────────┤
│ domain         models, math, ports, pure engines       │
└────────────────────────────────────────────────────────┘
        dependency direction: domain ← core ← data ← presentation ← app
```

- **domain** (pure Kotlin/JVM): sensor models, math (vectors, quaternions,
  rotation matrices, filters, calibration), the *ports* (interfaces) the other
  layers implement, and pure engines (`HeadingCalculator`, `LevelCalculator`,
  `MagneticFieldAnalyzer`, `SourceSelector`, `SamplingRateEstimator`,
  `DeclinationEngine`, `QiblaCalculator`, `NorthReferenceResolver`,
  `DeclinationCache`, `SensorFusionEngine`). Unit tests live here and never
  touch hardware.
- **core** (Android library): framework adapters — `SensorManagerGateway`
  (discovery + event streams), `LocationProviderImpl`, `GeomagneticField`
  declination, permission checks, device info, `NexaLogger`,
  `DeviceQuirkRegistry`.
- **data** (Android library): `CompassEngineImpl` (source selection, fusion,
  calibration, declination, north-reference resolution), `QiblaEngineImpl`
  (location-gated Qibla state), `LevelEngineImpl`, DataStore-backed
  `SettingsStore`/`CalibrationStore`, `DiagnosticReportFactory`.
- **presentation** (Android library): Material 3 Compose UI. ViewModels are
  thin — they observe engine `StateFlow`s and forward user intents.
- **app**: `MainActivity`, `NexaSenseApplication` and `AppContainerImpl`, the
  manual dependency graph (no DI framework).

## Dependency inversion

Domain ports (`domain/port/*.kt`) declare what the app needs:

| Port | Implemented by |
| --- | --- |
| `SensorDiscovery`, `SensorEventStream` | `SensorManagerGateway` (core) |
| `LocationProvider` | `LocationProviderImpl` (core) |
| `DeclinationProvider` | `GeomagneticFieldDeclinationProvider` (core) |
| `SettingsStore`, `CalibrationStore` | DataStore stores (data) |
| `CompassEngine`, `LevelEngine`, `MagneticFieldMonitor`, `QiblaEngine` | engine impls (data) |
| `DeviceInfoProvider` | `DeviceInfoProviderImpl` (core) |

## Qibla & north reference pipeline

```
Sensor Layer
     │
     ├── Magnetometer / Accelerometer / Rotation Vector
              │
              ▼
       Compass Engine
              │
       ┌──────┴──────┐
       ▼             ▼
Magnetic Heading   Location
       │             │
       ▼             ▼
Declination      Qibla Engine
       │             │
       ▼             ▼
True Heading    Qibla Bearing
       │             │
       └──────┬──────┘
              ▼
      Relative Qibla (same north reference)
              │
              ▼
         ViewModel → Compose UI
```

The Qibla engine is a separate port (`QiblaEngine`) that consumes the compass
engine's heading and its own distance-thresholded location stream (≈ 50 m
movement, ≥ 15 s interval). It is active only while the compass screen is
STARTED **and** the feature is enabled in Settings; disabling it releases the
location listener immediately. See [qibla.md](qibla.md).

## Sensor lifecycle & battery

Sensors are registered **only while a screen is in the STARTED lifecycle
state** (`EngineLifecycleEffect` in the UI calls `setActive(true/false)`).
When the app is backgrounded, every listener is unregistered — there is no
background sensor collection. Wake-up sensor instances are avoided for
continuous streams (the non-wake-up instance is preferred) so the SoC is not
woken per sample.

## Concurrency

- Sensor events arrive on a dedicated `HandlerThread` per stream and are
  handed to a `channelFlow`; processing is minimal inside the callback
  (copy values, emit).
- Engines run on the application scope (`Dispatchers.Default`); heavy math is
  never on the main thread.
- UI collects `StateFlow`s with `collectAsStateWithLifecycle`.

## Error handling

- Missing sensors: the stream completes silently; features degrade to
  "unavailable" UIs — no crash.
- Registration failures are caught and logged.
- Invalid values (NaN/Infinity), non-increasing timestamps and degenerate
  geometry are rejected by the analyzers/filters (unit-tested).
- Permission denial: True North and Qibla show the reason and a request
  button; compass/level/diagnostics are unaffected.
- Activity recreation: state lives in ViewModels/engines; sensors re-register
  on the new lifecycle.

## Extension points

- **Fusion**: `SensorFusionEngine` — add Complementary/Kalman/Madgwick/Mahony
  implementations without touching the compass engine.
- **Quirks**: `DeviceQuirkRegistry` — the only place device-specific
  workarounds may live (currently empty by design).
- **Features**: barometer/altimeter/thermometer/etc. each map to an
  independent engine + screen, following the same port/engine/viewmodel shape.
