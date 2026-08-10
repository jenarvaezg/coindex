package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The ladder of referents: **the comparison does not decorate the figure, it is the figure**.
 *
 * «6,95 kg» says nothing; «más que un gato y a 310 g de una bola de bolos» does
 * (`docs/ux/cifras-326.md`).
 */
class ReferentsTest {
    /**
     * The scale is **ordinal and not metric**: five rungs equally spaced, the collection interpolated
     * between its two neighbours.
     *
     * Logarithmic was tried first and piled three labels on top of the fourth, and that is also why the
     * ladder carries no zoom — zooming an ordinal scale means nothing, and making it metric brings the
     * overlaps back.
     */
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

    /** The collection sits between the two rungs it is between, and says which they are. */
    @Test
    fun `the collection is placed between its two neighbours`() {
        val placement = Ladders.weight.place(6.95)

        assertEquals(Referent.Cat, placement.justPassed?.referent)
        assertEquals(Referent.BowlingBall, placement.nextUp?.referent)
        assertTrue(placement.fraction > 0.25 && placement.fraction < 0.5)
    }

    /** Under the first rung there is nothing passed yet, and the mark sits at the foot. */
    @Test
    fun `below the first rung nothing has been passed`() {
        val placement = Ladders.row.place(0.4)

        assertEquals(0.0, placement.fraction)
        assertNull(placement.justPassed)
        assertEquals(Referent.Bicycle, placement.nextUp?.referent)
    }

    /**
     * Over the last rung there is nothing left to reach, which is the day the list has to grow.
     *
     * «La escalera se queda corta por arriba»: the referents are a datum of the app and not of the
     * collector, so the state that says so has to be legible from the outside.
     */
    @Test
    fun `over the last rung there is nothing left to reach`() {
        val placement = Ladders.stack.place(400.0)

        assertEquals(1.0, placement.fraction)
        assertEquals(Referent.Person, placement.justPassed?.referent)
        assertNull(placement.nextUp)
    }

    /** Exactly on a rung is on it, and not a hair past it. */
    @Test
    fun `landing on a rung is landing on it`() {
        val placement = Ladders.row.place(12.0)

        assertEquals(0.5, placement.fraction)
        assertEquals(Referent.Bus, placement.justPassed?.referent)
        assertEquals(Referent.Lorry, placement.nextUp?.referent)
    }

    /**
     * The three ladders, their units and their rungs, pinned.
     *
     * They are literals and the pinning is the point: nothing here is derived from the collection, so a
     * rung edited by accident is a figure that silently means something else.
     */
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

    /** Every referent of the enum is on exactly one ladder: an unused one is a drawing nobody sees. */
    @Test
    fun `every referent stands on one ladder and only one`() {
        val used = Ladders.all.flatMap { ladder -> ladder.rungs.map { it.referent } }

        assertEquals(Referent.entries.toSet(), used.toSet())
        assertEquals(used.size, used.distinct().size, "un referente aparece en dos escaleras")
    }
}
