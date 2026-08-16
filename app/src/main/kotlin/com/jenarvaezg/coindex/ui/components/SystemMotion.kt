package com.jenarvaezg.coindex.ui.components

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * The system's animation scale, watched while the app is composed.
 *
 * **Observed and not read once** (#514): the accessibility switch is thrown in Settings, with
 * Coindex still in the background and its process alive, and the collector comes back to the app he
 * left. A value read at `onCreate` would say «move» for the rest of the process, which is the whole
 * of the setting's audience getting the flight anyway. The observer is what Compose itself keeps
 * for the same URI, and it costs nothing while nothing changes.
 */
@Composable
fun rememberSystemMotion(): Boolean {
    val resolver = LocalContext.current.applicationContext.contentResolver
    var moving by remember(resolver) { mutableStateOf(movesAt(animatorDurationScale(resolver))) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                moving = movesAt(animatorDurationScale(resolver))
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return moving
}

/** One is the documented default of a device that has never been told otherwise. */
private fun animatorDurationScale(resolver: ContentResolver): Float =
    Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
