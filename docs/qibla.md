# Qibla Direction & North Reference

## Overview

NexaSense computes the Qibla direction **100% locally and offline**. Given a
location fix, it calculates the initial great-circle bearing to the Kaaba and
compares it against the live compass heading in the **same north reference**,
so the user gets turn-by-turn guidance that is correct regardless of whether
the compass is showing True or Magnetic North.

The feature is **opt-in and disabled by default**. Enabling it in
Settings → Qibla Direction requests the coarse-location permission (only
then); disabling it stops location updates immediately.

## Kaaba coordinates

Fixed local constants in `QiblaCalculator`:

```
latitude  21.422487°
longitude 39.826206°
```

There is no internet API, remote service, WebView or online Qibla lookup —
the coordinates are compiled into the app.

## The algorithm

### 1. WGS84 geodesic initial bearing (true north reference)

The bearing from the user's location to the Kaaba is computed with the
**Vincenty inverse formula on the WGS84 ellipsoid** — the reference algorithm
of professional geodesy. It iteratively solves the geodesic on the ellipsoid
(a = 6 378 137 m, f = 1/298.257223563) rather than assuming a sphere:

```
U1, U2  = reduced latitudes: atan((1 − f) · tan(φ))
λ       = Δλ, refined until |Δλ| converges (< 1e−12 rad)

sin σ = √( (cos U2 · sin λ)² + (cos U1 · sin U2 − sin U1 · cos U2 · cos λ)² )
cos σ = sin U1 · sin U2 + cos U1 · cos U2 · cos λ
σ     = atan2(sin σ, cos σ)

initial azimuth α1 = atan2(cos U2 · sin λ,
                           cos U1 · sin U2 − sin U1 · cos U2 · cos λ)
bearing = α1  →  normalized to [0, 360)
```

`atan2` handles all quadrants, negative longitudes, the International Date
Line, high latitudes and both poles. A zero-distance pair (already at the
Kaaba) returns `0°`. When Vincenty cannot converge (nearly antipodal points),
the calculation falls back to the spherical great-circle formula, so every
input still yields a valid bearing.

**Why the ellipsoid?** The Earth is not a sphere. Over the longest Qibla paths
(e.g. Sydney, New York) the spherical approximation is off by up to ~0.2° of
bearing and tens of kilometres of distance. The geodesic result is the
correct one; the spherical formulas remain available as the fast closed-form
approximation (`initialBearing` / `greatCircleDistance`) and as the fallback.

### 2. Geodesic distance

The optional distance to the Kaaba comes from the same Vincenty inverse
(result in meters, converted to km), falling back to the haversine formula on
non-convergence. The haversine formula:

```
a = sin²(Δφ/2) + cos(φ1)·cos(φ2)·sin²(Δλ/2)
c = 2 · atan2(√a, √(1−a))
distance = R · c            (R = 6371.0088 km)
```

### 3. Same north reference — the critical step

The Qibla bearing from step 1 is a **true-north** bearing. The compass heading
may be magnetic or true depending on the effective north reference:

- heading mode **True** → compare bearing directly;
- heading mode **Magnetic** → convert the bearing to the magnetic frame with
  `bearing_magnetic = bearing_true − declination` before comparing.

The relative angle is the **shortest signed difference** (normalized to
(−180, 180]):

```
relative = normalize(qibla − device)    // + = turn right, − = turn left
device = 120°, qibla = 138°  →  +18° (right)
device = 350°, qibla =  10°  →  +20° (right, short way, not −340°)
```

### 4. Alignment

With the default threshold of **±2°** (configurable constant in
`QiblaEngineImpl`):

```
|relative| ≤ 2°   → ALIGNED
relative  > 2°    → TURN_RIGHT
relative  < −2°   → TURN_LEFT
```

Entering the aligned zone from left or right fires **one short haptic pulse**
(with a 3 s cooldown) when haptics are enabled; there is no continuous
vibration.

## North Reference

`NorthReference` has three values — `AUTOMATIC` (default), `TRUE_NORTH`,
`MAGNETIC_NORTH`. `NorthReferenceResolver` maps the request plus
declination-availability to the **effective** reference:

| Requested | Declination available | Effective |
| --- | --- | --- |
| AUTOMATIC | yes | TRUE_NORTH |
| AUTOMATIC | no | MAGNETIC_NORTH |
| TRUE_NORTH | yes | TRUE_NORTH |
| TRUE_NORTH | no | MAGNETIC_NORTH (reason shown) |
| MAGNETIC_NORTH | any | MAGNETIC_NORTH |

The effective reference is always reported on the heading and shown in the
compass header (`Automatic · True North`), so the user always knows what is
being used.

## Declination caching

Declination comes from the platform's `GeomagneticField` model (WMM/IGRF
based — an estimate, labeled as such). It is evaluated through
`DeclinationCache`, which recomputes only when:

- the location moved more than **1 km**, or
- the cached value is older than **10 minutes**.

It is **never** recomputed per sensor event or per compass update.

## Location lifecycle

- **Not requested** on app start, on the Home screen, or while Qibla/True
  North are disabled.
- **Requested** when Qibla is enabled (or True North selected) and the
  compass screen is in the STARTED state. Last-known fix first, then a fresh
  fix with an 8 s timeout.
- **Updates** are distance-thresholded (`minDistance = 50 m`,
  `minInterval = 15 s`), so recalculation happens on significant movement —
  not per location callback.
- **Released** when the screen stops or the feature is disabled. No background
  location, no continuous GPS.

## Error states

Every failure mode maps to a clear, separate UI state (see `QiblaStatus`):

| State | Meaning |
| --- | --- |
| `QIBLA_DISABLED` | Feature off in Settings |
| `LOCATION_PERMISSION_REQUIRED` | Permission denied — button to request it |
| `LOCATION_UNAVAILABLE` | No fix obtainable |
| `LOCATION_ACCURACY_LOW` | Fix accuracy ≥ 200 m — shown separately |
| `COMPASS_UNAVAILABLE` | No heading source on this device |
| `COMPASS_ACCURACY_LOW` | Magnetometer low/unreliable — separate from location |
| `CALIBRATION_REQUIRED` | Compass calibration incomplete |
| `CALCULATING` | Waiting for the first fix |
| `READY` / `ALIGNED` | Working; aligned within the threshold |

**Location accuracy and compass accuracy are always reported separately** —
there is no combined "Qibla accuracy" status.

## Privacy & offline behavior

- All calculations happen on-device. No location, coordinates, or device
  information is ever sent anywhere — there is no analytics, tracking or ads.
- After a fix is obtained, the app works fully offline.
- The diagnostic report includes the north reference, effective reference,
  headings, declination and Qibla bearing/relative angle, but **never the
  user's coordinates**.

## Testing

The math is pure Kotlin and fully unit-tested (no hardware):

- geodesic (WGS84/Vincenty) bearings and distances from known cities —
  Berlin, London, New York, Tokyo, Sydney, Jakarta, Reykjavik, Casablanca,
  near the North Pole and across ±179° — validated against independently
  computed reference values; antipodal fallback and user-at-Kaaba.
- spherical bearings and distances (Berlin, London, New York, Jakarta,
  Sydney, Istanbul), high-latitude cities, both poles, ±179° longitudes.
- relative angle including the 350°/10° wrap, alignment thresholds, magnetic
  ↔ true conversions, effective north reference resolution, declination cache
  behavior (freshness, movement, staleness, clear).
- instrumented UI tests cover the Settings flow: North Reference options,
  Qibla disabled by default, enabling/disabling reveals/hides sub-options.
