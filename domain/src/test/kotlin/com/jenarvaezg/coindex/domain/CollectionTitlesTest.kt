package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The card-sized name of a collection (#22). It is unique across files: cutting `name` at its first
 * `·` left three Britannias with one name. Cards no file names, and a grouping's two cards, only
 * collide against the day's inventory, so the index resolves its names together (#565).
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
        val key = VariantKey(grouping.family, 1000, null, Metal.Silver)
        assertEquals("Dólar de plata clásico", titles.of(key))
    }

    @Test
    fun `what no curated file claims reads as Numista wrote it`() {
        val titles = CollectionTitles(emptyList(), emptyList())

        assertEquals(
            "Charlemagme - Mounted Knight",
            titles.of(VariantKey("Charlemagme - Mounted Knight", 1000, null, null)),
        )
        assertEquals(
            "Sistema monetario 1981-2001",
            titles.of(VariantKey("System 1981-2001", 1000, null, null)),
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

        // Neither species sees the collision alone; the curation that holds both does (#545).
        val error = assertFailsWith<CatalogSeedException> {
            Curation(catalogs = listOf(catalog), groupings = listOf(grouping))
        }

        assertTrue(error.message!!.contains("Dólar de plata clásico"), error.message!!)
    }

    /**
     * The defect of #565: «5 francs Semeuse» spans the 12 g coin of 1963 (N#679) and the 22,8 g
     * piéfort of 1960 (N#448144). Both are signed huérfanas, so only the name can tell them apart.
     */
    @Test
    fun `two cards of one Numista family say which weight they are`() {
        val titles = CollectionTitles(emptyList(), emptyList())
        val circulation = VariantKey("5 francs Semeuse", 386, null, Metal.Silver)
        val piefort = VariantKey("5 francs Semeuse", 733, null, Metal.Silver)
        val hercules = VariantKey("Hercules type", 965, null, Metal.Silver)

        val names = titles.of(listOf(circulation, piefort, hercules))

        assertEquals("5 francs Semeuse · 0,386 oz", names.getValue(circulation))
        assertEquals("5 francs Semeuse · 0,733 oz", names.getValue(piefort))
        // A card with no twin gets no variant (ADR 0026 §12), but does get its cured label
        // (ADR 0031).
        assertEquals("Tipo Hércules", names.getValue(hercules))
    }

    /** A grouping claims a family, and a family holds a key per Numista pattern (ADR 0013). */
    @Test
    fun `a curated grouping that names two cards disambiguates them too`() {
        val grouping = CuratedGrouping(
            schemaVersion = 1,
            id = "alemanas-plata-de-ley",
            name = "Alemanas de plata de ley · Alemania",
            shortName = "Alemanas de plata de ley",
            family = "Alemanas de plata de ley",
            issuerCode = "allemagne",
            source = "https://en.numista.com/catalogue/pieces13203.html",
            updatedAt = "2026-09-01",
            typeIds = listOf(13203, 13204),
        )
        val titles = CollectionTitles(emptyList(), listOf(grouping))
        val ten = VariantKey(grouping.family, 579, null, Metal.Silver)
        val twenty = VariantKey(grouping.family, 1_000, null, Metal.Silver)

        val names = titles.of(listOf(ten, twenty))

        assertEquals("Alemanas de plata de ley · 0,579 oz", names.getValue(ten))
        assertEquals("Alemanas de plata de ley · 1 oz", names.getValue(twenty))
    }
}

/** For tests about a single file. */
private fun CollectionTitles.of(key: VariantKey): String = of(listOf(key)).getValue(key)

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
