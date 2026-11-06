# Phase 2: Connection Proof-of-Concept Test Plan

The PoC APK (`0.1.0-poc`) is built and unit-tested, but **I have not run it on a real device.**
The build environment has no Galaxy A15 or iPad. Every step below must be run on hardware, and the
results decide what Phase 3 builds. Please fill in the Result column and send it back (or attach the
"Event log" text from the app).

Devices: Samsung Galaxy A15 LTE (Android 14) · iPad 10th gen (iPadOS ___)

## Setup
1. Install `app-debug.apk` on the phone (see README › Install).
2. Close any other "Bluetooth keyboard/mouse" apps on the phone.
3. Open ArtCompanion and grant **Nearby devices** and **Notifications**.

## Tests

| # | Step | Expected | Result |
|---|------|----------|--------|
| 1 | Watch the Connection card | Reaches **"Ready to pair"**. If it shows **"HID Device not available"**, copy the message: the A15 ROM lacks the profile, and we switch to Plan B (BLE HID) | |
| 2 | Tap **Make discoverable**. On the iPad: Settings → Bluetooth → Other Devices → tap the phone | iPad pairs. The app shows **"Connected to <iPad name>"**. Log shows "Connected to …" | |
| 3 | iPad: open **Notes**, new note. Phone: **Typing test → Type on iPad** | Text appears in Notes. *This proves real keyboard input, not just pairing.* | |
| 4 | In Notes: Send any shortcut `Cmd+Z`, then `Shift+Cmd+Z` | Typing is undone, then redone | |
| 5 | Pointer test: drag on trackpad, tap | iPad pointer circle moves; tap clicks | |
| 6 | **Procreate**: open a canvas, draw 2 strokes. Tap **Undo**, **Redo** | Last stroke disappears, then returns | |
| 7 | Procreate: **Brush**, **Eraser**, **Size +5%** / **Size −5%** | Tool switches; brush size slider moves | |
| 8 | Procreate: **Fullscreen** (⌘0), **QuickMenu** (Space), **Layers** (L), **Colours** (C) | Each does what its label says | |
| 9 | Procreate: scroll strip ⇕ over the canvas | Record what happens: zoom? scroll? nothing? | |
| 10 | Procreate: "Drag = press" ON, drag on trackpad over the canvas | Record: paints a stroke? pans? | |
| 11 | Procreate eyedropper workaround: Procreate › Actions › Prefs › Gesture controls › Eyedropper → "□ + Touch". Phone: in "Send any shortcut" enter `Alt` (or `M` on iPadOS ≤ 17), keep **Hold while pressed** held down and tap the canvas with a finger | Eyedropper appears while held. Record result | |
| 12 | **Artstudio Pro**: draw, then **Undo**, **Redo**, **Brush**, **Eraser**, **Eyedropper** | Each works | |
| 13 | Artstudio Pro: **Zoom in/out**, **Rotate ⟲/⟳**, **Fit**, **100%**, **Flip view**, **Fullscreen** | Each works. Record whether Fit resets rotation | |
| 14 | Artstudio Pro: **New layer**, **Merge down**, **Hide/Show**, **Save** | Each works. Record what Hide/Show affects | |
| 15 | Artstudio Pro: **Pan tool** (H), then "Drag = press" ON + drag | Record: does the canvas pan? | |
| 16 | Artstudio Pro: "Hold Space while dragging" ON + "Drag = press" ON + drag | Record: pan? | |
| 17 | Lock the phone screen for 2 min, unlock, tap Undo | Still connected (foreground service), undo works | |
| 18 | Turn iPad Bluetooth off then on | App shows Ready, then auto-reconnects within ~30 s | |
| 19 | Force-stop the app, reopen | Auto-reconnects to the last iPad | |
| 20 | Rotate the phone to landscape | Layout still usable, connection unaffected | |
| 21 | Procreate / Artstudio Pro HEX field: open colour panel, tap the HEX field on the iPad, phone: Typing test `ff8800` + Return | Record: colour changes? | |

## Diagnosing failures
* **Pairs but nothing types (test 3 fails):** copy the Event log. Look for `sendReport … failed` or
  `Host output report` (the latter means the iPad *does* treat us as a keyboard).
* **The iPad doesn't list the phone:** make sure the status is "Ready to pair" *before* tapping
  Make discoverable. Remove any old pairing on both devices ("Forget This Device") and retry.
* **Shortcut works in Notes but not in the app:** the app may need focus. Tap the canvas once first.
  Check the iPad's hardware keyboard layout (Settings › General › Keyboard › Hardware Keyboard) is
  English (US) for punctuation shortcuts like `[` `]` `=`.
