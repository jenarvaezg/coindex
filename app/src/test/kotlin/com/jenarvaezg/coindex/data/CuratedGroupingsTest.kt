package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.domain.CatalogSeedException
import com.jenarvaezg.coindex.domain.CatalogSeeds
import com.jenarvaezg.coindex.domain.CuratedGrouping
import com.jenarvaezg.coindex.domain.GroupingSeeds
import com.jenarvaezg.coindex.domain.normalizeFamily
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The shipped groupings say which coins belong together where Numista says nothing at all.
 *
 * They cannot invent a missing piece, but they can file a coin under the wrong heading, and
 * their type ids were verified against numista.com one by one just like the catalogs'.
 */
class CuratedGroupingsTest {
    private val groupings: List<CuratedGrouping> = GroupingSeeds.parseAll(GroupingFiles.all())

    private fun find(id: String) = groupings.first { it.id == id }

    @Test
    fun `every shipped grouping parses and validates`() {
        assertEquals(4, groupings.size)
        groupings.forEach { grouping ->
            assertNull(grouping.validate(), "inválida: ${grouping.id}")
            assertEquals(1, grouping.schemaVersion)
            assertEquals(grouping.family, normalizeFamily(grouping.family))
        }
    }

    @Test
    fun `the paquillos are the one spanish type Numista files under no series`() {
        val paquillos = find("espana-100-pesetas-franco")
        assertEquals("100 Pesetas de Franco", paquillos.family)
        assertEquals("espagne", paquillos.issuerCode)
        // N#1885: 19 g de plata .800, estrellas 66 a 70 dentro de un único tipo.
        assertEquals(listOf(1_885), paquillos.typeIds)
    }

    /**
     * One card per silver denomination, which is how the collector talks about them: a «medio»
     * is a quarter bolívar whatever the coin calls itself, and the 1960 25 céntimos is the same
     * coin under the decimal name.
     */
    @Test
    fun `the venezuelan silver is grouped by denomination`() {
        assertEquals(listOf(4_369, 9_488), find("venezuela-medios").typeIds)
        assertEquals("Medios de Venezuela", find("venezuela-medios").family)
        assertEquals(listOf(2_971, 7_297), find("venezuela-reales").typeIds)
        assertEquals("Reales de Venezuela", find("venezuela-reales").family)
        assertEquals(listOf(10_398, 7_034, 5_316), find("venezuela-1-bolivar").typeIds)
        assertEquals("1 Bolívar de Venezuela", find("venezuela-1-bolivar").family)
    }

    /**
     * The 2 bolívares are a catalog, not a grouping: a grouping declares no members, so it
     * could never point at the 1965 he is missing.
     */
    @Test
    fun `no grouping claims a type that a catalog already names`() {
        val catalogTypes = CatalogSeeds.parseAll(CatalogFiles.all())
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
          "name": "Prueba",
          "family": "$family",
          "issuer_code": "espagne",
          "source": "https://en.numista.com/catalogue/pieces1885.html",
          "updated_at": "2026-07-30",
          "type_ids": ${typeIds.joinToString(prefix = "[", postfix = "]")}
        }
    """.trimIndent()
}
