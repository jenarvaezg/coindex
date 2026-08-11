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

    /**
     * The card is dated by **the coin the collector has**, not by the first year of the design (#448).
     *
     * The father said it of his 75 bolívares and it turned out to be the smaller half: measured over
     * his collection, 63 rows of 34 types printed a year that is not their coin's, because `minYear`
     * is when Numista's type opens. His ¼ bolívar of 1948 said 1894; his Libertad of 2024 said 2000.
     */
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

    /**
     * A type the collector holds in several years prints the arc, not one of its ends.
     *
     * Seven of his 170 dated types are like this, and one of them is the whole reason it is a range
     * and not a list: his 5 bolívares N#10340 is twenty-one years, 1879 to 1936. The individual years
     * are still one screen away — «Piezas» prints them one by one — and still reachable, because the
     * year chips below take every one of them.
     */
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

    /**
     * With no year on any piece the ficha answers, which is what it is good for.
     *
     * Twenty-one of his types are in this state — a row Numista holds no issue for — and the type's
     * first year is a truthful thing to say about them: it is the year of the design, and there is no
     * coin's own year to contradict it.
     */
    @Test
    fun `a coin whose pieces carry no year falls back on the ficha`() {
        val state = stateOf(
            meta = TypeMeta(id = 10, title = "1 Onza", minYear = 1_978),
            items = listOf(piece(id = 1, typeId = 10, year = null)),
        )

        assertEquals(listOf(1_978), coinRows(state).single().years)
    }

    /**
     * And with neither, «Sin año» — the same hole, said out loud.
     *
     * This is the father's 75 bolívares as his phone has it (#448): the ficha never arrived, because
     * the valuation pass had spent his month (#452). The row's own piece carries 1980 and the card
     * now says so, ficha or no ficha.
     */
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
        // Money is the default with two chips and no third place to put it.
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
     * Dropping a type from a box does not touch the coin (ADR 0013, ADR 0021 §10).
     *
     * It stays in the inventory and in Coins with the same quantity, and what changes is only which
     * collections claim it — which is what makes a box a second membership rather than a move. Here
     * the box was the onza's only claim, so it lands under «Sin colección» and nowhere else.
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
     * The bottom bar promises a number and this screen has to be able to keep it (ADR 0021 §1).
     *
     * After #424 the bar reads [collectionFigures].types (via [SewnEdgeCounts]), not a parallel
     * type walk — so the fixture check is the same census [coinRows] draws (#426, #427).
     */
    @Test
    fun `the count the bottom bar prints is the number of rows Coins draws`() {
        val figures = collectionFigures(ShelfFixtures.state.items, ShelfFixtures.state.typeMeta)

        assertEquals(figures.types, rows.size)
    }

    /**
     * Sewn edge and Coins share one census (#426): figures coerce a hostile zero to one piece, and
     * [coinRows] must draw that type too — otherwise the bar says «Monedas · N+1» while the screen
     * paints N rows.
     */
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
     * Monedas rotula el mismo país que la tarjeta, y no la entidad emisora de Numista (#180).
     *
     * Aquí no es sólo el rótulo de una fila: la chip de país de la estantería se construye con estas
     * cadenas, y «Federación de Rusia (1991-presente)» se llevaba una fila de chips para ella sola.
     * Los 293 tipos `russie` de la caché sembrada son el emisor más numeroso que hay, así que es la
     * chip que más se toca.
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
        // Y la búsqueda sigue alcanzando el país que la fila pinta.
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

    private fun piece(id: Long, typeId: Int, year: Int?) =
        CollectedItem(id = id, quantity = 1, typeId = typeId, issueYear = year)

    private fun stateOf(meta: TypeMeta?, items: List<CollectedItem>) = CollectionState(
        AssembledCollection(
            items = items,
            typeMeta = meta?.let { mapOf(it.id to it) }.orEmpty(),
        ),
    )
}
