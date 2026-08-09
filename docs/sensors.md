# Sensors

## Discovery

`SensorDiscovery` (implemented by `SensorManagerGateway`) enumerates
`sensorManager.getSensorList(Sensor.TYPE_ALL)` at runtime. Every sensor is
described by `SensorDescriptor`:

`id, kind, name, vendor, version, stringType, resolution, maxRange, power,
minDelayMicros, maxDelayMicros, isWakeUp, isDynamic, reportingMode,
maxFifoCount`

**The type id is authoritative.** The sensor `name`/`vendor` strings are
reported as-is to the UI but are never used to infer what a sensor is.
Unknown/vendor type ids resolve to `SensorKind.UNKNOWN` — they are still listed
with their raw metadata, just not treated as a known feature.

Dynamic sensors (plugged-in USB sensors, etc.) are included in the list; the
`isDynamic` flag marks them.

## Streams

`SensorEventStream.stream(kind, delayMicros, sensorId)` returns a cold
`Flow<SensorReading>`:

- events carry `sensorId`, resolved `kind`, the raw `values` array,
  mapped `AccuracyLevel` and the framework `timestampNanos`;
- a missing sensor or failed registration completes the flow silently;
- the framework's non-wake-up instance of the kind is preferred for streaming.

## Battery

Streaming prefers **non-wake-up sensors**, so the SoC is not woken for every
sample while the screen is on. All streams are scoped to the screen that owns
them and are unregistered when it closes (`awaitClose`) — there is no
background collection. The current consumers (compass, level, sensor detail)
need fresh data and therefore register with zero report latency; if a future
feature collects a slow-changing sensor continuously (e.g. a pressure-based
altitude readout), it can request FIFO **batching** by extending
`SensorEventStream.stream` with a `maxReportLatencyMicros` parameter — the
`SensorManagerGateway` already threads that value into
`registerListener(listener, sensor, delay, maxReportLatency)`.

## Accuracy

Framework status codes map to `AccuracyLevel`:
`UNRELIABLE (0) → UNRELIABLE`, `LOW (1) → LOW`, `MEDIUM (2) → MEDIUM`,
`HIGH (3) → HIGH`. The UI shows the level and, for low/unreliable
magnetometer readings, recommends calibration.

## Sampling rate

The requested delay is only a hint. `SamplingRateEstimator` measures the
**actual** rate from event timestamps (EMA of inter-event intervals), skipping
non-positive intervals and resetting after long gaps (stream pauses). The
Sensor Detail screen shows both the requested delay and the measured
`~XX Hz`.

## Filtering

`SmoothingFilters` provides swappable pure filters:

- `ExponentialSmoothing` — classic low-pass (`y = αx + (1−α)y`);
- `AdaptiveFilter` — strength grows with observed deviation;
- `AngleSmoother` — heading-aware smoothing that always takes the short way
  across 0°/360° (no spurious 355°→5° jumps).

The compass uses `AngleSmoother` with a user-selectable strength
(None/Light/Medium/Strong). Invalid inputs are ignored without poisoning the
filter state.

## No fake data

If `SensorManager` does not expose a sensor, the app says so:

```
Barometer
Not available
This device does not expose a hardware pressure sensor.
```

No value is invented, GPS is never used as a substitute, and the "estimated"
pressure module (future work) will be visually and logically separate from the
hardware barometer.
