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

### 1. Initial great-circle bearing (true north reference)

The bearing from the user's location to the Kaaba uses the spherical
initial-bearing formula (not a simple `Δlon / Δlat` ratio):

```
φ1, φ2  = latitudes in radians
Δλ      = (toLon − fromLon) in radians

y = sin(Δλ) · cos(φ2)
x = cos(φ1) · sin(φ2) − sin(φ1) · cos(φ2) · cos(Δλ)
bearing = atan2(y, x)  →  normalized to [0, 360)
```

`atan2` handles all quadrants, negative longitudes, the International Date
Line (|Δλ| up to 180°), high latitudes and both poles. A zero-distance pair
(already at the Kaaba) returns `0°`.

### 2. Great-circle distance

The optional distance to the Kaaba uses the haversine formula:

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

- bearings from known cities (Berlin, London, New York, Jakarta, Sydney),
  distances (Istanbul, New York, Berlin), high-latitude cities (Reykjavik,
  Helsinki, Anchorage), both poles, ±179° longitudes, user-at-Kaaba.
- relative angle including the 350°/10° wrap, alignment thresholds, magnetic
  ↔ true conversions, effective north reference resolution, declination cache
  behavior (freshness, movement, staleness, clear).
- instrumented UI tests cover the Settings flow: North Reference options,
  Qibla disabled by default, enabling/disabling reveals/hides sub-options.
