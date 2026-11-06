package com.artcompanion

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.artcompanion.data.ProfileRepository
import com.artcompanion.data.SettingsRepository
import com.artcompanion.hid.ConnectionPrefs
import com.artcompanion.hid.HidController
import com.artcompanion.input.CommandDispatcher
import java.io.File

/**
 * Manual dependency container: one HidController (so the Activity and Service share a single
 * Bluetooth registration) and the local repositories. No DI framework, to keep memory and startup low.
 */
class ArtCompanionApp : Application() {
    val prefs by lazy { ConnectionPrefs(this) }
    val hid by lazy { HidController(this, prefs) }
    val dispatcher by lazy { CommandDispatcher(hid) }
    val profiles by lazy { ProfileRepository(File(filesDir, "profiles")) }
    val settings by lazy { SettingsRepository(this, prefs) }

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(this, object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                    BluetoothAdapter.STATE_OFF -> hid.onBluetoothOff()
                    BluetoothAdapter.STATE_ON -> hid.refresh()
                }
            }
        }, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
    }
}
