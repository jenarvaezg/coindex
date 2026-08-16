package com.jenarvaezg.coindex.ui.screens

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * La proximidad de la rejilla de Monedas, medida como la de la lámina y no a ojo (#411, #511).
 *
 * La auditoría del 14 de agosto de 2026 leyó «2008 · 2009 · 2010» como el encabezado de la fila de
 * abajo, y el AVD lo confirmó: el año estaba a 8,8 dp de su propio cartucho y a 14,5 dp de la moneda
 * siguiente. Aquí la comparación queda escrita, que es lo único que impide que la próxima costura
 * ajustada vuelva a dejar el año entre dos dueños.
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

    /**
     * La costura de la rejilla es lo que separa, y el pie de la tarjeta no puede sustituirla: subir
     * el pie acerca el año a la moneda de abajo *y* lo aleja del cartucho por igual, que es cambiar
     * el defecto de sitio.
     */
    @Test
    fun `la separacion la pone la costura y no el pie de la tarjeta`() {
        assertTrue(CoinsSpacing.rowSeam > CoinsSpacing.cardFoot)
    }
}
