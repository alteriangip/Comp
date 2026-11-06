package com.artcompanion.hid

/**
 * USB HID usage IDs (Keyboard/Keypad page 0x07) and modifier bits.
 *
 * These are the codes a Bluetooth HID keyboard puts in its input report. iPadOS maps the
 * GUI modifier to Command (⌘) and Alt to Option (⌥).
 */
object Mod {
    const val CTRL = 0x01
    const val SHIFT = 0x02
    const val ALT = 0x04   // Option ⌥ on iPadOS
    const val GUI = 0x08   // Command ⌘ on iPadOS

    fun describe(mods: Int): String = buildString {
        if (mods and CTRL != 0) append("⌃")
        if (mods and ALT != 0) append("⌥")
        if (mods and SHIFT != 0) append("⇧")
        if (mods and GUI != 0) append("⌘")
    }
}

object Usage {
    const val A = 0x04
    const val ENTER = 0x28
    const val ESCAPE = 0x29
    const val BACKSPACE = 0x2A
    const val TAB = 0x2B
    const val SPACE = 0x2C
    const val MINUS = 0x2D
    const val EQUAL = 0x2E
    const val LEFT_BRACKET = 0x2F
    const val RIGHT_BRACKET = 0x30
    const val BACKSLASH = 0x31
    const val SEMICOLON = 0x33
    const val APOSTROPHE = 0x34
    const val GRAVE = 0x35
    const val COMMA = 0x36
    const val PERIOD = 0x37
    const val SLASH = 0x38
    const val F1 = 0x3A
    const val DELETE_FORWARD = 0x4C
    const val RIGHT = 0x4F
    const val LEFT = 0x50
    const val DOWN = 0x51
    const val UP = 0x52

    fun letter(c: Char): Int = A + (c.lowercaseChar() - 'a')

    fun digit(c: Char): Int = if (c == '0') 0x27 else 0x1E + (c - '1')

    /** Named keys accepted by [KeyChord.parse] (case-insensitive). */
    val named: Map<String, Int> = buildMap {
        put("enter", ENTER); put("return", ENTER)
        put("esc", ESCAPE); put("escape", ESCAPE)
        put("backspace", BACKSPACE); put("delete", BACKSPACE); put("⌫", BACKSPACE)
        put("forwarddelete", DELETE_FORWARD); put("⌦", DELETE_FORWARD)
        put("tab", TAB)
        put("space", SPACE); put("spacebar", SPACE)
        put("left", LEFT); put("right", RIGHT); put("up", UP); put("down", DOWN)
        put("◀", LEFT); put("►", RIGHT); put("▶", RIGHT); put("▲", UP); put("▼", DOWN)
        put("plus", EQUAL) // '+' is the separator; "plus" means the =/+ key
        for (i in 1..12) put("f$i", F1 + i - 1)
    }

    /** Unshifted punctuation on a US layout. */
    val punctuation: Map<Char, Int> = mapOf(
        '-' to MINUS, '=' to EQUAL, '[' to LEFT_BRACKET, ']' to RIGHT_BRACKET,
        '\\' to BACKSLASH, ';' to SEMICOLON, '\'' to APOSTROPHE, '’' to APOSTROPHE,
        '`' to GRAVE, ',' to COMMA, '.' to PERIOD, '/' to SLASH, ' ' to SPACE,
    )

    /** Shifted punctuation on a US layout -> base key. */
    val shiftedPunctuation: Map<Char, Int> = mapOf(
        '_' to MINUS, '+' to EQUAL, '{' to LEFT_BRACKET, '}' to RIGHT_BRACKET,
        '|' to BACKSLASH, ':' to SEMICOLON, '"' to APOSTROPHE, '~' to GRAVE,
        '<' to COMMA, '>' to PERIOD, '?' to SLASH,
        '!' to 0x1E, '@' to 0x1F, '#' to 0x20, '$' to 0x21, '%' to 0x22,
        '^' to 0x23, '&' to 0x24, '*' to 0x25, '(' to 0x26, ')' to 0x27,
    )

    private val reverse: Map<Int, String> by lazy {
        val m = HashMap<Int, String>()
        for (c in 'A'..'Z') m[letter(c)] = c.toString()
        for (c in '0'..'9') m[digit(c)] = c.toString()
        punctuation.forEach { (c, u) -> if (c != ' ' && c != '’') m[u] = c.toString() }
        m[SPACE] = "Space"; m[ENTER] = "Return"; m[ESCAPE] = "Esc"; m[BACKSPACE] = "⌫"
        m[TAB] = "Tab"; m[DELETE_FORWARD] = "⌦"
        m[LEFT] = "◀"; m[RIGHT] = "▶"; m[UP] = "▲"; m[DOWN] = "▼"
        for (i in 1..12) m[F1 + i - 1] = "F$i"
        m
    }

    fun describe(usage: Int): String = reverse[usage] ?: "0x%02X".format(usage)

    /** Maps a printable character to (usage, needsShift) for a US keyboard layout. */
    fun forChar(c: Char): Pair<Int, Boolean>? = when {
        c in 'a'..'z' -> letter(c) to false
        c in 'A'..'Z' -> letter(c) to true
        c in '0'..'9' -> digit(c) to false
        c == '\n' -> ENTER to false
        c in punctuation -> punctuation.getValue(c) to false
        c in shiftedPunctuation -> shiftedPunctuation.getValue(c) to true
        else -> null
    }
}
