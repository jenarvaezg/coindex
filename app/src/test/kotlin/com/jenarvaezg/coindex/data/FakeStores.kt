package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.photos.PhotoCacheStatus
import com.jenarvaezg.coindex.data.photos.PhotoPrefetch
import com.jenarvaezg.coindex.data.photos.PrefetchRefusal
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.shelf.CoinsShelf
import com.jenarvaezg.coindex.ui.shelf.IndexShelf
import com.jenarvaezg.coindex.ui.shelf.ShelfStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred

/**
 * In-memory stand-ins for the four stores and the prefetch that need a device (#220).
 *
 * Each of them is one property or a keystore away from being untestable, and behind each sat a rule
 * that is not about storage at all: when a shelf is written, what a sync leaves behind, whether a
 * pass of photographs is worth starting.
 */
class FakeSyncLog(override var last: SyncRecord? = null) : SyncLog

class FakeCredentialStore(
    private var stored: Credentials? = null,
    override var monthlyBudget: Int = DEFAULT_MONTHLY_BUDGET,
) : CredentialStore {
    override fun credentials(): Credentials? = stored

    override fun save(apiKey: String, userId: Long) {
        stored = Credentials(apiKey, userId)
    }

    override fun clear() {
        stored = null
    }
}

class FakeShelfStore(
    override var index: IndexShelf = IndexShelf(),
    override var coins: CoinsShelf = CoinsShelf(),
) : ShelfStore

class FakeNotebookStore(override var options: NotebookOptions = NotebookOptions()) : NotebookStore

/**
 * A prefetch that fetches nothing and remembers being asked.
 *
 * [gate] is what makes a pass hold still: with it set, the pass waits there, which is the only way
 * to watch a sync take the network off one that is already running.
 */
class FakePhotoPrefetch(private val result: PhotoCacheStatus = PhotoCacheStatus()) : PhotoPrefetch {
    data class Pass(val images: List<TypeImages>, val held: PrefetchRefusal?)

    val passes = mutableListOf<Pass>()
    var cancelled = 0
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun run(
        images: Collection<TypeImages>,
        held: PrefetchRefusal?,
        onStatus: (PhotoCacheStatus) -> Unit,
    ): PhotoCacheStatus {
        passes += Pass(images.toList(), held)
        try {
            gate?.await()
        } catch (stopped: CancellationException) {
            cancelled += 1
            throw stopped
        }
        onStatus(result)
        return result
    }
}
