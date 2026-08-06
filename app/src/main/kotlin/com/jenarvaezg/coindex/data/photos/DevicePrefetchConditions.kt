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

/** Below this the battery is «low», and pictures nobody asked for stop being worth their charge. */
private const val LOW_BATTERY_FRACTION = 0.20f

/**
 * Reads the three facts [prefetchRefusal] decides on off the phone.
 *
 * A thin wrapper over three system services on purpose: the decision itself is arithmetic and lives
 * in [prefetchRefusal], where it can be read and tested without a device.
 */
class DevicePrefetchConditions(context: Context) {
    private val appContext = context.applicationContext

    /**
     * Off the main thread, which is this class's business and not its caller's: reading whether the
     * network is metered and how full the battery is are three binder calls, and the moment they are
     * asked for is the one right after the app has finished starting.
     */
    suspend fun current(syncing: Boolean): PrefetchConditions = withContext(Dispatchers.IO) {
        PrefetchConditions(
            unmeteredNetwork = isUnmetered(),
            powerSaveMode = isPowerSaving(),
            batteryLow = isBatteryLow(),
            syncing = syncing,
        )
    }

    /**
     * Whether the network in use is one the collector does not pay by the megabyte for.
     *
     * Asked as a capability rather than as «is it wifi»: a metered wifi hotspot is the collector's
     * tariff too, and an unmetered ethernet dock is not. No network at all answers false, which is
     * the right answer — there is nothing to bring.
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
     * The battery level, read from the sticky broadcast rather than subscribed to.
     *
     * Nothing here reacts to the battery changing: the prefetch is started at a moment, and the
     * question is only whether that moment is a good one. A phone that is charging is never «low»,
     * however empty it is.
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
