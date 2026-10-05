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
 * The preferences file in memory (#220). The stores over it are the real ones (#546), so their
 * rules are tested and only the storage is faked.
 */
class FakeNamedValues(initial: Map<String, Stored> = emptyMap()) : NamedValues {
    private val stored = initial.toMutableMap()

    /** The stored entries, so a test can check the shape a value was written in. */
    val entries: Map<String, Stored> get() = stored.toMap()

    override fun read(key: String): Stored? = stored[key]

    override fun write(values: Map<String, Stored?>) {
        values.forEach { (key, value) ->
            if (value == null) stored -= key else stored[key] = value
        }
    }
}

/**
 * The credential store with a JVM-made key instead of the device's (#546). One key per store, read
 * through a lambda as the app reads the keystore's: a new key per call couldn't decrypt.
 */
fun credentialsOnJvm(
    values: NamedValues = FakeNamedValues(),
    wall: RejectionWall = StoredRejectionWall(FakeNamedValues()),
): StoredCredentials {
    val secret = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    return StoredCredentials(values, wall) { secret }
}

class FakeShelfStore(
    override var index: IndexShelf = IndexShelf(),
    override var coins: CoinsShelf = CoinsShelf(),
) : ShelfStore

/**
 * A prefetch that fetches nothing and records each call. Setting [gate] holds a pass mid-run, so
 * a test can watch a sync take the network from it.
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
 * A valuation pass that asks nobody and records each call. Setting [gate] holds a pass mid-run, so
 * a test can watch a sync take the budget from it (ADR 0028 §6).
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
