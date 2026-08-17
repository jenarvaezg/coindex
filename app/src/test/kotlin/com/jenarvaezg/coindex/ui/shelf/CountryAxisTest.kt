package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.AssembledCollection
import com.jenarvaezg.coindex.domain.CatalogAlbums
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.Finish
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.domain.TypeMeta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The country axis: member country, ratio order, no «sueltas» band (ADR 0026 §9).
 */
class CountryAxisTest {
    @Test
    fun `Italia 2-2 opens before a larger unfinished country`() {
        val catalogs = listOf(
            catalog(
                id = "italia",
                issuer = "italia",
                members = listOf(
                    member("a", ITALIA_A, 2000),
                    member("b", ITALIA_B, 2001),
                ),
            ),
            catalog(
                id = "rusia",
                issuer = "russie",
                members = listOf(
                    member("a", RUSIA_A, 2020),
                    member("b", RUSIA_B, 2021),
                ),
            ),
        )
        val model = countryAxis(
            state = state(
                items = listOf(
                    item(1, ITALIA_A, year = 2000),
                    item(2, ITALIA_B, year = 2001),
                    item(3, RUSIA_A, year = 2020),
                ),
                typeMeta = mapOf(
                    ITALIA_A to meta(ITALIA_A, "italia", "Italia"),
                    ITALIA_B to meta(ITALIA_B, "italia", "Italia"),
                    RUSIA_A to meta(RUSIA_A, "russie", "Rusia"),
                    RUSIA_B to meta(RUSIA_B, "russie", "Rusia"),
                ),
                evidenced = setOf("italia", "rusia"),
                catalogs = catalogs,
            ),
            catalogs = catalogs,
        )

        assertEquals(listOf("Italia", "Rusia"), model.blocks.map { it.country })
        assertEquals("2/2", model.blocks[0].label)
        assertEquals("1/2", model.blocks[1].label)
    }

    @Test
    fun `a slot lives in the member country, not the catalog header`() {
        // Historia del real: catalog issuer méxico, members from new_south_wales (#170).
        val catalogs = listOf(
            catalog(
                id = "historia",
                issuer = "mexique",
                members = listOf(
                    member("a", NSW_A, 1813, issuerCode = "new_south_wales"),
                    member("b", NSW_B, 1813, issuerCode = "new_south_wales"),
                ),
            ),
        )
        val model = countryAxis(
            state = state(
                items = emptyList(),
                typeMeta = mapOf(
                    NSW_A to meta(NSW_A, "new_south_wales", "New South Wales"),
                    NSW_B to meta(NSW_B, "new_south_wales", "New South Wales"),
                ),
                evidenced = setOf("historia"),
                catalogs = catalogs,
            ),
            catalogs = catalogs,
        )

        assertEquals(listOf("Nueva Gales del Sur"), model.blocks.map { it.country })
        assertEquals("0/2", model.blocks.single().label)
    }

    @Test
    fun `Tokelau appears when Equilibrium members override the catalog issuer`() {
        val tokelauTypes = (0..5).map { TOKELAU + it }
        val catalogs = listOf(
            catalog(
                id = "equilibrium",
                issuer = "tokelau",
                members = listOf(
                    member("niue", NIUE, 2024, issuerCode = "niue"),
                ) + tokelauTypes.mapIndexed { index, typeId ->
                    member("t$index", typeId, 2020 + index)
                },
            ),
        )
        val model = countryAxis(
            state = state(
                items = listOf(item(1, NIUE, year = 2024)),
                typeMeta = buildMap {
                    put(NIUE, meta(NIUE, "niue", "Niue"))
                    tokelauTypes.forEach { id -> put(id, meta(id, "tokelau", "Tokelau")) }
                },
                evidenced = setOf("equilibrium"),
                catalogs = catalogs,
            ),
            catalogs = catalogs,
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
                evidenced = emptySet(),
            ),
            catalogs = emptyList(),
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
                evidenced = emptySet(),
            ),
            catalogs = emptyList(),
            claimedRowIds = emptySet(),
        )

        assertEquals(listOf("Francia"), model.tail.map { it.country })
        assertTrue(model.body.isEmpty())
    }

    @Test
    fun `a país chip keeps only that country's cells on a spanning plate`() {
        val catalogs = listOf(
            catalog(
                id = "historia",
                issuer = "mexique",
                members = listOf(
                    member("thaler", THALER, 1780, issuerCode = "autriche-habsbourg"),
                    member("real", REAL, 1791),
                ),
            ),
        )
        val model = countryAxis(
            state = state(
                items = listOf(item(1, THALER, year = 1780)),
                typeMeta = mapOf(
                    THALER to meta(THALER, "autriche-habsbourg", "Imperio austríaco"),
                    REAL to meta(REAL, "mexique", "México"),
                ),
                evidenced = setOf("historia"),
                catalogs = catalogs,
            ),
            catalogs = catalogs,
            keptCountry = "Imperio austríaco",
        )

        assertEquals(listOf("Imperio austríaco"), model.blocks.map { it.country })
        assertEquals("1/1", model.blocks.single().label)
    }

    private fun state(
        items: List<CollectedItem>,
        typeMeta: Map<Int, TypeMeta>,
        evidenced: Set<String>,
        catalogs: List<CollectionCatalog> = emptyList(),
    ) = CollectionState(
        AssembledCollection(
            items = items,
            typeMeta = typeMeta,
            // The albums the assembly carries (#537): a slot of this axis is a casilla of the plate
            // the card opens, and both read the same album.
            albums = CatalogAlbums.over(catalogs, items),
            evidencedCatalogIds = evidenced,
        ),
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

    private fun catalog(
        id: String,
        issuer: String,
        members: List<CollectionCatalogMember>,
    ) = CollectionCatalog(
        schemaVersion = 1,
        id = id,
        name = id,
        shortName = id,
        family = id,
        weightMillioz = 1_000,
        finish = Finish.Bullion,
        metal = Metal.Silver,
        issuerCode = issuer,
        seriesStatus = SeriesStatus.Closed,
        source = "test",
        updatedAt = "2026-08-10",
        members = members,
    )

    private fun member(
        id: String,
        typeId: Int,
        year: Int,
        issuerCode: String? = null,
    ) = CollectionCatalogMember(
        id = id,
        label = id,
        year = year,
        numistaTypeId = typeId,
        issuerCode = issuerCode,
    )

    companion object {
        private const val ITALIA_A = 10
        private const val ITALIA_B = 11
        private const val RUSIA_A = 20
        private const val RUSIA_B = 21
        private const val NSW_A = 30
        private const val NSW_B = 31
        private const val NIUE = 40
        private const val TOKELAU = 50
        private const val FRANCE_A = 60
        private const val FRANCE_B = 61
        private const val FRANCE_C = 62
        private const val THALER = 70
        private const val REAL = 71
    }
}
