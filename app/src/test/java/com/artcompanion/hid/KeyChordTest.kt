package com.artcompanion.hid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyChordTest {

    @Test fun undo() {
        val c = KeyChord.parse("Cmd+Z")
        assertEquals(Mod.GUI, c.modifiers)
        assertEquals(listOf(0x1D), c.keys)
        assertEquals("⌘Z", c.display())
    }

    @Test fun redoTextAndGlyphFormsAgree() {
        assertEquals(KeyChord.parse("Shift+Cmd+Z"), KeyChord.parse("⇧⌘Z"))
        assertEquals(KeyChord.parse("Cmd+Shift+Z"), KeyChord.parse("cmd + shift + z"))
    }

    @Test fun uppercaseLetterDoesNotImplyShift() {
        assertEquals(0, KeyChord.parse("B").modifiers)
    }

    @Test fun punctuationAndNamedKeys() {
        assertEquals(listOf(Usage.EQUAL), KeyChord.parse("Cmd+=").keys)
        assertEquals(listOf(Usage.MINUS), KeyChord.parse("Shift+Cmd+-").keys)
        assertEquals(listOf(Usage.RIGHT_BRACKET), KeyChord.parse("]").keys)
        assertEquals(listOf(Usage.SPACE), KeyChord.parse("Space").keys)
        assertEquals(listOf(Usage.BACKSPACE), KeyChord.parse("Cmd+Backspace").keys)
        assertEquals(listOf(Usage.COMMA), KeyChord.parse("Shift+Cmd+,").keys)
        assertEquals(listOf(0x27), KeyChord.parse("⌘0").keys)
    }

    @Test fun plusKey() {
        val c = KeyChord.parse("Cmd++")
        assertEquals(listOf(Usage.EQUAL), c.keys)
        assertTrue(c.modifiers and Mod.SHIFT != 0) // '+' is Shift+= on a US layout
    }

    @Test fun serializeRoundTrips() {
        listOf("Cmd+Z", "Shift+Cmd+Z", "Alt+[", "Space", "Cmd+=", "Shift+Cmd+,", "F", "Ctrl+Alt+Cmd+1").forEach {
            val c = KeyChord.parse(it)
            assertEquals(it, c, KeyChord.parse(c.serialize()))
        }
    }

    @Test(expected = IllegalArgumentException::class) fun unknownKeyRejected() {
        KeyChord.parse("Cmd+Banana")
    }

    @Test fun singleKeySerializationIsParseable() {
        // The button editor shows KeyChord(0, keys).serialize() in its key field.
        listOf("Space", "Enter", "Esc", "Tab", "Backspace", "ForwardDelete", "Left", "F5", "[", "=", ",", "Z", "0").forEach {
            val c = KeyChord.parse(it)
            assertEquals(it, c, KeyChord.parse(KeyChord(0, c.keys).serialize()))
        }
    }

    @Test fun modifierOnlyChord() {
        val c = KeyChord.parse("Alt")
        assertEquals(Mod.ALT, c.modifiers)
        assertTrue(c.keys.isEmpty())
    }
}
