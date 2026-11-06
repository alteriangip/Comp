package com.artcompanion.hid

/**
 * A keyboard shortcut: a set of modifiers plus zero or more keys pressed together.
 *
 * Text form (stored in profiles, editable by the user): tokens joined by '+',
 * e.g. "Cmd+Shift+Z", "Cmd+=", "Shift+Cmd+-", "B", "Space", "Alt+[".
 * Mac glyphs are accepted too: "⇧⌘Z", "⌘0", "⌥[".
 */
data class KeyChord(val modifiers: Int, val keys: List<Int>) {

    init {
        require(keys.size <= 6) { "A HID boot keyboard report holds at most 6 keys" }
        require(modifiers in 0..0xFF)
    }

    /** Human-readable form using macOS/iPadOS glyphs, e.g. "⇧⌘Z". */
    fun display(): String = Mod.describe(modifiers) + keys.joinToString("") { Usage.describe(it) }

    /** Canonical, parseable text form, e.g. "Shift+Cmd+Z". */
    fun serialize(): String {
        val parts = mutableListOf<String>()
        if (modifiers and Mod.CTRL != 0) parts += "Ctrl"
        if (modifiers and Mod.ALT != 0) parts += "Alt"
        if (modifiers and Mod.SHIFT != 0) parts += "Shift"
        if (modifiers and Mod.GUI != 0) parts += "Cmd"
        keys.forEach { parts += serializeKey(it) }
        return parts.joinToString("+")
    }

    private fun serializeKey(u: Int): String = when (u) {
        Usage.EQUAL -> "="
        Usage.SPACE -> "Space"
        Usage.ENTER -> "Enter"
        Usage.ESCAPE -> "Esc"
        Usage.BACKSPACE -> "Backspace"
        Usage.DELETE_FORWARD -> "ForwardDelete"
        Usage.TAB -> "Tab"
        Usage.LEFT -> "Left"
        Usage.RIGHT -> "Right"
        Usage.UP -> "Up"
        Usage.DOWN -> "Down"
        else -> Usage.describe(u)
    }

    companion object {
        private val modifierNames = mapOf(
            "cmd" to Mod.GUI, "command" to Mod.GUI, "gui" to Mod.GUI, "meta" to Mod.GUI, "win" to Mod.GUI,
            "shift" to Mod.SHIFT,
            "alt" to Mod.ALT, "opt" to Mod.ALT, "option" to Mod.ALT,
            "ctrl" to Mod.CTRL, "control" to Mod.CTRL,
        )
        private val modifierGlyphs = mapOf('⌘' to Mod.GUI, '⇧' to Mod.SHIFT, '⌥' to Mod.ALT, '⌃' to Mod.CTRL)

        /** Parses a chord, throwing [IllegalArgumentException] with a readable message on error. */
        fun parse(text: String): KeyChord {
            var s = text.trim()
            require(s.isNotEmpty()) { "Shortcut is empty" }
            var mods = 0
            // Leading Mac glyphs: "⇧⌘Z"
            while (s.isNotEmpty() && s[0] in modifierGlyphs) {
                mods = mods or modifierGlyphs.getValue(s[0])
                s = s.substring(1).trimStart()
            }
            // Split on '+', but treat a trailing "+" (as in "Cmd++") as the =/+ key.
            val tokens = mutableListOf<String>()
            if (s.endsWith("++")) {
                tokens += s.dropLast(2).split('+').filter { it.isNotBlank() }
                tokens += "+"
            } else if (s == "+") {
                tokens += "+"
            } else {
                tokens += s.split('+').map { it.trim() }.filter { it.isNotEmpty() }
            }
            val keys = mutableListOf<Int>()
            for (raw in tokens) {
                val t = raw.trim()
                val lower = t.lowercase()
                val modBit = modifierNames[lower]
                if (modBit != null) { mods = mods or modBit; continue }
                keys += parseKey(t)?.let { (usage, shift) ->
                    if (shift) mods = mods or Mod.SHIFT
                    usage
                } ?: throw IllegalArgumentException("Unknown key \"$t\"")
            }
            require(keys.isNotEmpty() || mods != 0) { "No key in \"$text\"" }
            return KeyChord(mods, keys.distinct())
        }

        fun parseOrNull(text: String): KeyChord? = runCatching { parse(text) }.getOrNull()

        private fun parseKey(t: String): Pair<Int, Boolean>? {
            Usage.named[t.lowercase()]?.let { return it to false }
            if (t.length == 1) {
                val c = t[0]
                if (c in 'A'..'Z') return Usage.letter(c) to false // "Cmd+Z" means z, not Shift+z
                return Usage.forChar(c)
            }
            return null
        }
    }
}
