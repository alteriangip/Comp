package com.artcompanion.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artcompanion.ui.connection.ConnectionScreen
import com.artcompanion.ui.deck.DeckScreen
import com.artcompanion.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

enum class Destination(val label: String, val icon: ImageVector) {
    DECK("Deck", Icons.Outlined.Dashboard),
    CONNECTION("Connection", Icons.Outlined.Bluetooth),
    SETTINGS("Settings", Icons.Outlined.Settings),
}

/**
 * Top-level shell: bottom navigation in portrait, a navigation rail in landscape, so the deck
 * always gets the most usable area. Only destinations with working features are listed.
 */
@Composable
fun AppRoot(
    vm: AppViewModel,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onMakeDiscoverable: () -> Unit,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var dest by rememberSaveable { mutableStateOf(Destination.DECK) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { msg ->
        scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(msg) }
    }

    val view = LocalView.current
    DisposableEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    ArtCompanionTheme(settings.theme) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints(Modifier.safeDrawingPadding()) {
                val landscape = maxWidth > maxHeight
                val content: @Composable (Modifier) -> Unit = { m ->
                    Box(m) {
                        when (dest) {
                            Destination.DECK -> DeckScreen(vm, onOpenConnection = { dest = Destination.CONNECTION }, showMessage = showMessage)
                            Destination.CONNECTION -> ConnectionScreen(
                                hid = vm.hid,
                                prefs = (vm.getApplication<com.artcompanion.ArtCompanionApp>()).prefs,
                                autoReconnect = settings.autoReconnect,
                                onAutoReconnectChange = { v -> vm.updateSettings { it.copy(autoReconnect = v) } },
                                onRequestPermissions = onRequestPermissions,
                                onEnableBluetooth = onEnableBluetooth,
                                onMakeDiscoverable = onMakeDiscoverable,
                            )
                            Destination.SETTINGS -> SettingsScreen(vm, showMessage)
                        }
                        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(8.dp))
                    }
                }
                if (landscape) {
                    Row(Modifier.fillMaxSize()) {
                        NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                            Destination.entries.forEach { d ->
                                NavigationRailItem(
                                    selected = dest == d, onClick = { dest = d },
                                    icon = { Icon(d.icon, d.label) }, label = { Text(d.label) },
                                )
                            }
                        }
                        content(Modifier.weight(1f).fillMaxSize())
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        content(Modifier.weight(1f).fillMaxSize())
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                            Destination.entries.forEach { d ->
                                NavigationBarItem(
                                    selected = dest == d, onClick = { dest = d },
                                    icon = { Icon(d.icon, d.label) }, label = { Text(d.label) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
