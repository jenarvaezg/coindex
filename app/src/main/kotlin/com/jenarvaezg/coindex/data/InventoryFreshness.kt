package com.jenarvaezg.coindex.data

/**
 * How long the inventory on this phone is believed before it is worth two consultas to ask again.
 *
 * **A day**, and like `THROTTLE_WALL_MILLIS` the number is a floor and a ceiling at once. Below it
 * the phone pays twice for the same answer on a day of eight launches, which is what his days look
 * like. Above it a coin bought on Monday is not on the plate on Tuesday, and putting the coin on the
 * plate is the whole of what he opens the app for.
 *
 * Measured against the budget it is 60 consultas a month of 1.500 — 4 %, inside the 300 the
 * inventory has reserved for itself ([INVENTORY_RESERVE]) with room for the fichas of what he buys.
 */
const val INVENTORY_LIFE_MILLIS: Long = 24L * 60 * 60 * 1_000

/**
 * Whether the inventory is old enough to be worth asking for again.
 *
 * Kept apart from everything that runs it so the deciding can be read and tested as what it is:
 * arithmetic on a timestamp, the same shape [rejectionStands] has.
 *
 * **Never having synced counts as stale**, which is how a freshly onboarded app fills itself — the
 * same reasoning ADR 0028 §3 gives the valuation pass, one object earlier in the chain. And a stamp
 * in the future counts as stale too, because a clock that went backwards is not evidence of
 * freshness; it costs two consultas to find out, and the alternative is a phone that never asks
 * again.
 */
fun inventoryIsStale(lastSyncMillis: Long?, nowMillis: Long): Boolean =
    lastSyncMillis == null || nowMillis - lastSyncMillis !in 0 until INVENTORY_LIFE_MILLIS
