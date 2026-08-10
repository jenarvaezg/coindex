package com.jenarvaezg.coindex.data.prices

import com.jenarvaezg.coindex.data.FakeValuationPass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

private val PLAN = ValuationPlan(owned = listOf(OwnedIssue(30, 297)), holes = emptyList())

/**
 * When a pass is worth starting, and who gets the budget when two things want it (ADR 0028 §3, §6).
 *
 * The sibling of `PhotoPrefetchLoopTest`, with one condition gone and one graver: there is no wifi to wait
 * for, and a sync does not merely take the network — it takes **calls out of the same monthly bote**, so a
 * pass still unwinding can make the sync fail with `BudgetExhausted`.
 */
class ValuationLoopTest {
    private val pass = FakeValuationPass(ValuationStatus(wanted = 1, missing = 0))
    private var syncing = false

    @Test
    fun `a pass runs once the first screen has had its three seconds`() = runTest {
        val loop = loop()

        loop.start(this, PLAN)
        assertTrue(pass.passes.isEmpty(), "el pase no arranca antes que el índice")

        advanceUntilIdle()
        assertEquals(listOf(PLAN), pass.passes.map { it.plan })
        assertEquals(0, loop.status.value.missing)
    }

    /** The same collection does not buy a second pass: with everything cached it would cost nothing. */
    @Test
    fun `the same plan does not buy a second pass`() = runTest {
        val loop = loop()

        loop.start(this, PLAN)
        advanceUntilIdle()
        loop.start(this, PLAN)
        advanceUntilIdle()

        assertEquals(1, pass.passes.size)
    }

    /** A plan that has changed does, because a new piece is a new issue to ask about. */
    @Test
    fun `a plan that has changed buys one`() = runTest {
        val loop = loop()

        loop.start(this, PLAN)
        advanceUntilIdle()
        loop.start(this, PLAN.copy(owned = PLAN.owned + OwnedIssue(30, 298)))
        advanceUntilIdle()

        assertEquals(2, pass.passes.size)
    }

    /** Forced is what the end of a sync uses: there is something new to ask about that the plan cannot show. */
    @Test
    fun `forcing starts a pass over the same plan`() = runTest {
        val loop = loop()

        loop.start(this, PLAN)
        advanceUntilIdle()
        loop.start(this, PLAN, force = true)
        advanceUntilIdle()

        assertEquals(2, pass.passes.size)
    }

    /** An empty plan is nothing to ask about, so no pass and no status. */
    @Test
    fun `an empty plan starts nothing`() = runTest {
        val loop = loop()

        loop.start(this, ValuationPlan(emptyList(), emptyList()))
        advanceUntilIdle()

        assertTrue(pass.passes.isEmpty())
    }

    /** Two passes would fight for the same allowance, so a second start while one runs is dropped. */
    @Test
    fun `only one pass runs at a time`() = runTest {
        pass.gate = CompletableDeferred()
        val loop = loop()

        loop.start(this, PLAN)
        advanceUntilIdle()
        loop.start(this, PLAN.copy(owned = PLAN.owned + OwnedIssue(30, 298)))
        advanceUntilIdle()

        assertEquals(1, pass.passes.size)
        pass.gate?.complete(Unit)
        advanceUntilIdle()
    }

    /**
     * A sync in flight when the pass starts holds it, and the pass writes nothing.
     *
     * Read **when the pass starts** and not when it was asked for: three seconds of a cold start is long
     * enough for the collector to have pressed «Sincronizar».
     */
    @Test
    fun `a sync in flight holds the pass`() = runTest {
        val loop = loop()

        loop.start(this, PLAN)
        syncing = true
        advanceUntilIdle()

        assertEquals(listOf(ValuationRefusal.Syncing), pass.passes.map { it.held })
    }

    /**
     * A pass held by a sync has covered nothing, so the next launch tries again.
     *
     * Remembering the plan would strand every issue the held pass never asked about until the collection
     * itself changed, which on a phone that syncs once a month is a month.
     */
    @Test
    fun `a held pass does not talk the next one out of trying`() = runTest {
        val loop = loop()

        loop.start(this, PLAN)
        syncing = true
        advanceUntilIdle()
        syncing = false
        loop.start(this, PLAN)
        advanceUntilIdle()

        assertEquals(listOf(ValuationRefusal.Syncing, null), pass.passes.map { it.held })
    }

    /**
     * Yielding waits for the pass to finish unwinding, which is stricter than the photographs' cancel.
     *
     * What the sync needs back is not bandwidth but **calls**, and a pass still unwinding can still be
     * inside `reserve()` taking one of them.
     */
    @Test
    fun `yielding cancels the pass and waits for it`() = runTest {
        pass.gate = CompletableDeferred()
        val loop = loop()

        loop.start(this, PLAN)
        advanceUntilIdle()
        loop.yieldNetwork()

        assertEquals(1, pass.cancelled)
    }

    /** After a yield the plan is forgotten, so the pass that follows the sync starts from scratch. */
    @Test
    fun `after yielding the same plan buys a pass again`() = runTest {
        val loop = loop()

        loop.start(this, PLAN)
        advanceUntilIdle()
        loop.yieldNetwork()
        loop.start(this, PLAN)
        advanceUntilIdle()

        assertEquals(2, pass.passes.size)
    }

    /** An export stands it down without being waited for: the export spends no API budget. */
    @Test
    fun `an export cancels the pass without waiting`() = runTest {
        pass.gate = CompletableDeferred()
        val loop = loop()

        loop.start(this, PLAN)
        advanceUntilIdle()
        loop.cancel()
        advanceUntilIdle()

        assertEquals(1, pass.cancelled)
    }

    private fun loop() = ValuationLoop(pass, { syncing })
}
