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
 * When a pass starts and who gets the budget (ADR 0028 §3, §6). Unlike `PhotoPrefetchLoopTest`
 * there is no wifi to wait for, but a sync draws on the same monthly budget, so a pass still
 * unwinding can make it fail with `BudgetExhausted`.
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

    /** With everything cached a second pass would cost nothing anyway. */
    @Test
    fun `the same plan does not buy a second pass`() = runTest {
        val loop = loop()

        loop.start(this, PLAN)
        advanceUntilIdle()
        loop.start(this, PLAN)
        advanceUntilIdle()

        assertEquals(1, pass.passes.size)
    }

    @Test
    fun `a plan that has changed buys one`() = runTest {
        val loop = loop()

        loop.start(this, PLAN)
        advanceUntilIdle()
        loop.start(this, PLAN.copy(owned = PLAN.owned + OwnedIssue(30, 298)))
        advanceUntilIdle()

        assertEquals(2, pass.passes.size)
    }

    /** The end of a sync forces it: there may be something new the plan cannot show. */
    @Test
    fun `forcing starts a pass over the same plan`() = runTest {
        val loop = loop()

        loop.start(this, PLAN)
        advanceUntilIdle()
        loop.start(this, PLAN, force = true)
        advanceUntilIdle()

        assertEquals(2, pass.passes.size)
    }

    @Test
    fun `an empty plan starts nothing`() = runTest {
        val loop = loop()

        loop.start(this, ValuationPlan(emptyList(), emptyList()))
        advanceUntilIdle()

        assertTrue(pass.passes.isEmpty())
    }

    /** Two passes would share one allowance. */
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
     * The sync is checked when the pass starts, not when it is asked for: the collector can press
     * «Sincronizar» during the three-second wait.
     */
    @Test
    fun `a sync in flight holds the pass`() = runTest {
        val loop = loop()

        loop.start(this, PLAN)
        syncing = true
        advanceUntilIdle()

        assertEquals(listOf(ValuationRefusal.Syncing), pass.passes.map { it.held })
    }

    /** Remembering a held plan would strand its issues until the collection changed. */
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
     * Stricter than the photographs' cancel: the sync needs calls back, and an unwinding pass can
     * still be inside `reserve()` taking one.
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

    /** An export spends no API budget, so it does not wait for the pass. */
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
