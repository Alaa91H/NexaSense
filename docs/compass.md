# Compass Engine

## Heading sources

`SourceSelector` picks the best source present at runtime, in priority order:

1. **Rotation Vector** — the framework's fused orientation (accel + gyro +
   magnetometer). Most stable, least affected by motion.
2. **Geomagnetic Rotation Vector** — fused without the gyroscope; useful on
   devices whose gyro drifts or is absent.
3. **Accelerometer + Magnetometer** — pure tilt-compensated fallback
   implemented locally (`HeadingCalculator.fromAccelerometerMagnetometer`).
4. **Unavailable** — if none exist, the compass reports "unavailable" and
   never fakes a heading.

GPS alone is never used to derive a heading while stationary.

## Heading math

Both paths produce a rotation matrix in the framework's convention
(device → world, X=East, Y=North, Z=Up):

- **Rotation vector path**: `Quaternion.fromRotationVector(x, y, z)` derives
  `w = cos(θ/2)` and converts to the matrix via the AOSP
  `getRotationMatrixFromVector` formula.
- **Tilt-compensated path**: `H = M × A` (magnetometer × accelerometer,
  normalized), `M' = A × H`, rows `[H; M'; A]` — the AOSP
  `getRotationMatrix` algorithm. Degenerate cases (free fall, magnetometer
  parallel to gravity) return *no heading* rather than a wrong one.

The heading uses the **actual AOSP convention** (verified against the
framework source):

```
azimuth = atan2(R[0][1], R[1][1])     // top-axis heading, clockwise from north
pitch   = asin(-R[2][1])
roll    = atan2(-R[2][0], R[2][2])
```

`azimuth` is the heading of the device's **top axis**, 0 at magnetic north,
increasing clockwise (0°, 90° E, 180° S, 270° W), normalized to [0, 360).

## Display rotation

Sensors report azimuth in the device's **natural** frame (portrait), but the
user reads the heading off the screen top. With auto-rotate enabled the two
frames differ, so the engine converts via
`displayDegrees = normalize360(deviceDegrees − displayRotationDegrees)`, where
`displayRotationDegrees = Display.getRotation() × 90`. The sign matches the
level engine's validated `mapToDisplay` frame rotation. The Qibla relative
bearing and the dial's Qibla marker stay consistent automatically because
both derive from the same (display-frame) heading.

## Smoothing & jitter

`AngleSmoother` operates in the continuous angular domain so the needle never
spins the long way around the wrap boundary. The UI additionally animates the
dial with a short tween, which visually absorbs residual jitter without
adding meaningful latency. Strength is user-configurable.

## North Reference (Automatic / True / Magnetic)

`NorthReference` is a single setting with three values:

- **Magnetic North** — the raw heading from the sensor pipeline, no
  declination applied.
- **True North** — `true = magnetic + declination` (mod 360).
- **Automatic** (default) — resolves to True North whenever a valid
  declination is available, otherwise Magnetic North.

`NorthReferenceResolver` computes the *effective* reference; the `Heading`
always carries both the requested and the effective reference so the UI can
show e.g. `Automatic · True North`. True North never touches the location
services (no permission request, no fix) when Magnetic North is selected.

## Declination

`GeomagneticField(lat, lon, alt, time)` provides the declination — a model
estimate (WMM/IGRF based), reported as such in the UI. It is **cached** by
`DeclinationCache`: recomputed only when the location moved by > 1 km or the
cache is older than 10 minutes, never per sensor event. A location fix is
requested (last known first, then a fresh fix with an 8 s timeout) only when
the coarse-location permission is granted.

If permission is denied or no fix is available, the UI shows
`True North unavailable — Location required` and keeps showing the magnetic
heading; the magnetic heading is never silently relabelled as true.

## Qibla

The compass dial can render a **Qibla marker** and a **Qibla card** (bearing,
relative turn guidance, optional distance, location accuracy separate from
compass accuracy). The relative angle is computed in the same north reference
as the displayed heading. See [qibla.md](qibla.md) for the full algorithm.

## Interference detection

`MagneticFieldAnalyzer` tracks a rolling baseline (EWMA of magnitude) and a
rolling deviation estimate. A reading is flagged as interference when:

- `|magnitude − baseline| > max(minThreshold, deviation × jitterMultiplier)`
  (adaptive — no single worldwide threshold), or
- `magnitude` leaves the configurable sanity band [5, 120] µT.

The compass UI shows `Magnetic interference detected — move the phone away
from magnetic objects.`

## Dial styles & customization

The dial renders in three styles, switchable from Settings → Compass:

- **Classic** — 2° minor + 30° major ticks and the full 8-point cardinal
  labels (the original design).
- **Azimuth** — a numbered military/aviation dial: degree values every 30°
  (0–330) with the four main cardinal points inside the number ring.
- **Minimal** — clean and uncluttered: only the 4 main cardinal points and
  30° major ticks, relying on the digital readout.

Each style can be fine-tuned with per-element toggles (all persisted):

- `showCardinalLabels` — N/E/S/W (and intercardinals in Classic) on the dial.
- `showDegreeTicks` — the tick ring (the azimuth style always keeps its
  numbered ring, which is its defining feature).
- `showDegreeNumbers` — 0/30/60… numbers on Classic/Minimal dials.
- `showHeadingReadout` — the large degrees + cardinal text above the dial.
- `showNorthReferenceBadge` — the True/Magnetic North pill.
- `showCompassDetails` — the sensor-source and declination lines.
- `showAccuracyPanel` — field strength, accuracy and calibration status.

All options default to the original behavior, so existing installs are
unchanged until the user customizes.
