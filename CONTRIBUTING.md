# Contributing to NexaSense

Thanks for helping make a solid open-source sensor suite for AOSP devices!

## Ground rules

The project has hard architectural constraints — pull requests that violate
them will be sent back:

1. **No fake values.** Every value shown to the user must come from a real
   sensor reading, or be explicitly labelled as calculated/estimated/unavailable.
2. **No hardware assumptions.** Never assume a sensor exists, never guess a
   sensor's kind from its name, never use GPS as a substitute for a pressure
   sensor.
3. **Domain stays pure.** The `domain` module must not import Android classes.
   Framework access goes through the ports defined in `domain/port`.
4. **No TODO/FIXME in merged code.** Finish the work or leave it out.
5. **Device quirks are isolated.** Any device-specific workaround must live in
   `DeviceQuirkRegistry` and must never affect other devices.

## Development setup

```bash
git clone <repo>
cd NexaSense
./gradlew assembleDebug
```

Requires JDK 17 and the Android SDK (platform 36). The SDK path goes in
`local.properties` (`sdk.dir=...`) or `ANDROID_HOME`.

## Making changes

1. Create a branch: `git checkout -b feature/your-feature`.
2. Follow the architecture in [docs/architecture.md](docs/architecture.md).
3. Add unit tests for all pure logic in `domain`.
4. Run the checks before pushing:

```bash
./gradlew test lint assembleDebug
```

5. CI runs `test`, `lint` and `assembleDebug` — all must pass.

## Commit messages

Short subject line, body explaining *why*. Keep the change focused.

## Testing on real hardware

Sensor features can only be fully validated on real devices. When you test,
note the device, ROM and Android version, and record what the Sensors screen
reports — that data is valuable for [docs/compatibility.md](docs/compatibility.md).
