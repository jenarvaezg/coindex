package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The ladder of everyday referents a figure is read against (`docs/ux/cifras-326.md`). */
class ReferentsTest {
    /** Ordinal on purpose: a log scale piled the labels up, and an ordinal one can't be zoomed. */
    @Test
    fun `the rungs are equally spaced whatever their amounts`() {
        // Brick 2 · cat 4,5 · bowling ball 7,26 · tyre 9,5 · labrador 30.
        assertEquals(0.25, Ladders.weight.place(4.5).fraction)
        assertEquals(0.5, Ladders.weight.place(7.26).fraction)
        assertEquals(0.75, Ladders.weight.place(9.5).fraction)
        // Half way between the tyre and the labrador is three quarters of the way *plus* an eighth,
        // even though 19,75 kg is nowhere near the middle of the ladder in kilos.
        assertEquals(0.875, Ladders.weight.place(19.75).fraction)
    }

    @Test
    fun `the collection is placed between its two neighbours`() {
        val placement = Ladders.weight.place(6.95)

        assertEquals(Referent.Cat, placement.justPassed?.referent)
        assertEquals(Referent.BowlingBall, placement.nextUp?.referent)
        assertTrue(placement.fraction > 0.25 && placement.fraction < 0.5)
    }

    /** The mark sits at the foot. */
    @Test
    fun `below the first rung nothing has been passed`() {
        val placement = Ladders.row.place(0.4)

        assertEquals(0.0, placement.fraction)
        assertNull(placement.justPassed)
        assertEquals(Referent.Bicycle, placement.nextUp?.referent)
    }

    /** The referents are app data, so this has to be visible: it means the list must grow. */
    @Test
    fun `over the last rung there is nothing left to reach`() {
        val placement = Ladders.stack.place(400.0)

        assertEquals(1.0, placement.fraction)
        assertEquals(Referent.Person, placement.justPassed?.referent)
        assertNull(placement.nextUp)
    }

    /** Exactly on a rung counts as on it, not past it. */
    @Test
    fun `landing on a rung is landing on it`() {
        val placement = Ladders.row.place(12.0)

        assertEquals(0.5, placement.fraction)
        assertEquals(Referent.Bus, placement.justPassed?.referent)
        assertEquals(Referent.Lorry, placement.nextUp?.referent)
    }

    /** Literals, pinned: a rung edited by accident would silently change what a figure means. */
    @Test
    fun `the three ladders are the ones the prototype settled`() {
        assertEquals(listOf(LadderKind.Weight, LadderKind.Row, LadderKind.Stack), Ladders.all.map { it.kind })
        assertEquals(
            listOf(LadderUnit.Kilograms, LadderUnit.Metres, LadderUnit.Centimetres),
            Ladders.all.map { it.unit },
        )
        Ladders.all.forEach { ladder ->
            assertEquals(5, ladder.rungs.size, "la escalera ${ladder.kind} no tiene cinco referentes")
            assertEquals(
                ladder.rungs.map { it.amount }.sorted(),
                ladder.rungs.map { it.amount },
                "los referentes de ${ladder.kind} no van de menos a más",
            )
        }
        assertEquals(
            listOf(2.0, 4.5, 7.26, 9.5, 30.0),
            Ladders.weight.rungs.map { it.amount },
        )
    }

    /** An unused referent is a drawing nobody sees. */
    @Test
    fun `every referent stands on one ladder and only one`() {
        val used = Ladders.all.flatMap { ladder -> ladder.rungs.map { it.referent } }

        assertEquals(Referent.entries.toSet(), used.toSet())
        assertEquals(used.size, used.distinct().size, "un referente aparece en dos escaleras")
    }
}
