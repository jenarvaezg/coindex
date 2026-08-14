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
 * What the annex draws, worded once for the screen and the exported list (ADR 0029 §6).
 *
 * The row is the plate's casilla plus **which plate it came from**, which is the one thing a list that
 * crosses plates needs and a casilla on its own plate never does.
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
        // The amount alone inside the hole, as on a plate: the criterion was said in the header.
        assertEquals(listOf("42 €", null), subject.rows.map { it.cost })
        assertEquals("2 casillas en 1 lámina", subject.census)
    }

    /**
     * A casilla labelled with its own year prints it once, which is the plate's rule (#473).
     *
     * Measured on the emulator the first time this screen was drawn: a date run labels every casilla
     * with its year, so «1886» came out on the tag sunk into the cardboard and «1886» again underneath.
     */
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
     * A row is keyed by the plate it is drawn under, because a lazy grid needs a stable key and two
     * catalogs can name the same coin (ADR 0021 §10).
     *
     * The two slots are assembled by hand here on purpose: `wishedSlots` hands the coin to the first
     * catalog that claims it, so this is the shape a **later** delivery would produce — and the key
     * has to survive it rather than collapse two rows into one.
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
     * With no price on the phone no row invents one, and an empty list says no census.
     *
     * Absence and not zero, which is the rule every amount in the app follows (ADR 0028 §7): a phone
     * whose pass has not landed shows the same silence a plate over the threshold does. And what the
     * marks cost a month is **not here at all** — ADR 0029 §5 names the gesture and Ajustes, and a
     * third printing over a list that is being used rather than budgeted is what ADR 0026 §5 prices.
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
