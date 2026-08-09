# Calibration

## Magnetometer (compass)

Real magnetometers read distorted fields: **hard-iron** errors are constant
per-axis offsets (nearby metal, magnets in the device), **soft-iron** errors
are direction-dependent gain differences.

NexaSense implements a **two-tier** calibration (`MagneticCalibrationMath`):

**Tier 1 — min/max (axis-aligned):**

1. While the compass is active, raw magnetometer samples are collected.
   Invalid (NaN/∞/zero) samples are rejected.
2. Per-axis `min`/`max` yield the hard-iron offsets:
   `offset = (min + max) / 2`.
3. Per-axis half-ranges yield soft-iron scales that map the ellipsoid onto
   the sphere of the largest axis: `scale_i = maxHalf / half_i`.
4. **Coverage** = `minHalf / maxHalf` (0..1) — how much of orientation space
   has been sampled. Calibration is considered complete with ≥ 60 samples and
   coverage ≥ 0.35 (both configurable).

**Tier 2 — full 3D ellipsoid fit (least squares):**

5. Once calibrated, a full least-squares ellipsoid fit (`mᵀPm + qᵀm = 1`)
   runs on the retained samples. The data is centered and normalized to unit
   radius (Hartley normalization) before the 9-unknown solve, keeping the
   algebraic fit well-conditioned.
6. The hard-iron offset is `b = −½P⁻¹q`; the soft-iron correction is the
   transpose of the Cholesky factor of `P` (the whitening transform: for
   `P = LLᵀ`, `|Lᵀv|² = vᵀPv`). This corrects **rotated** soft iron that
   couples axes — something axis-aligned min/max scaling cannot undo.
7. Degenerate fits (too few samples, planar/linear data, non-positive-
   definite `P`, or a residual spread above 35%) are rejected and the app
   falls back to tier 1.

Calibration is applied before the tilt-compensated heading:
`corrected = (raw − offset) × scale` (tier 1) or
`corrected = softIron × (raw − ellipsoidOffset)` (tier 2).

The live result (sample count, coverage, calibrated flag) is shown on the
compass screen and auto-persisted (throttled) to DataStore
(`nexasense_calibration`). The user can reset it at any time.

**Limits (documented):** the calibration needs samples covering the full
orientation space for good results — the UI instructs the user to move the
phone in a figure-eight / rotate through different orientations. It assumes
the field is roughly constant during collection (move away from magnets).

## Level (zero point)

The level measures pitch/roll from the accelerometer. Real devices' surfaces
and mounting can have small offsets, so the user can:

1. place the phone on a flat reference surface,
2. press **Set Zero** — the current (pitch, roll) is stored as
   `pitchOffset`/`rollOffset` in DataStore,
3. optionally reset.

Every subsequent reading subtracts the offsets (wrapped to ±180°). A device
that is level on the reference surface then reads 0.0°/0.0°.

## Persistence

Both calibrations use **DataStore Preferences** (not SharedPreferences) and are
deleted when the app is uninstalled.
