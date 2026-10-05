package com.jenarvaezg.coindex.data.photos


/**
 * What the phone says about spending the collector's data and battery on pictures nobody has asked
 * for yet.
 *
 * @param unmeteredNetwork wifi, or anything else the system does not consider metered.
 * @param syncing whether a Numista sync is in flight: not a device property, but the one competitor
 *   for the network.
 */
data class PrefetchConditions(
    val unmeteredNetwork: Boolean,
    val powerSaveMode: Boolean = false,
    val batteryLow: Boolean = false,
    val syncing: Boolean = false,
)

/** Why the photographs are not being brought right now. Each one is shown in settings. */
enum class PrefetchRefusal {
    Syncing,
    MeteredNetwork,
    PowerSave,
    LowBattery,
}

/**
 * Whether the photographs may be brought now, and if not, what is in the way (#191). Only on an
 * unmetered network: on mobile data the collector would pay for plates they may never open, and
 * these CDN URLs fall outside the API budget of ADR 0003 that would otherwise cap them.
 *
 * The sync goes first because it is the only reason that clears by itself; the rest need the
 * collector to act.
 */
fun prefetchRefusal(conditions: PrefetchConditions): PrefetchRefusal? = when {
    conditions.syncing -> PrefetchRefusal.Syncing
    !conditions.unmeteredNetwork -> PrefetchRefusal.MeteredNetwork
    conditions.powerSaveMode -> PrefetchRefusal.PowerSave
    conditions.batteryLow -> PrefetchRefusal.LowBattery
    else -> null
}

/**
 * Every photograph the index is going to draw, once, in the order the cards hold them. Both faces,
 * since cards and plate cells draw them side by side (the notebook's warm-up needs only the one its
 * plate declares, #227). Only the first candidate of each face, the thumbnail: the original is the
 * rare fallback (ADR 0017), and a card that needs it asks for it itself.
 *
 * @param gone the photographs Numista already answered `404` for, so they aren't asked for on every
 *   launch.
 */
fun photographsToPrefetch(
    images: Collection<TypeImages>,
    gone: Set<String> = emptySet(),
): List<String> = images
    .asSequence()
    .flatMap { sequenceOf(it.obverse, it.reverse) }
    .mapNotNull { face -> face.candidates.firstOrNull() }
    .filterNot { it in gone }
    .distinct()
    .toList()

/** How often the collector-visible count is updated while the prefetch runs. */
const val PREFETCH_PROGRESS_EVERY = 25

/**
 * Whether this opening status is already the whole pass: nothing to fetch, or a reason not to.
 * Pulled out of [CoilPhotoPrefetch.run] so a test can read it.
 */
fun prefetchAlreadySettled(missingCount: Int, held: PrefetchRefusal?): Boolean =
    missingCount == 0 || held != null

/**
 * How many of the photographs asked for in this pass are still missing after [landed] arrived.
 * Failures are not subtracted: they are still missing.
 */
fun prefetchMissingAfter(askedFor: Int, landed: Int): Int = askedFor - landed

/**
 * Whether this many requests is a moment to tell settings what is left. [asked] counts attempts,
 * not arrivals, so progress moves even when some fail.
 */
fun shouldReportPrefetchProgress(asked: Int, every: Int = PREFETCH_PROGRESS_EVERY): Boolean =
    asked > 0 && asked % every == 0

/**
 * The counts settings can show for a set of wanted URLs. Used at the start of a pass and again at
 * the end, with the wanted list rebuilt, since the interceptor may have found some photographs gone.
 */
fun photoCacheStatus(
    wanted: Collection<String>,
    cached: (String) -> Boolean,
    bytes: Long,
    held: PrefetchRefusal?,
): PhotoCacheStatus = PhotoCacheStatus(
    wanted = wanted.size,
    missing = wanted.count { !cached(it) },
    bytes = bytes,
    held = held,
)
