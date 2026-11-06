package com.artcompanion.hid

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** What the HID link is doing right now. Every state is derived from real Bluetooth callbacks. */
sealed interface HidState {
    /** Device has no Bluetooth adapter. */
    data object NoBluetooth : HidState
    data object PermissionRequired : HidState
    data object BluetoothOff : HidState
    /** Waiting for the system HID Device service proxy. */
    data object Binding : HidState
    /** getProfileProxy failed or never answered: the ROM does not expose HID Device. */
    data class Unsupported(val reason: String) : HidState
    /** Proxy obtained; registerApp in flight. */
    data object Registering : HidState
    /** Registered as a keyboard/mouse, no host connected. Pair from the iPad now. */
    data object Ready : HidState
    data class Connecting(val device: DeviceInfo) : HidState
    data class Connected(val device: DeviceInfo) : HidState
}

data class DeviceInfo(val name: String, val address: String)

data class LogLine(val timeMs: Long, val text: String)

/**
 * Owns the Android [BluetoothHidDevice] registration and sends keyboard/mouse reports.
 *
 * Lifecycle: [start] binds the proxy and registers the SDP record; [stop] unregisters.
 * A single background thread serialises every Bluetooth call so reports keep their order.
 */
@SuppressLint("MissingPermission") // Callers check BLUETOOTH_CONNECT via HidPermissions before start().
class HidController(context: Context, private val prefs: ConnectionPrefs) {

    private val appContext = context.applicationContext
    private val adapter: BluetoothAdapter? =
        appContext.getSystemService(BluetoothManager::class.java)?.adapter
    private val io = Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "hid-io") }

    private val _state = MutableStateFlow<HidState>(HidState.Binding)
    val state: StateFlow<HidState> = _state.asStateFlow()

    private val _log = MutableStateFlow<List<LogLine>>(emptyList())
    val log: StateFlow<List<LogLine>> = _log.asStateFlow()

    /** Last measured time for sendReport() to return, as a crude local latency indicator. */
    private val _lastSendMicros = MutableStateFlow<Long?>(null)
    val lastSendMicros: StateFlow<Long?> = _lastSendMicros.asStateFlow()

    @Volatile private var hid: BluetoothHidDevice? = null
    @Volatile private var host: BluetoothDevice? = null
    @Volatile private var registered = false
    @Volatile private var started = false
    private var reconnectAttempts = 0

    private val sdp = BluetoothHidDeviceAppSdpSettings(
        "ArtCompanion",
        "ArtCompanion drawing controller",
        "ArtCompanion",
        BluetoothHidDevice.SUBCLASS1_COMBO,
        HidDescriptor.bytes,
    )

    // ---------------------------------------------------------------- lifecycle

    fun start() = io.execute {
        if (started && hid != null) return@execute
        val a = adapter
        when {
            a == null -> { set(HidState.NoBluetooth); return@execute }
            !HidPermissions.hasConnect(appContext) -> { set(HidState.PermissionRequired); return@execute }
            !a.isEnabled -> { set(HidState.BluetoothOff); return@execute }
        }
        started = true
        set(HidState.Binding)
        log("Requesting HID Device profile proxy")
        val ok = a!!.getProfileProxy(appContext, profileListener, BluetoothProfile.HID_DEVICE)
        if (!ok) {
            started = false
            set(HidState.Unsupported("getProfileProxy(HID_DEVICE) returned false. This ROM does not expose the Bluetooth HID Device profile."))
            return@execute
        }
        // Some ROMs accept the call but never bind: detect that instead of spinning forever.
        io.schedule({
            if (hid == null && _state.value == HidState.Binding) {
                started = false
                set(HidState.Unsupported("The HID Device service did not respond within 5 s. The profile is probably disabled on this firmware."))
            }
        }, 5, TimeUnit.SECONDS)
    }

    fun stop() = io.execute {
        val h = hid
        if (h != null) {
            host?.let { runCatching { h.disconnect(it) } }
            runCatching { h.unregisterApp() }
            adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, h)
        }
        hid = null; host = null; registered = false; started = false
        log("Stopped HID")
        set(HidState.Binding)
    }

    /** Re-evaluates permission/adapter state, e.g. after the user granted permission. */
    fun refresh() = io.execute {
        if (!started) start()
    }

    /** Called when the adapter turns off: drop the (now dead) registration. */
    fun onBluetoothOff() = io.execute {
        hid?.let { runCatching { adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it) } }
        hid = null; host = null; registered = false; started = false
        set(HidState.BluetoothOff)
        log("Bluetooth turned off")
    }

    // ---------------------------------------------------------------- host connection

    fun bondedDevices(): List<DeviceInfo> {
        val a = adapter ?: return emptyList()
        if (!HidPermissions.hasConnect(appContext)) return emptyList()
        return runCatching {
            a.bondedDevices.orEmpty().map { DeviceInfo(it.name ?: it.address, it.address) }.sortedBy { it.name }
        }.getOrDefault(emptyList())
    }

    fun connect(address: String) = io.execute {
        val h = hid ?: run { log("Not registered yet"); return@execute }
        val dev = runCatching { adapter?.getRemoteDevice(address) }.getOrNull() ?: return@execute
        log("Connecting to ${dev.name ?: address}")
        val ok = h.connect(dev)
        if (!ok) log("connect() refused by the stack. Pair from the iPad's Bluetooth settings first.")
    }

    fun disconnect() = io.execute {
        val h = hid ?: return@execute
        val d = host ?: return@execute
        prefs.autoReconnectSuppressed = true
        h.disconnect(d)
    }

    // ---------------------------------------------------------------- input

    val isConnected: Boolean get() = _state.value is HidState.Connected

    /** Taps a shortcut (press then release). Returns false if no host is connected. */
    fun tap(chord: KeyChord): Boolean {
        if (!isConnected) return false
        io.execute { Reports.chordTap(chord).forEach { sendKeyboard(it) } }
        return true
    }

    /** Holds keys down until [releaseAll], e.g. holding Space or a modifier. */
    fun press(chord: KeyChord): Boolean {
        if (!isConnected) return false
        io.execute {
            if (chord.modifiers != 0) sendKeyboard(Reports.keyboard(chord.modifiers, emptyList()))
            sendKeyboard(Reports.keyboard(chord.modifiers, chord.keys))
        }
        return true
    }

    fun releaseAll() {
        io.execute { if (isConnected) sendKeyboard(Reports.keyboardRelease()) }
    }

    /** Types text as individual key taps (US layout; set the iPad's hardware keyboard to US/English). */
    fun typeText(text: String): Boolean {
        if (!isConnected) return false
        io.execute {
            for (c in text) {
                val (usage, shift) = Usage.forChar(c) ?: continue
                val mods = if (shift) Mod.SHIFT else 0
                sendKeyboard(Reports.keyboard(mods, listOf(usage)))
                sendKeyboard(Reports.keyboardRelease())
            }
        }
        return true
    }

    @Volatile private var mouseButtons = 0

    fun mouseMove(dx: Int, dy: Int) {
        if (!isConnected || (dx == 0 && dy == 0)) return
        val b = mouseButtons
        io.execute { Reports.splitMove(dx, dy).forEach { (x, y) -> sendMouse(Reports.mouse(b, x, y)) } }
    }

    fun mouseButton(button: Int, down: Boolean) {
        if (!isConnected) return
        mouseButtons = if (down) mouseButtons or button else mouseButtons and button.inv()
        val b = mouseButtons
        io.execute { sendMouse(Reports.mouse(b, 0, 0)) }
    }

    fun mouseClick(button: Int = 1) {
        mouseButton(button, true)
        mouseButton(button, false)
    }

    fun scroll(wheel: Int, pan: Int = 0) {
        if (!isConnected || (wheel == 0 && pan == 0)) return
        val b = mouseButtons
        io.execute { sendMouse(Reports.mouse(b, 0, 0, wheel, pan)) }
    }

    private fun sendKeyboard(data: ByteArray) = send(HidDescriptor.REPORT_ID_KEYBOARD, data)
    private fun sendMouse(data: ByteArray) = send(HidDescriptor.REPORT_ID_MOUSE, data)

    private fun send(id: Int, data: ByteArray) {
        val h = hid ?: return
        val d = host ?: return
        val t0 = System.nanoTime()
        val ok = h.sendReport(d, id, data)
        _lastSendMicros.value = (System.nanoTime() - t0) / 1000
        if (!ok) log("sendReport(id=$id) failed")
    }

    // ---------------------------------------------------------------- callbacks

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            io.execute {
                val h = proxy as BluetoothHidDevice
                hid = h
                log("HID Device proxy bound; registering keyboard+mouse")
                set(HidState.Registering)
                val ok = h.registerApp(sdp, null, null, io, callback)
                if (!ok) {
                    set(HidState.Unsupported("registerApp() was rejected. Another app may already be registered as a HID device. Close other keyboard/mouse emulator apps and try again."))
                }
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            io.execute {
                log("HID Device service disconnected")
                hid = null; host = null; registered = false; started = false
                set(HidState.Binding)
            }
        }
    }

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            this@HidController.registered = registered
            log("App registered=$registered${pluggedDevice?.let { " (plugged: ${it.name})" } ?: ""}")
            if (!registered) {
                if (_state.value !is HidState.Unsupported) set(HidState.Registering)
                return
            }
            if (host == null) set(HidState.Ready)
            // Prefer the device the stack says was plugged; else the last host we used.
            val target = pluggedDevice?.address ?: prefs.lastHostAddress
            if (target != null && prefs.autoReconnect && !prefs.autoReconnectSuppressed) {
                reconnectAttempts = 0
                connect(target)
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            val info = DeviceInfo(device.name ?: device.address, device.address)
            when (state) {
                BluetoothProfile.STATE_CONNECTING -> set(HidState.Connecting(info))
                BluetoothProfile.STATE_CONNECTED -> {
                    host = device
                    prefs.lastHostAddress = device.address
                    prefs.lastHostName = info.name
                    prefs.autoReconnectSuppressed = false
                    reconnectAttempts = 0
                    log("Connected to ${info.name}")
                    set(HidState.Connected(info))
                }
                BluetoothProfile.STATE_DISCONNECTING -> log("Disconnecting from ${info.name}")
                BluetoothProfile.STATE_DISCONNECTED -> {
                    val wasHost = host?.address == device.address
                    if (wasHost) host = null
                    log("Disconnected from ${info.name}")
                    set(if (registered) HidState.Ready else HidState.Registering)
                    if (wasHost) scheduleReconnect(device.address)
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            val h = hid ?: return
            when (id.toInt()) {
                HidDescriptor.REPORT_ID_KEYBOARD -> h.replyReport(device, type, id, Reports.keyboardRelease())
                HidDescriptor.REPORT_ID_MOUSE -> h.replyReport(device, type, id, Reports.mouse(0, 0, 0))
                else -> h.reportError(device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID)
            }
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            hid?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
        }

        override fun onSetProtocol(device: BluetoothDevice, protocol: Byte) {
            log("Host set protocol=$protocol")
        }

        override fun onInterruptData(device: BluetoothDevice, reportId: Byte, data: ByteArray) {
            // Keyboard LED output report (Caps Lock etc.). Receiving it proves the iPad
            // treats us as a keyboard, which is useful diagnostic evidence.
            log("Host output report id=$reportId data=${data.joinToString(" ") { "%02X".format(it) }}")
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice) {
            log("Host unplugged virtual cable (${device.name})")
        }
    }

    private fun scheduleReconnect(address: String) {
        if (!prefs.autoReconnect || prefs.autoReconnectSuppressed) return
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            log("Auto-reconnect gave up after $MAX_RECONNECT_ATTEMPTS attempts")
            return
        }
        val delay = 2L shl reconnectAttempts // 2, 4, 8, 16, 32 s
        reconnectAttempts++
        log("Auto-reconnect in ${delay}s (attempt $reconnectAttempts)")
        io.schedule({
            if (host == null && registered) connect(address)
        }, delay, TimeUnit.SECONDS)
    }

    // ---------------------------------------------------------------- helpers

    private fun set(s: HidState) { _state.value = s }

    private fun log(text: String) {
        Log.i(TAG, text)
        _log.update { (it + LogLine(System.currentTimeMillis(), text)).takeLast(80) }
    }

    companion object {
        private const val TAG = "ArtCompanionHid"
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }
}
