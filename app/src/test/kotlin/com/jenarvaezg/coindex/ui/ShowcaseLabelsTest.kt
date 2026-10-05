package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.prices.PRICE_LIFETIME_MILLIS
import com.jenarvaezg.coindex.data.prices.ValuationRefusal
import com.jenarvaezg.coindex.domain.PrintedSide
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val MADRID = ZoneId.of("Europe/Madrid")

/** 11 de agosto de 2026, 12:00 en Madrid. */
private const val NOW = 1_786_442_400_000L
private const val DAY = 24L * 60 * 60 * 1_000

/**
 * What «Explorar» says (ADR 0030): the gesture names its spend before it is pressed, the figure
 * says which it is, where it came from and when, and neither prints a zero.
 */
class ShowcaseLabelsTest {
    /** «Volver a tasar» never disappears (ADR 0030 §4): the price it asks for never expires. */
    @Test
    fun `the gesture names its spend, and says «volver» once the plate has a price`() {
        assertEquals(
            "Tasar esta lámina · 34 consultas",
            showcaseValueAction(calls = 34, valued = false, valuing = false),
        )
        assertEquals(
            "Volver a tasar · 12 consultas",
            showcaseValueAction(calls = 12, valued = true, valuing = false),
        )
        assertEquals(
            "Tasar esta lámina · 1 consulta",
            showcaseValueAction(calls = 1, valued = false, valuing = false),
        )
    }

    /** Pressing it then answers with [ShowcaseLabels.ALREADY_FRESH]. */
    @Test
    fun `a plate with nothing left to ask drops the figure instead of printing a zero`() {
        val label = showcaseValueAction(calls = 0, valued = true, valuing = false)

        assertEquals(ShowcaseLabels.REVALUE_ACTION, label)
        assertFalse("0" in label)
        assertTrue(ShowcaseLabels.ALREADY_FRESH.startsWith("Esta lámina ya está tasada"))
    }

    /**
     * The snackbar quotes `PRICE_LIFETIME_MILLIS` (#561), so it must hold for the oldest price the
     * pass leaves alone, which past a month is shown by its date (ADR 0028 §5).
     */
    @Test
    fun `the snackbar holds over the oldest price the pass will not re-ask`() {
        val oldest = NOW - (PRICE_LIFETIME_MILLIS - DAY)

        assertTrue("menos de tres meses" in ShowcaseLabels.ALREADY_FRESH)
        assertTrue(
            valuedAgeLabel(oldest, NOW, MADRID).startsWith("tasada el "),
            "el precio más viejo que la pasada no vuelve a pedir se dice con su día, no con su edad",
        )
    }

    @Test
    fun `the gesture says it is asking while it asks`() {
        assertEquals(
            ShowcaseLabels.VALUING,
            showcaseValueAction(calls = 34, valued = false, valuing = true),
        )
    }

    /** «Entrar», not «cerrar» (ADR 0030 §6): this buys the first of a set, not the last. */
    @Test
    fun `the figure of a plate that is not yours says entering, in unc, with its date`() {
        val label = showcaseEntryLabel(
            ShowcaseCost(eur = 412.0, holes = 8, slots = 8, readAt = NOW),
            nowMillis = NOW,
            zone = MADRID,
        )

        assertEquals("Coste de entrar: 412 € · en sin circular · tasada hoy", label)
        assertFalse(FiguresLabels.PLATE_COST_LABEL in label)
        assertFalse(FiguresLabels.MONEY_CRITERION in label)
    }

    /** Partial when Numista lacked prices or the budget ran out mid-pass (ADR 0028 §4, §7). */
    @Test
    fun `the figure says what part of the plate it covers, and stays quiet when it covers all of it`() {
        val partial = showcaseEntryLabel(
            ShowcaseCost(eur = 150.0, holes = 4, slots = 12, readAt = NOW),
            nowMillis = NOW,
            zone = MADRID,
        )
        val whole = showcaseEntryLabel(
            ShowcaseCost(eur = 412.0, holes = 8, slots = 8, readAt = NOW),
            nowMillis = NOW,
            zone = MADRID,
        )

        assertEquals("Coste de entrar: 150 € · en sin circular · 4 de 12 casillas · tasada hoy", partial)
        assertFalse("casillas" in whole)
    }

    /** The ficha's wording; elapsed hours would contradict the calendar day, as in #398. */
    @Test
    fun `the age of a price is counted in calendar days`() {
        assertEquals("tasada hoy", valuedAgeLabel(NOW, NOW, MADRID))
        assertEquals("tasada ayer", valuedAgeLabel(NOW - DAY, NOW, MADRID))
        assertEquals("tasada hace 6 días", valuedAgeLabel(NOW - 6 * DAY, NOW, MADRID))
        // Past a month, the date: nothing refreshes this price on its own (ADR 0030 §4).
        assertEquals("tasada el 2 jul 2026", valuedAgeLabel(NOW - 40 * DAY, NOW, MADRID))
        assertEquals("tasada el 7 jul 2025", valuedAgeLabel(NOW - 400 * DAY, NOW, MADRID))
        // A clock that went backwards reads as today.
        assertEquals("tasada hoy", valuedAgeLabel(NOW + 5 * DAY, NOW, MADRID))
    }

    /** Never a fraction: «0/12» on a plate you don't collect is the reproach of ADR 0026 §10. */
    @Test
    fun `a tile says how many casillas it is, or what entering costs and when`() {
        assertEquals("12 casillas", showcaseSlotsLabel(12))
        assertEquals("1 casilla", showcaseSlotsLabel(1))
        assertEquals(
            "412 € · tasada hoy",
            showcaseTileCostLabel(
                ShowcaseCost(eur = 412.0, holes = 8, slots = 8, readAt = NOW),
                nowMillis = NOW,
                zone = MADRID,
            ),
        )
        // The provenance is said once, inside the plate.
        assertFalse(
            FiguresLabels.HOLE_CRITERION in showcaseTileCostLabel(
                ShowcaseCost(eur = 1.0, holes = 1, slots = 1, readAt = NOW),
                NOW,
                MADRID,
            ),
        )
    }

    /**
     * Unvalued plates go last (ADR 0030 §8 clause 3), so on a lightly valued shelf the order barely
     * moves without this line (#513).
     */
    @Test
    fun `the cost order says how many plates it could not place, and the other order says nothing`() {
        val valued = shelfTile("panda", entryEur = 412.0)
        val unvalued = shelfTile("kooka")

        // The default order needs no prices.
        assertNull(showcaseOrderNote(ShowcaseSort.ByCasillas, listOf(valued, unvalued)))
        // Every plate has an amount.
        assertNull(showcaseOrderNote(ShowcaseSort.ByEntryCost, listOf(valued)))
        // An empty shelf already explains itself.
        assertNull(showcaseOrderNote(ShowcaseSort.ByEntryCost, emptyList()))

        assertEquals(
            "1 lámina sin tasar, al final: este orden sólo coloca las tasadas.",
            showcaseOrderNote(ShowcaseSort.ByEntryCost, listOf(valued, unvalued)),
        )
        assertEquals(
            "3 láminas sin tasar, al final: este orden sólo coloca las tasadas.",
            showcaseOrderNote(
                ShowcaseSort.ByEntryCost,
                listOf(valued, unvalued, unvalued, unvalued),
            ),
        )
    }

    /**
     * A count would describe an order that placed nothing (#513). The line speaks of prices, not of
     * the grid: the collector's own plates lead the default order, so switching can still move it.
     */
    @Test
    fun `an unvalued shelf is told the cost order has no prices to sort by`() {
        val note = showcaseOrderNote(
            ShowcaseSort.ByEntryCost,
            listOf(shelfTile("kooka"), shelfTile("libertad")),
        )

        assertEquals(ShowcaseLabels.NOTHING_VALUED, note)
        // No count: there is nothing to compare it against.
        assertFalse("2" in note.orEmpty())
        assertFalse("no cambia nada" in note.orEmpty())
    }

    /** Own plates never get «Coste de entrar» nor its gesture (ADR 0030 §3, §6; #513). */
    @Test
    fun `the collector's own plates are not counted among the ones left to value`() {
        val valued = shelfTile("panda", entryEur = 412.0)
        val mine = shelfTile("britannia", mine = true)

        assertNull(showcaseOrderNote(ShowcaseSort.ByEntryCost, listOf(valued, mine)))
        assertEquals(
            "1 lámina sin tasar, al final: este orden sólo coloca las tasadas.",
            showcaseOrderNote(ShowcaseSort.ByEntryCost, listOf(valued, mine, shelfTile("kooka"))),
        )
        // Nothing in the window valued: the prices line, without a count.
        assertEquals(
            ShowcaseLabels.NOTHING_VALUED,
            showcaseOrderNote(ShowcaseSort.ByEntryCost, listOf(mine, shelfTile("kooka"))),
        )
        // Only own plates: nothing could be valued, so there is no line.
        assertNull(showcaseOrderNote(ShowcaseSort.ByEntryCost, listOf(mine)))
    }

    /** Why an own plate is on this shelf, in the words of the hole's chip. */
    @Test
    fun `a plate of yours says how many of its casillas you are looking for`() {
        assertEquals("2 lo busco", showcaseWishedLabel(2))
        assertTrue(WishLabels.MARK_WORD in showcaseWishedLabel(1))
    }

    /**
     * The annex row at the foot of the index (ADR 0026 §8 clause 3; #520). It counts only the shelf
     * window, the plates the index doesn't already hold (ADR 0030 §8).
     */
    @Test
    fun `the shelf row names the plates the collector does not collect, and is absent at zero`() {
        assertEquals("Y otras 20 láminas que no coleccionas", showcaseDoorLabel(plates = 20))
        // The number disappears in the singular: «otra 1 lámina» is not Spanish.
        assertEquals("Y otra lámina que no coleccionas", showcaseDoorLabel(plates = 1))
        assertNull(showcaseDoorLabel(plates = 0))
        // The marks have their own row, `wishDoorLabel`.
        assertFalse(WishLabels.DESTINATION in showcaseDoorLabel(plates = 20).orEmpty())
        // The arrow is drawn, not typed (#298).
        assertFalse('→' in showcaseDoorLabel(20).orEmpty())
    }

    /** Worded as a reply to a press, apart from the settings line, which is about the pass. */
    @Test
    fun `a refused tasación says so in its own words`() {
        assertEquals(
            "No se ha podido tasar: no hay red.",
            showcaseRefusalMessage(ValuationRefusal.Offline),
        )
        ValuationRefusal.entries.forEach { refusal ->
            assertTrue(showcaseRefusalMessage(refusal).startsWith("No se ha podido tasar: "))
        }
    }

    @Test
    fun `the spend is counted in consultas, like the marks`() {
        assertEquals("2 consultas", queriesLabel(2))
        assertEquals("1 consulta", queriesLabel(1))
        assertTrue("consultas" in WishLabels.MARK_HINT)
    }
}

/** A bare tile with only what the order note reads; assembled ones are in `ShowcaseSubjectTest`. */
private fun shelfTile(
    catalogId: String,
    entryEur: Double? = null,
    mine: Boolean = false,
) = ShowcaseTile(
    catalogId = catalogId,
    name = catalogId,
    typeId = null,
    printedSide = PrintedSide.Reverse,
    mine = mine,
    footnote = showcaseSlotsLabel(3),
    entryEur = entryEur,
    slots = 3,
)
