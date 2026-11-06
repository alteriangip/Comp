package com.artcompanion.data

import kotlinx.serialization.Serializable

/** Which drawing app a profile targets. Used for labels and for "reset to defaults". */
@Serializable
enum class TargetApp(val displayName: String) {
    PROCREATE("Procreate"),
    ARTSTUDIO_PRO("Artstudio Pro"),
    CUSTOM("Custom"),
}

/** How a deck button behaves when touched. */
@Serializable
enum class PressMode(val displayName: String, val help: String) {
    TAP("Tap", "Presses and releases the shortcut once"),
    HOLD("Hold", "Keeps the keys down while your finger is on the button"),
    REPEAT("Repeat", "Taps once, then repeats while held (good for brush size)"),
}

@Serializable
data class DeckButton(
    val id: String,
    val label: String,
    /** Text chord, parsed by KeyChord.parse, e.g. "Shift+Cmd+Z". */
    val chord: String,
    /** Key into DeckIcons; null = text only. */
    val icon: String? = null,
    val mode: PressMode = PressMode.TAP,
    /** Width in grid columns, 1..3. */
    val span: Int = 1,
    /** Double-height button. */
    val tall: Boolean = false,
    /** Optional caveat shown in the editor (e.g. "opens a panel"). */
    val note: String? = null,
)

@Serializable
data class DeckPage(
    val id: String,
    val name: String,
    val buttons: List<DeckButton>,
)

/**
 * A complete, independent shortcut configuration. Each profile is stored in its own file, so
 * editing one can never change another.
 */
@Serializable
data class Profile(
    val id: String,
    val name: String,
    val target: TargetApp,
    val pages: List<DeckPage>,
    val builtIn: Boolean = false,
    val schemaVersion: Int = 1,
)

@Serializable
enum class ThemeMode { SYSTEM, DARK, LIGHT }

@Serializable
enum class ButtonScale(val displayName: String, val rowHeightDp: Int) {
    COMPACT("Compact", 64), NORMAL("Normal", 80), LARGE("Large", 100)
}

@Serializable
data class AppSettings(
    val theme: ThemeMode = ThemeMode.DARK,
    val haptics: Boolean = true,
    val buttonScale: ButtonScale = ButtonScale.NORMAL,
    /** 0 = automatic from screen width. */
    val deckColumns: Int = 0,
    val keepScreenOn: Boolean = true,
    val showShortcutHints: Boolean = true,
    val activeProfileId: String = DefaultProfiles.PROCREATE_ID,
    val autoReconnect: Boolean = true,
)
