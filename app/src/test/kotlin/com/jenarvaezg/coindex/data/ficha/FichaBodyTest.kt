package com.jenarvaezg.coindex.data.ficha

import com.jenarvaezg.coindex.data.Fixtures
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** What a stored Numista body says beyond its columns, as a function of the body alone (#221). */
class FichaBodyTest {
    private val australianKookaburra = Fixtures.type(404_044)

    @Test
    fun `a real ficha gives up its five fields`() {
        val reading = readFichaBody(australianKookaburra)

        // `australie` is the column; «Australia» is the only issuer string a card can print.
        assertEquals("Australia", reading.issuerName)
        assertEquals("coin", reading.category)
        assertEquals("Plata 999,9", reading.composition)
        assertEquals(32.6, reading.sizeMillimetres)
        assertEquals("https://es.numista.com/404044", reading.numistaUrl)
    }

    /**
     * A medal has no `min_year` or `max_year`; its date is `issue_terms.issue_date`, with zeros
     * where Numista does not know the month or the day (#460).
     */
    @Test
    fun `a medal gives up the year of its issue date`() {
        assertEquals(
            1_995,
            readFichaBody("""{"issue_terms":{"is_issued":true,"issue_date":"1995-00-00"}}""")
                .issuedYear,
        )
    }

    @Test
    fun `an issue date with no year in it is no year`() {
        listOf(
            """{"issue_terms":{"issue_date":"0000-00-00"}}""",
            """{"issue_terms":{"issue_date":""}}""",
            """{"issue_terms":{"issue_date":"sin fecha"}}""",
            """{"issue_terms":{"is_issued":true}}""",
            """{"issue_terms":"1995"}""",
            "{}",
        ).forEach { body ->
            assertNull(readFichaBody(body).issuedYear, "«$body» no trae año y no debe inventarlo")
        }
    }

    @Test
    fun `reading the same body twice says the same thing`() {
        assertEquals(readFichaBody(australianKookaburra), readFichaBody(australianKookaburra))
    }

    @Test
    fun `a body nobody can parse says nothing at all`() {
        assertEquals(FichaReading(), readFichaBody("no es json"))
        assertEquals(FichaReading(), readFichaBody(""))
        assertEquals(FichaReading(), readFichaBody("[]"))
    }

    @Test
    fun `an empty ficha says nothing either`() {
        assertEquals(FichaReading(), readFichaBody("{}"))
    }

    @Test
    fun `a field that is present and blank is a field nobody filled in`() {
        val reading = readFichaBody(
            """
            {
              "issuer": {"code": "australie", "name": "  "},
              "composition": {"text": ""},
              "category": "",
              "url": ""
            }
            """.trimIndent(),
        )

        assertEquals(FichaReading(), reading)
    }

    @Test
    fun `a diameter of zero is not a diameter`() {
        assertNull(readFichaBody("""{"size": 0}""").sizeMillimetres)
        assertEquals(38.6, readFichaBody("""{"size": 38.6}""").sizeMillimetres)
    }

    @Test
    fun `a field of the wrong shape costs only itself`() {
        // The issuer has no name and the size is an object rather than a number: the other
        // fields still come back.
        val reading = readFichaBody(
            """{"issuer": {"code": "australie"}, "size": {"mm": 32.6}, "category": "exonumia"}""",
        )

        assertNull(reading.issuerName)
        assertNull(reading.sizeMillimetres)
        assertEquals("exonumia", reading.category)
    }

    /**
     * Columns are written once and cached types are never fetched again, so a new field reaches the
     * cache only if [FICHA_READING] goes up with it. Both numbers are pinned so that changing one
     * without the other fails here.
     */
    @Test
    fun `an eleventh field would have to be read into the fichas already cached`() {
        // Instance fields only: the Compose compiler adds a static `$stable` to every class it
        // sees, and this module is one of them.
        val fields = FichaReading::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }

        assertEquals(
            10,
            fields.size,
            "si añades un campo a FichaReading, sube FICHA_READING: si no, las fichas ya " +
                "cacheadas se quedan sin él para siempre",
        )
        assertEquals(3, FICHA_READING)
    }
}
