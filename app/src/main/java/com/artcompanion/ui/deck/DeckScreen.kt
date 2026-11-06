package com.artcompanion.ui.deck

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artcompanion.data.AppSettings
import com.artcompanion.data.DeckButton
import com.artcompanion.data.DeckPage
import com.artcompanion.data.PressMode
import com.artcompanion.data.Profile
import com.artcompanion.hid.HidState
import com.artcompanion.hid.KeyChord
import com.artcompanion.input.SendResult
import com.artcompanion.ui.AppViewModel
import com.artcompanion.ui.DeckIcons
import com.artcompanion.ui.Haptics
import com.artcompanion.ui.rememberHaptics
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val REPEAT_DELAY_MS = 380L
private const val REPEAT_INTERVAL_MS = 90L

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DeckScreen(vm: AppViewModel, onOpenConnection: () -> Unit, showMessage: (String) -> Unit) {
    val profile by vm.activeProfile.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val hidState by vm.hid.state.collectAsStateWithLifecycle()
    val connected = hidState is HidState.Connected
    val haptics = rememberHaptics(settings.haptics)

    var editMode by rememberSaveable { mutableStateOf(false) }
    var pageIndex by rememberSaveable(profile?.id) { mutableStateOf(0) }
    var editing by remember { mutableStateOf<Pair<String, DeckButton?>?>(null) } // pageId to button (null = new)
    var pageDialog by remember { mutableStateOf<PageDialog?>(null) }

    val p = profile ?: return
    val safeIndex = pageIndex.coerceIn(0, (p.pages.size - 1).coerceAtLeast(0))
    val page = p.pages.getOrNull(safeIndex)

    Column(Modifier.fillMaxSize()) {
        DeckTopBar(
            profile = p,
            profiles = profiles,
            hidState = hidState,
            editMode = editMode,
            onSelectProfile = { vm.selectProfile(it) },
            onToggleEdit = { editMode = !editMode },
            onOpenConnection = onOpenConnection,
        )

        if (p.pages.size > 1 || editMode) {
            PrimaryScrollableTabRow(
                selectedTabIndex = safeIndex,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                p.pages.forEachIndexed { i, pg ->
                    Tab(selected = i == safeIndex, onClick = { pageIndex = i }, text = { Text(pg.name, maxLines = 1) })
                }
                if (editMode) {
                    Tab(selected = false, onClick = { pageDialog = PageDialog.Add }, icon = { Icon(Icons.Outlined.Add, "Add page") })
                }
            }
        }

        if (editMode && page != null) {
            PageEditBar(
                canDelete = p.pages.size > 1,
                canMoveLeft = safeIndex > 0,
                canMoveRight = safeIndex < p.pages.lastIndex,
                onRename = { pageDialog = PageDialog.Rename(page) },
                onDelete = { pageDialog = PageDialog.Delete(page) },
                onMove = { d -> vm.movePage(p.id, page.id, d); pageIndex = (safeIndex + d).coerceIn(0, p.pages.lastIndex) },
            )
        }

        if (page != null) {
            DeckGrid(
                page = page,
                settings = settings,
                editMode = editMode,
                connected = connected,
                haptics = haptics,
                onSend = { result ->
                    if (result == SendResult.NOT_CONNECTED) {
                        haptics.reject(); showMessage("Not connected to an iPad. Open Connection to pair.")
                    } else if (result == SendResult.INVALID) {
                        haptics.reject(); showMessage("This button has an invalid shortcut. Edit it.")
                    }
                },
                dispatcherTap = { vm.dispatcher.tap(it) },
                dispatcherPress = { vm.dispatcher.press(it) },
                dispatcherRelease = { vm.dispatcher.release(it) },
                onEdit = { b -> editing = page.id to b },
                onAdd = { editing = page.id to null },
            )
        }
    }

    editing?.let { (pageId, button) ->
        val pg = p.pages.firstOrNull { it.id == pageId }
        ButtonEditorDialog(
            initial = button,
            profileName = p.name,
            indexInPage = pg?.buttons?.indexOfFirst { it.id == button?.id } ?: -1,
            pageSize = pg?.buttons?.size ?: 0,
            onTest = { chord -> vm.dispatcher.tap(chord).also { if (it == SendResult.NOT_CONNECTED) showMessage("Not connected: test not sent") } },
            onSave = { vm.upsertButton(p.id, pageId, it); editing = null },
            onDelete = { button?.let { vm.deleteButton(p.id, pageId, it.id) }; editing = null },
            onMove = { d -> button?.let { vm.moveButton(p.id, pageId, it.id, d) } },
            onDismiss = { editing = null },
        )
    }

    pageDialog?.let { d ->
        PageDialogs(
            dialog = d,
            onAdd = { name -> vm.addPage(p.id, name); pageIndex = p.pages.size },
            onRename = { pg, name -> vm.renamePage(p.id, pg.id, name) },
            onDelete = { pg -> vm.deletePage(p.id, pg.id); pageIndex = (safeIndex - 1).coerceAtLeast(0) },
            onDismiss = { pageDialog = null },
        )
    }
}

@Composable
private fun DeckTopBar(
    profile: Profile,
    profiles: List<Profile>,
    hidState: HidState,
    editMode: Boolean,
    onSelectProfile: (String) -> Unit,
    onToggleEdit: () -> Unit,
    onOpenConnection: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            TextButton(onClick = { menu = true }) {
                Text(profile.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Outlined.ArrowDropDown, null)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                profiles.forEach { pr ->
                    DropdownMenuItem(
                        text = { Text(pr.name) },
                        onClick = { onSelectProfile(pr.id); menu = false },
                        trailingIcon = { if (pr.id == profile.id) Icon(Icons.Outlined.Check, null) },
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        ConnectionChip(hidState, onOpenConnection)
        IconButton(onClick = onToggleEdit) {
            Icon(if (editMode) Icons.Outlined.Check else Icons.Outlined.Edit, if (editMode) "Done editing" else "Edit deck")
        }
    }
}

@Composable
private fun ConnectionChip(state: HidState, onClick: () -> Unit) {
    val (text, color) = when (state) {
        is HidState.Connected -> state.device.name to MaterialTheme.colorScheme.primary
        is HidState.Connecting -> "Connecting…" to MaterialTheme.colorScheme.secondary
        HidState.Ready -> "Not connected" to MaterialTheme.colorScheme.secondary
        HidState.Binding, HidState.Registering -> "Starting…" to MaterialTheme.colorScheme.secondary
        HidState.PermissionRequired -> "Needs permission" to MaterialTheme.colorScheme.error
        HidState.BluetoothOff -> "Bluetooth off" to MaterialTheme.colorScheme.error
        HidState.NoBluetooth, is HidState.Unsupported -> "Unavailable" to MaterialTheme.colorScheme.error
    }
    Row(
        Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(4.dp)))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(IntrinsicWidthLimit))
    }
}

private val IntrinsicWidthLimit = 110.dp

@Composable
private fun PageEditBar(
    canDelete: Boolean,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMove: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Page", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp))
        IconButton(onClick = { onMove(-1) }, enabled = canMoveLeft) { Icon(Icons.Outlined.ChevronLeft, "Move page left") }
        IconButton(onClick = { onMove(1) }, enabled = canMoveRight) { Icon(Icons.Outlined.ChevronRight, "Move page right") }
        TextButton(onClick = onRename) { Text("Rename") }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onDelete, enabled = canDelete) { Icon(Icons.Outlined.DeleteOutline, "Delete page") }
    }
}

@Composable
private fun DeckGrid(
    page: DeckPage,
    settings: AppSettings,
    editMode: Boolean,
    connected: Boolean,
    haptics: Haptics,
    onSend: (SendResult) -> Unit,
    dispatcherTap: (KeyChord) -> SendResult,
    dispatcherPress: (KeyChord) -> SendResult,
    dispatcherRelease: (KeyChord) -> Unit,
    onEdit: (DeckButton) -> Unit,
    onAdd: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val auto = (maxWidth / 112.dp).toInt().coerceIn(2, 8)
        val columns = if (settings.deckColumns > 0) settings.deckColumns else auto
        val rowH = settings.buttonScale.rowHeightDp.dp
        val gap = 10.dp
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(page.buttons, key = { it.id }, span = { GridItemSpan(it.span.coerceIn(1, columns)) }) { b ->
                val h = if (b.tall) rowH * 2 + gap else rowH
                DeckButtonView(
                    button = b,
                    height = h,
                    showHint = settings.showShortcutHints,
                    editMode = editMode,
                    connected = connected,
                    haptics = haptics,
                    onSend = onSend,
                    tap = dispatcherTap,
                    press = dispatcherPress,
                    release = dispatcherRelease,
                    onEdit = { onEdit(b) },
                )
            }
            if (editMode) {
                item(key = "__add") {
                    Box(
                        Modifier
                            .height(rowH)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(16.dp))
                            .clickable(onClick = onAdd),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.Add, "Add button", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            if (page.buttons.isEmpty() && !editMode) {
                item(span = { GridItemSpan(columns) }) {
                    Text(
                        "This page is empty. Tap the pencil to add buttons.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeckButtonView(
    button: DeckButton,
    height: androidx.compose.ui.unit.Dp,
    showHint: Boolean,
    editMode: Boolean,
    connected: Boolean,
    haptics: Haptics,
    onSend: (SendResult) -> Unit,
    tap: (KeyChord) -> SendResult,
    press: (KeyChord) -> SendResult,
    release: (KeyChord) -> Unit,
    onEdit: () -> Unit,
) {
    val chord = remember(button.chord) { KeyChord.parseOrNull(button.chord) }
    var pressed by remember { mutableStateOf(false) }
    val currentConnected by rememberUpdatedState(connected)
    val container = when {
        pressed -> MaterialTheme.colorScheme.primary
        button.mode == PressMode.HOLD -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val content = if (pressed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        Modifier
            .height(height)
            .fillMaxWidth()
            .alpha(if (connected || editMode) 1f else 0.55f)
            .background(container, RoundedCornerShape(16.dp))
            .then(
                if (editMode) Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(16.dp))
                else Modifier
            )
            .pointerInput(button, chord, editMode) {
                coroutineScope {
                    detectTapGestures(onPress = {
                        if (editMode) {
                            if (tryAwaitRelease()) onEdit()
                            return@detectTapGestures
                        }
                        if (chord == null) { onSend(SendResult.INVALID); return@detectTapGestures }
                        if (!currentConnected) { onSend(SendResult.NOT_CONNECTED); return@detectTapGestures }
                        pressed = true
                        haptics.tick()
                        try {
                            when (button.mode) {
                                PressMode.TAP -> {
                                    // Fire on touch-down for the lowest latency.
                                    onSend(tap(chord))
                                    tryAwaitRelease()
                                }
                                PressMode.HOLD -> {
                                    val r = press(chord)
                                    onSend(r)
                                    try { tryAwaitRelease() } finally { if (r == SendResult.SENT) release(chord) }
                                }
                                PressMode.REPEAT -> {
                                    onSend(tap(chord))
                                    val job = launch {
                                        delay(REPEAT_DELAY_MS)
                                        while (true) {
                                            if (tap(chord) != SendResult.SENT) break
                                            delay(REPEAT_INTERVAL_MS)
                                        }
                                    }
                                    try { tryAwaitRelease() } finally { job.cancel() }
                                }
                            }
                        } finally { pressed = false }
                    })
                }
            }
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            DeckIcons[button.icon]?.let { Icon(it, null, tint = content, modifier = Modifier.size(if (button.tall) 34.dp else 26.dp)) }
            Text(
                button.label,
                color = content,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (showHint) {
                Text(
                    chord?.display() ?: "invalid",
                    color = if (chord == null) MaterialTheme.colorScheme.error else content.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                )
            }
        }
    }
}

private sealed interface PageDialog {
    data object Add : PageDialog
    data class Rename(val page: DeckPage) : PageDialog
    data class Delete(val page: DeckPage) : PageDialog
}

@Composable
private fun PageDialogs(
    dialog: PageDialog,
    onAdd: (String) -> Unit,
    onRename: (DeckPage, String) -> Unit,
    onDelete: (DeckPage) -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        PageDialog.Add -> NameDialog("New page", "", "Add", { onAdd(it); onDismiss() }, onDismiss)
        is PageDialog.Rename -> NameDialog("Rename page", dialog.page.name, "Save", { onRename(dialog.page, it); onDismiss() }, onDismiss)
        is PageDialog.Delete -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Delete \"${dialog.page.name}\"?") },
            text = { Text("Its ${dialog.page.buttons.size} buttons will be removed from this profile.") },
            confirmButton = { TextButton(onClick = { onDelete(dialog.page); onDismiss() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        )
    }
}

@Composable
fun NameDialog(title: String, initial: String, confirm: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it.take(40) }, singleLine = true, label = { Text("Name") }) },
        confirmButton = { TextButton(onClick = { onConfirm(text.trim()) }, enabled = text.isNotBlank()) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
