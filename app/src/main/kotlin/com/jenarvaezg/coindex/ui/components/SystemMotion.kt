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
 * The system's animation scale, observed while the app is composed rather than read once (#514):
 * the setting is usually changed while Coindex sits in the background with its process alive.
 * Compose observes the same URI.
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

/** One is the documented default. */
private fun animatorDurationScale(resolver: ContentResolver): Float =
    Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
