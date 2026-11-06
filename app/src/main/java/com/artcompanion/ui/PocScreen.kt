package com.artcompanion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artcompanion.hid.ConnectionPrefs
import com.artcompanion.hid.HidController
import com.artcompanion.hid.HidState
import com.artcompanion.hid.KeyChord
import com.artcompanion.profiles.AppProfile
import com.artcompanion.profiles.DefaultProfiles
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Phase 2 proof-of-concept screen. Every control sends real HID reports; nothing here
 * pretends to work while disconnected (buttons are disabled until a host is connected).
 */
@Composable
fun PocScreen(
    hid: HidController,
    prefs: ConnectionPrefs,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onMakeDiscoverable: () -> Unit,
) {
    val state by hid.state.collectAsStateWithLifecycle()
    val log by hid.log.collectAsStateWithLifecycle()
    val connected = state is HidState.Connected

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("ArtCompanion", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Connection proof of concept",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { StatusCard(state, hid, prefs, onRequestPermissions, onEnableBluetooth, onMakeDiscoverable) }
        item { ShortcutTestCard(hid, connected) }
        item { CustomChordCard(hid, connected) }
        item { TypeTestCard(hid, connected) }
        item { PointerPadCard(hid, connected) }
        item { LogCard(log.map { it.timeMs to it.text }) }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusCard(
    state: HidState,
    hid: HidController,
    prefs: ConnectionPrefs,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onMakeDiscoverable: () -> Unit,
) = Section("Connection") {
    val (label, detail) = describe(state)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(12.dp)
                .background(statusColor(state), RoundedCornerShape(6.dp))
        )
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.titleSmall)
    }
    if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

    val sendUs by hid.lastSendMicros.collectAsStateWithLifecycle()
    if (state is HidState.Connected && sendUs != null) {
        Text(
            "Last report queued in ${sendUs} µs (local stack time only; the iPad gives no delivery acknowledgement)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (state) {
            HidState.PermissionRequired -> Button(onClick = onRequestPermissions) { Text("Grant Bluetooth permission") }
            HidState.BluetoothOff -> Button(onClick = onEnableBluetooth) { Text("Turn on Bluetooth") }
            is HidState.Unsupported -> OutlinedButton(onClick = { hid.refresh() }) { Text("Retry") }
            HidState.Ready -> Button(onClick = onMakeDiscoverable) { Text("Make discoverable (120 s)") }
            is HidState.Connected -> OutlinedButton(onClick = { hid.disconnect() }) { Text("Disconnect") }
            else -> {}
        }
    }

    var autoReconnect by remember { mutableStateOf(prefs.autoReconnect) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Auto-reconnect to last iPad", Modifier.weight(1f))
        Switch(checked = autoReconnect, onCheckedChange = { autoReconnect = it; prefs.autoReconnect = it })
    }

    if (state == HidState.Ready || state is HidState.Connected) {
        val bonded = remember(state) { hid.bondedDevices() }
        if (bonded.isNotEmpty()) {
            Text("Paired devices", style = MaterialTheme.typography.labelLarge)
            bonded.forEach { d ->
                val isHost = (state as? HidState.Connected)?.device?.address == d.address
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(d.name)
                        Text(d.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!isHost) FilledTonalButton(onClick = { hid.connect(d.address) }) { Text("Connect") }
                    else Text("Connected", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

private fun describe(s: HidState): Pair<String, String?> = when (s) {
    HidState.NoBluetooth -> "No Bluetooth hardware" to null
    HidState.PermissionRequired -> "Permission needed" to "Android requires the Nearby devices permission to act as a Bluetooth keyboard."
    HidState.BluetoothOff -> "Bluetooth is off" to null
    HidState.Binding -> "Starting…" to "Requesting the Bluetooth HID Device service."
    is HidState.Unsupported -> "HID Device not available" to s.reason
    HidState.Registering -> "Registering keyboard + mouse…" to null
    HidState.Ready -> "Ready to pair" to "On the iPad open Settings → Bluetooth and tap this phone under Other Devices. " +
        "If it is already paired, tap Connect below."
    is HidState.Connecting -> "Connecting to ${s.device.name}…" to null
    is HidState.Connected -> "Connected to ${s.device.name}" to "The iPad now sees this phone as a keyboard and mouse."
}

@Composable
private fun statusColor(s: HidState) = when (s) {
    is HidState.Connected -> MaterialTheme.colorScheme.primary
    HidState.Ready, is HidState.Connecting, HidState.Registering, HidState.Binding -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.error
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShortcutTestCard(hid: HidController, connected: Boolean) = Section("Shortcut test") {
    var profile by rememberSaveable { mutableStateOf(AppProfile.PROCREATE) }
    val haptic = LocalHapticFeedback.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(AppProfile.PROCREATE, AppProfile.ARTSTUDIO_PRO).forEach { p ->
            FilterChip(selected = profile == p, onClick = { profile = p }, label = { Text(p.displayName) })
        }
    }
    Text(
        "Shortcuts are taken from each app's official documentation. Open a canvas on the iPad and tap a button.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DefaultProfiles.forProfile(profile).forEach { def ->
            val chord = remember(def.chord) { KeyChord.parse(def.chord) }
            FilledTonalButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    hid.tap(chord)
                },
                enabled = connected,
                modifier = Modifier.heightIn(min = 64.dp).widthIn(min = 96.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(def.label, textAlign = TextAlign.Center)
                    Text(chord.display(), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun CustomChordCard(hid: HidController, connected: Boolean) = Section("Send any shortcut") {
    var text by rememberSaveable { mutableStateOf("Cmd+Z") }
    val parsed = remember(text) { runCatching { KeyChord.parse(text) } }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("e.g. Cmd+Shift+Z, Alt+[, F, Space") },
        singleLine = true,
        isError = parsed.isFailure,
        supportingText = {
            Text(parsed.fold({ "Will send ${it.display()}" }, { it.message ?: "Invalid" }))
        },
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = { parsed.getOrNull()?.let(hid::tap) }, enabled = connected && parsed.isSuccess) { Text("Send") }
        val chord = parsed.getOrNull()
        val holdColor = if (connected && chord != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
        Box(
            Modifier
                .height(48.dp)
                .widthIn(min = 140.dp)
                .background(holdColor, RoundedCornerShape(24.dp))
                .pointerInput(connected, chord) {
                    if (!connected || chord == null) return@pointerInput
                    detectTapGestures(onPress = {
                        hid.press(chord)
                        try { tryAwaitRelease() } finally { hid.releaseAll() }
                    })
                },
            contentAlignment = Alignment.Center,
        ) { Text("Hold while pressed", color = MaterialTheme.colorScheme.onPrimary) }
    }
}

@Composable
private fun TypeTestCard(hid: HidController, connected: Boolean) = Section("Typing test") {
    Text(
        "Open Notes on the iPad and send text. If it appears, keyboard input is proven independent of any drawing app.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    var text by rememberSaveable { mutableStateOf("Hello from ArtCompanion\n") }
    OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth())
    Button(onClick = { hid.typeText(text) }, enabled = connected) { Text("Type on iPad") }
}

@Composable
private fun PointerPadCard(hid: HidController, connected: Boolean) = Section("Pointer test") {
    var holdSpace by rememberSaveable { mutableStateOf(false) }
    var sensitivity by rememberSaveable { mutableStateOf(1.5f) }
    Text(
        "Drag to move the iPad pointer, tap to click. \"Drag = press\" holds the button while dragging. " +
            "Use the right strip to scroll. These tests establish what pointer input each app accepts.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    var dragPress by rememberSaveable { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Drag = press", Modifier.weight(1f))
        Switch(checked = dragPress, onCheckedChange = { dragPress = it })
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Hold Space while dragging", Modifier.weight(1f))
        Switch(checked = holdSpace, onCheckedChange = { holdSpace = it })
    }
    val space = remember { KeyChord.parse("Space") }
    val padColor = MaterialTheme.colorScheme.surfaceVariant
    Row(Modifier.fillMaxWidth().height(220.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxSize()
                .background(padColor, RoundedCornerShape(16.dp))
                .pointerInput(connected, dragPress, holdSpace, sensitivity) {
                    if (!connected) return@pointerInput
                    var accX = 0f; var accY = 0f
                    detectDragGestures(
                        onDragStart = {
                            if (holdSpace) hid.press(space)
                            if (dragPress) hid.mouseButton(1, true)
                        },
                        onDragEnd = {
                            if (dragPress) hid.mouseButton(1, false)
                            if (holdSpace) hid.releaseAll()
                        },
                        onDragCancel = {
                            if (dragPress) hid.mouseButton(1, false)
                            if (holdSpace) hid.releaseAll()
                        },
                    ) { change, delta ->
                        change.consume()
                        accX += delta.x * sensitivity; accY += delta.y * sensitivity
                        val dx = accX.roundToInt(); val dy = accY.roundToInt()
                        accX -= dx; accY -= dy
                        hid.mouseMove(dx, dy)
                    }
                }
                .pointerInput(connected) {
                    if (connected) detectTapGestures(onTap = { hid.mouseClick() })
                },
            contentAlignment = Alignment.Center,
        ) { Text(if (connected) "Trackpad" else "Connect first", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Box(
            Modifier
                .width(56.dp)
                .fillMaxSize()
                .background(padColor, RoundedCornerShape(16.dp))
                .pointerInput(connected) {
                    if (!connected) return@pointerInput
                    var acc = 0f
                    detectDragGestures { change, delta ->
                        change.consume()
                        acc += delta.y / 12f
                        val w = acc.roundToInt()
                        acc -= w
                        hid.scroll(-w)
                    }
                },
            contentAlignment = Alignment.Center,
        ) { Text("⇕", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    Text("Sensitivity ${"%.1f".format(sensitivity)}×", style = MaterialTheme.typography.bodySmall)
    androidx.compose.material3.Slider(value = sensitivity, onValueChange = { sensitivity = it }, valueRange = 0.5f..4f)
}

@Composable
private fun LogCard(lines: List<Pair<Long, String>>) = Section("Event log") {
    val fmt = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    if (lines.isEmpty()) Text("No events yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
    lines.takeLast(25).reversed().forEach { (t, msg) ->
        Text("${fmt.format(Date(t))}  $msg", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
}
