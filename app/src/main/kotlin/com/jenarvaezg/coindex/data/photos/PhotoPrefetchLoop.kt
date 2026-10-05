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
 * things want it (#191). [PhotoPrefetch] fetches; this decides whether it should:
 *
 * - One pass at a time: a second would fight the first for the loader's two slots.
 * - Never the same fichas twice. Compared as lists, not by count or hash: refreshing a ficha (#185)
 *   can bring a new photograph without changing the count, and a hash collision would skip a pass.
 * - The screen and the sync outrank it: [yieldNetwork] waits for the pass to unwind, or it would
 *   overlap the pass started after the sync, and both write the count.
 *
 * The scope is the caller's: the prefetch only matters while the app is open, and being cut short
 * loses only the photographs not yet asked for, which the next launch asks for.
 */
class PhotoPrefetchLoop(
    private val prefetch: PhotoPrefetch,
    /** The phone's conditions for spending data, read when the pass starts, after the delay. */
    private val conditions: suspend (Boolean) -> PrefetchConditions,
    private val startDelayMillis: Long = PREFETCH_START_DELAY_MILLIS,
) {
    private var job: Job? = null

    /** The fichas the last pass covered, so the same ones don't get a second pass. */
    private var covered: List<TypeImages>? = null

    private val _status = MutableStateFlow(PhotoCacheStatus())

    /**
     * What the phone holds of the catalog's photographs, as far as the last pass got. Observed
     * rather than returned because it must outlive the screen: a new ViewModel over the same fichas
     * runs no pass, and settings still needs the count (ADR 0024).
     */
    val status: StateFlow<PhotoCacheStatus> = _status.asStateFlow()

    /**
     * Starts a pass unless one of the rules says not to.
     *
     * @param syncing read inside the pass, after the delay, not here.
     * @param force starts one even for the same fichas: after a sync ends or an export gives the
     *   network back.
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
            // Lets the first screen draw before the pass checks every photograph in the disk cache.
            delay(startDelayMillis)
            val held = prefetchRefusal(conditions(syncing()))
            val status = prefetch.run(images, held) { partial -> _status.value = partial }
            covered = images
            _status.value = status
        }
    }

    /**
     * Gives the network up without waiting, for an export about to take all four slots. Not joined:
     * the export doesn't write the photograph count.
     */
    fun cancel() {
        job?.cancel()
    }

    /** Gives the network up and waits for the pass to have finished unwinding. */
    suspend fun yieldNetwork() {
        job?.cancelAndJoin()
    }
}
