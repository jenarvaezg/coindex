package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.AlbumSlot
import com.jenarvaezg.coindex.domain.AssembledCollection
import com.jenarvaezg.coindex.domain.CoinClaims
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.TypeMeta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The country axis: blocks, ratio order and the compact tail (ADR 0026 §9). What a casilla is
 * (#538) is asserted in `AlbumSlotsTest`; here, how cells are grouped, counted and ordered.
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

    /** Equilibrium is struck for Tokelau and Niue (#170): blocks follow the casilla's country. */
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
            claims = CoinClaims(),
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
            claims = CoinClaims(),
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

    /** The weight, estado and serie chips narrow the sheet by plate. */
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
