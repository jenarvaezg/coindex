package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FinishInferenceTest {
    @Test
    fun `proof coloured is one distinct finish resolved before either single finish`() {
        assertEquals(
            Finish.ProofColoured,
            inferFinish("1 Dollar - Coloured Proof Dragon", "Lunar Series III"),
        )
        assertEquals(Finish.Proof, inferFinish("2 Pounds - Proof Yale of Beaufort", null))
        assertEquals(Finish.Coloured, inferFinish("1 Dollar - Colorized Tiger", null))
    }

    @Test
    fun `spanish and english wording reach the same finish`() {
        assertEquals(Finish.Gilded, inferFinish("1 Onza dorada", null))
        assertEquals(Finish.Gilded, inferFinish("1 Dollar gilded edition", null))
        assertEquals(Finish.Gilded, inferFinish("10 Euros chapado en oro", null))
        assertEquals(Finish.Antiqued, inferFinish("2 Pounds antiqued finish", null))
        assertEquals(Finish.Antiqued, inferFinish("2 Libras con acabado antiguo", null))
        assertEquals(Finish.Coloured, inferFinish("1 Dólar coloreado", null))
    }

    @Test
    fun `the two bullion series are bullion even when the title never says so`() {
        assertEquals(Finish.Bullion, inferFinish("1 Dollar - Year of the Dragon", "Lunar Series III"))
        assertEquals(
            Finish.Bullion,
            inferFinish("5 Pounds - Seymour Panther", "The Royal Tudor Beasts"),
        )
        assertEquals(Finish.Bullion, inferFinish("1 Dollar bullion", null))
    }

    @Test
    fun `lunar colour variants are coloured even without a colour word`() {
        assertEquals(
            Finish.Coloured,
            inferFinish("1 Dollar - Year of the Blue Dragon", "Lunar Series III"),
        )
        // The same wording outside Lunar Series III stays unknown: it is not a finish claim.
        assertNull(inferFinish("1 Dollar - Year of the Blue Dragon", "Some Other Series"))
    }

    @Test
    fun `an ordinary title leaves the finish unconfirmed`() {
        assertNull(inferFinish("5 Bolívares", "5 Bolívares de Venezuela"))
        assertNull(inferFinish(null, "Lunar Series III"))
    }
}

class CatalogSeedsTest {
    @Test
    fun `a typo in a curated seed fails loudly with the file name`() {
        val error = kotlin.runCatching {
            CatalogSeeds.parse(
                "typo.json",
                """
                {
                  "schema_version": 1,
                  "id": "typo",
                  "name": "Typo",
                  "issuer_code": "serbie",
                  "familly": "Nikola Tesla",
                  "weight_millioz": 1000,
                  "finish": null,
                  "source": "https://en.numista.com/catalogue/series.php?id=5303",
                  "updated_at": "2026-07-29",
                  "members": []
                }
                """.trimIndent(),
            )
        }.exceptionOrNull()

        assertEquals(CatalogSeedException::class, error!!::class)
        assertEquals(true, error.message!!.contains("typo.json"))
    }

    @Test
    fun `an invalid seed is rejected after parsing`() {
        val error = kotlin.runCatching {
            CatalogSeeds.parse(
                "empty.json",
                """
                {
                  "schema_version": 1,
                  "id": "empty",
                  "name": "Empty",
                  "issuer_code": "serbie",
                  "family": "Nikola Tesla",
                  "weight_millioz": 1000,
                  "finish": null,
                  "source": "https://en.numista.com/catalogue/series.php?id=5303",
                  "updated_at": "2026-07-29",
                  "members": []
                }
                """.trimIndent(),
            )
        }.exceptionOrNull()

        assertEquals(true, error!!.message!!.contains("at least one member"))
    }

    @Test
    fun `duplicate catalog ids across files are rejected`() {
        val contents = """
            {
              "schema_version": 1,
              "id": "nikola-tesla-serbia-1oz",
              "name": "Nikola Tesla",
              "issuer_code": "serbie",
              "family": "Nikola Tesla",
              "weight_millioz": 1000,
              "finish": null,
              "source": "https://en.numista.com/catalogue/series.php?id=5303",
              "updated_at": "2026-07-29",
              "members": [
                {
                  "id": "alternating-current",
                  "label": "Alternating current",
                  "year": 2018,
                  "numista_type_id": 150352
                }
              ]
            }
        """.trimIndent()

        val error = kotlin.runCatching {
            CatalogSeeds.parseAll(listOf("a.json" to contents, "b.json" to contents))
        }.exceptionOrNull()

        assertEquals(true, error!!.message!!.contains("is duplicated"))
    }

    @Test
    fun `a curated finish round trips through the seed format`() {
        val parsed = CatalogSeeds.parse(
            "bullion.json",
            """
            {
              "schema_version": 1,
              "id": "tudor-beasts-uk-2oz-bullion",
              "name": "Tudor Beasts 2 oz",
              "issuer_code": "royaume-uni",
              "family": "The Royal Tudor Beasts",
              "weight_millioz": 2000,
              "finish": "Bullion",
              "source": "https://en.numista.com/catalogue/series.php?id=6118",
              "updated_at": "2026-07-29",
              "members": [
                {
                  "id": "seymour-panther",
                  "label": "Seymour Panther",
                  "year": 2022,
                  "numista_type_id": 244811
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(Finish.Bullion, parsed.finish)
        assertEquals(CollectionProposalKey("The Royal Tudor Beasts", 2_000, Finish.Bullion), parsed.key())
    }
}
