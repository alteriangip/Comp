package com.artcompanion

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.artcompanion.hid.ConnectionPrefs
import com.artcompanion.hid.HidController

/** Holds the single HidController so the Activity and Service share one Bluetooth registration. */
class ArtCompanionApp : Application() {
    val prefs by lazy { ConnectionPrefs(this) }
    val hid by lazy { HidController(this, prefs) }

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
