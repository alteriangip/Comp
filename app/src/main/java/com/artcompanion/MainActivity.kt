package com.artcompanion

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.artcompanion.hid.HidPermissions
import com.artcompanion.service.HidForegroundService
import com.artcompanion.ui.AppRoot
import com.artcompanion.ui.AppViewModel

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

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
            AppRoot(
                vm = vm,
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
        if (savedInstanceState == null) {
            if (HidPermissions.hasConnect(this)) startHid() else permissionLauncher.launch(HidPermissions.runtime)
        }
    }

    override fun onResume() {
        super.onResume()
        // Covers returning from system Settings after granting permission / enabling Bluetooth.
        if (HidPermissions.hasConnect(this)) startHid()
    }

    private fun startHid() {
        if (HidPermissions.hasConnect(this)) HidForegroundService.start(this)
        (application as ArtCompanionApp).hid.refresh()
    }
}
