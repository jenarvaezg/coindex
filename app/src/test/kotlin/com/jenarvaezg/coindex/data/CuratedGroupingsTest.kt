package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.domain.CatalogSeedException
import com.jenarvaezg.coindex.domain.CuratedGrouping
import com.jenarvaezg.coindex.domain.GroupingSeeds
import com.jenarvaezg.coindex.domain.normalizeFamily
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The shipped groupings put coins together where Numista says nothing. They can't invent a missing
 * piece but can file a coin under the wrong heading; their type ids were verified on numista.com.
 */
class CuratedGroupingsTest {
    private val groupings: List<CuratedGrouping> = SHIPPED_CURATION.groupings

    private fun find(id: String) = groupings.first { it.id == id }

    @Test
    fun `every shipped grouping parses and validates`() {
        assertTrue(groupings.isNotEmpty())
        groupings.forEach { grouping ->
            assertNull(grouping.validate(), "inválida: ${grouping.id}")
            assertEquals(1, grouping.schemaVersion)
            assertEquals(grouping.family, normalizeFamily(grouping.family))
        }
    }

    /**
     * Medios, reales y el 1 bolívar ya tienen date run (#115, #114, #113). No queda
     * denominación venezolana de plata como agrupación.
     */
    @Test
    fun `no venezuelan silver denomination remains a grouping`() {
        assertTrue(groupings.none { it.id.startsWith("venezuela-") })
    }

    /**
     * Morgan y Peace comparten los 26,73 g de plata .900 y ninguno declara `series`: la afirmación
     * es nuestra, y por eso no hay cobertura. El Silver Eagle (31,1 g de .999) es otra variante y
     * otra tarjeta; de ahí el «clásico» de la familia.
     */
    @Test
    fun `the classic us silver dollar is morgan and peace`() {
        val dollar = find("us-classic-silver-dollar")
        assertEquals(listOf(1_492, 5_580), dollar.typeIds)
        assertEquals("Dólar de plata clásico de EE. UU.", dollar.family)
        assertEquals("etats-unis", dollar.issuerCode)
    }

    /**
     * Onzas de plata de la Royal Mint de un solo año y fuera de toda gama. Las de 2 £ que
     * resultaron ser programas de la ceca, como el British Lion, son catálogos. N#596807 entra con
     * su ficha publicada (#451): su familia «The» es un placeholder (#404) y la agrupación le gana.
     */
    @Test
    fun `the loose royal mint ounces are what no range claims`() {
        val loose = find("uk-royal-mint-1oz-silver-sueltas")
        assertEquals(listOf(436_016, 581_702, 596_807), loose.typeIds)
        assertTrue(476_689 !in loose.typeIds, "el British Lion es un date run, no una suelta")
        assertEquals("Onzas de plata sueltas de la Royal Mint", loose.family)
    }

    /**
     * Las 18 g de plata .925 alemanas son una secuencia real, pero el coleccionista no la persigue
     * (#154) y una lámina de 94 casillas afirmaría una cobertura que nadie va a completar. Los años
     * 2011-2015 son otra clave de variante, no un hueco.
     */
    @Test
    fun `the german sterling silver is a family and never a plate`() {
        val german = find("alemania-plata-de-ley-18g")
        assertEquals(listOf(13_203, 451_961), german.typeIds)
        assertEquals("Alemanas de plata de ley de 18 g", german.family)
        assertEquals("allemagne", german.issuerCode)
    }

    /** Where both could claim a type the catalog wins: a grouping has no members, so no holes. */
    @Test
    fun `no grouping claims a type that a catalog already names`() {
        val catalogTypes = SHIPPED_CURATION.catalogs
            .flatMap { catalog -> catalog.members.map { it.numistaTypeId } }
            .toSet()
        val claimed = groupings.flatMap { it.typeIds }.filter { it in catalogTypes }
        assertTrue(claimed.isEmpty(), "tipos reclamados dos veces: $claimed")
    }

    @Test
    fun `two groupings cannot claim the same type`() {
        val duplicated = listOf(
            "a.json" to grouping("primera", listOf(1_885)),
            "b.json" to grouping("segunda", listOf(1_885)),
        )
        val error = assertFailsWith<CatalogSeedException> { GroupingSeeds.parseAll(duplicated) }
        assertTrue(error.message!!.contains("1885"), error.message!!)
    }

    @Test
    fun `a grouping with an uncanonical family is rejected`() {
        val error = assertFailsWith<CatalogSeedException> {
            GroupingSeeds.parse("mala.json", grouping("mala", listOf(1), family = "Dos  espacios"))
        }
        assertTrue(error.message!!.contains("canonical family"), error.message!!)
    }

    private fun grouping(
        id: String,
        typeIds: List<Int>,
        family: String = "Familia de prueba",
    ): String = """
        {
          "schema_version": 1,
          "id": "$id",
          "name": "Prueba $id",
          "short_name": "Prueba $id",
          "family": "$family",
          "issuer_code": "espagne",
          "source": "https://en.numista.com/catalogue/pieces1885.html",
          "updated_at": "2026-07-30",
          "type_ids": ${typeIds.joinToString(prefix = "[", postfix = "]")}
        }
    """.trimIndent()
}
