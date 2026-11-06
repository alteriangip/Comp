package com.artcompanion.hid

import android.content.Context

/** Small persisted connection settings (last host, auto-reconnect). */
class ConnectionPrefs(context: Context) {
    private val sp = context.getSharedPreferences("connection", Context.MODE_PRIVATE)

    var lastHostAddress: String?
        get() = sp.getString("last_host", null)
        set(v) = sp.edit().putString("last_host", v).apply()

    var lastHostName: String?
        get() = sp.getString("last_host_name", null)
        set(v) = sp.edit().putString("last_host_name", v).apply()

    var autoReconnect: Boolean
        get() = sp.getBoolean("auto_reconnect", true)
        set(v) = sp.edit().putBoolean("auto_reconnect", v).apply()

    /** Set when the user disconnects on purpose, so we don't immediately reconnect. */
    var autoReconnectSuppressed: Boolean
        get() = sp.getBoolean("reconnect_suppressed", false)
        set(v) = sp.edit().putBoolean("reconnect_suppressed", v).apply()
}
