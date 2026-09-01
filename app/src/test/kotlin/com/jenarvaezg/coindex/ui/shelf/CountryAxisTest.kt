package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.AlbumSlot
import com.jenarvaezg.coindex.domain.AssembledCollection
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.TypeMeta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The country axis: blocks, ratio order and the compact tail (ADR 0026 §9).
 *
 * It is built from the casillas the assembly resolved and no longer from curated files (#538), which
 * is what this file is about after the move: what a casilla *is* — the evidence behind its plate, its
 * Owned/Missing status and the country it falls in — is asserted in `AlbumSlotsTest`, on the domain.
 * What is left here is the axis' own share: how the cells are grouped, counted and ordered.
 */
class CountryAxisTest {
    @Test
    fun `Italia 2-2 opens before a larger unfinished country`() {
        val model = countryAxis(
            state = state(
                slots = listOf(
                    slot("italia", "a", ITALIA_A, owned = true, country = "Italia"),
                    slot("italia", "b", ITALIA_B, owned = true, country = "Italia"),
                    slot("rusia", "a", RUSIA_A, owned = true, country = "Rusia"),
                    slot("rusia", "b", RUSIA_B, owned = false, country = "Rusia"),
                ),
            ),
        )

        assertEquals(listOf("Italia", "Rusia"), model.blocks.map { it.country })
        assertEquals("2/2", model.blocks[0].label)
        assertEquals("1/2", model.blocks[1].label)
        assertEquals(3, model.ownedSlots)
        assertEquals(4, model.totalSlots)
    }

    /**
     * One plate, two blocks: Equilibrium is struck for Tokelau and for Niue (#170).
     *
     * The axis groups by the country of the casilla and never by the plate it came from, so a series
     * a mint did not split does not have to be split to be painted.
     */
    @Test
    fun `a plate spanning two countries opens a block in each`() {
        val model = countryAxis(
            state = state(
                slots = listOf(slot("equilibrium", "niue", NIUE, owned = true, country = "Niue")) +
                    (0..5).map { index ->
                        slot("equilibrium", "t$index", TOKELAU + index, owned = false, country = "Tokelau")
                    },
            ),
        )

        assertEquals(setOf("Niue", "Tokelau"), model.blocks.map { it.country }.toSet())
        assertEquals("0/6", model.blocks.first { it.country == "Tokelau" }.label)
        assertEquals("1/1", model.blocks.first { it.country == "Niue" }.label)
    }

    @Test
    fun `loose pieces join their issuer without a sueltas band or a denominator`() {
        val model = countryAxis(
            state = state(
                items = listOf(
                    item(1, FRANCE_A, year = 1960),
                    item(2, FRANCE_B, year = 1960),
                    item(3, FRANCE_C, year = 1960),
                ),
                typeMeta = mapOf(
                    FRANCE_A to meta(FRANCE_A, "france", "Francia"),
                    FRANCE_B to meta(FRANCE_B, "france", "Francia"),
                    FRANCE_C to meta(FRANCE_C, "france", "Francia"),
                ),
            ),
            claimedRowIds = emptySet(),
        )

        val francia = model.blocks.single()
        assertEquals("Francia", francia.country)
        assertEquals("3", francia.label)
        assertEquals(null, francia.issued)
        assertTrue(francia.cells.all { it is CountryAxisCell.Loose })
        assertTrue(model.tail.isEmpty()) // three coins → body, not compact
    }

    @Test
    fun `one or two loose coins go in the compact tail`() {
        val model = countryAxis(
            state = state(
                items = listOf(item(1, FRANCE_A, year = 1960)),
                typeMeta = mapOf(FRANCE_A to meta(FRANCE_A, "france", "Francia")),
            ),
            claimedRowIds = emptySet(),
        )

        assertEquals(listOf("Francia"), model.tail.map { it.country })
        assertTrue(model.body.isEmpty())
    }

    @Test
    fun `a país chip keeps only that country's cells on a spanning plate`() {
        val model = countryAxis(
            state = state(
                slots = listOf(
                    slot("historia", "thaler", THALER, owned = true, country = "Imperio austríaco"),
                    slot("historia", "real", REAL, owned = false, country = "México"),
                ),
            ),
            keptCountry = "Imperio austríaco",
        )

        assertEquals(listOf("Imperio austríaco"), model.blocks.map { it.country })
        assertEquals("1/1", model.blocks.single().label)
    }

    /** The weight, estado and serie chips narrow the sheet by plate: a hidden card paints nothing. */
    @Test
    fun `a plate the shelf hid leaves no cell behind`() {
        val model = countryAxis(
            state = state(
                slots = listOf(
                    slot("italia", "a", ITALIA_A, owned = true, country = "Italia"),
                    slot("rusia", "a", RUSIA_A, owned = false, country = "Rusia"),
                ),
            ),
            keptCatalogIds = setOf("italia"),
        )

        assertEquals(listOf("Italia"), model.blocks.map { it.country })
    }

    /** A casilla nobody can name a country for is not painted under an invented one. */
    @Test
    fun `a casilla with no country opens no block`() {
        val model = countryAxis(
            state = state(
                slots = listOf(
                    slot("italia", "a", ITALIA_A, owned = true, country = "Italia"),
                    slot("huerfana", "a", RUSIA_A, owned = true, country = null),
                ),
            ),
        )

        assertEquals(listOf("Italia"), model.blocks.map { it.country })
    }

    private fun state(
        items: List<CollectedItem> = emptyList(),
        typeMeta: Map<Int, TypeMeta> = emptyMap(),
        slots: List<AlbumSlot> = emptyList(),
    ) = CollectionState(
        AssembledCollection(items = items, typeMeta = typeMeta, slots = slots),
    )

    /** A casilla as the assembly hands it over: already measurable, already placed (#538). */
    private fun slot(
        catalogId: String,
        memberId: String,
        typeId: Int,
        owned: Boolean,
        country: String?,
    ) = AlbumSlot(
        catalogId = catalogId,
        memberId = memberId,
        typeId = typeId,
        owned = owned,
        quantity = if (owned) 1 else 0,
        country = country,
        year = 2_000,
    )

    private fun item(id: Long, typeId: Int, year: Int) = CollectedItem(
        id = id,
        quantity = 1,
        typeId = typeId,
        issueYear = year,
    )

    private fun meta(id: Int, code: String, name: String) = TypeMeta(
        id = id,
        issuerCode = code,
        issuerName = name,
        minYear = 1900,
    )

    companion object {
        private const val ITALIA_A = 10
        private const val ITALIA_B = 11
        private const val RUSIA_A = 20
        private const val RUSIA_B = 21
        private const val NIUE = 40
        private const val TOKELAU = 50
        private const val FRANCE_A = 60
        private const val FRANCE_B = 61
        private const val FRANCE_C = 62
        private const val THALER = 70
        private const val REAL = 71
    }
}
