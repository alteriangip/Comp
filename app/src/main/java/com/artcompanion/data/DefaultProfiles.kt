package com.artcompanion.data

/**
 * Default decks, using only shortcuts that each vendor's OFFICIAL documentation lists:
 *  - Procreate: help.procreate.com/procreate/handbook/interface-gestures/keyboard
 *  - Artstudio Pro: luckyclan.com/artstudiopro/artstudio_pro_manual.pdf, "Keyboard Shortcuts" pp. 132–135
 *
 * Requested commands that an app does not document are NOT given a made-up key. See the
 * compatibility matrix in docs/01-feasibility-report.md. Every button here is user-editable.
 */
object DefaultProfiles {
    const val PROCREATE_ID = "procreate"
    const val ARTSTUDIO_ID = "artstudio_pro"
    const val CUSTOM_ID = "custom"

    private fun b(
        id: String, label: String, chord: String, icon: String? = null,
        mode: PressMode = PressMode.TAP, span: Int = 1, tall: Boolean = false, note: String? = null,
    ) = DeckButton(id, label, chord, icon, mode, span, tall, note)

    fun procreate() = Profile(
        id = PROCREATE_ID, name = "Procreate", target = TargetApp.PROCREATE, builtIn = true,
        pages = listOf(
            DeckPage("p_main", "Draw", listOf(
                b("undo", "Undo", "Cmd+Z", "undo", PressMode.REPEAT, tall = true),
                b("redo", "Redo", "Shift+Cmd+Z", "redo", PressMode.REPEAT, tall = true),
                b("brush", "Brush", "B", "brush", note = "Opens Brushes if Paint is already active"),
                b("eraser", "Eraser", "E", "eraser", note = "Opens Brushes if Erase is already active"),
                b("size_down", "Size −5%", "[", "size_down", PressMode.REPEAT),
                b("size_up", "Size +5%", "]", "size_up", PressMode.REPEAT),
                b("modify", "Modify (hold)", "Alt", "modify", PressMode.HOLD,
                    note = "Holds the Modify button (⌥ on iPadOS 18+, use M on iPadOS 17 and below). " +
                        "Set Procreate › Gesture controls › Eyedropper to \"□ + Touch\" to use it as an eyedropper."),
                b("swap_color", "Prev colour", "X", "swap", note = "Switches between current and previous colour"),
                b("quickmenu", "QuickMenu", "Space", "menu"),
                b("fullscreen", "Fullscreen", "Cmd+0", "fullscreen", note = "Toggles fullscreen within the Procreate interface"),
            )),
            DeckPage("p_panels", "Panels & edit", listOf(
                b("layers", "Layers", "L", "layers", note = "Opens the Layers panel (Procreate has no new/delete/merge layer keys)"),
                b("colors", "Colours", "C", "palette", note = "Opens the Colours panel"),
                b("actions", "Actions", "Cmd+K", "settings", note = "Actions menu: Flip canvas is under Canvas"),
                b("selection", "Select", "S", "select"),
                b("transform", "Transform", "V", "transform"),
                b("deselect", "Deselect", "Cmd+D", "deselect"),
                b("duplicate", "Duplicate", "Cmd+J", "copy", note = "Duplicates layer or selection to a new layer above"),
                b("clear_layer", "Clear layer", "Cmd+Backspace", "clear", note = "Erases layer content; does not delete the layer"),
                b("size_down_1", "Size −1%", "Cmd+[", "size_down", PressMode.REPEAT),
                b("size_up_1", "Size +1%", "Cmd+]", "size_up", PressMode.REPEAT),
                b("size_down_10", "Size −10%", "Shift+[", "size_down", PressMode.REPEAT),
                b("size_up_10", "Size +10%", "Shift+]", "size_up", PressMode.REPEAT),
            )),
        ),
    )

    fun artstudioPro() = Profile(
        id = ARTSTUDIO_ID, name = "Artstudio Pro", target = TargetApp.ARTSTUDIO_PRO, builtIn = true,
        pages = listOf(
            DeckPage("a_main", "Draw", listOf(
                b("undo", "Undo", "Cmd+Z", "undo", PressMode.REPEAT, tall = true),
                b("redo", "Redo", "Shift+Cmd+Z", "redo", PressMode.REPEAT, tall = true),
                b("brush", "Brush", "B", "brush"),
                b("eraser", "Eraser", "E", "eraser"),
                b("eyedropper", "Eyedropper", "I", "eyedropper"),
                b("swap_color", "Old/New col.", "X", "swap"),
                b("size_down", "Size −", "[", "size_down", PressMode.REPEAT),
                b("size_up", "Size +", "]", "size_up", PressMode.REPEAT),
                b("save", "Save", "Cmd+S", "save"),
                b("fullscreen", "Fullscreen", "F", "fullscreen"),
            )),
            DeckPage("a_layers", "Layers", listOf(
                b("new_layer", "New layer", "Shift+Cmd+N", "layer_add"),
                b("merge_down", "Merge down", "Cmd+E", "merge"),
                b("merge_visible", "Merge visible", "Shift+Cmd+E", "merge"),
                b("hide_layers", "Hide/Show", "Shift+Cmd+,", "visibility", note = "Manual: \"Hide/Show Layers\""),
                b("layer_next", "Next layer", "Alt+]", "arrow_up", note = "Manual: Layer › Select Next"),
                b("layer_prev", "Prev layer", "Alt+[", "arrow_down", note = "Manual: Layer › Select Previous"),
                b("dup_layer", "Layer via copy", "Cmd+J", "copy"),
                b("transform", "Transform", "Cmd+T", "transform"),
                b("deselect", "Deselect", "Cmd+D", "deselect"),
            )),
            DeckPage("a_view", "View", listOf(
                b("zoom_in", "Zoom in", "Cmd+=", "zoom_in", PressMode.REPEAT),
                b("zoom_out", "Zoom out", "Cmd+-", "zoom_out", PressMode.REPEAT),
                b("rotate_left", "Rotate ⟲", "Shift+Cmd+-", "rotate_left", PressMode.REPEAT),
                b("rotate_right", "Rotate ⟳", "Shift+Cmd+=", "rotate_right", PressMode.REPEAT),
                b("fit", "Fit", "Cmd+0", "fit"),
                b("zoom_100", "100%", "Cmd+1", "zoom_100"),
                b("mirror", "Flip view", "Shift+Cmd+M", "flip", note = "Manual: View › Mirror"),
                b("pan_tool", "Pan tool", "H", "pan"),
                b("rotate_tool", "Rotate tool", "R", "rotate_right"),
            )),
        ),
    )

    fun custom() = Profile(
        id = CUSTOM_ID, name = "Custom", target = TargetApp.CUSTOM, builtIn = true,
        pages = listOf(
            DeckPage("c_main", "Page 1", listOf(
                b("undo", "Undo", "Cmd+Z", "undo", PressMode.REPEAT),
                b("redo", "Redo", "Shift+Cmd+Z", "redo", PressMode.REPEAT),
            )),
        ),
    )

    fun all(): List<Profile> = listOf(procreate(), artstudioPro(), custom())

    fun forTarget(t: TargetApp): Profile = when (t) {
        TargetApp.PROCREATE -> procreate()
        TargetApp.ARTSTUDIO_PRO -> artstudioPro()
        TargetApp.CUSTOM -> custom()
    }

    fun builtInById(id: String): Profile? = all().firstOrNull { it.id == id }
}
