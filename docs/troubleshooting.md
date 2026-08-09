# Troubleshooting

## Compass

### "Compass unavailable"
The device exposes neither a rotation vector nor a magnetometer (rare).
Check Diagnostics → Capabilities. If the magnetometer exists but shows
`UNRELIABLE`, move away from magnetic sources and calibrate
(compass screen → Reset → move in a figure-eight).

### Heading jumps 355° → 5° backwards
The heading smoothing operates in the continuous angular domain and always
takes the short way across 0°/360° — a sudden jump usually means interference
or a magnetometer accuracy drop. Watch for the interference banner.

### Heading is wrong but stable
- Verify compass mode: "Magnetic North" vs "True North".
- True North needs location; if it shows `Location required`, enable location
  or grant permission in Settings.
- Near metal/electronics the field is distorted — recalibrate.

## True North

### "Location required" / "Location permission denied"
Enable the True North mode in Settings (this is the only thing that requests
location), then grant the coarse-location permission and ensure location
services are on. Declination needs a recent fix; the app uses the last known
fix or requests one with an 8 s timeout.

## Level

### Reading is offset by a constant
The device's surface/mounting may not be perfectly flat — use
Calibrate level → Set Zero on a known-flat surface.

### "Level unavailable"
No accelerometer — nearly impossible on real phones.

## Sensors screen

### A sensor shows no live data
One-shot and on-change sensors (step detector, significant motion, proximity)
do not stream continuously by design. Continuous sensors show raw values,
accuracy, timestamps and the **measured** sampling rate.

### Measured rate is far from the requested rate
Normal — the requested delay is only a hint; the HAL decides. The rate shown
is measured from actual timestamps.

## Build issues

### `SDK location not found`
Set `sdk.dir` in `local.properties` or export `ANDROID_HOME`.

### Instrumented tests fail on a device without sensors
The tests are written to degrade gracefully; on unusual hardware, run
`./gradlew connectedDebugAndroidTest` and attach the diagnostic report.

## Still broken?

Gather a diagnostic report (Diagnostics → Copy diagnostic report) and open an
issue with: device model, ROM, Android version, what you did, what you
expected, and what happened. The report contains no personal data.
