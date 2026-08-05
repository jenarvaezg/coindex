package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.domain.SeriesStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What survives an `am force-stop` and what deliberately does not (ADR 0021 §1).
 *
 * The search text has no key here at all, which is the whole point: it cannot be persisted by
 * accident, because there is nowhere to put it.
 */
class ShelfCodecTest {
    private fun roundTrip(shelf: IndexShelf): IndexShelf {
        val stored = ShelfCodec.encode(shelf)
        return ShelfCodec.decodeIndex { key -> stored[key] }
    }

    private fun roundTrip(shelf: CoinsShelf): CoinsShelf {
        val stored = ShelfCodec.encode(shelf)
        return ShelfCodec.decodeCoins { key -> stored[key] }
    }

    @Test
    fun `a whole shelf of the index comes back as it went in`() {
        val shelf = IndexShelf(
            sort = IndexSort.RecentlyAdded,
            issuer = "Federación de Rusia (1991-presente)",
            weight = OunceBand.Spanning,
            startsIn = StartBand.BeforeFifty,
            status = PlateStatus.NoPlate,
            series = SeriesStatus.Closed,
        )

        assertEquals(shelf, roundTrip(shelf))
    }

    @Test
    fun `a whole shelf of Coins comes back as it went in`() {
        val shelf = CoinsShelf(
            issuer = "México",
            weight = GramBand.Ounce,
            year = YearBand.SinceTwoThousand,
            objectClass = ObjectClass.Exonumia,
            membership = Membership.InNone,
        )

        assertEquals(shelf, roundTrip(shelf))
    }

    @Test
    fun `an empty shelf stores nothing but the sort, and reads back empty`() {
        assertEquals(IndexShelf(), roundTrip(IndexShelf()))
        assertEquals(CoinsShelf(), roundTrip(CoinsShelf()))
        assertEquals(
            listOf(ShelfCodec.INDEX_SORT),
            ShelfCodec.encode(IndexShelf()).filterValues { it != null }.keys.toList(),
        )
        assertEquals(emptyMap(), ShelfCodec.encode(CoinsShelf()).filterValues { it != null })
    }

    @Test
    fun `nothing stored at all is the default shelf, not a crash`() {
        assertEquals(IndexShelf(), ShelfCodec.decodeIndex { null })
        assertEquals(IndexSort.MostComplete, ShelfCodec.decodeIndex { null }.sort)
        assertEquals(CoinsShelf(), ShelfCodec.decodeCoins { null })
    }

    @Test
    fun `a value this version has never heard of is no filter at all`() {
        // What a downgrade looks like: a chip added later, read by an APK that predates it.
        val stored = mapOf(
            ShelfCodec.INDEX_SORT to "MasBonitas",
            ShelfCodec.INDEX_WEIGHT to "DosOnzasJustas",
            ShelfCodec.INDEX_STATUS to "",
        )

        val shelf = ShelfCodec.decodeIndex { key -> stored[key] }

        assertEquals(IndexShelf(), shelf)
    }

    @Test
    fun `a blank country is not a country`() {
        assertEquals(
            CoinsShelf(),
            ShelfCodec.decodeCoins { key -> if (key == ShelfCodec.COINS_ISSUER) "  " else null },
        )
    }

    @Test
    fun `every key the two shelves own is listed, so a store can clear them`() {
        assertEquals(
            (ShelfCodec.encode(IndexShelf()).keys + ShelfCodec.encode(CoinsShelf()).keys).toSet(),
            ShelfCodec.keys.toSet(),
        )
    }
}
