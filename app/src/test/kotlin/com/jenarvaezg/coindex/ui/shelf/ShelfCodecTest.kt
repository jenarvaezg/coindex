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
        // What an upgrade looks like: a phone that still had «Desde 2000» stored under the old
        // era-band codec reopens with no year filter rather than inventing a year from the name.
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
        // The sort is written as the default rather than as an absence, so «chosen on purpose» and
        // «never chosen» read back the same — which they are. Nothing else is stored.
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

    /**
     * La migración de las nueve etiquetas que retiró el ADR 0023, que es la única que el país
     * necesita.
     *
     * El país es la única faceta que no es un enum, así que la promesa de arriba —un valor que esta
     * versión no reconoce se lee como «sin filtro»— no la heredaba: guardaba la etiqueta misma. Un
     * teléfono con «Federación de Rusia (1991-presente)» puesto habría reabierto filtrando por una
     * cadena que ya no produce ninguna fila —lista vacía, contador de filtros en 1 y ninguna chip
     * encendida—, y `russie` es el emisor de cerca de un tercio de las fichas, así que es la chip que
     * más probable es que se dejara puesta. No hace falta clave de versión: las chips se construyen
     * con lo que dicen las filas, así que esto no se puede volver a escribir.
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
        // Y un país que sí se pinta sigue siendo un filtro, que es lo que esto no puede romper.
        assertEquals(
            "Rusia",
            ShelfCodec.decodeCoins { key -> "Rusia".takeIf { key == ShelfCodec.COINS_ISSUER } }.issuer,
        )
    }
}
