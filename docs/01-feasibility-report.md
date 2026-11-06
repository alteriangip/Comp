# ArtCompanion: Phase 1 Technical Feasibility Report

Status: Phase 1 complete. Phase 2 proof of concept **tested by the user on Galaxy A15 + iPad: "It works"**
(2026-11-06). That confirms the core route: the A15 exposes the HID Device profile, the iPad pairs, and
shortcuts reach the iPad. Results for the individual exploratory items (§6, tests 9–11, 15, 16 and 21 in the
test plan) have not been reported yet, so the 🧪 rows below remain hypotheses.
Last updated: 2026-11-06

How to read this report:
- **Verified (documentation)**: confirmed from official vendor documentation (sources at the end).
- **Verified (device)**: confirmed on a real Galaxy A15 + iPad. So far: HID registration, pairing, and keyboard shortcuts (user report, Phase 2).
- **Hypothesis**: technically plausible but unverified. It must not ship as "working" until it passes a device test.

---

## 1. The core constraint

An app on the phone can only affect Procreate or Artstudio Pro through input channels that iPadOS
itself delivers to those apps. iPadOS sandboxing means:

* No third-party iPad app (and nothing arriving over Wi‑Fi) can inject touches into, or read state from, another app.
* What *is* delivered to the foreground drawing app:
  1. **Hardware keyboard events** from a paired Bluetooth keyboard. Both apps define keyboard shortcuts.
  2. **Pointer events** from a paired Bluetooth mouse/trackpad (iPadOS 13.4+). Clicks and drags arrive as
     indirect-pointer touches. Scroll wheel arrives as scroll events, which an app may or may not handle.
  3. **Accessibility features** the *user* configures on the iPad (e.g. AssistiveTouch actions on pointer buttons).
* There is **no feedback channel**. A Bluetooth keyboard cannot read the current brush size, layer list,
  colour or zoom level. So the phone can send *relative* commands ("size +5 %") but cannot show the
  app's real state. The UI must never imply it knows that state.

So the phone has to **be a Bluetooth keyboard + mouse**. Everything else follows from that.

---

## 2. Connection methods evaluated

| # | Method | Needs iPad app? | Can control Procreate / Artstudio Pro? | Verdict |
|---|--------|-----------------|------------------------------------------|---------|
| 1 | **Bluetooth Classic HID** (Android `BluetoothHidDevice`, API 28+), composite keyboard + mouse | **No**: pairs in iPad Settings → Bluetooth | **Yes**, via keyboard shortcuts and pointer | **Selected (primary)** |
| 2 | Bluetooth LE HID (HOGP) via Android `BluetoothGattServer` advertising HID service 0x1812 | No | Same as #1 in principle | **Fallback (Plan B)** if a phone's ROM disables #1. More code: GATT HID service, battery service, bonding/encryption. Some iPadOS/Android combinations are known to be flaky. Not built yet. |
| 3 | Wi‑Fi / LAN (TCP, WebSocket, Bonjour) | **Yes**, a custom iPad app | **No.** An iPad app cannot drive another app's UI. At most it could put a colour HEX on the iPad clipboard. | Rejected for control. Could support an optional colour-to-clipboard bridge later, but #1 already covers that without an iPad app (see §5 C). |
| 4 | USB (Android as USB HID gadget) | No | Would work if possible | **Not feasible** for a normal app: USB gadget/configfs HID needs root. The Android Open Accessory protocol needs the *host* to speak AOA, and iPadOS does not. |
| 5 | Apple Universal Control / Sidecar / Continuity | — | — | Apple devices only. Not available to Android. |
| 6 | Apple "Universal Clipboard" | — | — | Apple ID devices only. Not available to Android. |
| 7 | AssistiveTouch (iPad accessibility), driven by our HID mouse | No (user configures iPad Settings) | **Possibly**, for gestures with no shortcut (pinch/rotate) | **Hypothesis, to be tested.** iPadOS lets users assign actions, including custom recorded gestures, to extra mouse buttons. If this works, mouse buttons 3/4/5 from the phone could trigger recorded pinch/rotate gestures. |

### 2.1 Bluetooth HID Device on Android: requirements and risks

* API: `BluetoothAdapter.getProfileProxy(…, HID_DEVICE)` → `BluetoothHidDevice.registerApp(sdp, …)` →
  `sendReport(host, reportId, bytes)`. Available since **Android 9 (API 28)**.
* Permissions (Android 12+): `BLUETOOTH_CONNECT` (runtime, "Nearby devices"), plus `BLUETOOTH_ADVERTISE`
  for the "make discoverable" request. A `connectedDevice` foreground service keeps the registration
  alive while you are in another app or the screen is off.
* **OEM variability is real.** Android's documentation describes the API, but each ROM decides whether the
  HID Device profile is enabled in the Bluetooth stack. On a ROM that disables it, `getProfileProxy` returns
  false, or never calls back. The app detects both cases (5‑second timeout) and says so plainly. It does
  not pretend to connect.
* **Galaxy A15 LTE (Android 14, One UI 6.x):** I found **no authoritative public statement** either way.
  Samsung ROMs commonly have the profile enabled, and third-party "Bluetooth keyboard & mouse" apps
  are widely used on Galaxy phones. That is anecdotal, not verification. **Test step 1 of the PoC
  answers this definitively.** If it fails, the plan is Plan B (BLE HID).
* Only **one app** at a time can register as a HID device. Other keyboard-emulator apps must be closed.
* Report format: we use a standard boot-style 8‑byte keyboard report (report ID 1) and a 5‑byte relative
  mouse report with wheel + horizontal pan (report ID 2). iPadOS maps HID GUI → ⌘ Command, Alt → ⌥ Option.
* The 🌐 Globe/Fn key is an Apple-vendor HID usage, not a standard key code. Globe-based shortcuts (window
  management, dictation) are **not** supported.
* Typed characters assume the iPad's *hardware keyboard layout* is set to US/English. Shortcuts that use
  letters still work on most layouts. Punctuation-based shortcuts (`[ ] = - ,`) can move on non-US layouts.

### 2.2 Connection quality

Android does not expose RSSI or link quality for a connected Classic HID host, and the iPad sends no
delivery acknowledgement. The app shows only what it can measure: the connection state from the stack,
and the local time `sendReport()` took. It will **not** show a fabricated "signal strength" bar.

---

## 3. What each app accepts (official shortcut lists)

### Procreate (handbook 5.x, "Keyboard Shortcuts")
Undo ⌘Z · Redo ⇧⌘Z · Paint B · Erase E · Brush size `[` `]` (±5 %), `⌘[` `⌘]` (±1 %), `⇧[` `⇧]` (±10 %) ·
Selections S · Transform V (arrow keys nudge 1 px) · Layers panel L · Colours panel C ·
Previous/current colour X · Modify button ⌥ (iPadOS > 17) or M (≤ 17) · QuickMenu Space ·
Fullscreen interface ⌘0 · Perspective guide ⌘; · Actions menu ⌘K · Cut/Copy/Paste ⌘X/⌘C/⌘V ·
Copy all ⌘A · Duplicate layer/selection ⌘J · Deselect ⌘D · Clear layer content ⌘⌫ ·
Hue/Saturation/Brightness ⌘U · Colour Balance ⌘B.

**Not in Procreate's list:** smudge, eyedropper, opacity, new layer, delete layer, merge, layer visibility,
flip canvas, zoom, rotate, fit, save. Procreate's shortcuts are fixed (not user-assignable). It
autosaves, so "Save" has no meaning there.

### Artstudio Pro (official manual, "Keyboard Shortcuts", pp. 132–135)
Shortcuts are **user-customisable** in *Preferences › Interface › Keyboard Shortcuts*. Defaults include:
Undo ⌘Z · Redo ⇧⌘Z · Save ⌘S · New document ⌘N · Add empty layer ⇧⌘N · Layer via copy ⌘J ·
Merge down ⌘E · Merge visible ⇧⌘E · Hide/Show layers ⇧⌘, · Group ⌘G · Select prev/next layer ⌥[ / ⌥] ·
Fullscreen F · Mirror ⇧⌘M · Zoom in ⌘= · Zoom out ⌘- · Zoom to fit ⌘0 · 100 % ⌘1 ·
Rotate left ⇧⌘- · Rotate right ⇧⌘= · Paint B · Eraser E · Eyedropper I · Smudge D · Move V · Transform ⌘T ·
Zoom tool Z · **Pan tool H** · **Rotate canvas tool R** · Increase/Decrease size `]` / `[` ·
Previous/Next brush `,` / `.` · Favourite presets ⌥⌘1…9 · Switch old/new colour X · Deselect ⌘D.
Categories marked "(Set your own Shortcuts)" (Image, Drawing modes, Panels, Adjust…) can be assigned by
the user. The manual also documents ⌃⌥ + swipe to change brush size/opacity, and ⌥ + Tab to sample colour.

---

## 4. Feature compatibility matrix

Legend: ✅ supported via documented shortcut · 🟡 partial / workaround · 🧪 hypothesis, needs device test ·
❌ not possible through supported interfaces · 📱 phone-only feature (no iPad needed)

### A. Shortcut deck

| Command | Procreate | Artstudio Pro |
|---|---|---|
| Undo | ✅ ⌘Z | ✅ ⌘Z |
| Redo | ✅ ⇧⌘Z | ✅ ⇧⌘Z |
| Brush | ✅ B | ✅ B |
| Eraser | ✅ E | ✅ E |
| Eyedropper | 🟡 No key. Set Procreate *Gesture controls › Eyedropper* to "□ + Touch" and the phone **holds ⌥** (or M on iPadOS ≤17) while you tap the canvas. 🧪 | ✅ I |
| Brush size + / − | ✅ `]` / `[` (also 1 % and 10 % steps) | ✅ `]` / `[` |
| Save | ❌ N/A: Procreate autosaves | ✅ ⌘S |
| New layer | ❌ no key. Alternative: L opens the Layers panel. ⌘J duplicates. | ✅ ⇧⌘N |
| Delete layer | ❌ no key (⌘⌫ only *clears* content). Alternative: L | 🟡 no default. User assigns one in Artstudio Pro, then enters the same chord in ArtCompanion |
| Merge layer | ❌ no key. Alternative: L | ✅ ⌘E (merge down), ⇧⌘E (merge visible) |
| Toggle layer visibility | ❌ no key | ✅ ⇧⌘, (Hide/Show Layers) 🧪 confirm scope (current layer vs all) |
| Flip canvas | ❌ no key. Alternative: ⌘K opens Actions (Flip is under Canvas) | ✅ ⇧⌘M (View › Mirror) |
| Fit canvas | ❌ no key (touch "pinch-snap" gesture only) | ✅ ⌘0 |
| Fullscreen | ✅ ⌘0 | ✅ F |
| Custom key / chord | ✅ any HID key + modifiers | ✅ any HID key + modifiers |

### B. Canvas navigation pad

| Function | Procreate | Artstudio Pro |
|---|---|---|
| Zoom in / out | ❌ no key. 🧪 mouse scroll wheel (undocumented). 🧪 AssistiveTouch recorded pinch | ✅ ⌘= / ⌘- |
| Rotate ⟲ / ⟳ | ❌ no key. 🧪 AssistiveTouch recorded rotate | ✅ ⇧⌘- / ⇧⌘= |
| Reset rotation | ❌ | 🟡 ⌘0 (fit) / ⌘1 (100 %). 🧪 whether either resets the angle |
| Fit to screen | ❌ | ✅ ⌘0 |
| Pan | ❌ no key. Pointer drag on canvas = a stroke, not a pan. Space = QuickMenu (not pan). 🧪 scroll wheel | 🧪 **H (Pan tool) + pointer drag**, then B to return to brush. 🧪 Space-hold is undocumented |
| Space + mouse drag | ❌ Space is QuickMenu | 🧪 undocumented; the PoC has a test toggle |
| Two-finger pinch / rotate from phone | ❌ Bluetooth mice/keyboards cannot send multi-touch. The standard HID "precision touchpad" gestures are not documented as supported by iPadOS for third-party devices | same |

**Conclusion:** the navigation pad is **fully implementable for Artstudio Pro** (keyboard zoom/rotate/fit
plus the Pan-tool + pointer-drag route). For **Procreate**, keyboard navigation does not exist. The pad
will only offer what device tests prove (scroll-wheel behaviour, AssistiveTouch gestures) and will say so
in the UI rather than show dead controls.

### C. Colour Studio

| Feature | Status |
|---|---|
| HSV wheel, RGB sliders, HEX input, swatches, palettes, history, rename/reorder/delete, copy HEX/RGB | 📱 fully local |
| Send colour to app | ❌ No API to set the colour directly in either app. **🟡 Practical alternative, no iPad app needed:** open the app's colour panel (Procreate C, or tap Artstudio Pro's colour), tap its HEX field on the iPad, then press "Type HEX" on the phone. It types `RRGGBB` + Return through the HID keyboard. 🧪 confirm each app's HEX field accepts hardware-keyboard text and whether it wants "#" |
| Confirmation that the colour was applied | ❌ Impossible (no feedback channel). The UI will say "typed", never "applied" |

### D. Reference viewer
📱 Entirely phone-side: import (Storage Access Framework / Photo Picker), swipe, zoom/pan/rotate,
collections, colour extraction, dimensions, JPEG/PNG/WebP/HEIF/GIF (first frame). Works with no iPad connection.

### E. Brush controls

| Feature | Procreate | Artstudio Pro |
|---|---|---|
| Size slider | 🟡 **relative only**: a step dial sending ±1/5/10 % keys. Absolute size is impossible (no state readback) | 🟡 relative `[` `]` steps |
| Opacity slider | ❌ no key | ❌ no key (⌃⌥ + swipe is a touch gesture). 🧪 ⌃⌥ held + pointer drag |
| Size presets | ❌ no absolute set. Can only send N steps from an unknown start | 🟡 Favourite presets ⌥⌘1–9 select saved brush presets (which can store size) |
| Customisable ranges / frequent settings | 📱 phone-side config of step sizes and preset buttons | 📱 |

### F. Radial menu
📱 + ✅ Purely a phone UI that sends the same verified shortcuts. Fully feasible, with haptics.

### Connection management
✅ Status, connected iPad name, reconnect, auto-reconnect to last host (exponential back-off),
troubleshooting text, remembered profile. Connection quality: 🟡 only connection state and local send
time. No fabricated signal meter.

---

## 5. Recommended architecture

```
┌──────────────── Android app (single module, layered packages) ─────────────────┐
│ ui/         Compose screens: Deck, NavPad, Colour, References, Brush, Radial   │
│ profiles/   Profile + Shortcut models (text chords, editable, JSON-persisted)  │
│ input/      CommandDispatcher: maps a UI action → KeyChord / pointer sequence  │
│ hid/        HidController (BluetoothHidDevice), HidDescriptor, Reports,        │
│             KeyChord parser, HidState machine, auto-reconnect                  │
│ service/    Foreground service keeping the HID registration alive              │
│ data/       DataStore (settings) + JSON files (profiles, palettes, collections)│
└────────────────────────────────────────────────────────────────────────────────┘
          │ Bluetooth Classic HID (keyboard report 1, mouse report 2)
          ▼
   iPad: built-in Bluetooth keyboard + pointer support → foreground app
```

Key decisions:
1. **Bluetooth Classic HID, no iPad app.** It is the only route that reaches the drawing apps without
   private APIs. It also meets the "no software on the iPad" priority.
2. **Shortcuts are data, not code.** Each profile is a list of `{label, icon, chord}`, with chords stored as
   text (`"Shift+Cmd+Z"`). The defaults come only from vendor documentation (`profiles/DefaultProfiles.kt`,
   each with its source). Editing one profile cannot touch another, because profiles are separate documents.
3. **One serial I/O thread** for all Bluetooth calls, so press/release reports are never reordered.
   Chords are sent as modifier‑down → modifier+key → modifier‑only → release, as a real keyboard does.
4. **Foreground service** (`connectedDevice` type). Without it Android 14 can kill the process when you
   switch apps, and the iPad silently loses the keyboard.
5. **Honest state machine.** Every UI state (`Unsupported`, `Ready`, `Connected`, …) comes from a real
   Bluetooth callback, and controls are disabled when no host is connected.
6. **Low memory.** No DI framework, no image libraries in the PoC, and later no network stack. The
   reference viewer will decode down-sampled bitmaps (`ImageDecoder` with target size) to stay within a
   4 GB phone's budget.
7. **Plan B (BLE HID / HOGP)** sits behind the same `HidController`-style interface, if the A15 (or another
   phone) does not expose the Classic HID Device profile.

---

## 6. Risks and open questions (resolved by Phase 2 device tests)

1. ~~Does the Galaxy A15 LTE expose `HID_DEVICE`?~~ **Yes** (user test).
2. ~~Does the iPad pair with the phone as a keyboard/mouse?~~ **Yes** (user test). Still open: does it stay connected with the phone screen off?
3. ~~Do the shortcuts work on the iPad?~~ **Yes** per user test. Still open: which specific shortcuts were checked in each app.
4. Procreate: does the scroll wheel zoom or scroll the canvas? Does a pointer drag paint?
5. Artstudio Pro: does H + pointer drag pan? Does Space-hold + drag pan?
6. Do the HEX fields accept typed input from a hardware keyboard?
7. AssistiveTouch: can a recorded pinch/rotate gesture be mapped to an extra mouse button?

## Sources
* Procreate Handbook: Keyboard Shortcuts, https://help.procreate.com/procreate/handbook/interface-gestures/keyboard
* Artstudio Pro User Manual (Lucky Clan), Keyboard Shortcuts pp. 132–135, https://www.luckyclan.com/artstudiopro/artstudio_pro_manual.pdf
* Android `BluetoothHidDevice` reference, https://developer.android.com/reference/android/bluetooth/BluetoothHidDevice
