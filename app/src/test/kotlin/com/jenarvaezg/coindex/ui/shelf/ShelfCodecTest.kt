package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.domain.SeriesStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What survives an `am force-stop` (ADR 0021 §1). The search text has no key here, so it cannot be
 * persisted by accident.
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
            axis = NotebookAxis.ByCountry,
            issuer = "Rusia",
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
            axis = NotebookAxis.ByYear,
            issuer = "México",
            weight = GramBand.Ounce,
            year = YearFilter.Of(2020),
            objectClass = ObjectClass.Exonumia,
            membership = Membership.InNone,
        )

        assertEquals(shelf, roundTrip(shelf))
    }

    @Test
    fun `Sin ano persists as Undated and an old era name is no filter`() {
        assertEquals(
            YearFilter.Undated,
            ShelfCodec.decodeCoins { key -> "Undated".takeIf { key == ShelfCodec.COINS_YEAR } }.year,
        )
        // An upgrade: «Desde 2000» stored by the old era-band codec reopens with no year filter.
        assertEquals(
            CoinsShelf(),
            ShelfCodec.decodeCoins { key ->
                "SinceTwoThousand".takeIf { key == ShelfCodec.COINS_YEAR }
            },
        )
    }

    @Test
    fun `an empty shelf stores nothing but the sort, and reads back empty`() {
        assertEquals(IndexShelf(), roundTrip(IndexShelf()))
        assertEquals(CoinsShelf(), roundTrip(CoinsShelf()))
        // The default sort is written rather than left absent; nothing else is stored.
        assertEquals(
            listOf(ShelfCodec.INDEX_SORT, ShelfCodec.INDEX_AXIS),
            ShelfCodec.encode(IndexShelf()).filterValues { it != null }.keys.toList(),
        )
        assertEquals(
            listOf(ShelfCodec.COINS_SORT, ShelfCodec.COINS_AXIS),
            ShelfCodec.encode(CoinsShelf()).filterValues { it != null }.keys.toList(),
        )
    }

    @Test
    fun `nothing stored at all is the default shelf, not a crash`() {
        assertEquals(IndexShelf(), ShelfCodec.decodeIndex { null })
        assertEquals(IndexSort.MostComplete, ShelfCodec.decodeIndex { null }.sort)
        assertEquals(CoinsShelf(), ShelfCodec.decodeCoins { null })
    }

    @Test
    fun `a value this version has never heard of is no filter at all`() {
        // A downgrade: a chip added later, read by an APK that predates it.
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

    /**
     * The labels ADR 0023 retired. The country is the only facet that is not an enum, so a stale
     * label would reopen filtering by a string no row produces: an empty list with one filter on.
     */
    @Test
    fun `a country this version no longer paints is no filter at all`() {
        val retired = listOf(
            "Federación de Rusia (1991-presente)",
            "China, República Popular",
            "Alemania, República Federal de",
            "Haití (1804-presente)",
            "Romano, Imperio (27 a. C. - 395 d. C.)",
        )

        for (label in retired) {
            assertEquals(
                CoinsShelf(),
                ShelfCodec.decodeCoins { key -> label.takeIf { key == ShelfCodec.COINS_ISSUER } },
            )
            assertEquals(
                IndexShelf(),
                ShelfCodec.decodeIndex { key -> label.takeIf { key == ShelfCodec.INDEX_ISSUER } },
            )
        }
        // A country still painted stays a filter.
        assertEquals(
            "Rusia",
            ShelfCodec.decodeCoins { key -> "Rusia".takeIf { key == ShelfCodec.COINS_ISSUER } }.issuer,
        )
    }
}
