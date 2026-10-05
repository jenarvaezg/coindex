package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.AssembledCollection
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CoverageRatio
import com.jenarvaezg.coindex.domain.DerivedCollection
import com.jenarvaezg.coindex.domain.Finish
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.OwnGrouping
import com.jenarvaezg.coindex.domain.OwnGroupingView
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.VariantKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What the merged screen is handed, whichever card sent the collector there (ADR 0021 §9): one
 * shape, with the box's upkeep as a field.
 */
class PiecesSubjectTest {
    private val francesas = DerivedCollection(
        family = "Monnaie de Paris",
        weightMillioz = 1_000,
        finish = Finish.Bullion,
        metal = Metal.Silver,
        distinctTypes = 2,
        quantity = 3,
    )

    /** A state holding only the pieces of one key, which is all this screen ever reads. */
    private fun state(vararg pieces: Pair<VariantKey, List<CollectedItem>>) =
        CollectionState(AssembledCollection(itemsByKey = pieces.toMap()))

    private fun piece(id: Long, typeId: Int, year: Int?, title: String? = null) = CollectedItem(
        id = id,
        quantity = 1,
        typeId = typeId,
        title = title,
        issueYear = year,
    )

    private fun derivedCard(
        plateCatalogId: String? = null,
        coverage: CoverageRatio? = null,
    ) = IndexCard.Derived(
        name = "Las francesas",
        coverage = coverage,
        issuer = "Francia",
        collection = francesas,
        plateCatalogId = plateCatalogId,
    )

    private fun boxCard(items: List<CollectedItem>) = IndexCard.Box(
        name = "Tribute to the Spanish Army",
        issuer = "España",
        box = OwnGroupingView(OwnGrouping(id = 7, name = "Tribute to the Spanish Army", typeIds = items.map { it.typeId }), items),
    )

    @Test
    fun `a card without an issue list brings its pieces, its country and its variant`() {
        val pieces = listOf(piece(2, 100, 1996), piece(1, 101, 1994))
        val state = state(francesas.key() to pieces)

        val subject = piecesSubject(state, derivedCard())

        assertEquals("Las francesas", subject.title)
        assertEquals("Francia", subject.issuer)
        assertEquals("1 oz · Bullion", subject.variant)
        assertEquals(2, subject.distinctTypes)
        assertEquals(3, subject.quantity)
        assertEquals(listOf(1L, 2L), subject.pieces.map { it.item.id })
    }

    /** A box spans whatever the collector put in it, so it has no physical variant. */
    @Test
    fun `a box brings its own pieces and states no variant`() {
        val pieces = listOf(piece(9, 300, 2011), piece(8, 301, 2010))

        val subject = piecesSubject(CollectionState(), boxCard(pieces))

        assertEquals("Tribute to the Spanish Army", subject.title)
        assertEquals("España", subject.issuer)
        assertNull(subject.variant)
        assertEquals(2, subject.distinctTypes)
        assertEquals(2, subject.quantity)
        assertEquals(listOf(8L, 9L), subject.pieces.map { it.item.id })
    }

    /**
     * Only a catalog with no owned issued member reaches this screen with a ratio; with evidence
     * the card opens its plate (ADR 0021 §7, §9). The ratio must survive the tap.
     */
    @Test
    fun `a card that counts a ratio keeps counting it inside`() {
        val state = state(francesas.key() to listOf(piece(1, 100, 1996)))

        val subject = piecesSubject(state, derivedCard(coverage = CoverageRatio(0, 12)))

        assertEquals(CoverageRatio(0, 12), subject.coverage)
    }

    /** A box can never contain a gap, so it has no ratio to carry (ADR 0021 §2). */
    @Test
    fun `a box has no ratio`() {
        assertNull(piecesSubject(CollectionState(), boxCard(emptyList())).coverage)
    }

    /** Upkeep is what tells a box apart, as an `if` on this field (ADR 0021 §9). */
    @Test
    fun `only a box carries an id to maintain`() {
        assertEquals(7L, piecesSubject(CollectionState(), boxCard(emptyList())).boxId)
        assertNull(piecesSubject(CollectionState(), derivedCard()).boxId)
    }

    /** An undated row goes last: it is the least identified thing on the screen. */
    @Test
    fun `pieces are ordered by year, then title, then row`() {
        val pieces = listOf(
            piece(4, 100, null, title = "Sin fecha"),
            piece(3, 101, 1996, title = "B"),
            piece(2, 102, 1996, title = "A"),
            piece(1, 103, 1994),
        )
        val state = state(francesas.key() to pieces)

        val subject = piecesSubject(state, derivedCard())

        assertEquals(listOf(1L, 2L, 3L, 4L), subject.pieces.map { it.item.id })
    }

    /** A derived collection can vanish while open: a piece sold on Numista and synced away. */
    @Test
    fun `a route whose card is gone resolves to nothing rather than to an empty screen`() {
        val state = CollectionState(AssembledCollection(index = listOf(derivedCard())))

        assertNull(state.piecesCardFor(francesas.key().copy(weightMillioz = 500)))
        assertNull(state.piecesCardForBox(99))
        assertTrue(state.piecesCardFor(francesas.key()) is IndexCard.Derived)
    }

    /** The emission is a field of the piece, so both cards show it (#225). */
    @Test
    fun `a piece brings the emission that names it`() {
        val star = piece(1, 1_885, 1966)
        val plain = piece(2, 1_885, 1966)
        val assembled = AssembledCollection(
            itemsByKey = mapOf(francesas.key() to listOf(star, plain)),
            emissionLabels = mapOf(1L to "Estrella 67"),
        )
        val state = CollectionState(assembled)

        val fromCollection = piecesSubject(state, derivedCard()).pieces
        val fromBox = piecesSubject(state, boxCard(listOf(star))).pieces

        assertEquals(listOf("Estrella 67", null), fromCollection.map { it.emissionLabel })
        assertEquals("Estrella 67 · Numista 1885", pieceLine(fromCollection.first()))
        assertEquals("1966 · Numista 1885", pieceLine(fromCollection.last()))
        assertEquals(listOf("Estrella 67"), fromBox.map { it.emissionLabel })
    }

    @Test
    fun `a piece gets one structured name from its ficha with row and id fallbacks`() {
        val cached = piece(1, 100, 1996, "Title from the inventory row")
        val rowOnly = piece(2, 101, 1997, "8 Reales - Charles IV")
        val unnamed = piece(3, 102, null)
        val state = CollectionState(
            AssembledCollection(
                typeMeta = mapOf(
                    100 to TypeMeta(
                        id = 100,
                        title = "1 Dollar - Elizabeth II (2nd portrait, Confederation)",
                    ),
                ),
            ),
        )

        assertEquals(CoinName("1 Dollar", "Confederation"), pieceName(state, cached))
        assertEquals(CoinName("8 Reales", "Charles IV"), pieceName(state, rowOnly))
        assertEquals(CoinName("Pieza 3", null), pieceName(state, unnamed))
    }
}
