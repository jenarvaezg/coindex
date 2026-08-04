package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.domain.CoverageRatio
import com.jenarvaezg.coindex.domain.Finish
import com.jenarvaezg.coindex.domain.Metal
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The lines a card writes about itself.
 *
 * Both cases here were UX findings rather than bugs: «Por confirmar» on its own line read as a
 * finish called that, and the metal, which the key needs in all four positions, is worth a word on
 * screen only where it is not the silver almost every card is made of.
 */
class LabelsTest {
    @Test
    fun `an unconfirmed finish names what is unconfirmed when nothing else does`() {
        // Under a row already labelled ACABADO the word would be said twice.
        assertEquals("Sin confirmar", finishLabel(null))
        assertEquals("Acabado sin confirmar", standaloneFinishLabel(null))

        // A known finish is the same word either way.
        assertEquals("Bullion", finishLabel(Finish.Bullion))
        assertEquals("Bullion", standaloneFinishLabel(Finish.Bullion))
        assertEquals("0,804 oz · Acabado sin confirmar", variantLabel(804, null, Metal.Silver))
    }

    /**
     * El metal solo se nombra cuando no es plata (#40): decirlo en las 73 tarjetas de plata que
     * miden las dos colecciones alargaría cada línea para no distinguir nada, y la onza de oro
     * —la que obligó a meterlo en la clave— es justo la que necesita la palabra.
     */
    @Test
    fun `the metal is named only when it is not silver`() {
        assertEquals("1 oz · Bullion", variantLabel(1_000, Finish.Bullion, Metal.Silver))
        // Una ficha sin composición legible tampoco escribe nada: no se sabe, no se afirma.
        assertEquals("1 oz · Bullion", variantLabel(1_000, Finish.Bullion, null))
        assertEquals("1 oz · Bullion · Oro", variantLabel(1_000, Finish.Bullion, Metal.Gold))
        assertEquals(
            "0,25 oz · Acabado sin confirmar · Cuproníquel",
            variantLabel(250, null, Metal.Cupronickel),
        )
        // Un conjunto no tiene variante física que describir, y el metal no cambia eso.
        assertEquals(
            "Conjunto de varias denominaciones",
            variantLabel(null, null, Metal.Gold),
        )
    }

    /**
     * La tercera línea de una tarjeta con lista de emisiones es el mismo ratio por el que está
     * ordenado el índice (ADR 0021 §3 y §6). Que se lea es lo que hace legible el orden: sin ella
     * el índice se mueve con el ratio mientras cada tarjeta cuenta piezas.
     */
    @Test
    fun `a card with an issue list says its progress, and claims no closure when nothing is missing`() {
        assertEquals("4 de 12 · te faltan 8", coverageLabel(CoverageRatio(4, 12)))
        assertEquals("0 de 52 · te faltan 52", coverageLabel(CoverageRatio(0, 52)))
        // Ni «completa» ni «22/22 ✓»: por el ADR 0020 una serie abierta no tiene completitud que
        // afirmar, y este enero las mismas 22 casillas pueden ser 23.
        assertEquals("22 de 22", coverageLabel(CoverageRatio(22, 22)))
    }
}
