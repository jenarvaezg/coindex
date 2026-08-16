package com.jenarvaezg.coindex.ui

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
 * What «Explorar» says, and the three things it must never say (ADR 0030).
 *
 * The gesture has to name its spend **before** it is pressed; the one figure of a plate that is not
 * yours has to say which figure it is, where it came from and **when**; and neither of them may print a
 * zero — «· 0 consultas» and «0/12» are the two ways this screen could turn a plate the collector went
 * looking at into a reproach.
 */
class ShowcaseLabelsTest {
    /**
     * The spend rides in the gesture, and the word changes once there is something to refresh.
     *
     * «Volver a tasar» never disappears (ADR 0030 §4), because the price it asks for never expires: what
     * would otherwise be left is an amount from March with no way on earth to bring a newer one.
     */
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

    /**
     * With nothing left to ask the figure is dropped rather than printed as a zero.
     *
     * «Volver a tasar · 0 consultas» reads as a gesture that is broken; what happens when it is pressed
     * is [ShowcaseLabels.ALREADY_FRESH], which is a sentence about the prices and not about the button.
     */
    @Test
    fun `a plate with nothing left to ask drops the figure instead of printing a zero`() {
        val label = showcaseValueAction(calls = 0, valued = true, valuing = false)

        assertEquals(ShowcaseLabels.REVALUE_ACTION, label)
        assertFalse("0" in label)
        assertTrue(ShowcaseLabels.ALREADY_FRESH.startsWith("Esta lámina ya está tasada"))
    }

    /** While the calls are in flight the gesture says so, in the ficha's own words. */
    @Test
    fun `the gesture says it is asking while it asks`() {
        assertEquals(
            ShowcaseLabels.VALUING,
            showcaseValueAction(calls = 34, valued = false, valuing = true),
        )
    }

    /**
     * The one figure of the header: its name, its amount, its provenance and its date.
     *
     * «Coste de entrar» and not «Coste de cerrar», which is ADR 0030 §6: closing a plate is buying the
     * last of something you collect, and this is buying the first of something you do not.
     */
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

    /**
     * What the amount covers, whenever it is not the whole plate (ADR 0028 §4, §7).
     *
     * A plate of twelve casillas where four were priced — Numista had no price for the rest, or the budget
     * ran out halfway and the pass is resumable, so it wrote what it had asked — says so. «412 €» over
     * that plate is not an incomplete total, it is a false one.
     */
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

    /**
     * How old a price is, in calendar days and in the coarsest unit that is still true.
     *
     * The ficha's own wording applied to a price, because it is the same fact about the same phone: what
     * this app brought, and when. Elapsed milliseconds would make last night's tasación «hace 11 horas»
     * rounded to today, which is the contradiction #398 measured for the spot.
     */
    @Test
    fun `the age of a price is counted in calendar days`() {
        assertEquals("tasada hoy", valuedAgeLabel(NOW, NOW, MADRID))
        assertEquals("tasada ayer", valuedAgeLabel(NOW - DAY, NOW, MADRID))
        assertEquals("tasada hace 6 días", valuedAgeLabel(NOW - 6 * DAY, NOW, MADRID))
        // Past a month it says the **day**: nothing will ever refresh this price, so «hace 8 meses» is
        // the date ADR 0030 §4 asks for, rounded away.
        assertEquals("tasada el 2 jul 2026", valuedAgeLabel(NOW - 40 * DAY, NOW, MADRID))
        assertEquals("tasada el 7 jul 2025", valuedAgeLabel(NOW - 400 * DAY, NOW, MADRID))
        // A clock that has gone backwards is not a price from the future: it is today's.
        assertEquals("tasada hoy", valuedAgeLabel(NOW + 5 * DAY, NOW, MADRID))
    }

    /**
     * A tile of the window says its casillas or its cost, and **never** a fraction.
     *
     * «0/12» is a fraction of a plate you are collecting; on one you are not it says you are nought of
     * the way through a collection you never started, which is the reproach ADR 0026 §10 exists to avoid.
     */
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
        // The provenance is not repeated on twenty tiles: the plate says it once, inside.
        assertFalse(
            FiguresLabels.HOLE_CRITERION in showcaseTileCostLabel(
                ShowcaseCost(eur = 1.0, holes = 1, slots = 1, readAt = NOW),
                NOW,
                MADRID,
            ),
        )
    }

    /**
     * The order that sorts by money says how much of the shelf it could not place (#513).
     *
     * «Por coste de entrar» leaves everything with no amount behind it (ADR 0030 §8 clause 3), and on a
     * shelf where little has been valued that is a screen that barely moves when the order is changed.
     * The line is the same transparency Ajustes prints under the pass: what the app is doing, in the one
     * place the collector is looking at it.
     */
    @Test
    fun `the cost order says how many plates it could not place, and the other order says nothing`() {
        val valued = shelfTile("panda", entryEur = 412.0)
        val unvalued = shelfTile("kooka")

        // The default order needs no price, so nothing is owed about the ones that have none.
        assertNull(showcaseOrderNote(ShowcaseSort.ByCasillas, listOf(valued, unvalued)))
        // Nothing to warn about: every plate on the shelf carries an amount.
        assertNull(showcaseOrderNote(ShowcaseSort.ByEntryCost, listOf(valued)))
        // An empty shelf already says why it is empty, and a second sentence under it would be furniture.
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
     * With nothing valued at all the line stops counting and says the order does nothing (#513).
     *
     * «4 láminas sin tasar, al final» over a shelf where *everything* is at the end describes an order
     * that placed nothing as though it had placed something: the shelf the collector is looking at is
     * the default one, in a different name.
     */
    @Test
    fun `an unvalued shelf is told the cost order changes nothing at all`() {
        val note = showcaseOrderNote(
            ShowcaseSort.ByEntryCost,
            listOf(shelfTile("kooka"), shelfTile("libertad")),
        )

        assertEquals(ShowcaseLabels.NOTHING_VALUED, note)
        // No count in it: there is nothing to compare the number against.
        assertFalse("2" in note.orEmpty())
        assertTrue(ShowcaseLabels.NOTHING_VALUED.startsWith("Todavía no hay ninguna lámina tasada"))
    }

    /** What put one of the collector's own plates on this shelf, in the words the hole's chip uses. */
    @Test
    fun `a plate of yours says how many of its casillas you are looking for`() {
        assertEquals("2 lo busco", showcaseWishedLabel(2))
        assertTrue(WishLabels.MARK_WORD in showcaseWishedLabel(1))
    }

    /**
     * The door of the index, in the two forms ADR 0026 §8 clause 3 wrote, and the zero that is not
     * printed.
     *
     * The count is the **twenty and not the twenty-three**: what is behind that door which the index does
     * not already hold is the shelf window (ADR 0030 §8).
     */
    @Test
    fun `the index door names the marks, the plates, or both, and is absent at zero`() {
        assertEquals("Lo que busco · 7, y otras 20 láminas", annexDoorLabel(wishes = 7, plates = 20))
        assertEquals("Y otras 20 láminas que no coleccionas", annexDoorLabel(wishes = 0, plates = 20))
        // The number disappears in the singular: «otra 1 lámina» is not Spanish.
        assertEquals("Y otra lámina que no coleccionas", annexDoorLabel(wishes = 0, plates = 1))
        assertEquals("Lo que busco · 1, y otra lámina", annexDoorLabel(wishes = 1, plates = 1))
        assertEquals("Lo que busco · 7", annexDoorLabel(wishes = 7, plates = 0))
        assertNull(annexDoorLabel(wishes = 0, plates = 0))
        // The arrow is drawn and not typed, on this door as on the one it replaced (#298).
        assertFalse('→' in annexDoorLabel(7, 20).orEmpty())
    }

    /**
     * A refusal is the answer to a press and says nothing was spent.
     *
     * Its own sentence and not the settings line's: that one is about prices arriving on their own.
     */
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

    /** The spend of this feature is spoken in the unit the marking mode already spends in. */
    @Test
    fun `the spend is counted in consultas, like the marks`() {
        assertEquals("2 consultas", queriesLabel(2))
        assertEquals("1 consulta", queriesLabel(1))
        assertTrue("consultas" in WishLabels.MARK_HINT)
    }
}

/**
 * A tile of the shelf with only the fact the order note reads: whether it carries an amount.
 *
 * The assembled ones live in `ShowcaseSubjectTest`, where the catalogs and the price table decide what
 * a tile says; this line is about a list of tiles and nothing else touches them.
 */
private fun shelfTile(catalogId: String, entryEur: Double? = null) = ShowcaseTile(
    catalogId = catalogId,
    name = catalogId,
    typeId = null,
    printedSide = PrintedSide.Reverse,
    mine = false,
    footnote = showcaseSlotsLabel(3),
    entryEur = entryEur,
    slots = 3,
)
