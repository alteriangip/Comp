package com.artcompanion.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/** Thin wrapper around View haptics that honours the user's setting. No vibrator permission needed. */
class Haptics(private val view: View, private val enabled: Boolean) {
    fun tick() {
        if (enabled) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun confirm() {
        if (!enabled) return
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
            else HapticFeedbackConstants.VIRTUAL_KEY
        )
    }

    fun reject() {
        if (!enabled) return
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT
            else HapticFeedbackConstants.LONG_PRESS
        )
    }
}

@Composable
fun rememberHaptics(enabled: Boolean): Haptics {
    val view = LocalView.current
    return remember(view, enabled) { Haptics(view, enabled) }
}
