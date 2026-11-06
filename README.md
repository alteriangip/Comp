# ArtCompanion

ArtCompanion turns an Android phone into a wireless companion controller for iPad drawing apps
(Procreate, Artstudio Pro, and others). It is free, works entirely offline, and needs no account and no iPad app.

**How it works:** the phone registers itself as a standard **Bluetooth keyboard + mouse** using Android's
`BluetoothHidDevice` API. The iPad pairs with it in *Settings → Bluetooth*, exactly like a physical keyboard.
Buttons on the phone send the drawing app's own documented keyboard shortcuts.

> **Current status: Phase 2 (connection proof of concept).** The APK builds and the unit tests pass,
> but it has **not yet been tested on a real Galaxy A15 + iPad**. Run `docs/02-poc-test-plan.md` first.

## Documents
| | |
|---|---|
| [`docs/01-feasibility-report.md`](docs/01-feasibility-report.md) | Connection methods evaluated, architecture, and the full **feature compatibility matrix** for Procreate and Artstudio Pro |
| [`docs/02-poc-test-plan.md`](docs/02-poc-test-plan.md) | Step-by-step hardware tests for the PoC, with a results column |

## What the PoC does (all real, nothing simulated)
* Registers as a composite HID keyboard + mouse. It detects and reports ROMs that don't support this.
* Pairing helper ("Make discoverable"), a list of paired devices with Connect, Disconnect.
* Auto-reconnect to the last iPad, with back-off. A foreground service keeps the link alive in the background.
* Shortcut test buttons for **Procreate** and **Artstudio Pro**, taken only from each vendor's official shortcut list.
* "Send any shortcut": type any chord (`Cmd+Shift+Z`, `Alt+[`, `⇧⌘=`), tap it, or hold it down.
* Typing test (types text into iPad Notes), which proves input works independently of the drawing apps.
* Pointer test: trackpad, click, press-drag, scroll wheel, and "hold Space while dragging" for the navigation experiments.
* An event log of every Bluetooth callback.

## Requirements
* Android 9 (API 28) or newer, with a ROM that exposes the Bluetooth **HID Device** profile.
* An iPad with Bluetooth keyboard/mouse support (iPadOS 13.4+ for the pointer).

## Build

Prerequisites: JDK 17+ and the Android SDK (Platform 35, Build-Tools 35). Android Studio Ladybug or newer works,
or the command line:

```bash
# Tell Gradle where the SDK is (Android Studio does this automatically)
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

./gradlew testDebugUnitTest      # unit tests (chord parser, HID reports, descriptor)
./gradlew assembleDebug          # → app/build/outputs/apk/debug/app-debug.apk
```

### Release APK
```bash
keytool -genkeypair -v -keystore artcompanion.jks -alias artcompanion -keyalg RSA -keysize 2048 -validity 10000
./gradlew assembleRelease
$ANDROID_HOME/build-tools/35.0.0/apksigner sign --ks artcompanion.jks \
    --out app-release.apk app/build/outputs/apk/release/app-release-unsigned.apk
```

## Install
1. Copy `app-debug.apk` to the phone and open it. Allow "Install unknown apps" for your file manager when asked.
   Or with USB debugging on: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
2. Open ArtCompanion and grant **Nearby devices** and **Notifications**.
3. Wait for **"Ready to pair"**, tap **Make discoverable**, then on the iPad go to *Settings → Bluetooth* and tap the phone.

## Permissions
| Permission | Why |
|---|---|
| `BLUETOOTH_CONNECT` | Register as a HID device, connect to the iPad (Android 12+) |
| `BLUETOOTH_ADVERTISE` | Make the phone discoverable for pairing (Android 12+) |
| `BLUETOOTH`, `BLUETOOTH_ADMIN` | Same, on Android 9–11 |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE` | Keep the keyboard connected while you use other apps |
| `POST_NOTIFICATIONS` | Show the "connected" notification the foreground service needs |

No internet permission is requested.

## Known limitations (see the feasibility report for detail)
* **No feedback from the iPad.** A Bluetooth keyboard cannot read brush size, colour, layers or zoom.
  All controls are relative commands.
* **Procreate has no keyboard shortcuts** for zoom, rotate, pan, fit, flip, new/delete/merge layer, layer
  visibility, opacity, or eyedropper. Those are marked unsupported or offered as documented workarounds.
* Colours cannot be pushed into either app directly. The planned workaround types the HEX code into the
  app's HEX field through the keyboard.
* Multi-touch gestures (pinch/rotate) cannot be sent over Bluetooth HID.
* Some Android ROMs disable the HID Device profile. The app detects this and says so. A BLE HID fallback is planned.
* Only one app on the phone can act as a Bluetooth HID device at a time.
* Punctuation shortcuts assume the iPad hardware keyboard layout is English (US).

## Project layout
```
app/src/main/java/com/artcompanion/
  hid/        HidController, HidDescriptor + Reports, KeyChord parser, HID key codes, permissions, prefs
  profiles/   Default (documented) shortcut sets for Procreate / Artstudio Pro / Custom
  service/    Foreground service holding the HID registration
  ui/         Compose theme and the PoC screen
```

## Roadmap
Phase 3: main UI, editable shortcut deck and profiles (JSON-persisted), settings. Phase 4: navigation pad,
colour studio, reference viewer, brush controls, radial menu. Phase 5: device testing and optimisation.
Phase 6: release. Phase 3 starts once the Phase 2 hardware results are in.
