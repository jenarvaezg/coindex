package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.Finish
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
        assertEquals("0,804 oz · Acabado sin confirmar", variantLabel(804, null))
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
        // A type whose cache row predates the field, or a type not cached at all.
        assertNull(issuerEyebrow(listOf(item(3, 1_885)), types))
        assertNull(issuerEyebrow(listOf(item(4, 999_999)), types))
        assertNull(issuerEyebrow(emptyList(), types))
    }
}
