package com.jenarvaezg.coindex.data

/**
 * How long the inventory on this phone is believed before asking again, at two calls a time.
 * Shorter, and several launches a day pay for the same answer; longer, and yesterday's purchase is
 * not on the plate today. About 60 calls a month, within [INVENTORY_RESERVE].
 */
const val INVENTORY_LIFE_MILLIS: Long = 24L * 60 * 60 * 1_000

/**
 * Whether the inventory is old enough to ask for again. Never having synced counts as stale, which
 * is how a freshly onboarded app fills itself (as ADR 0028 §3 does for the pass). A stamp in the
 * future counts too: after the clock goes backwards, the phone would otherwise never ask again.
 */
fun inventoryIsStale(lastSyncMillis: Long?, nowMillis: Long): Boolean =
    lastSyncMillis == null || nowMillis - lastSyncMillis !in 0 until INVENTORY_LIFE_MILLIS
