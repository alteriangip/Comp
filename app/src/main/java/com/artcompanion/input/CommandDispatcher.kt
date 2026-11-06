package com.artcompanion.input

import com.artcompanion.hid.HidController
import com.artcompanion.hid.KeyChord

/** Result of trying to send a command, so the UI can give honest feedback. */
enum class SendResult { SENT, NOT_CONNECTED, INVALID }

/**
 * The single place UI actions turn into HID input. Keeping this separate from the screens
 * means the deck, radial menu and other modules (Phase 4) share identical behaviour.
 */
class CommandDispatcher(private val hid: HidController) {

    fun tap(chordText: String): SendResult {
        val chord = KeyChord.parseOrNull(chordText) ?: return SendResult.INVALID
        return if (hid.tap(chord)) SendResult.SENT else SendResult.NOT_CONNECTED
    }

    fun tap(chord: KeyChord): SendResult = if (hid.tap(chord)) SendResult.SENT else SendResult.NOT_CONNECTED

    fun press(chord: KeyChord): SendResult = if (hid.press(chord)) SendResult.SENT else SendResult.NOT_CONNECTED

    fun release(chord: KeyChord) = hid.release(chord)

    fun releaseAll() = hid.releaseAll()

    val isConnected: Boolean get() = hid.isConnected
}
