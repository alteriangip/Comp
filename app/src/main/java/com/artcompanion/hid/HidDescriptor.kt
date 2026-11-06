package com.artcompanion.hid

/**
 * HID report descriptor for a composite keyboard + mouse, as accepted by iPadOS.
 *
 * Report 1: keyboard (boot-compatible layout): [modifiers, reserved, key1..key6]; LED output report.
 * Report 2: mouse: [buttons(3 bits), dx, dy, wheel, horizontal pan], relative int8 axes.
 */
object HidDescriptor {
    const val REPORT_ID_KEYBOARD = 1
    const val REPORT_ID_MOUSE = 2

    val bytes: ByteArray = intArrayOf(
        // ---- Keyboard ----
        0x05, 0x01,       // Usage Page (Generic Desktop)
        0x09, 0x06,       // Usage (Keyboard)
        0xA1, 0x01,       // Collection (Application)
        0x85, REPORT_ID_KEYBOARD,
        0x05, 0x07,       //   Usage Page (Key Codes)
        0x19, 0xE0,       //   Usage Minimum (Left Control)
        0x29, 0xE7,       //   Usage Maximum (Right GUI)
        0x15, 0x00,       //   Logical Minimum (0)
        0x25, 0x01,       //   Logical Maximum (1)
        0x75, 0x01,       //   Report Size (1)
        0x95, 0x08,       //   Report Count (8)
        0x81, 0x02,       //   Input (Data, Var, Abs)      ; modifier byte
        0x95, 0x01,       //   Report Count (1)
        0x75, 0x08,       //   Report Size (8)
        0x81, 0x01,       //   Input (Const)               ; reserved byte
        0x95, 0x05,       //   Report Count (5)
        0x75, 0x01,       //   Report Size (1)
        0x05, 0x08,       //   Usage Page (LEDs)
        0x19, 0x01,       //   Usage Minimum (Num Lock)
        0x29, 0x05,       //   Usage Maximum (Kana)
        0x91, 0x02,       //   Output (Data, Var, Abs)     ; LED report
        0x95, 0x01,       //   Report Count (1)
        0x75, 0x03,       //   Report Size (3)
        0x91, 0x01,       //   Output (Const)              ; LED padding
        0x95, 0x06,       //   Report Count (6)
        0x75, 0x08,       //   Report Size (8)
        0x15, 0x00,       //   Logical Minimum (0)
        0x25, 0x65,       //   Logical Maximum (101)
        0x05, 0x07,       //   Usage Page (Key Codes)
        0x19, 0x00,       //   Usage Minimum (0)
        0x29, 0x65,       //   Usage Maximum (101)
        0x81, 0x00,       //   Input (Data, Array)         ; 6 key slots
        0xC0,             // End Collection

        // ---- Mouse ----
        0x05, 0x01,       // Usage Page (Generic Desktop)
        0x09, 0x02,       // Usage (Mouse)
        0xA1, 0x01,       // Collection (Application)
        0x85, REPORT_ID_MOUSE,
        0x09, 0x01,       //   Usage (Pointer)
        0xA1, 0x00,       //   Collection (Physical)
        0x05, 0x09,       //     Usage Page (Buttons)
        0x19, 0x01,       //     Usage Minimum (1)
        0x29, 0x03,       //     Usage Maximum (3)
        0x15, 0x00,       //     Logical Minimum (0)
        0x25, 0x01,       //     Logical Maximum (1)
        0x95, 0x03,       //     Report Count (3)
        0x75, 0x01,       //     Report Size (1)
        0x81, 0x02,       //     Input (Data, Var, Abs)
        0x95, 0x01,       //     Report Count (1)
        0x75, 0x05,       //     Report Size (5)
        0x81, 0x01,       //     Input (Const)             ; padding
        0x05, 0x01,       //     Usage Page (Generic Desktop)
        0x09, 0x30,       //     Usage (X)
        0x09, 0x31,       //     Usage (Y)
        0x09, 0x38,       //     Usage (Wheel)
        0x15, 0x81,       //     Logical Minimum (-127)
        0x25, 0x7F,       //     Logical Maximum (127)
        0x75, 0x08,       //     Report Size (8)
        0x95, 0x03,       //     Report Count (3)
        0x81, 0x06,       //     Input (Data, Var, Rel)
        0x05, 0x0C,       //     Usage Page (Consumer)
        0x0A, 0x38, 0x02, //     Usage (AC Pan)
        0x15, 0x81,       //     Logical Minimum (-127)
        0x25, 0x7F,       //     Logical Maximum (127)
        0x75, 0x08,       //     Report Size (8)
        0x95, 0x01,       //     Report Count (1)
        0x81, 0x06,       //     Input (Data, Var, Rel)
        0xC0,             //   End Collection
        0xC0,             // End Collection
    ).map { it.toByte() }.toByteArray()

    const val KEYBOARD_REPORT_SIZE = 8
    const val MOUSE_REPORT_SIZE = 5
}

/** Pure functions that build report payloads (without the report ID, which sendReport adds). */
object Reports {
    fun keyboard(modifiers: Int, keys: List<Int>): ByteArray {
        require(keys.size <= 6)
        val r = ByteArray(HidDescriptor.KEYBOARD_REPORT_SIZE)
        r[0] = modifiers.toByte()
        keys.forEachIndexed { i, k -> r[2 + i] = k.toByte() }
        return r
    }

    fun keyboardRelease(): ByteArray = ByteArray(HidDescriptor.KEYBOARD_REPORT_SIZE)

    fun mouse(buttons: Int, dx: Int, dy: Int, wheel: Int = 0, pan: Int = 0): ByteArray = byteArrayOf(
        (buttons and 0x07).toByte(),
        dx.coerceIn(-127, 127).toByte(),
        dy.coerceIn(-127, 127).toByte(),
        wheel.coerceIn(-127, 127).toByte(),
        pan.coerceIn(-127, 127).toByte(),
    )

    /**
     * The report sequence for tapping a chord: modifiers first, then modifiers+keys,
     * then modifiers alone, then all released. Pressing modifiers in their own report first
     * mirrors a physical keyboard and avoids hosts that miss a modifier arriving with the key.
     */
    fun chordTap(chord: KeyChord): List<ByteArray> = buildList {
        if (chord.modifiers != 0) add(keyboard(chord.modifiers, emptyList()))
        add(keyboard(chord.modifiers, chord.keys))
        if (chord.modifiers != 0) add(keyboard(chord.modifiers, emptyList()))
        add(keyboardRelease())
    }

    /** Splits a large relative movement into int8-sized steps. */
    fun splitMove(dx: Int, dy: Int): List<Pair<Int, Int>> {
        val steps = maxOf(1, (maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) + 126) / 127)
        val out = ArrayList<Pair<Int, Int>>(steps)
        var sentX = 0; var sentY = 0
        for (i in 1..steps) {
            val tx = dx * i / steps; val ty = dy * i / steps
            out += (tx - sentX) to (ty - sentY)
            sentX = tx; sentY = ty
        }
        return out
    }
}
