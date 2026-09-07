package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.photos.PhotoCacheStatus
import com.jenarvaezg.coindex.data.photos.PhotoPrefetch
import com.jenarvaezg.coindex.data.photos.PrefetchRefusal
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.data.prices.ValuationPass
import com.jenarvaezg.coindex.data.prices.ValuationPlan
import com.jenarvaezg.coindex.data.prices.ValuationRefusal
import com.jenarvaezg.coindex.data.prices.ValuationStatus
import com.jenarvaezg.coindex.ui.shelf.CoinsShelf
import com.jenarvaezg.coindex.ui.shelf.IndexShelf
import com.jenarvaezg.coindex.ui.shelf.ShelfStore
import javax.crypto.KeyGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred

/**
 * In-memory stand-ins for what needs a device: the preferences file, the shelf store and the two
 * passes (#220).
 *
 * There used to be one per store, and behind each sat a rule that is not about storage at all: when
 * a shelf is written, what a sync leaves behind, whether a pass of photographs is worth starting.
 * Three of those stores were a seam apiece over the same preferences file, so what stands in for
 * them now is [FakeNamedValues] and the stores themselves are the real ones (#546).
 */
class FakeNamedValues(initial: Map<String, Stored> = emptyMap()) : NamedValues {
    private val stored = initial.toMutableMap()

    /** Everything that is in the file, so a test can check the shape a value went in as. */
    val entries: Map<String, Stored> get() = stored.toMap()

    override fun read(key: String): Stored? = stored[key]

    override fun write(values: Map<String, Stored?>) {
        values.forEach { (key, value) ->
            if (value == null) stored -= key else stored[key] = value
        }
    }
}

/**
 * The credential store with a key the JVM can make instead of the device's (#546).
 *
 * One key per store and read through a lambda, exactly as the app reads the keystore's: a fresh key
 * on every call would encrypt what it could no longer decrypt.
 */
fun credentialsOnJvm(values: NamedValues = FakeNamedValues()): StoredCredentials {
    val secret = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    return StoredCredentials(values) { secret }
}

class FakeShelfStore(
    override var index: IndexShelf = IndexShelf(),
    override var coins: CoinsShelf = CoinsShelf(),
) : ShelfStore

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

/**
 * A valuation pass that asks nobody and remembers being asked.
 *
 * [gate] does what its sibling's does: with it set the pass holds still there, which is the only way to
 * watch a sync take the **budget** off one that is already running (ADR 0028 §6).
 */
class FakeValuationPass(private val result: ValuationStatus = ValuationStatus()) : ValuationPass {
    data class Pass(val plan: ValuationPlan, val held: ValuationRefusal?)

    val passes = mutableListOf<Pass>()
    var cancelled = 0
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun run(
        plan: ValuationPlan,
        held: ValuationRefusal?,
        onStatus: (ValuationStatus) -> Unit,
    ): ValuationStatus {
        passes += Pass(plan, held)
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
