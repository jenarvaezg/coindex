package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.domain.CoverageRatio
import com.jenarvaezg.coindex.domain.Finish
import com.jenarvaezg.coindex.domain.Metal
import kotlin.test.Test
import kotlin.test.assertEquals

/** The lines a card writes about itself. */
class LabelsTest {
    /** Un acabado sin declarar es un hueco de la curación, no un hecho de la moneda (#409). */
    @Test
    fun `an undeclared finish says nothing at all`() {
        assertEquals("0,804 oz", variantLabel(804, null, Metal.Silver))
        assertEquals(listOf("Variante" to "0,804 oz"), variantEntries(804, null))
    }

    @Test
    fun `a declared finish is the same word on the card and in the specification`() {
        assertEquals("Bullion", finishLabel(Finish.Bullion))
        assertEquals("1 oz · Proof coloreado", variantLabel(1_000, Finish.ProofColoured, Metal.Silver))
        assertEquals(
            listOf("Variante" to "1 oz", "Acabado" to "Envejecido"),
            variantEntries(1_000, Finish.Antiqued),
        )
    }

    /** Casi todas las tarjetas son de plata: nombrarla alargaría la línea sin distinguir (#40). */
    @Test
    fun `the metal is named only when it is not silver`() {
        assertEquals("1 oz · Bullion", variantLabel(1_000, Finish.Bullion, Metal.Silver))
        // Una ficha sin composición legible no afirma ningún metal.
        assertEquals("1 oz · Bullion", variantLabel(1_000, Finish.Bullion, null))
        assertEquals("1 oz · Bullion · Oro", variantLabel(1_000, Finish.Bullion, Metal.Gold))
        // Sin acabado no queda un separador suelto entre el peso y el metal (#409).
        assertEquals("0,25 oz · Cuproníquel", variantLabel(250, null, Metal.Cupronickel))
        // Un conjunto no tiene variante física, tenga el metal que tenga.
        assertEquals(
            "Conjunto de varias denominaciones",
            variantLabel(null, null, Metal.Gold),
        )
    }

    /** Es el ratio por el que se ordena el índice (ADR 0021 §3 y §6): así el orden se entiende. */
    @Test
    fun `a card with an issue list says its progress, and claims no closure when nothing is missing`() {
        assertEquals("4 de 12 · te faltan 8", coverageLabel(CoverageRatio(4, 12)))
        assertEquals("0 de 52 · te faltan 52", coverageLabel(CoverageRatio(0, 52)))
        // Ni «completa» ni marca: una serie abierta no tiene completitud que afirmar (ADR 0020).
        assertEquals("22 de 22", coverageLabel(CoverageRatio(22, 22)))
    }

    /**
     * La frase que fija el ADR 0021 §3 y `CONTEXT.md`: las monedas primero, porque es lo que hay en
     * casa; los tipos distinguen «cinco monedas distintas» de «la misma cinco veces».
     */
    @Test
    fun `a card without an issue list counts coins first and types second`() {
        assertEquals("5 monedas · 5 tipos", countLabel(distinctTypes = 5, quantity = 5))
        assertEquals("3 monedas · 2 tipos", countLabel(distinctTypes = 2, quantity = 3))
        // El singular del español no cuenta nada, así que el cero va en plural (#19).
        assertEquals("1 moneda · 1 tipo", countLabel(distinctTypes = 1, quantity = 1))
        assertEquals("0 monedas · 0 tipos", countLabel(distinctTypes = 0, quantity = 0))
    }
}
