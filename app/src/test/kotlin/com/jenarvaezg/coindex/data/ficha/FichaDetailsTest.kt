package com.jenarvaezg.coindex.data.ficha

import com.jenarvaezg.coindex.data.TypeCacheFile
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/** What the coin's screen reads from a stored body beyond its columns (#643), on seeded fichas. */
class FichaDetailsTest {
    @Test
    fun `the national police two euros gives up its edge, faces and cartouche`() {
        val details = readFichaDetails(seeded(401_375))

        assertEquals("2 euros", details.faceValue)
        assertEquals("Euro (2002-presente)", details.currency)
        assertEquals(listOf("Felipe VI"), details.rulers)
        assertEquals("200 aniversario Policía Nacional de España", details.topic)
        assertEquals(CoinOrientation.Medal, details.orientation)
        assertEquals("Acuñación a máquina", details.technique)
        assertEquals(listOf(FichaReference("KM", "1571")), details.references)

        val edge = assertNotNull(details.edge)
        assertTrue(edge.description!!.startsWith("Letras sobre lengüeta."))
        assertEquals("2 ★ ★ ↊ ★ ★ 2 ★ ★ ↊ ★ ★ 2 ★ ★ ↊ ★ ★", edge.lettering)
        assertEquals(
            "https://en.numista.com/catalogue/photos/espagne/65d8ce4fa90775.60620718-original.jpg",
            edge.photo?.picture,
        )

        val obverse = assertNotNull(details.obverse)
        assertEquals(
            "POLICÍA NACIONAL 1824 - 2024\nPOLICIA\nNACIONAL\nM\nESPAÑA",
            obverse.lettering,
        )
        assertTrue(obverse.description!!.startsWith("Sello del Cuerpo Nacional de Policía"))
        assertEquals("2 EURO\nLL", details.reverse?.lettering)
    }

    @Test
    fun `the eight reales lists its related types with their country and years`() {
        val related = readFichaDetails(seeded(18_852)).related

        assertEquals(4, related.size)
        assertEquals(
            RelatedType(26_228, "8 Reales - Charles IV", "Bolivia", minYear = 1791, maxYear = 1808),
            related.first(),
        )
    }

    @Test
    fun `a demonetized coin keeps the date Numista gives`() {
        assertEquals(LocalDate.of(2011, 12, 31), readFichaDetails(seeded(10_340)).demonetizedOn)
        assertNull(readFichaDetails(seeded(127_263)).demonetizedOn)
    }

    /** Numista marks a text nobody translated with «(en)»; on screen it is noise. */
    @Test
    fun `the untranslated mark is dropped and the text kept`() {
        val medal = readFichaDetails(seeded(578_835))

        assertEquals("Cuatricentenario de Caracas", medal.topic)
        // Entities decoded, as in every plain field.
        assertEquals("Plata 999,9 (Marked \"PLATA 1000\")", medal.composition)
        assertTrue(medal.comments!!.startsWith("<p>The reverse shows"), medal.comments)
        assertTrue(medal.obverse!!.description!!.startsWith("Bust of Diego de Losada"))
    }

    @Test
    fun `the comments stay html for the screen to render`() {
        val comments = readFichaDetails(seeded(10_340)).comments

        assertTrue(comments!!.startsWith("<p>Y# 24.1, NCV# mv5bs-aa:"), comments)
    }

    @Test
    fun `an unabridged legend is read beside the lettering`() {
        val obverse = readFichaDetails(seeded(107_370)).obverse!!

        assertEquals("Elizabeth II Dei Gratia Regina Fidei Defensatrix", obverse.unabridgedLegend)
        assertEquals("ELIZABETH II·D·G·REG·F·D·5 POUNDS·\nJ.C", obverse.lettering)
    }

    @Test
    fun `html entities in plain fields are decoded`() {
        val details = readFichaDetails(
            """{"edge":{"description":"Liso con &quot;letras&quot; &amp; motivos"}}""",
        )

        assertEquals("Liso con \"letras\" & motivos", details.edge?.description)
    }

    @Test
    fun `a body that isn't a ficha gives empty details`() {
        assertEquals(FichaDetails(), readFichaDetails("no es json"))
        assertEquals(FichaDetails(), readFichaDetails("[]"))
        assertEquals(FichaDetails(), readFichaDetails("{}"))
    }

    /** One malformed field doesn't lose the others, as in [readFichaBody]. */
    @Test
    fun `a malformed field loses only itself`() {
        val details = readFichaDetails(
            """{"ruler":"no es una lista","technique":{"text":"Acuñación a martillo"}}""",
        )

        assertEquals(emptyList(), details.rulers)
        assertEquals("Acuñación a martillo", details.technique)
    }

    private fun seeded(typeId: Int): String =
        assertNotNull(seed[typeId.toString()], "N#$typeId no está en la caché sembrada").toString()

    private companion object {
        val seed by lazy { Json.parseToJsonElement(TypeCacheFile.read()).jsonObject }
    }
}
