package com.artcompanion.ui.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.artcompanion.data.DeckButton
import com.artcompanion.data.PressMode
import com.artcompanion.data.ProfileRepository
import com.artcompanion.hid.KeyChord
import com.artcompanion.hid.Mod
import com.artcompanion.input.SendResult
import com.artcompanion.ui.DeckIcons

/**
 * Full-screen editor for one deck button. The shortcut is built from modifier toggles plus one
 * key, then stored as text, so it stays human-readable in backups.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ButtonEditorDialog(
    initial: DeckButton?,
    profileName: String,
    indexInPage: Int,
    pageSize: Int,
    onTest: (KeyChord) -> SendResult,
    onSave: (DeckButton) -> Unit,
    onDelete: () -> Unit,
    onMove: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val start = remember(initial) { initial?.chord?.let { KeyChord.parseOrNull(it) } }
    var label by rememberSaveable { mutableStateOf(initial?.label ?: "") }
    var mods by rememberSaveable { mutableIntStateOf(start?.modifiers ?: 0) }
    var keyText by rememberSaveable {
        mutableStateOf(start?.keys?.takeIf { it.isNotEmpty() }?.let { KeyChord(0, it).serialize() } ?: "")
    }
    var icon by rememberSaveable { mutableStateOf(initial?.icon) }
    var mode by rememberSaveable { mutableStateOf(initial?.mode ?: PressMode.TAP) }
    var span by rememberSaveable { mutableIntStateOf(initial?.span ?: 1) }
    var tall by rememberSaveable { mutableStateOf(initial?.tall ?: false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    // Compose the chord: modifiers from the toggles, key (which may itself imply Shift, e.g. "?").
    val chord: Result<KeyChord> = remember(mods, keyText) {
        runCatching {
            if (keyText.isBlank()) {
                require(mods != 0) { "Choose a key or at least one modifier" }
                KeyChord(mods, emptyList())
            } else {
                val k = KeyChord.parse(keyText)
                require(k.keys.size == 1) { "Enter a single key; use the toggles for modifiers" }
                KeyChord(mods or k.modifiers, k.keys)
            }
        }
    }
    val canSave = label.isNotBlank() && chord.isSuccess

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.safeDrawingPadding().imePadding()) {
                Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Cancel") }
                    Column(Modifier.weight(1f)) {
                        Text(if (initial == null) "New button" else "Edit button", style = MaterialTheme.typography.titleMedium)
                        Text(profileName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(enabled = canSave, onClick = {
                        onSave(
                            DeckButton(
                                id = initial?.id ?: ("b_" + ProfileRepository.newId()),
                                label = label.trim(),
                                chord = chord.getOrThrow().serialize(),
                                icon = icon,
                                mode = mode,
                                span = span,
                                tall = tall,
                                note = initial?.note?.takeIf { initial.chord == chord.getOrThrow().serialize() },
                            )
                        )
                    }) { Text("Save") }
                }

                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedTextField(
                        value = label, onValueChange = { label = it.take(24) },
                        label = { Text("Label") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )

                    Text("Shortcut", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Mod.GUI to "⌘ Cmd", Mod.SHIFT to "⇧ Shift", Mod.ALT to "⌥ Opt", Mod.CTRL to "⌃ Ctrl").forEach { (bit, name) ->
                            FilterChip(selected = mods and bit != 0, onClick = { mods = mods xor bit }, label = { Text(name) })
                        }
                    }
                    OutlinedTextField(
                        value = keyText,
                        onValueChange = { keyText = it.take(16) },
                        label = { Text("Key") },
                        singleLine = true,
                        isError = chord.isFailure,
                        supportingText = {
                            Text(
                                chord.fold(
                                    { "Sends ${it.display()}" },
                                    { it.message ?: "Invalid" },
                                ) + "\nLetter, digit, symbol ([ ] = - , . / ;) or Space, Enter, Esc, Tab, Backspace, Left, Right, Up, Down, F1–F12",
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(enabled = chord.isSuccess, onClick = {
                            testResult = when (onTest(chord.getOrThrow())) {
                                SendResult.SENT -> "Sent ${chord.getOrThrow().display()} to the iPad"
                                SendResult.NOT_CONNECTED -> "Not connected"
                                SendResult.INVALID -> "Invalid shortcut"
                            }
                        }) { Text("Test on iPad") }
                        testResult?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    initial?.note?.let {
                        Text("Note: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Text("Behaviour", style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        PressMode.entries.forEachIndexed { i, m ->
                            SegmentedButton(
                                selected = mode == m, onClick = { mode = m },
                                shape = SegmentedButtonDefaults.itemShape(i, PressMode.entries.size),
                            ) { Text(m.displayName) }
                        }
                    }
                    Text(mode.help, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Text("Size", style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        (1..3).forEach { w ->
                            SegmentedButton(
                                selected = span == w, onClick = { span = w },
                                shape = SegmentedButtonDefaults.itemShape(w - 1, 3),
                            ) { Text(if (w == 1) "1 column" else "$w columns") }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Double height", Modifier.weight(1f))
                        Switch(checked = tall, onCheckedChange = { tall = it })
                    }

                    Text("Icon", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconChoice(selected = icon == null, onClick = { icon = null }) {
                            Icon(Icons.Outlined.Block, "No icon")
                        }
                        DeckIcons.all.forEach { (name, vec) ->
                            IconChoice(selected = icon == name, onClick = { icon = name }) { Icon(vec, name) }
                        }
                    }

                    if (initial != null) {
                        Text("Position", style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onMove(-1) }, enabled = indexInPage > 0) {
                                Icon(Icons.Outlined.ChevronLeft, null); Text("Earlier")
                            }
                            OutlinedButton(onClick = { onMove(1) }, enabled = indexInPage in 0 until pageSize - 1) {
                                Text("Later"); Icon(Icons.Outlined.ChevronRight, null)
                            }
                            Text("${indexInPage + 1} of $pageSize", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.size(8.dp))
                        Button(
                            onClick = onDelete,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.background,
                            ),
                        ) { Text("Delete button") }
                    }
                    Text(
                        "Shortcuts are sent exactly as configured. ArtCompanion cannot check what the drawing app does with them.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Default,
                    )
                    Spacer(Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun IconChoice(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .size(48.dp)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape)
            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
