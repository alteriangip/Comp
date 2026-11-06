package com.artcompanion.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallMerge
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Icons a deck button can use, keyed by a stable name that is stored in profile JSON.
 * Only these vectors are referenced, so R8 strips the rest of the icon library from release builds.
 */
object DeckIcons {
    val all: LinkedHashMap<String, ImageVector> = linkedMapOf(
        "undo" to Icons.AutoMirrored.Outlined.Undo,
        "redo" to Icons.AutoMirrored.Outlined.Redo,
        "brush" to Icons.Outlined.Brush,
        "eraser" to Icons.Outlined.AutoFixNormal,
        "eyedropper" to Icons.Outlined.Colorize,
        "size_up" to Icons.Outlined.AddCircleOutline,
        "size_down" to Icons.Outlined.RemoveCircleOutline,
        "save" to Icons.Outlined.Save,
        "layers" to Icons.Outlined.Layers,
        "layer_add" to Icons.Outlined.LibraryAdd,
        "merge" to Icons.AutoMirrored.Outlined.CallMerge,
        "visibility" to Icons.Outlined.Visibility,
        "clear" to Icons.Outlined.LayersClear,
        "delete" to Icons.Outlined.DeleteOutline,
        "flip" to Icons.Outlined.Flip,
        "fit" to Icons.Outlined.FitScreen,
        "fullscreen" to Icons.Outlined.Fullscreen,
        "zoom_in" to Icons.Outlined.ZoomIn,
        "zoom_out" to Icons.Outlined.ZoomOut,
        "zoom_100" to Icons.Outlined.CropFree,
        "rotate_left" to Icons.Outlined.RotateLeft,
        "rotate_right" to Icons.Outlined.RotateRight,
        "pan" to Icons.Outlined.PanTool,
        "palette" to Icons.Outlined.Palette,
        "swap" to Icons.Outlined.SwapHoriz,
        "menu" to Icons.Outlined.Menu,
        "settings" to Icons.Outlined.Tune,
        "select" to Icons.Outlined.HighlightAlt,
        "deselect" to Icons.Outlined.Deselect,
        "transform" to Icons.Outlined.Transform,
        "copy" to Icons.Outlined.ContentCopy,
        "modify" to Icons.Outlined.KeyboardOptionKey,
        "command" to Icons.Outlined.KeyboardCommandKey,
        "arrow_up" to Icons.Outlined.ArrowUpward,
        "arrow_down" to Icons.Outlined.ArrowDownward,
        "move" to Icons.Outlined.OpenWith,
        "crop" to Icons.Outlined.Crop,
        "fill" to Icons.Outlined.FormatColorFill,
        "gradient" to Icons.Outlined.Gradient,
        "text" to Icons.Outlined.TextFields,
        "grid" to Icons.Outlined.GridOn,
        "ruler" to Icons.Outlined.Straighten,
        "opacity" to Icons.Outlined.Opacity,
        "draw" to Icons.Outlined.Draw,
        "gesture" to Icons.Outlined.Gesture,
        "touch" to Icons.Outlined.TouchApp,
        "space" to Icons.Outlined.SpaceBar,
        "star" to Icons.Outlined.Star,
        "favorite" to Icons.Outlined.Favorite,
        "bolt" to Icons.Outlined.Bolt,
        "bookmark" to Icons.Outlined.Bookmark,
        "keyboard" to Icons.Outlined.Keyboard,
    )

    operator fun get(name: String?): ImageVector? = name?.let { all[it] }
}
