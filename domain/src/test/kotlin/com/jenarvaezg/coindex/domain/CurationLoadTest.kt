package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The one door curated files come through (#545).
 *
 * What it is for is the second test: a rule that spans two species used to be applied by whoever
 * loaded the files, and only one loader — the app's container — applied it. A curation the app
 * would have refused to start with was a curation the suite read happily.
 */
class CurationLoadTest {
    @Test
    fun `the door brings in the three species`() {
        val curation = Curation.load(
            fakeFiles(
                catalogs = listOf("britannia.json" to catalogJson()),
                groupings = listOf("dolar.json" to groupingJson()),
                programmes = listOf("herculano.json" to programmeJson()),
            ),
        )

        assertEquals(listOf("uk-silver-britannia-1oz-bullion"), curation.catalogs.map { it.id })
        assertEquals(listOf("us-classic-silver-dollar"), curation.groupings.map { it.id })
        assertEquals(listOf("portugal-alexandre-herculano-1977"), curation.programmes.map { it.id })
        assertEquals(setOf(295_025, 1_492, 5_580, 7_338, 9_831), curation.curatedTypeIds())
    }

    /**
     * The collision of #22 seen where it is visible: a catalog's card and a grouping's card read
     * the same, and the index draws the two side by side with nothing else to tell them apart.
     */
    @Test
    fun `a catalog and a grouping cannot come in reading the same`() {
        val error = assertFailsWith<CatalogSeedException> {
            Curation.load(
                fakeFiles(
                    catalogs = listOf(
                        "dolar.json" to catalogJson(
                            shortName = "Dólar de plata clásico",
                            name = "Dólar de plata clásico · Canadá",
                        ),
                    ),
                    groupings = listOf("dolar-us.json" to groupingJson()),
                    programmes = listOf("herculano.json" to programmeJson()),
                ),
            )
        }

        assertTrue(error.message!!.contains("Dólar de plata clásico"), error.message!!)
    }

    /**
     * A species that brings nothing is a build that lost a directory. Before the door, that was
     * fatal on the phone and invisible in the suite, which read `data/` with a loader of its own.
     */
    @Test
    fun `a species with no file at all stops the load`() {
        val error = assertFailsWith<CatalogSeedException> {
            Curation.load(
                fakeFiles(
                    catalogs = listOf("britannia.json" to catalogJson()),
                    groupings = listOf("dolar.json" to groupingJson()),
                    programmes = emptyList(),
                ),
            )
        }

        assertTrue(error.message!!.contains("programmes"), error.message!!)
    }
}

private fun fakeFiles(
    catalogs: List<Pair<String, String>>,
    groupings: List<Pair<String, String>>,
    programmes: List<Pair<String, String>>,
): CuratedFiles = object : CuratedFiles {
    override fun read(species: CuratedSpecies): List<Pair<String, String>> = when (species) {
        CuratedSpecies.Catalogs -> catalogs
        CuratedSpecies.Groupings -> groupings
        CuratedSpecies.Programmes -> programmes
    }
}

private fun catalogJson(
    shortName: String = "Silver Britannia",
    name: String = "Silver Britannia · Reino Unido · 1 oz .999 bullion",
): String = """
    {
      "schema_version": 1,
      "id": "uk-silver-britannia-1oz-bullion",
      "name": "$name",
      "short_name": "$shortName",
      "issuer_code": "royaume-uni",
      "family": "$shortName",
      "weight_millioz": 1000,
      "finish": "Bullion",
      "metal": "silver",
      "series_status": "open",
      "source": "https://en.numista.com/catalogue/pieces295025.html",
      "updated_at": "2026-08-04",
      "members": [{ "id": "2024", "label": "2024", "year": 2024, "numista_type_id": 295025 }]
    }
""".trimIndent()

private fun groupingJson(): String = """
    {
      "schema_version": 1,
      "id": "us-classic-silver-dollar",
      "name": "Dólar de plata clásico · EE. UU. · Morgan y Peace",
      "short_name": "Dólar de plata clásico",
      "family": "Dólar de plata clásico de EE. UU.",
      "issuer_code": "etats-unis",
      "source": "https://en.numista.com/catalogue/pieces1492.html",
      "updated_at": "2026-08-01",
      "type_ids": [1492, 5580]
    }
""".trimIndent()

private fun programmeJson(): String = """
    {
      "schema_version": 1,
      "id": "portugal-alexandre-herculano-1977",
      "name": "Serie Alexandre Herculano 1977",
      "short_name": "Serie Alexandre Herculano 1977",
      "issuer_code": "portugal",
      "year": 1977,
      "source": "https://www.incm.pt/",
      "source_note": "La ceca acuñó tres denominaciones para el centenario.",
      "updated_at": "2026-08-20",
      "members": [
        { "numista_type_id": 7338, "label": "2,50 escudos" },
        { "numista_type_id": 9831, "label": "5 escudos" }
      ]
    }
""".trimIndent()
