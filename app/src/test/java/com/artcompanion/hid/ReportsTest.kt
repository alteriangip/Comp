package com.artcompanion.hid

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportsTest {

    private fun bytes(vararg v: Int) = v.map { it.toByte() }.toByteArray()

    @Test fun undoReportSequence() {
        val seq = Reports.chordTap(KeyChord.parse("Cmd+Z"))
        assertEquals(4, seq.size)
        assertArrayEquals(bytes(0x08, 0, 0, 0, 0, 0, 0, 0), seq[0])
        assertArrayEquals(bytes(0x08, 0, 0x1D, 0, 0, 0, 0, 0), seq[1])
        assertArrayEquals(bytes(0x08, 0, 0, 0, 0, 0, 0, 0), seq[2])
        assertArrayEquals(bytes(0, 0, 0, 0, 0, 0, 0, 0), seq[3])
    }

    @Test fun plainKeyHasNoModifierReports() {
        val seq = Reports.chordTap(KeyChord.parse("B"))
        assertEquals(2, seq.size)
        assertArrayEquals(bytes(0, 0, 0x05, 0, 0, 0, 0, 0), seq[0])
    }

    @Test fun mouseClampsAxes() {
        assertArrayEquals(bytes(1, 127, -127, 0, 0), Reports.mouse(1, 500, -500))
    }

    @Test fun splitMovePreservesTotal() {
        val parts = Reports.splitMove(400, -130)
        assertEquals(400, parts.sumOf { it.first })
        assertEquals(-130, parts.sumOf { it.second })
        parts.forEach { (x, y) -> assert(x in -127..127 && y in -127..127) }
    }

    /** Walks the descriptor's short items and checks the input report sizes match what Reports builds. */
    @Test fun descriptorMatchesReportSizes() {
        val d = HidDescriptor.bytes
        val inputBits = HashMap<Int, Int>()
        var i = 0; var size = 0; var count = 0; var id = 0; var depth = 0
        while (i < d.size) {
            val prefix = d[i].toInt() and 0xFF
            val len = when (prefix and 0x03) { 3 -> 4; else -> prefix and 0x03 }
            val data = (0 until len).fold(0) { acc, k -> acc or ((d[i + 1 + k].toInt() and 0xFF) shl (8 * k)) }
            when (prefix and 0xFC) {
                0x74 -> size = data
                0x94 -> count = data
                0x84 -> id = data
                0x80 -> inputBits[id] = (inputBits[id] ?: 0) + size * count
                0xA0 -> depth++
                0xC0 -> depth--
            }
            i += 1 + len
        }
        assertEquals(0, depth)
        assertEquals(HidDescriptor.KEYBOARD_REPORT_SIZE * 8, inputBits[HidDescriptor.REPORT_ID_KEYBOARD])
        assertEquals(HidDescriptor.MOUSE_REPORT_SIZE * 8, inputBits[HidDescriptor.REPORT_ID_MOUSE])
    }

    @Test fun typingMap() {
        assertEquals(Usage.letter('h') to true, Usage.forChar('H'))
        assertEquals(0x1E to true, Usage.forChar('!'))
        assertEquals(Usage.SPACE to false, Usage.forChar(' '))
    }
}
