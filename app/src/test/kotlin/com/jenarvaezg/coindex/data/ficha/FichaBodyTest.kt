package com.jenarvaezg.coindex.data.ficha

import com.jenarvaezg.coindex.data.Fixtures
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * What a stored Numista body says beyond its columns.
 *
 * Every assertion here reads the same ficha as many times as it likes, in any order, and gets the
 * same answer: [Ficha] is a function of the body and of nothing else. The version this replaced
 * had to be handed a different `fetchedAt` per assertion — 5, 1, 2, 4, 10, 12… — because a memo
 * nobody could empty was keyed on it, and nothing in the interface said so (#221).
 */
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
        // The issuer is there but has no name, and the size arrives as an object rather than a
        // number: the other three still come back, which is how these read before they shared a
        // parse.
        val reading = readFichaBody(
            """{"issuer": {"code": "australie"}, "size": {"mm": 32.6}, "category": "exonumia"}""",
        )

        assertNull(reading.issuerName)
        assertNull(reading.sizeMillimetres)
        assertEquals("exonumia", reading.category)
    }

    /**
     * The tripwire the memo did not need.
     *
     * A memo re-read the body whenever the row changed; a column is written once, so a tenth field
     * added here reaches nothing that is already cached unless [FICHA_READING] goes up with it —
     * and no cached type is ever fetched again. Both halves are pinned so that adding one without
     * the other turns this red.
     *
     * It has caught it once already, which is what the pinning is for: the four fields «Las cifras»
     * needed (ADR 0028 §7) arrived with the reading still at 1, and every ficha in the collector's
     * cache would have stayed without a thickness, a demonetization, a hand or a mint for ever.
     */
    @Test
    fun `a tenth field would have to be read into the fichas already cached`() {
        // Instance fields only: the Compose compiler adds a static `$stable` to every class it
        // sees, and this module is one of them.
        val fields = FichaReading::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }

        assertEquals(
            9,
            fields.size,
            "si añades un campo a FichaReading, sube FICHA_READING: si no, las fichas ya " +
                "cacheadas se quedan sin él para siempre",
        )
        assertEquals(2, FICHA_READING)
    }
}
