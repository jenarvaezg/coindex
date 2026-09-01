package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FinishInferenceTest {
    @Test
    fun `proof coloured is one distinct finish resolved before either single finish`() {
        assertEquals(
            Finish.ProofColoured,
            inferFinish("1 Dollar - Coloured Proof Dragon", "Lunar Series III", null),
        )
        assertEquals(Finish.Proof, inferFinish("2 Pounds - Proof Yale of Beaufort", null, null))
        assertEquals(Finish.Coloured, inferFinish("1 Dollar - Colorized Tiger", null, null))
    }

    @Test
    fun `spanish and english wording reach the same finish`() {
        assertEquals(Finish.Gilded, inferFinish("1 Onza dorada", null, null))
        assertEquals(Finish.Gilded, inferFinish("1 Dollar gilded edition", null, null))
        assertEquals(Finish.Gilded, inferFinish("10 Euros chapado en oro", null, null))
        assertEquals(Finish.Antiqued, inferFinish("2 Pounds antiqued finish", null, null))
        assertEquals(Finish.Antiqued, inferFinish("2 Libras con acabado antiguo", null, null))
        assertEquals(Finish.Coloured, inferFinish("1 Dólar coloreado", null, null))
    }

    @Test
    fun `the two bullion series are bullion even when the title never says so`() {
        assertEquals(
            Finish.Bullion,
            inferFinish("1 Dollar - Year of the Dragon", "Lunar Series III", null),
        )
        assertEquals(
            Finish.Bullion,
            inferFinish("5 Pounds - Seymour Panther", "The Royal Tudor Beasts", null),
        )
        assertEquals(Finish.Bullion, inferFinish("1 Dollar bullion", null, null))
    }

    @Test
    fun `lunar colour variants are coloured even without a colour word`() {
        assertEquals(
            Finish.Coloured,
            inferFinish("1 Dollar - Year of the Blue Dragon", "Lunar Series III", null),
        )
        // The same wording outside Lunar Series III stays unknown: it is not a finish claim.
        assertNull(inferFinish("1 Dollar - Year of the Blue Dragon", "Some Other Series", null))
    }

    @Test
    fun `an ordinary title leaves the finish unconfirmed`() {
        assertNull(inferFinish("5 Bolívares", "5 Bolívares de Venezuela", null))
        assertNull(inferFinish(null, "Lunar Series III", null))
    }

    /**
     * Las once fichas doradas de la libra redonda, medidas una a una el 1 de septiembre de 2026
     * contra `en.numista.com` (#573): las diez del estuche del 25.º aniversario de 2008 que se
     * pudieron enumerar y la del Jubileo de Diamante de 2012, todas con la misma composición.
     *
     * El título dice «Silver Proof» en las quince y también en las treinta y dos que sí son proof,
     * así que el título no las parte y la composición sí — y el acabado que gana es el dorado,
     * porque es el que el catálogo de la libra declaró suyo: «Son `Gilded`, no `Proof`».
     */
    @Test
    fun `a gold plating the title never mentions outranks the proof the title does`() {
        assertEquals(
            Finish.Gilded,
            inferFinish(
                "1 Pound - Elizabeth II (Scottish Thistle; Silver Proof)",
                null,
                "Plata 925 (with selective gold plating)",
            ),
        )
        // La misma moneda sin dorar, que es la casilla de la lámina, se queda en proof.
        assertEquals(
            Finish.Proof,
            inferFinish(
                "1 Pound - Elizabeth II (Scottish Thistle; Silver Proof)",
                null,
                "Plata 925",
            ),
        )
    }

    /**
     * Las tres formas que las fichas sembradas usan para decir lo mismo, y la que no lo dice.
     *
     * Numista se pide en español y aun así el paréntesis llega en inglés, que es la trampa que
     * [inferMetal] documenta desde el otro lado: la cabeza dice de qué está hecha y el paréntesis
     * cómo está acabada.
     */
    @Test
    fun `every wording a seeded composition uses for a gold coating is read`() {
        assertEquals(
            Finish.Gilded,
            inferFinish("2 Dollars - Charles III (Bitcoin)", null, "Plata 999,9 chapado en oro"),
        )
        assertEquals(
            Finish.Gilded,
            inferFinish(
                "1 Dollar - Elizabeth II (4th Portrait - Koala - Silver Bullion Coin)",
                "Australian Koala",
                "Plata 999 (highlighted in 24-carat gold)",
            ),
        )
        assertEquals(Finish.Gilded, inferFinish("1 Dollar", null, "Silver .925, gilded"))
    }

    /**
     * Una moneda **de** oro no está dorada, y ninguna regla de acabado puede leerla como tal.
     *
     * El guardarraíl es el metal dominante y no una lista de excepciones: lo que separa el baño
     * de la aleación es que el oro venga después de otro metal, así que la pregunta la contesta
     * [inferMetal], que es quien ya sabe leer la cabeza de esa misma frase.
     */
    @Test
    fun `a coin made of gold is not a gilded one`() {
        assertNull(inferFinish("100 Dollars - Elizabeth II (Bitcoin)", null, "Oro 999,9"))
        assertEquals(
            Finish.Proof,
            inferFinish("1 Pound - Elizabeth II (Royal Arms; Gold Proof)", null, "Oro 999,9"),
        )
        // Y una de oro chapada en otra cosa tampoco: el baño que se lee es el de oro.
        assertNull(inferFinish("1 Onza", null, "Oro 999,9 chapado en rodio"))
        // Al revés, el metal se lee antes del baño y no en toda la frase: `inferMetal` resuelve
        // «oro» antes que «cobre», así que preguntándole la frase entera ésta sería de oro.
        assertEquals(Finish.Gilded, inferFinish("1 Onza", null, "Cobre chapado en oro"))
    }

    @Test
    fun `a composition that says nothing about a coating leaves the title in charge`() {
        assertNull(inferFinish("5 Bolívares", "5 Bolívares de Venezuela", "Plata 900"))
        assertEquals(Finish.Proof, inferFinish("2 Pounds - Proof", null, "Plata 999"))
        // Y al revés: el dorado es lo único que un tipo sin título llega a decir de su acabado.
        assertEquals(
            Finish.Gilded,
            inferFinish(null, null, "Plata 925 (with selective gold plating)"),
        )
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
                  "short_name": "Typo",
                  "issuer_code": "serbie",
                  "familly": "Nikola Tesla",
                  "weight_millioz": 1000,
                  "finish": null,
                  "metal": "silver",
                  "series_status": "open",
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

    /**
     * Un catálogo que se calla el estado de su serie afirma por omisión que ya no falta nada
     * (#28), así que callárselo no se tolera: la app no arranca y dice qué fichero es.
     */
    @Test
    fun `a seed that keeps quiet about its series status is rejected`() {
        val seed = { status: String ->
            """
            {
              "schema_version": 1,
              "id": "muda",
              "name": "Muda",
              "short_name": "Muda",
              "issuer_code": "serbie",
              "family": "Nikola Tesla",
              "weight_millioz": 1000,
              "finish": null,
              "metal": "silver",
              $status
              "source": "https://en.numista.com/catalogue/series.php?id=5303",
              "updated_at": "2026-08-01",
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
        }

        val missing = kotlin.runCatching { CatalogSeeds.parse("muda.json", seed("")) }
            .exceptionOrNull()
        assertEquals(CatalogSeedException::class, missing!!::class)
        assertEquals(true, missing.message!!.contains("muda.json"))
        assertEquals(true, missing.message!!.contains("series_status"))

        val unknownValue = kotlin.runCatching {
            CatalogSeeds.parse("muda.json", seed("\"series_status\": \"dormant\","))
        }.exceptionOrNull()
        assertEquals(CatalogSeedException::class, unknownValue!!::class)
        assertEquals(true, unknownValue.message!!.contains("muda.json"))

        assertEquals(
            SeriesStatus.Open,
            CatalogSeeds.parse("muda.json", seed("\"series_status\": \"open\",")).seriesStatus,
        )
    }

    /**
     * Qué cara imprime el cuaderno lo declara la lámina, y callarse es declarar el reverso (#227).
     *
     * El campo es de la cabecera y de un solo nivel: la excepción es siempre de la lámina entera,
     * así que no hay `printed_side` por miembro que validar. Y como sólo hay dos caras, un tercer
     * valor no es una preferencia rara sino un fichero curado que no dice nada — la app no arranca
     * y nombra el fichero, igual que con `series_status`.
     */
    @Test
    fun `a seed declares which face the notebook prints, and silence is the reverse`() {
        val seed = { side: String ->
            """
            {
              "schema_version": 1,
              "id": "cara",
              "name": "Cara",
              "short_name": "Cara",
              "issuer_code": "haiti",
              "family": "Nikola Tesla",
              "weight_millioz": 1000,
              "finish": null,
              "metal": "silver",
              "series_status": "open",
              $side
              "source": "https://en.numista.com/catalogue/series.php?id=5303",
              "updated_at": "2026-08-06",
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
        }

        // Ausente es el reverso, que es lo que las 73 láminas de hoy imprimen sin haberlo dicho.
        assertEquals(PrintedSide.Reverse, CatalogSeeds.parse("cara.json", seed("")).printedSide)
        assertEquals(
            PrintedSide.Obverse,
            CatalogSeeds.parse("cara.json", seed("\"printed_side\": \"obverse\",")).printedSide,
        )
        assertEquals(
            PrintedSide.Reverse,
            CatalogSeeds.parse("cara.json", seed("\"printed_side\": \"reverse\",")).printedSide,
        )

        val unknownValue = kotlin.runCatching {
            CatalogSeeds.parse("cara.json", seed("\"printed_side\": \"canto\","))
        }.exceptionOrNull()
        assertEquals(CatalogSeedException::class, unknownValue!!::class)
        assertEquals(true, unknownValue.message!!.contains("cara.json"))
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
                  "short_name": "Empty",
                  "issuer_code": "serbie",
                  "family": "Nikola Tesla",
                  "weight_millioz": 1000,
                  "finish": null,
                  "metal": "silver",
                  "series_status": "open",
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
              "short_name": "Nikola Tesla",
              "issuer_code": "serbie",
              "family": "Nikola Tesla",
              "weight_millioz": 1000,
              "finish": null,
              "metal": "silver",
              "series_status": "open",
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
    fun `shared types across catalogs require disjoint issue-qualified identities`() {
        fun seed(id: String, issueIds: String?) = """
            {
              "schema_version": 1,
              "id": "$id",
              "name": "$id",
              "short_name": "$id",
              "issuer_code": "australia",
              "family": "$id",
              "weight_millioz": 1000,
              "metal": "silver",
              "series_status": "open",
              "source": "https://en.numista.com/catalogue/series.php?id=4750",
              "updated_at": "2026-08-02",
              "members": [{
                "id": "2020-rat",
                "label": "Rat",
                "year": 2020,
                "numista_type_id": 400000${issueIds?.let { ",\n                \"numista_issue_ids\": $it" } ?: ""}
              }]
            }
        """.trimIndent()

        assertEquals(
            2,
            CatalogSeeds.parseAll(
                listOf(
                    "one.json" to seed("lunar-one", "[70001]"),
                    "half.json" to seed("lunar-half", "[70002]"),
                ),
            ).size,
        )

        val unqualified = assertFailsWith<CatalogSeedException> {
            CatalogSeeds.parseAll(
                listOf(
                    "one.json" to seed("lunar-one", "[70001]"),
                    "half.json" to seed("lunar-half", null),
                ),
            )
        }
        assertTrue(unqualified.message!!.contains("type `400000`"))

        val overlap = assertFailsWith<CatalogSeedException> {
            CatalogSeeds.parseAll(
                listOf(
                    "one.json" to seed("lunar-one", "[70001, 70002]"),
                    "half.json" to seed("lunar-half", "[70002]"),
                ),
            )
        }
        assertTrue(overlap.message!!.contains("issue `70002`"))
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
              "short_name": "Tudor Beasts 2 oz",
              "issuer_code": "royaume-uni",
              "family": "The Royal Tudor Beasts",
              "weight_millioz": 2000,
              "finish": "Bullion",
              "metal": "silver",
              "series_status": "open",
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
        assertEquals(Metal.Silver, parsed.metal)
        assertEquals(
            VariantKey("The Royal Tudor Beasts", 2_000, Finish.Bullion, Metal.Silver),
            parsed.key(),
        )
    }
}
