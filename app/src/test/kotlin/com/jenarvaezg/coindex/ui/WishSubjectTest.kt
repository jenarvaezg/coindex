package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.domain.Wish
import com.jenarvaezg.coindex.domain.WishedSlot
import com.jenarvaezg.coindex.domain.wishKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val NOW = 1_786_400_000_000L

/**
 * What the annex draws, worded once for the screen and the exported list (ADR 0029 §6). A row is a
 * casilla plus the plate it came from.
 */
class WishSubjectTest {
    @Test
    fun `a row carries the coin, its plate and its price`() {
        val run = dateRun("kooka", 2_010..2_011, typeId = 30)
        val slots = run.members.map { slot(run, it.id) }

        val subject = wishSubject(slots = slots, costs = mapOf(slots.first().key to 41.5))

        assertEquals(listOf("2010", "2011"), subject.rows.map { it.label })
        assertEquals(listOf("kooka", "kooka"), subject.rows.map { it.plate })
        assertEquals(listOf(PrintedSide.Reverse, PrintedSide.Reverse), subject.rows.map { it.printedSide })
        // The amount alone, as in a plate's hole: the header says the criterion.
        assertEquals(listOf("42 €", null), subject.rows.map { it.cost })
        assertEquals("2 casillas en 1 lámina", subject.census)
    }

    /** The plate's rule (#473). */
    @Test
    fun `a row labelled with its year does not print it twice`() {
        val run = dateRun("kooka", 2_010..2_010, typeId = 30)
        val named = run.copy(members = run.members.map { it.copy(label = "Kookaburra 2010") })

        assertNull(wishSubject(listOf(slot(run, "kooka-2010"))).rows.single().printedName)
        assertEquals(
            "Kookaburra 2010",
            wishSubject(listOf(slot(named, "kooka-2010"))).rows.single().printedName,
        )
    }

    /**
     * Two catalogs can name the same coin (ADR 0021 §10), so the grid keys rows by plate too. Built
     * by hand: `wishedSlots` gives the coin only to the first catalog that claims it.
     */
    @Test
    fun `rows of two plates keep separate keys in the grid`() {
        val kooka = dateRun("kooka", 2_010..2_010, typeId = 30)
        val koala = dateRun("koala", 2_010..2_010, typeId = 30)

        val subject = wishSubject(listOf(slot(kooka, "kooka-2010"), slot(koala, "koala-2010")))

        assertEquals(listOf("kooka/kooka-2010", "koala/koala-2010"), subject.rows.map { it.id })
        assertEquals("2 casillas en 2 láminas", subject.census)
    }

    /**
     * Absence, not zero (ADR 0028 §7). The marks' monthly cost is said by the gesture and Ajustes
     * (ADR 0029 §5), not here.
     */
    @Test
    fun `an unpriced list says no amount and an empty one says no census`() {
        val run = dateRun("kooka", 2_010..2_010, typeId = 30)

        val unpriced = wishSubject(listOf(slot(run, "kooka-2010")))
        assertNull(unpriced.rows.single().cost)

        // «0 casillas en 0 láminas» over «no queda ninguna casilla marcada» is the same fact twice.
        assertNull(wishSubject(emptyList()).census)
    }
}

private fun slot(catalog: CollectionCatalog, memberId: String): WishedSlot {
    val member = catalog.members.first { it.id == memberId }
    return WishedSlot(Wish(requireNotNull(member.wishKey()), NOW), catalog, member)
}

private fun dateRun(id: String, years: IntRange, typeId: Int): CollectionCatalog = CollectionCatalog(
    schemaVersion = 2,
    id = id,
    name = id,
    shortName = id,
    family = id,
    issuerCode = "australie",
    seriesStatus = SeriesStatus.Open,
    source = "https://en.numista.com/catalogue/pieces1.html",
    updatedAt = "2026-08-14",
    members = years.map { year ->
        CollectionCatalogMember(
            id = "$id-$year",
            label = year.toString(),
            year = year,
            numistaTypeId = typeId,
        )
    },
)
