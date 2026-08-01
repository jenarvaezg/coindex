package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.Finish
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.TypeMeta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The lines a card writes about itself.
 *
 * Both cases here were UX findings rather than bugs: «Por confirmar» on its own line read as a
 * finish called that, and every card in the index wore the same eyebrow, which said what the
 * section heading above it already said.
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

    private fun item(id: Long, typeId: Int) = CollectedItem(id = id, quantity = 1, typeId = typeId)

    private fun meta(typeId: Int, issuer: String?) =
        typeId to TypeMeta(id = typeId, issuerName = issuer)

    @Test
    fun `the eyebrow names the issuer of the pieces behind the proposal`() {
        val types = mapOf(meta(10_340, "Venezuela"), meta(10_339, "Venezuela"))

        assertEquals(
            "Venezuela",
            issuerEyebrow(listOf(item(1, 10_340), item(2, 10_340)), types),
        )
    }

    @Test
    fun `an issuer nobody agrees on, or nobody recorded, is left unsaid`() {
        val types = mapOf(meta(10_340, "Venezuela"), meta(25_340, "Australia"), meta(1_885, null))

        // Two issuers under one heading would be an eyebrow that lies about half its card.
        assertNull(issuerEyebrow(listOf(item(1, 10_340), item(2, 25_340)), types))
        // A type whose cache row has no issuer, or a type not cached at all.
        assertNull(issuerEyebrow(listOf(item(3, 1_885)), types))
        assertNull(issuerEyebrow(listOf(item(4, 999_999)), types))
        assertNull(issuerEyebrow(emptyList(), types))
        // One unchecked piece among Venezuelans: «Venezuela» would be a claim about it too.
        assertNull(issuerEyebrow(listOf(item(5, 10_340), item(6, 1_885)), types))
    }
}
