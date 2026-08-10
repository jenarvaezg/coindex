package com.jenarvaezg.coindex.data.photos


/**
 * What the phone says about spending the collector's data and battery on pictures nobody has
 * asked for yet.
 *
 * @param unmeteredNetwork wifi, or anything else the system does not consider metered.
 * @param syncing whether a Numista sync is in flight, which is not a property of the device but
 *   belongs in the same decision: it is the one competitor for the same network.
 */
data class PrefetchConditions(
    val unmeteredNetwork: Boolean,
    val powerSaveMode: Boolean = false,
    val batteryLow: Boolean = false,
    val syncing: Boolean = false,
)

/** Why the photographs are not being brought right now. Each one is said in the settings screen. */
enum class PrefetchRefusal {
    Syncing,
    MeteredNetwork,
    PowerSave,
    LowBattery,
}

/**
 * Whether the photographs may be brought now, and if not, what is in the way (#191).
 *
 * The index is around 1.600 photographs and some 22 MB. Over wifi, with the app open, that is
 * invisible and it is what makes a plate open **with** its pictures instead of filling in before
 * the collector's eyes; over a mobile tariff it is the collector paying for plates they may never
 * open, and these photographs are not part of the API budget that would otherwise cap them
 * (ADR 0003 counts calls to `api.numista.com`, and these are CDN URLs).
 *
 * The sync is named first because it is the only reason that clears by itself in a minute; the
 * rest need the collector to walk into a wifi, plug the phone in, or turn power saving off.
 */
fun prefetchRefusal(conditions: PrefetchConditions): PrefetchRefusal? = when {
    conditions.syncing -> PrefetchRefusal.Syncing
    !conditions.unmeteredNetwork -> PrefetchRefusal.MeteredNetwork
    conditions.powerSaveMode -> PrefetchRefusal.PowerSave
    conditions.batteryLow -> PrefetchRefusal.LowBattery
    else -> null
}

/**
 * Every photograph the index is going to draw, once, in the order the cards hold them.
 *
 * **Both faces**, unlike the notebook's warm-up, which needs one of the two — the one its plate
 * declared (#227): a card and a plate cell draw obverse and reverse side by side, so warming one of
 * the two would leave half of every plate filling in before the collector's eyes — which is the thing
 * this is for.
 *
 * **Only the first candidate of each face**, which is the thumbnail. The original behind it is the
 * fallback for a thumbnail that is refused (ADR 0017); warming both would double the traffic to
 * pre-empt a failure that mostly does not happen, and a card that does fall back still asks for it
 * itself.
 *
 * @param gone the photographs Numista has already answered `404` for. Without this the prefetch
 *   would ask for them again on every single launch, for ever, which is exactly the eternal retry
 *   `PhotoRetryPolicy` refuses to do inside one session.
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
 *
 * Kept as arithmetic so the early return in [CoilPhotoPrefetch.run] is the same sentence a test
 * can read, rather than an `if` buried under `Dispatchers.IO`.
 */
fun prefetchAlreadySettled(missingCount: Int, held: PrefetchRefusal?): Boolean =
    missingCount == 0 || held != null

/**
 * How many of the photographs asked for in this pass are still missing after [landed] arrived.
 *
 * Failures are not subtracted: a photograph that did not land is still missing, and a progress
 * line that counted it as brought would be a lie that only settles at the end of the pass.
 */
fun prefetchMissingAfter(askedFor: Int, landed: Int): Int = askedFor - landed

/**
 * Whether this many requests is a moment to tell the settings screen what is left.
 *
 * [asked] counts attempts, not arrivals: every URL is asked once, and progress must move even
 * when some of them fail.
 */
fun shouldReportPrefetchProgress(asked: Int, every: Int = PREFETCH_PROGRESS_EVERY): Boolean =
    asked > 0 && asked % every == 0

/**
 * The counts a settings screen can show for a set of wanted URLs.
 *
 * Used at the opening of a pass and again at the end: the interceptor may have learnt meanwhile
 * that some photographs are gone, so the wanted list is rebuilt and counted from scratch rather
 * than derived from what landed.
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
