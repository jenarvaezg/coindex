package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.SeriesStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Las monedas que ninguna colección reclama, que el cuaderno no imprimía (#275). Usa el mismo
 * `ShelfFixtures` que Monedas porque debe contestar lo mismo que su chip «Sin colección»
 * (ADR 0021 §1, §12).
 */
class UnclaimedRowsTest {
    private val loose = unclaimedFacts(ShelfFixtures.state)

    /**
     * La caja «Las mexicanas» reclama la onza, así que no sale aquí. La fila 10 de la Britannia es
     * del tipo de la 9, que sí llena su casilla: el tipo está en una colección y esa fila no
     * (ADR 0019).
     */
    @Test
    fun `una caja tapa a su moneda, y una fila suelta asoma bajo un tipo que sí está`() {
        assertEquals(listOf(10L, 12L), loose.map { it.piece.id })

        // Sin la caja, nadie reclama la onza y aparece.
        val sinCaja = unclaimedFacts(ShelfFixtures.stateWithoutTheBox)
        assertEquals(listOf(5L, 10L, 12L), sinCaja.map { it.piece.id })
    }

    /**
     * Se trata como una tarjeta de una pieza sin lámina para reutilizar `matches`. La serie queda
     * vacía, como en una tarjeta sin catálogo.
     */
    @Test
    fun `una suelta contesta las cinco facetas como la tarjeta que no es`() {
        val britannia = loose.single { it.piece.id == 10L }

        assertEquals("Reino Unido", britannia.issuer)
        assertEquals(StartBand.SinceTwoThousand, britannia.startsIn)
        assertEquals(OunceBand.HalfToOne, britannia.weight)
        assertEquals(PlateStatus.NoPlate, britannia.status)
        assertNull(britannia.series)
    }

    /** En una tarjeta `OunceBand.of(null)` es «Varias onzas»; en una suelta sería falso. */
    @Test
    fun `la suelta sin ficha no tiene peso y por eso no entra en ningún filtro de peso`() {
        val sinFicha = loose.single { it.piece.id == 12L }

        assertNull(sinFicha.issuer)
        assertNull(sinFicha.weight)
        assertEquals(StartBand.Unknown, sinFicha.startsIn)

        for (banda in OunceBand.entries) {
            assertTrue(
                sinFicha.piece.id !in narrowed(IndexShelf(weight = banda)),
                "una moneda sin peso ha pasado por el filtro ${banda.label}",
            )
        }
        assertTrue(sinFicha.piece.id in narrowed(IndexShelf()))
    }

    @Test
    fun `el estante recorta la lámina por país, época y peso`() {
        assertEquals(listOf(10L), narrowed(IndexShelf(issuer = "Reino Unido")))
        assertEquals(emptyList(), narrowed(IndexShelf(issuer = "México")))
        assertEquals(listOf(10L), narrowed(IndexShelf(startsIn = StartBand.SinceTwoThousand)))
        assertEquals(listOf(12L), narrowed(IndexShelf(startsIn = StartBand.Unknown)))
        assertEquals(listOf(10L), narrowed(IndexShelf(weight = OunceBand.HalfToOne)))
    }

    /** Si no pasa ninguna, la lámina no se imprime: el interruptor queda gris, sin hoja vacía. */
    @Test
    fun `filtrar por lámina hecha o por serie no deja ninguna suelta`() {
        assertEquals(listOf(10L, 12L), narrowed(IndexShelf(status = PlateStatus.NoPlate)))
        assertEquals(emptyList(), narrowed(IndexShelf(status = PlateStatus.Complete)))
        assertEquals(emptyList(), narrowed(IndexShelf(status = PlateStatus.PartlyDone)))
        for (serie in SeriesStatus.entries) {
            assertEquals(
                emptyList(),
                narrowed(IndexShelf(series = serie)),
                "una moneda sin catálogo ha pasado por el filtro de serie $serie",
            )
        }
    }

    /** Título, país y el número de Numista impreso. */
    @Test
    fun `la búsqueda encuentra una suelta por lo que la casilla dice de ella`() {
        assertEquals(listOf(10L), narrowed(query = "britannia"))
        assertEquals(listOf(10L), narrowed(query = "reino unido"))
        assertEquals(listOf(10L), narrowed(query = ShelfFixtures.BRITANNIA.toString()))
        assertEquals(emptyList(), narrowed(query = "kookaburra"))
    }

    /** País, año y título: el orden de Monedas, porque esta lámina es su desbordamiento. */
    @Test
    fun `se leen por país y por año, y las que no tienen ficha van al final`() {
        val muchas = ShelfFixtures.stateWithoutTheBox.let { sinCaja ->
            sinCaja.copy(
                collection = sinCaja.collection.copy(
                    items = sinCaja.items + CollectedItem(id = 20, quantity = 1, typeId = 100),
                ),
            )
        }

        // La 20 es un fuerte venezolano de 1929 suelto: Venezuela va detrás de México y de Reino
        // Unido, y la pieza sin ficha detrás de las tres.
        assertEquals(
            listOf("México", "Reino Unido", "Venezuela", null),
            unclaimedFacts(muchas).map { it.issuer },
        )
        assertEquals(listOf(5L, 10L, 20L, 12L), unclaimedFacts(muchas).map { it.piece.id })
    }

    /** Cantidad cero es una fila que ya no está en la colección. */
    @Test
    fun `una fila vendida no aparece`() {
        val vendida = ShelfFixtures.state.let { state ->
            state.copy(
                collection = state.collection.copy(
                    items = state.items.map { if (it.id == 12L) it.copy(quantity = 0) else it },
                ),
            )
        }

        assertEquals(listOf(10L), unclaimedFacts(vendida).map { it.piece.id })
    }

    private fun narrowed(shelf: IndexShelf = IndexShelf(), query: String = ""): List<Long> =
        shelf.narrowUnclaimed(loose, query).map { it.id }
}
