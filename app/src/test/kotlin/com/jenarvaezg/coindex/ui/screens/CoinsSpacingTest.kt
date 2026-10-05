package com.jenarvaezg.coindex.ui.screens

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * La proximidad de la rejilla de Monedas, medida como la de la lámina (#411, #511): un año casi
 * equidistante de su cartucho y de la moneda de abajo se lee como encabezado de esa fila.
 */
class CoinsSpacingTest {
    @Test
    fun `un ano esta al menos al triple de la fila siguiente que de su propio cartucho`() {
        assertTrue(
            CoinsSpacing.betweenCards >= CoinsSpacing.underTheCartouche * 3,
            "${CoinsSpacing.betweenCards} entre tarjetas no es el triple de los " +
                "${CoinsSpacing.underTheCartouche} que separan el cartucho de su año",
        )
    }

    /** Subir el pie acerca el año a la moneda de abajo tanto como lo aleja del cartucho. */
    @Test
    fun `la separacion la pone la costura y no el pie de la tarjeta`() {
        assertTrue(CoinsSpacing.rowSeam > CoinsSpacing.cardFoot)
    }
}
