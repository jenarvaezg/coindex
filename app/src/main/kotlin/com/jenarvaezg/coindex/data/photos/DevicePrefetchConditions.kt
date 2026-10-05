package com.jenarvaezg.coindex.data.photos

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.PowerManager
import androidx.core.content.getSystemService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Below this the battery is «low», and prefetching pictures is not worth the charge. */
private const val LOW_BATTERY_FRACTION = 0.20f

/**
 * Reads off the phone the facts [prefetchRefusal] decides on. A thin wrapper over system services;
 * the decision lives in [prefetchRefusal], testable without a device.
 */
class DevicePrefetchConditions(context: Context) {
    private val appContext = context.applicationContext

    /** Off the main thread, since it makes several binder calls right after the app starts. */
    suspend fun current(syncing: Boolean): PrefetchConditions = withContext(Dispatchers.IO) {
        PrefetchConditions(
            unmeteredNetwork = isUnmetered(),
            powerSaveMode = isPowerSaving(),
            batteryLow = isBatteryLow(),
            syncing = syncing,
        )
    }

    /**
     * Whether the network in use is unmetered. Asked as a capability rather than «is it wifi»: a
     * metered wifi hotspot costs the collector data too. No network answers false.
     */
    private fun isUnmetered(): Boolean {
        val manager = appContext.getSystemService<ConnectivityManager>() ?: return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isPowerSaving(): Boolean =
        appContext.getSystemService<PowerManager>()?.isPowerSaveMode == true

    /**
     * The battery level, read once from the sticky broadcast: the prefetch only asks at the moment
     * it starts. A charging phone is never «low».
     */
    private fun isBatteryLow(): Boolean {
        val status: Intent = appContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        ) ?: return false
        val plugged = status.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        if (plugged) return false
        val level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = status.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return false
        return level.toFloat() / scale < LOW_BATTERY_FRACTION
    }
}
