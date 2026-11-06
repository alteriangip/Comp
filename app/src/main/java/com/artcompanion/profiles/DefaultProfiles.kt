package com.artcompanion.profiles

/**
 * Default shortcut sets, limited to shortcuts that each app's OFFICIAL documentation lists.
 * Sources:
 *  - Procreate: help.procreate.com/procreate/handbook/interface-gestures/keyboard (5.x handbook)
 *  - Artstudio Pro: luckyclan.com/artstudiopro/artstudio_pro_manual.pdf, "Keyboard Shortcuts" (pp. 132–135)
 *
 * Shortcuts are stored as text (see KeyChord.parse) so that Phase 3 can make them user-editable
 * without changing code. Commands absent from an app's documentation are not listed here; the
 * compatibility matrix in docs/01-feasibility-report.md records them as unsupported or user-assignable.
 */
enum class AppProfile(val displayName: String) { PROCREATE("Procreate"), ARTSTUDIO_PRO("Artstudio Pro"), CUSTOM("Custom") }

data class ShortcutDef(
    val id: String,
    val label: String,
    val chord: String,
    /** True if a single tap of the key does what the label says; false if it opens a panel instead. */
    val note: String? = null,
)

object DefaultProfiles {

    val procreate: List<ShortcutDef> = listOf(
        ShortcutDef("undo", "Undo", "Cmd+Z"),
        ShortcutDef("redo", "Redo", "Shift+Cmd+Z"),
        ShortcutDef("brush", "Brush", "B", "Opens Brushes if Paint is already active"),
        ShortcutDef("eraser", "Eraser", "E", "Opens brushes if Erase is already active"),
        ShortcutDef("size_up", "Size +5%", "]"),
        ShortcutDef("size_down", "Size −5%", "["),
        ShortcutDef("size_up_10", "Size +10%", "Shift+]"),
        ShortcutDef("size_down_10", "Size −10%", "Shift+["),
        ShortcutDef("fullscreen", "Fullscreen", "Cmd+0", "Toggles fullscreen within the Procreate interface"),
        ShortcutDef("swap_color", "Prev colour", "X", "Switches between current and previous colour"),
        ShortcutDef("quickmenu", "QuickMenu", "Space"),
        ShortcutDef("layers", "Layers", "L", "Opens the Layers panel"),
        ShortcutDef("colors", "Colours", "C", "Opens the Colours panel"),
        ShortcutDef("selection", "Select", "S"),
        ShortcutDef("transform", "Transform", "V"),
        ShortcutDef("duplicate", "Duplicate", "Cmd+J", "Duplicates layer/selection to a new layer above"),
        ShortcutDef("deselect", "Deselect", "Cmd+D"),
        ShortcutDef("clear_layer", "Clear layer", "Cmd+Backspace", "Erases layer content (does not delete the layer)"),
        ShortcutDef("actions", "Actions", "Cmd+K"),
    )

    val artstudioPro: List<ShortcutDef> = listOf(
        ShortcutDef("undo", "Undo", "Cmd+Z"),
        ShortcutDef("redo", "Redo", "Shift+Cmd+Z"),
        ShortcutDef("brush", "Brush", "B"),
        ShortcutDef("eraser", "Eraser", "E"),
        ShortcutDef("eyedropper", "Eyedropper", "I"),
        ShortcutDef("size_up", "Size +", "]"),
        ShortcutDef("size_down", "Size −", "["),
        ShortcutDef("save", "Save", "Cmd+S"),
        ShortcutDef("new_layer", "New layer", "Shift+Cmd+N"),
        ShortcutDef("merge_down", "Merge down", "Cmd+E"),
        ShortcutDef("hide_layers", "Hide/Show", "Shift+Cmd+,", "Manual: \"Hide/Show Layers\""),
        ShortcutDef("mirror", "Flip view", "Shift+Cmd+M", "Manual: View > Mirror"),
        ShortcutDef("fit", "Fit", "Cmd+0"),
        ShortcutDef("zoom_in", "Zoom in", "Cmd+="),
        ShortcutDef("zoom_out", "Zoom out", "Cmd+-"),
        ShortcutDef("zoom_100", "100%", "Cmd+1"),
        ShortcutDef("rotate_left", "Rotate ⟲", "Shift+Cmd+-"),
        ShortcutDef("rotate_right", "Rotate ⟳", "Shift+Cmd+="),
        ShortcutDef("fullscreen", "Fullscreen", "F"),
        ShortcutDef("pan_tool", "Pan tool", "H"),
        ShortcutDef("rotate_tool", "Rotate tool", "R"),
        ShortcutDef("swap_color", "Old/New col.", "X"),
        ShortcutDef("transform", "Transform", "Cmd+T"),
        ShortcutDef("deselect", "Deselect", "Cmd+D"),
    )

    fun forProfile(p: AppProfile): List<ShortcutDef> = when (p) {
        AppProfile.PROCREATE -> procreate
        AppProfile.ARTSTUDIO_PRO -> artstudioPro
        AppProfile.CUSTOM -> listOf(ShortcutDef("undo", "Undo", "Cmd+Z"), ShortcutDef("redo", "Redo", "Shift+Cmd+Z"))
    }
}
