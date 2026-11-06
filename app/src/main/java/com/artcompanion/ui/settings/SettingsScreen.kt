package com.artcompanion.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artcompanion.data.ButtonScale
import com.artcompanion.data.Profile
import com.artcompanion.data.ThemeMode
import com.artcompanion.ui.AppViewModel
import com.artcompanion.ui.deck.NameDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: AppViewModel, showMessage: (String) -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    var rename by remember { mutableStateOf<Profile?>(null) }
    var duplicate by remember { mutableStateOf<Profile?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = vm.exportBackup()
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) } }.isSuccess
            }
            showMessage(if (ok) "Backup saved" else "Could not write the backup file")
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)!!.use { input ->
                        val bytes = input.readBytes()
                        require(bytes.size < 5_000_000) { "File too large" }
                        String(bytes)
                    }
                }.getOrNull()
            }
            if (text == null) { showMessage("Could not read that file"); return@launch }
            val err = vm.importBackup(text)
            showMessage(err ?: "Configuration restored")
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineSmall) }

        item {
            Group("Appearance") {
                Label("Theme")
                Segmented(ThemeMode.entries, s.theme, { it.name.lowercase().replaceFirstChar(Char::uppercase) }) { m -> vm.updateSettings { it.copy(theme = m) } }
                Label("Button size")
                Segmented(ButtonScale.entries, s.buttonScale, { it.displayName }) { v -> vm.updateSettings { it.copy(buttonScale = v) } }
                Label("Columns")
                Segmented(listOf(0, 2, 3, 4, 5, 6), s.deckColumns, { if (it == 0) "Auto" else "$it" }) { v -> vm.updateSettings { it.copy(deckColumns = v) } }
                Toggle("Show shortcut under each button", s.showShortcutHints) { v -> vm.updateSettings { it.copy(showShortcutHints = v) } }
            }
        }

        item {
            Group("Behaviour") {
                Toggle("Haptic feedback", s.haptics) { v -> vm.updateSettings { it.copy(haptics = v) } }
                Toggle("Keep screen on while app is open", s.keepScreenOn) { v -> vm.updateSettings { it.copy(keepScreenOn = v) } }
            }
        }

        item {
            Group("Connection") {
                Toggle("Auto-reconnect to the last iPad", s.autoReconnect) { v -> vm.updateSettings { it.copy(autoReconnect = v) } }
                Hint("Method: Bluetooth HID keyboard + mouse. No iPad app is required.")
            }
        }

        item {
            Group("Profiles & shortcuts") {
                Hint("Each profile is stored separately. Editing one never changes another. Edit buttons from the Deck (pencil icon).")
                profiles.forEach { p ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(p.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Text(
                                "${p.pages.size} pages · ${p.pages.sumOf { it.buttons.size }} buttons",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (p.id != s.activeProfileId) TextButton(onClick = { vm.selectProfile(p.id) }) { Text("Use") }
                            TextButton(onClick = { duplicate = p }) { Text("Duplicate") }
                            if (!p.builtIn) TextButton(onClick = { rename = p }) { Text("Rename") }
                            TextButton(onClick = { confirm = Confirm.ResetProfile(p) }) { Text("Reset") }
                            if (!p.builtIn) TextButton(onClick = { confirm = Confirm.DeleteProfile(p) }) { Text("Delete") }
                        }
                    }
                }
            }
        }

        item {
            Group("Backup & restore") {
                Hint("Saves all profiles and settings to a JSON file you choose (e.g. Downloads or Drive). Nothing leaves the phone otherwise.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = {
                        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                        exportLauncher.launch("artcompanion-backup-$stamp.json")
                    }) { Text("Export") }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) { Text("Import") }
                }
                Hint("Importing replaces all current profiles and settings.")
            }
        }

        item {
            Group("Reset") {
                OutlinedButton(onClick = { confirm = Confirm.ResetAll }) { Text("Reset everything to defaults") }
            }
        }

        item {
            Hint("ArtCompanion is free and works entirely on your devices: no account, no ads, no internet access.")
        }
    }

    confirm?.let { c ->
        val (title, body, action) = when (c) {
            is Confirm.ResetProfile -> Triple("Reset \"${c.p.name}\"?", "All its pages and buttons return to the documented defaults.") { vm.resetProfile(c.p.id); Unit }
            is Confirm.DeleteProfile -> Triple("Delete \"${c.p.name}\"?", "This cannot be undone (unless you have a backup).") { vm.deleteProfile(c.p.id) }
            Confirm.ResetAll -> Triple("Reset everything?", "All profiles and settings return to defaults. Export a backup first if you may want them back.") { vm.resetEverything() }
        }
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(title) },
            text = { Text(body) },
            confirmButton = { TextButton(onClick = { action(); confirm = null; showMessage("Done") }) { Text("Confirm") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
    rename?.let { p ->
        NameDialog("Rename profile", p.name, "Save", { vm.renameProfile(p.id, it); rename = null }, { rename = null })
    }
    duplicate?.let { p ->
        NameDialog("Duplicate \"${p.name}\"", "${p.name} copy", "Create", { vm.duplicateProfile(p.id, it); duplicate = null; showMessage("Profile created and selected") }, { duplicate = null })
    }
}

private sealed interface Confirm {
    data class ResetProfile(val p: Profile) : Confirm
    data class DeleteProfile(val p: Profile) : Confirm
    data object ResetAll : Confirm
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun Label(text: String) = Text(text, style = MaterialTheme.typography.labelLarge)

@Composable
private fun Hint(text: String) =
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun Toggle(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, o ->
            SegmentedButton(
                selected = o == selected,
                onClick = { onSelect(o) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                icon = {},
            ) { Text(label(o), maxLines = 1) }
        }
    }
}
