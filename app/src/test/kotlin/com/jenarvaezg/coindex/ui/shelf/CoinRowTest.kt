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
        assertNull(orphan.year)
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

        assertEquals(britannia.year.toString(), coinAlbumFootnote(britannia))
        assertEquals("${fuerte.year} · ×3", coinAlbumFootnote(fuerte))
        assertTrue(ShelfFixtures.ONZA_MEXICANA.toString() !in coinAlbumFootnote(britannia))
    }
}
