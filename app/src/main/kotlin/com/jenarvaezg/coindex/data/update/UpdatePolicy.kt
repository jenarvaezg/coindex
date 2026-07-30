package com.jenarvaezg.coindex.data.update

/** How often Coindex looks for a new release while it is running. */
const val UPDATE_CHECK_INTERVAL_MILLIS: Long = 6 * 60 * 60 * 1_000L

/**
 * Whether enough time has passed to look for a new release again.
 *
 * The check happens on open, when the app comes back to the foreground and on a timer while it
 * stays open. This keeps all three from hammering GitHub: without a floor, every return to the
 * app would be another request.
 */
fun shouldCheckForUpdate(
    lastCheckMillis: Long?,
    nowMillis: Long,
    intervalMillis: Long = UPDATE_CHECK_INTERVAL_MILLIS,
): Boolean {
    if (lastCheckMillis == null) return true
    // A clock that jumped backwards must not lock checking out until it catches up.
    if (nowMillis < lastCheckMillis) return true
    return nowMillis - lastCheckMillis >= intervalMillis
}
