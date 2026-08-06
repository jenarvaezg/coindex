package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.SeriesStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Las monedas que ninguna colección reclama, que son las que el cuaderno no imprimía (#275).
 *
 * Se mide sobre el mismo `ShelfFixtures` que Monedas, y no por comodidad: la lámina promete ser el
 * complemento de lo que sale en papel, así que tiene que contestar exactamente lo que contesta el
 * chip «Sin colección» de la otra jerarquía (ADR 0021 §1, §12). Dos fixtures con dos ideas de
 * «suelta» serían dos verdades sobre el mismo cajón.
 */
class UnclaimedRowsTest {
    private val loose = unclaimedFacts(ShelfFixtures.state)

    /**
     * Las dos direcciones en que el residuo del dominio y esto se separan.
     *
     * **La caja cuenta**: la onza mexicana no cae en ninguna colección derivada, pero la caja «Las
     * mexicanas» la reclama y su lámina la imprime, así que aquí no está — repetirla sería mentir
     * sobre lo que hay en el cajón. **Y la fila cuenta**: la fila 10 de la Britannia es del mismo
     * tipo que la 9, que sí llena su casilla, y es la forma del American Silver Eagle N#298883 del
     * padre (ADR 0019). El tipo está en una colección y esa moneda suya no.
     */
    @Test
    fun `una caja tapa a su moneda, y una fila suelta asoma bajo un tipo que sí está`() {
        assertEquals(listOf(10L, 12L), loose.map { it.piece.id })

        // Sacada de la caja, la onza no tiene quien la reclame y aparece: lo que cambió no fue la
        // moneda sino quién la imprime.
        val sinCaja = unclaimedFacts(ShelfFixtures.stateWithoutTheBox)
        assertEquals(listOf(5L, 10L, 12L), sinCaja.map { it.piece.id })
    }

    /**
     * Una suelta se mide **como una tarjeta de una pieza sin lámina**, que es lo que deja usar
     * `matches` tal cual en vez de escribir un segundo filtro con reglas propias.
     *
     * Tres facetas salen de su ficha y dos son la respuesta honesta de algo que no tiene lista de
     * emisiones: «Sin lámina» es literalmente verdad, y de serie no dice nada, igual que ya se calla
     * hoy una tarjeta sin catálogo.
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

    /**
     * La suelta cuya ficha aún no ha llegado dice lo que sabe y no inventa lo demás.
     *
     * Y lo que no sabe la deja fuera de los filtros que preguntan por ello, en vez de colarla bajo
     * una etiqueta falsa: `OunceBand.of(null)` es «Conjunto o caja» para una tarjeta que abarca
     * varios pesos, y una moneda suelta no es una caja.
     */
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

    /** El estante recorta la lámina con las mismas respuestas que acaba de dar cada pieza. */
    @Test
    fun `el estante recorta la lámina por país, época y peso`() {
        assertEquals(listOf(10L), narrowed(IndexShelf(issuer = "Reino Unido")))
        assertEquals(emptyList(), narrowed(IndexShelf(issuer = "México")))
        assertEquals(listOf(10L), narrowed(IndexShelf(startsIn = StartBand.SinceTwoThousand)))
        assertEquals(listOf(12L), narrowed(IndexShelf(startsIn = StartBand.Unknown)))
        assertEquals(listOf(10L), narrowed(IndexShelf(weight = OunceBand.HalfToOne)))
    }

    /**
     * Las dos facetas que una moneda suelta no tiene, y que por eso la dejan fuera.
     *
     * «Sin lámina» las deja pasar todas porque es lo que son; cualquier otra pregunta sobre una
     * lámina o sobre una serie no la contesta ninguna, y entonces la lámina no se imprime — que es
     * el gris del interruptor y no una hoja vacía.
     */
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

    /** Lo que se busca es la moneda: su título, su país y el número de Numista que lleva impreso. */
    @Test
    fun `la búsqueda encuentra una suelta por lo que la casilla dice de ella`() {
        assertEquals(listOf(10L), narrowed(query = "britannia"))
        assertEquals(listOf(10L), narrowed(query = "reino unido"))
        assertEquals(listOf(10L), narrowed(query = ShelfFixtures.BRITANNIA.toString()))
        assertEquals(emptyList(), narrowed(query = "kookaburra"))
    }

    /**
     * País, año y título: el orden de cuaderno de campo de Monedas, porque esta lámina es su
     * desbordamiento. Las que aún no tienen ficha van al final — dicen menos que una fechada, y
     * abrir la página con ellas sería abrirla por lo que la última sincronización no terminó.
     */
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

    /** Una fila que ya no tienes no es una moneda suelta: no es una moneda. */
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
