package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun piece(id: Long, typeId: Int, quantity: Int = 1) =
    CollectedItem(id = id, quantity = quantity, typeId = typeId)

class OwnGroupingTest {
    private val paquillos = OwnGrouping(id = 1, name = "Los paquillos", typeIds = listOf(1_885))

    @Test
    fun `a grouping gathers every owned row of its types`() {
        val views = buildOwnGroupingViews(
            listOf(paquillos.copy(typeIds = listOf(1_885, 4_369))),
            listOf(
                piece(1, 1_885),
                piece(2, 1_885),
                piece(3, 4_369, quantity = 59),
                piece(4, 9_999),
            ),
        )

        val view = views.single()
        assertEquals(listOf(1L, 2L, 3L), view.items.map { it.id })
        assertEquals(2, view.distinctTypes)
        assertEquals(61, view.quantity)
    }

    /** A grouping is an extra view: grouping a piece never takes it out of its proposal. */
    @Test
    fun `grouping a piece leaves the derivation untouched`() {
        val items = listOf(piece(1, 1_885))
        val typeMeta = mapOf(
            1_885 to TypeMeta(
                id = 1_885,
                family = "100 Pesetas de Franco",
                weightOz = 19.0 / GRAMS_PER_TROY_OUNCE,
            ),
        )

        val derivation = deriveCollection(items, typeMeta, emptyList())
        val views = buildOwnGroupingViews(listOf(paquillos), items)

        assertEquals(1, derivation.proposals.size)
        assertEquals(1, derivation.proposals[0].quantity)
        assertEquals(1, views.single().quantity)
    }

    @Test
    fun `pieces no longer owned do not count, and the heading survives them`() {
        val views = buildOwnGroupingViews(listOf(paquillos), listOf(piece(1, 1_885, quantity = 0)))

        val view = views.single()
        assertTrue(view.items.isEmpty())
        assertEquals(0, view.quantity)
        assertEquals(0, view.distinctTypes)
        assertEquals("Los paquillos", view.name)
    }
}
