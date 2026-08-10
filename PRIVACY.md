# Privacy

NexaSense is designed to be **offline-first** and privacy-respecting.

## Data collection

- **No analytics, tracking, ads, telemetry or cloud services.** The app works
  fully offline and never communicates over the network.
- **No personal data is stored.** Sensor readings, calibration data and
  settings live only on the device in app-private storage.

## Permissions

The app declares exactly **one** runtime permission:

- `ACCESS_COARSE_LOCATION` — used **only** by the True North feature to compute
  magnetic declination (computed fully on-device with the official NOAA/BGS
  WMM2025 geomagnetic model; no network involved). It is requested at runtime
  only when the user enables True North mode, and is never used otherwise.

No internet, storage, contacts, phone, camera, microphone or Bluetooth
permissions are requested.

## Diagnostic report

The shareable diagnostic report contains:

- device model, manufacturer, Android version, build fingerprint, kernel,
- sensor list with vendors, types and capabilities,
- calibration status.

It contains **no** location, **no** sensor value streams, and **no** personal
identifiers. You can review exactly what is shared before sending it.

## Data on device

| Data | Location | Removable |
| --- | --- | --- |
| Settings | DataStore (`nexasense_settings`) | Reset settings / uninstall |
| Calibration | DataStore (`nexasense_calibration`) | Reset calibration / uninstall |

Uninstalling the app deletes all app data.
