package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The card-sized name of a collection, and the three rules that keep it honest (#22).
 *
 * The rule that does the work is uniqueness: cutting `name` at its first `·` would have been
 * free, and it collapsed twelve curated files into five names — three Britannias reading the
 * same thing on three cards, with the weight and finish that tell them apart nowhere in sight.
 */
class CollectionTitlesTest {
    @Test
    fun `a catalog names its own card, and the plate keeps the long name`() {
        val britannia = catalogJson(
            id = "uk-silver-britannia-quarter-oz-bullion",
            name = "Silver Britannia ¼ oz · Reino Unido · .999 bullion (sin proof ni Gairsoppa)",
            shortName = "Silver Britannia ¼ oz",
            family = "Silver Britannia ¼ oz bullion",
            weightMillioz = 250,
        )

        val catalog = CatalogSeeds.parse("britannia.json", britannia)
        val titles = CollectionTitles(listOf(catalog), emptyList())

        assertEquals("Silver Britannia ¼ oz", titles.of(catalog.key()))
        assertTrue(catalog.name.startsWith(catalog.shortName))
    }

    @Test
    fun `a curated grouping names its card by family, which is all it supplies`() {
        val grouping = CuratedGrouping(
            schemaVersion = 1,
            id = "us-classic-silver-dollar",
            name = "Dólar de plata clásico · EE. UU. · Morgan y Peace",
            shortName = "Dólar de plata clásico",
            family = "Dólar de plata clásico de EE. UU.",
            issuerCode = "etats-unis",
            source = "https://en.numista.com/catalogue/pieces1492.html",
            updatedAt = "2026-08-01",
            typeIds = listOf(1492),
        )
        val titles = CollectionTitles(emptyList(), listOf(grouping))

        // A grouping declares no weight, finish or metal (ADR 0013), so the key it names is
        // whatever each piece's own metadata resolved to.
        val key = CollectionProposalKey(grouping.family, 1000, null, Metal.Silver)
        assertEquals("Dólar de plata clásico", titles.of(key))
    }

    @Test
    fun `what no curated file claims reads as Numista wrote it`() {
        val titles = CollectionTitles(emptyList(), emptyList())

        assertEquals(
            "Charlemagme - Mounted Knight",
            titles.of(CollectionProposalKey("Charlemagme - Mounted Knight", 1000, null, null)),
        )
        assertEquals(
            "Sistema monetario 1981-2001",
            titles.of(CollectionProposalKey("System 1981-2001", 1000, null, null)),
        )
    }

    @Test
    fun `two catalogs cannot read the same on two cards`() {
        val files = listOf(
            "bullion.json" to catalogJson(
                id = "lunar-iii-perth-1oz-bullion",
                name = "Lunar Series III · Perth Mint · 1 oz bullion",
                shortName = "Lunar Series III",
                family = "Lunar Series III",
                weightMillioz = 1000,
            ),
            "proof.json" to catalogJson(
                id = "lunar-iii-perth-1oz-proof-coloured",
                name = "Lunar Series III · Perth Mint · 1 oz proof coloured",
                shortName = "Lunar Series III",
                family = "Lunar Series III",
                weightMillioz = 1001,
            ),
        )

        val error = assertFailsWith<CatalogSeedException> { CatalogSeeds.parseAll(files) }

        assertTrue(error.message!!.contains("Lunar Series III"), error.message!!)
    }

    @Test
    fun `a card name that is not a prefix of the editorial one is rejected`() {
        val error = assertFailsWith<CatalogSeedException> {
            CatalogSeeds.parse(
                "britannia.json",
                catalogJson(
                    id = "uk-silver-britannia-quarter-oz-bullion",
                    name = "Silver Britannia · Reino Unido · ¼ oz .999 bullion",
                    shortName = "Silver Britannia ¼ oz",
                    family = "Silver Britannia ¼ oz bullion",
                    weightMillioz = 250,
                ),
            )
        }

        assertTrue(error.message!!.contains("prefix"), error.message!!)
    }

    @Test
    fun `a catalog and a grouping cannot read the same either`() {
        val catalog = CatalogSeeds.parse(
            "dolar.json",
            catalogJson(
                id = "canada-dolar-plata-800",
                name = "Dólar de plata clásico · Canadá 1935-1967",
                shortName = "Dólar de plata clásico",
                family = "Dólar de plata .800 de Canadá",
                weightMillioz = 600,
            ),
        )
        val grouping = CuratedGrouping(
            schemaVersion = 1,
            id = "us-classic-silver-dollar",
            name = "Dólar de plata clásico · EE. UU. · Morgan y Peace",
            shortName = "Dólar de plata clásico",
            family = "Dólar de plata clásico de EE. UU.",
            issuerCode = "etats-unis",
            source = "https://en.numista.com/catalogue/pieces1492.html",
            updatedAt = "2026-08-01",
            typeIds = listOf(1492),
        )

        // Neither species can see the collision on its own; the index that draws them side by
        // side can, and so can the place where both are loaded.
        val error = assertFailsWith<CatalogSeedException> {
            validateShortNamesAcross(listOf(catalog), listOf(grouping))
        }

        assertTrue(error.message!!.contains("Dólar de plata clásico"), error.message!!)
    }
}

private fun catalogJson(
    id: String,
    name: String,
    shortName: String,
    family: String,
    weightMillioz: Int,
): String = """
    {
      "schema_version": 1,
      "id": "$id",
      "name": "$name",
      "short_name": "$shortName",
      "issuer_code": "royaume-uni",
      "family": "$family",
      "weight_millioz": $weightMillioz,
      "finish": "Bullion",
      "metal": "silver",
      "series_status": "open",
      "source": "https://en.numista.com/catalogue/pieces295025.html",
      "updated_at": "2026-08-04",
      "members": [{ "id": "2024", "label": "2024", "year": 2024, "numista_type_id": 295025 }]
    }
""".trimIndent()
