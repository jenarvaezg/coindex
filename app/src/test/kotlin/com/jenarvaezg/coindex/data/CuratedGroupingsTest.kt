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
        // Toda la plata venezolana ya es catálogo; quedan las dos de sueltas y la alemana.
        assertEquals(3, groupings.size)
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
     * Morgan y Peace, el caso limpio de agrupación: los dos tipos comparten los 26,73 g de plata
     * .900 del acta de 1837, ninguno declara `series` y la única cosa que los junta es que el
     * coleccionista los llama lo mismo. El programa oficial de 2021 que los reedita es plata .999
     * moderna y no estos dos, así que la afirmación es nuestra — y por eso no hay cobertura.
     *
     * El Silver Eagle se queda fuera aunque también sea un dólar de plata: 31,1 g de .999 es otra
     * variante física, así que estaría en otro cartón, y de ahí que la familia diga «clásico».
     */
    @Test
    fun `the classic us silver dollar is morgan and peace`() {
        val dollar = find("us-classic-silver-dollar")
        assertEquals(listOf(1_492, 5_580), dollar.typeIds)
        assertEquals("Dólar de plata clásico de EE. UU.", dollar.family)
        assertEquals("etats-unis", dollar.issuerCode)
    }

    /**
     * Lo que la Royal Mint acuñó en una onza de plata y no metió en ninguna gama: el D-Day 80, el
     * British Lion y The Angel. Las otras dos onzas de 2 £ que parecían sobras con ellas —St George
     * and the Dragon y The Lion and the Eagle— resultaron ser programas de la propia ceca y salieron
     * de aquí como catálogo, así que esta agrupación es el residuo de verdad y no la lista entera.
     *
     * N#596807 no entra: su ficha está sin publicar (#38) y un borrador se puede borrar con su id.
     */
    @Test
    fun `the loose royal mint ounces are what no range claims`() {
        val loose = find("uk-royal-mint-1oz-silver-sueltas")
        assertEquals(listOf(436_016, 476_689, 581_702), loose.typeIds)
        assertEquals("Onzas de plata sueltas de la Royal Mint", loose.family)
        assertTrue(596_807 !in loose.typeIds)
    }

    /**
     * Las 18 g de plata .925 alemanas son una secuencia de verdad —el 20 € nació en 2016 como
     * sucesor oficial del 10 €, y el BMF encargó 94 motivos en esa misma variante— y aun así no
     * son catálogo: el coleccionista no las persigue, «es que la cantidad es abrumadora» (#154),
     * así que una lámina de 94 casillas afirmaría una cobertura que nadie va a completar. La
     * agrupación les da familia a las dos que hay, una en cada colección, y no afirma nada más.
     *
     * Los cinco años del corte métrico —2011-2015, cuproníquel de 14 g y plata .625 de 16 g— no
     * faltan aquí: son otra clave de variante, no un hueco.
     */
    @Test
    fun `the german sterling silver is a family and never a plate`() {
        val german = find("alemania-plata-de-ley-18g")
        assertEquals(listOf(13_203, 451_961), german.typeIds)
        assertEquals("Alemanas de plata de ley de 18 g", german.family)
        assertEquals("allemagne", german.issuerCode)
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
