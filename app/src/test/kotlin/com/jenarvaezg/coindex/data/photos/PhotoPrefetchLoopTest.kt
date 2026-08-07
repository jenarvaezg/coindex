package com.jenarvaezg.coindex.data.photos

import com.jenarvaezg.coindex.data.FakePhotoPrefetch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/** The three seconds the loop lets the first screen have the phone to itself. */
private const val START_DELAY = 3_000L

private fun images(vararg urls: String): List<TypeImages> =
    urls.map { TypeImages(obverse = CoinPhoto(thumbnail = it)) }

/**
 * When a pass of the photograph prefetch is worth starting, and who gets the network (#191, #220).
 *
 * These four rules used to be guards inside a coroutine in the ViewModel, and one of them was
 * wrong: the pass was skipped when `images.hashCode()` matched, which is «probably the same
 * photographs» — a collision cost a whole pass until the next launch. Here it is the fichas
 * themselves, and that is a difference a test can see.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PhotoPrefetchLoopTest {
    private val wifi = PrefetchConditions(unmeteredNetwork = true)

    private fun loop(
        prefetch: FakePhotoPrefetch,
        conditions: (Boolean) -> PrefetchConditions = { syncing -> wifi.copy(syncing = syncing) },
    ) = PhotoPrefetchLoop(prefetch, { syncing -> conditions(syncing) }, START_DELAY)

    /**
     * Lets the pass past the three seconds it gives the first screen.
     *
     * `advanceUntilIdle` would not do: since coroutines 1.10 it leaves work started in another scope
     * where it is, and the pass is launched into the caller's — which in the app is the ViewModel's.
     */
    private fun TestScope.settle() = advanceTimeBy(START_DELAY + 1)

    @Test
    fun `nothing is asked for while the first screen is still laying itself out`() = runTest {
        val prefetch = FakePhotoPrefetch()

        loop(prefetch).start(backgroundScope, images("a.jpg"), syncing = { false })
        advanceTimeBy(START_DELAY - 1)
        runCurrent()

        assertEquals(0, prefetch.passes.size)

        settle()

        assertEquals(1, prefetch.passes.size)
    }

    @Test
    fun `the same fichas asked for again do not buy a second pass`() = runTest {
        val prefetch = FakePhotoPrefetch()
        val loop = loop(prefetch)

        loop.start(backgroundScope, images("a.jpg", "b.jpg"), syncing = { false })
        settle()
        loop.start(backgroundScope, images("a.jpg", "b.jpg"), syncing = { false })
        settle()

        assertEquals(1, prefetch.passes.size)
    }

    @Test
    fun `a photograph the phone has never seen buys one, with the count standing still`() = runTest {
        val prefetch = FakePhotoPrefetch()
        val loop = loop(prefetch)

        loop.start(backgroundScope, images("a.jpg", "b.jpg"), syncing = { false })
        settle()
        // A refreshed ficha (#185): two photographs before and two after, one of them new.
        loop.start(backgroundScope, images("a.jpg", "c.jpg"), syncing = { false })
        settle()

        assertEquals(2, prefetch.passes.size)
        assertEquals(images("a.jpg", "c.jpg"), prefetch.passes.last().images)
    }

    @Test
    fun `a sync that ended forces a pass the fichas would not have asked for`() = runTest {
        val prefetch = FakePhotoPrefetch()
        val loop = loop(prefetch)

        loop.start(backgroundScope, images("a.jpg"), syncing = { false })
        settle()
        loop.start(backgroundScope, images("a.jpg"), syncing = { false }, force = true)
        settle()

        assertEquals(2, prefetch.passes.size)
    }

    @Test
    fun `two passes never run at once, however often the collection emits`() = runTest {
        val prefetch = FakePhotoPrefetch().apply { gate = CompletableDeferred() }
        val loop = loop(prefetch)

        loop.start(backgroundScope, images("a.jpg"), syncing = { false })
        settle()
        loop.start(backgroundScope, images("b.jpg"), syncing = { false }, force = true)
        settle()

        assertEquals(1, prefetch.passes.size)
    }

    @Test
    fun `a sync takes the network and waits for the pass to have unwound`() = runTest {
        val prefetch = FakePhotoPrefetch().apply { gate = CompletableDeferred() }
        val loop = loop(prefetch)
        loop.start(backgroundScope, images("a.jpg"), syncing = { false })
        settle()

        loop.yieldNetwork()

        assertEquals(1, prefetch.cancelled)
        // Nothing was covered, so the forced pass a finished sync starts finds work to do.
        prefetch.gate = null
        loop.start(backgroundScope, images("a.jpg"), syncing = { false }, force = true)
        settle()
        assertEquals(2, prefetch.passes.size)
    }

    @Test
    fun `the phone is asked about the network when the pass starts, not when it is asked for`() =
        runTest {
            val prefetch = FakePhotoPrefetch()
            var metered = false
            val loop = loop(prefetch) { syncing ->
                PrefetchConditions(unmeteredNetwork = !metered, syncing = syncing)
            }

            loop.start(backgroundScope, images("a.jpg"), syncing = { false })
            // The collector walks out of the wifi during the three seconds of the cold start.
            metered = true
            settle()

            assertEquals(PrefetchRefusal.MeteredNetwork, prefetch.passes.single().held)
        }

    @Test
    fun `a sync in flight is the reason the photographs are held`() = runTest {
        val prefetch = FakePhotoPrefetch()

        loop(prefetch).start(backgroundScope, images("a.jpg"), syncing = { true })
        settle()

        assertEquals(PrefetchRefusal.Syncing, prefetch.passes.single().held)
    }

    @Test
    fun `a collection with no photographs at all is not a pass`() = runTest {
        val prefetch = FakePhotoPrefetch()

        loop(prefetch).start(backgroundScope, emptyList(), syncing = { false })
        settle()

        assertEquals(0, prefetch.passes.size)
    }
}
