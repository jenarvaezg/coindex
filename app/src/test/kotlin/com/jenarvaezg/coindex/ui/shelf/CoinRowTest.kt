package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.AssembledCollection
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.collectionFigures
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.CoinName
import com.jenarvaezg.coindex.ui.coinFichaIdentity
import com.jenarvaezg.coindex.ui.matchesQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The other hierarchy: a coin exists whether or not any collection claims it (ADR 0021 §1). */
class CoinRowTest {
    private val rows = coinRows(ShelfFixtures.state)

    /** `minYear` is when the Numista type opens, not the year of the coin held (#448). */
    @Test
    fun `a coin is dated by the piece the collector holds, not by the type`() {
        val state = stateOf(
            meta = TypeMeta(id = 10, title = "¼ Bolívar", minYear = 1_894),
            items = listOf(piece(id = 1, typeId = 10, year = 1_948)),
        )

        val row = coinRows(state).single()

        assertEquals(listOf(1_948), row.years)
        assertEquals("1948", coinAlbumFootnote(row))
    }

    /** A range, not a list: «Piezas» prints each year, and the year chips reach every one. */
    @Test
    fun `a type held in several years prints the arc it covers`() {
        val state = stateOf(
            meta = TypeMeta(id = 10, title = "5 Bolívares", minYear = 1_879),
            items = listOf(
                piece(id = 1, typeId = 10, year = 1_936),
                piece(id = 2, typeId = 10, year = 1_879),
                piece(id = 3, typeId = 10, year = 1_904),
            ),
        )

        val row = coinRows(state).single()

        assertEquals(listOf(1_879, 1_904, 1_936), row.years)
        assertEquals("1879 – 1936 · ×3", coinAlbumFootnote(row))
    }

    /** With no issue on the row, the design's first year has no coin's year to contradict it. */
    @Test
    fun `a coin whose pieces carry no year falls back on the ficha`() {
        val state = stateOf(
            meta = TypeMeta(id = 10, title = "1 Onza", minYear = 1_978),
            items = listOf(piece(id = 1, typeId = 10, year = null)),
        )

        assertEquals(listOf(1_978), coinRows(state).single().years)
    }

    /** The ficha can fail to arrive when the pass spent the month's quota (#448, #452). */
    @Test
    fun `a coin with no ficha still prints the year its piece carries`() {
        val state = stateOf(
            meta = null,
            items = listOf(piece(id = 1, typeId = 18_940, year = 1_980)),
        )

        val row = coinRows(state).single()

        assertEquals(listOf(1_980), row.years)
        assertEquals("1980 · N# 18940", coinFichaIdentity(row))
        assertTrue(matchesQuery(row.haystack, "1980"))
    }

    /**
     * Numista writes `"year": 0, "is_dated": true` for an issue with no date (#460).
     * `placementYear` already ignores it; on the card a «0» would read as a real year.
     */
    @Test
    fun `the zero Numista stores on an undated medal is not a year`() {
        val state = stateOf(
            meta = null,
            items = listOf(piece(id = 1, typeId = 581_856, year = 0)),
        )

        val row = coinRows(state).single()

        assertTrue(row.years.isEmpty())
        assertEquals("Sin año", coinAlbumFootnote(row))
    }

    @Test
    fun `an undated row takes the year its ficha knows`() {
        val state = stateOf(
            meta = TypeMeta(id = 581_856, title = "Medalla", minYear = 1_995),
            items = listOf(piece(id = 1, typeId = 581_856, year = 0)),
        )

        assertEquals(listOf(1_995), coinRows(state).single().years)
    }

    @Test
    fun `a coin with neither ficha nor dated piece says so`() {
        val state = stateOf(meta = null, items = listOf(piece(id = 1, typeId = 500, year = null)))

        val row = coinRows(state).single()

        assertTrue(row.years.isEmpty())
        assertEquals("Sin año", coinAlbumFootnote(row))
    }

    @Test
    fun `a coin held twice is one coin, not two receipts`() {
        val fuerte = rows.single { it.typeId == ShelfFixtures.FUERTE }

        assertEquals(3, fuerte.quantity)
        assertEquals(6, ShelfFixtures.state.items.size)
        assertEquals(4, rows.size)
    }

    @Test
    fun `every coin appears, including the one no collection claims`() {
        assertEquals(
            listOf(
                ShelfFixtures.ONZA_MEXICANA,
                ShelfFixtures.BRITANNIA,
                ShelfFixtures.FUERTE,
                ShelfFixtures.UNCACHED,
            ),
            rows.map { it.typeId },
        )
    }

    @Test
    fun `a coin links back to the collections that claim it`() {
        val medal = rows.single { it.typeId == ShelfFixtures.ONZA_MEXICANA }
        val orphan = rows.single { it.typeId == ShelfFixtures.UNCACHED }

        assertEquals(listOf("Las mexicanas"), medal.claims.map { it.name })
        assertEquals(CardDestination.Box(7), medal.claims.single().destination)
        assertTrue(orphan.claims.isEmpty())
    }

    @Test
    fun `an uncached type says what it can and guesses nothing`() {
        val orphan = rows.single { it.typeId == ShelfFixtures.UNCACHED }

        assertNull(orphan.issuer)
        assertTrue(orphan.years.isEmpty())
        assertNull(orphan.weightOz)
        // Coin is the default: the class chips have no third place for an unknown.
        assertEquals(ObjectClass.Coin, orphan.objectClass)
        assertEquals("Pieza 12", orphan.title)
    }

    @Test
    fun `a medal inside a collection is a medal and stays in its collection`() {
        val medal = rows.single { it.typeId == ShelfFixtures.ONZA_MEXICANA }

        assertEquals(ObjectClass.Exonumia, medal.objectClass)
        assertEquals(listOf("Las mexicanas"), medal.claims.map { it.name })
    }

    /**
     * A box is a second membership, not a move (ADR 0013, ADR 0021 §10). The box was the onza's
     * only claim, so it ends with none.
     */
    @Test
    fun `a coin dropped from a box is still a coin, with one claim fewer`() {
        val after = coinRows(ShelfFixtures.stateWithoutTheBox)
        val before = rows.single { it.typeId == ShelfFixtures.ONZA_MEXICANA }
        val onza = after.single { it.typeId == ShelfFixtures.ONZA_MEXICANA }

        assertEquals(rows.size, after.size)
        assertEquals(before.quantity, onza.quantity)
        assertEquals(before.title, onza.title)
        assertTrue(onza.claims.isEmpty())
    }

    /**
     * The bar reads [collectionFigures].types via [SewnEdgeCounts] (ADR 0021 §1, #424), so both
     * must count the same census (#426, #427).
     */
    @Test
    fun `the count the bottom bar prints is the number of rows Coins draws`() {
        val figures = collectionFigures(ShelfFixtures.state.items, ShelfFixtures.state.typeMeta)

        assertEquals(figures.types, rows.size)
    }

    /** Figures coerce a zero quantity to one piece, so [coinRows] must draw it too (#426). */
    @Test
    fun `a coerced zero still draws a row, matching the figures type count`() {
        val state = CollectionState(
            AssembledCollection(
                items = listOf(
                    CollectedItem(id = 1, quantity = 3, typeId = 100),
                    CollectedItem(id = 2, quantity = 0, typeId = 200),
                ),
                typeMeta = emptyMap(),
            ),
        )
        val figures = collectionFigures(state.items, state.typeMeta)
        val drawn = coinRows(state)

        assertEquals(figures.types, drawn.size)
        assertEquals(1, drawn.single { it.typeId == 200 }.quantity)
    }

    @Test
    fun `the search box reaches the name, the country and the Numista number`() {
        val fuerte = rows.single { it.typeId == ShelfFixtures.FUERTE }

        assertTrue(matchesQuery(fuerte.haystack, "bolivar"))
        assertTrue(matchesQuery(fuerte.haystack, "venezuela"))
        assertTrue(matchesQuery(fuerte.haystack, "100"))
    }

    /**
     * The shelf's country chips are built from these strings, and Numista's issuer name «Federación
     * de Rusia (1991-presente)» took a whole row of chips (#180).
     */
    @Test
    fun `Coins says the country its card says`() {
        val rusas = CollectionState(
            AssembledCollection(
                items = listOf(
                    CollectedItem(id = 1, quantity = 1, typeId = 500),
                    CollectedItem(id = 2, quantity = 1, typeId = 501),
                ),
                typeMeta = mapOf(
                    500 to TypeMeta(
                        id = 500,
                        displayTitle = "3 rublos del Libro Rojo",
                        issuerCode = "russie",
                        issuerName = "Federación de Rusia (1991-presente)",
                    ),
                    501 to TypeMeta(
                        id = 501,
                        displayTitle = "1 rublo soviético",
                        issuerCode = "ancienne_urss",
                        issuerName = "Unión Soviética",
                    ),
                ),
            ),
        )

        assertEquals(
            listOf("Rusia", "Unión Soviética"),
            coinRows(rusas).map { it.issuer },
        )
        // The search still reaches the country the row prints.
        assertTrue(matchesQuery(coinRows(rusas).first().haystack, "rusia"))
    }

    @Test
    fun `a coin keeps its full title searchable behind its structured album name`() {
        val state = CollectionState(
            AssembledCollection(
                items = listOf(CollectedItem(id = 1, quantity = 1, typeId = 500)),
                typeMeta = mapOf(
                    500 to TypeMeta(
                        id = 500,
                        title = "1 Dollar - Elizabeth II (Red Dragon of Wales; 2 oz Fine Silver)",
                    ),
                ),
            ),
        )

        val row = coinRows(state).single()

        assertEquals(CoinName("1 Dollar", "Red Dragon of Wales"), row.name)
        assertEquals(
            "1 Dollar - Elizabeth II (Red Dragon of Wales; 2 oz Fine Silver)",
            row.rawTitle,
        )
        assertTrue(matchesQuery(row.haystack, "Elizabeth"))
        assertTrue(matchesQuery(row.haystack, "Fine Silver"))
        assertEquals("Sin año · N# 500", coinFichaIdentity(row))
    }

    @Test
    fun `the album grid keeps only year and necessary quantity under the cartouche`() {
        val britannia = rows.single { it.typeId == ShelfFixtures.ONZA_MEXICANA }
        val fuerte = rows.single { it.typeId == ShelfFixtures.FUERTE }

        assertEquals(britannia.oldestYear.toString(), coinAlbumFootnote(britannia))
        assertEquals("${fuerte.oldestYear} · ×3", coinAlbumFootnote(fuerte))
        assertTrue(ShelfFixtures.ONZA_MEXICANA.toString() !in coinAlbumFootnote(britannia))
    }

    /** The sheet a casilla opens reuses the grid's row, so a coin can't read differently (#508). */
    @Test
    fun `the row of a coin the collector holds is the row the grid draws`() {
        assertEquals(
            rows.single { it.typeId == ShelfFixtures.FUERTE },
            coinRowOf(ShelfFixtures.state, ShelfFixtures.FUERTE),
        )
    }

    /** Every hole on a lámina opens this sheet, so the row must work with no piece behind it. */
    @Test
    fun `a type no piece of which is held reads as a coin with nothing in it`() {
        val state = stateOf(
            meta = TypeMeta(
                id = 10,
                title = "1 Bolívar",
                issuerName = "Venezuela",
                minYear = 1_886,
                category = "coin",
            ),
            items = emptyList(),
        )

        val row = coinRowOf(state, 10)

        assertEquals("1 Bolívar", row.rawTitle)
        assertEquals(0, row.quantity)
        assertEquals(emptyList(), row.claims)
        assertEquals("Venezuela · 1886 · N# 10", coinFichaIdentity(row))
    }

    /** A type's opening year is a coin's year only when the type has one year (#448). */
    @Test
    fun `a type held by nobody is dated by the arc the type covers`() {
        val state = stateOf(
            meta = TypeMeta(id = 10, title = "5 Bolívares", minYear = 1_879, maxYear = 1_936),
            items = emptyList(),
        )

        assertEquals(listOf(1_879, 1_936), coinRowOf(state, 10).years)
    }

    @Test
    fun `a type with no ficha at all is named by its Numista number`() {
        val row = coinRowOf(stateOf(meta = null, items = emptyList()), 596_807)

        assertEquals("N# 596807", row.rawTitle)
        assertEquals("Sin año · N# 596807", coinFichaIdentity(row))
    }

    private fun piece(id: Long, typeId: Int, year: Int?) =
        CollectedItem(id = id, quantity = 1, typeId = typeId, issueYear = year)

    private fun stateOf(meta: TypeMeta?, items: List<CollectedItem>) = CollectionState(
        AssembledCollection(
            items = items,
            typeMeta = meta?.let { mapOf(it.id to it) }.orEmpty(),
        ),
    )
}
