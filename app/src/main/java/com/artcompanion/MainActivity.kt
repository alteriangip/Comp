package com.artcompanion

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.artcompanion.hid.HidPermissions
import com.artcompanion.service.HidForegroundService
import com.artcompanion.ui.ArtCompanionTheme
import com.artcompanion.ui.PocScreen

class MainActivity : ComponentActivity() {

    private val app get() = application as ArtCompanionApp

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { startHid() }

    private val enableBtLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { startHid() }

    private val discoverableLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArtCompanionTheme {
                PocScreen(
                    hid = app.hid,
                    prefs = app.prefs,
                    onRequestPermissions = { permissionLauncher.launch(HidPermissions.runtime) },
                    onEnableBluetooth = { enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) },
                    onMakeDiscoverable = {
                        discoverableLauncher.launch(
                            Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                                .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
                        )
                    },
                )
            }
        }
        if (HidPermissions.hasConnect(this)) startHid() else permissionLauncher.launch(HidPermissions.runtime)
    }

    private fun startHid() {
        if (HidPermissions.hasConnect(this)) HidForegroundService.start(this)
        app.hid.refresh()
    }
}
