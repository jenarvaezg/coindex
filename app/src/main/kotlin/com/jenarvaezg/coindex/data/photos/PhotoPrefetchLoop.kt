package com.jenarvaezg.coindex.data.photos

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** How long the photograph prefetch lets the first screen have the phone to itself (#191). */
private const val PREFETCH_START_DELAY_MILLIS = 3_000L

/**
 * When a pass of the photograph prefetch is worth starting, and who gets the network when two
 * things want it (#191).
 *
 * [PhotoPrefetch] fetches; this decides *whether it should*, and it is the part that used to be
 * four guards and a mutable field inside the ViewModel. There are three rules and they are the
 * whole class:
 *
 * - **One pass at a time.** A second one would fight the first for the loader's two slots.
 * - **Nothing to do twice.** A pass covers the fichas it was given, and the same fichas asked for
 *   again cost nothing to skip. It is the *fichas* and not how many there are: refreshing one ficha
 *   (#185) can bring a photograph the phone has never seen without the count moving at all, and an
 *   item arriving as another leaves would cancel out exactly. Compared as the lists they are —
 *   `images.hashCode()` was two mapfuls of photographs that only *probably* differ, and a collision
 *   skipped a whole pass until the next launch.
 * - **The screen and the sync outrank it.** [yieldNetwork] hands the network over and waits for the
 *   pass to unwind; a pass still unwinding would otherwise overlap the one started when the sync is
 *   over, and both write the count.
 *
 * The scope is the caller's, per pass, and on purpose: what this saves is a wait the collector would
 * otherwise see **in this app**, and every photograph is independent, so being cut short when they
 * leave costs nothing but the ones that had not been asked for yet. They are asked for on the next
 * launch.
 */
class PhotoPrefetchLoop(
    private val prefetch: PhotoPrefetch,
    /**
     * The phone's own answer about spending the collector's data, read when a pass starts rather
     * than when it is asked for: three seconds of a cold start is long enough to walk into a wifi.
     */
    private val conditions: suspend (Boolean) -> PrefetchConditions,
    private val startDelayMillis: Long = PREFETCH_START_DELAY_MILLIS,
) {
    private var job: Job? = null

    /** The fichas the last pass covered, so the same ones do not buy a second pass. */
    private var covered: List<TypeImages>? = null

    private val _status = MutableStateFlow(PhotoCacheStatus())

    /**
     * What the phone holds of the catalog's photographs, as far as the last pass got.
     *
     * Observed rather than handed back through a callback, and it matters: this outlives the screen
     * that started the pass. A collector who leaves the app and comes back gets a new ViewModel and
     * **no new pass** — the fichas are the same ones — so a status that only travelled with the pass
     * would leave the settings screen saying «no hay fotos que traer» over 1.600 photographs it has
     * (ADR 0024), with nothing able to correct it.
     */
    val status: StateFlow<PhotoCacheStatus> = _status.asStateFlow()

    /**
     * Starts a pass unless one of the three rules says not to.
     *
     * @param syncing read inside the pass and not here: the delay below means the answer at the tap
     *   and the answer at the first request are different questions.
     * @param force starts one anyway, for the two moments when there is something new to fetch that
     *   the fichas themselves do not show: a sync that ended and an export that gave the network
     *   back.
     */
    fun start(
        scope: CoroutineScope,
        images: List<TypeImages>,
        syncing: () -> Boolean,
        force: Boolean = false,
    ) {
        if (job?.isActive == true) return
        if (images.isEmpty()) return
        if (!force && covered == images) return
        job = scope.launch {
            // The index is drawn first. This pass opens sixteen hundred cache snapshots before it
            // asks for anything, and doing that while the first screen is still laying itself out
            // is exactly the cold start the collector would feel.
            delay(startDelayMillis)
            val held = prefetchRefusal(conditions(syncing()))
            val status = prefetch.run(images, held) { partial -> _status.value = partial }
            covered = images
            _status.value = status
        }
    }

    /**
     * Gives the network up without waiting, for an export that is about to take all four slots.
     *
     * Not joined, unlike [yieldNetwork]: the export does not write the photograph count, so a pass
     * unwinding beside it collides with nothing.
     */
    fun cancel() {
        job?.cancel()
    }

    /** Gives the network up and waits for the pass to have finished unwinding. */
    suspend fun yieldNetwork() {
        job?.cancelAndJoin()
    }
}
