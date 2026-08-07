package com.jenarvaezg.coindex.ui.print

import com.google.zxing.qrcode.decoder.Decoder
import com.jenarvaezg.coindex.data.TypeCacheFile
import com.jenarvaezg.coindex.data.numista.NumistaTypeDto
import com.jenarvaezg.coindex.data.typeMetaEntity
import com.jenarvaezg.coindex.data.toDomain
import com.jenarvaezg.coindex.domain.TypeMeta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * The code that goes under a coin on paper, which is the half of #234 a printer is not needed for.
 *
 * What a printer *is* needed for is the closing criterion of the ticket — a folio scanned with a
 * phone at arm's length — and nothing here stands in for it. What is pinned here is everything a
 * bad print would otherwise hide: that the modules decode back to the page they promise, and that
 * the whole seeded cache fits in the version the millimetres were reserved for.
 */
class NumistaQrTest {
    private val json = Json { ignoreUnknownKeys = true }

    /** The five paquillos share this one: type N#1885, and 27 characters of it. */
    private val paquillos = "https://es.numista.com/1885"

    private val seeded: List<TypeMeta> = json
        .parseToJsonElement(TypeCacheFile.read())
        .jsonObject
        .entries
        .mapNotNull { (typeIdText, element) ->
            val raw = element as? JsonObject ?: return@mapNotNull null
            val typeId = typeIdText.toIntOrNull() ?: return@mapNotNull null
            val dto = runCatching {
                json.decodeFromJsonElement(NumistaTypeDto.serializer(), raw)
            }.getOrNull() ?: return@mapNotNull null
            typeMetaEntity(typeId, dto, raw.toString(), 0L).toDomain()
        }

    /**
     * The one thing the paper cannot be asked about later: the code says what it claims to say.
     *
     * Decoded from the modules themselves rather than from a bitmap, because a bitmap would be
     * testing the renderer's scaling and this is testing the payload.
     */
    @Test
    fun `the modules decode back to the numista page they promise`() {
        val code = numistaQr(paquillos)!!

        assertEquals(paquillos, Decoder().decode(code).text)
    }

    /**
     * Every type of the seeded cache carries a URL, and every one of them is a version 2.
     *
     * This is the measurement the whole cost of the switch rests on. 25 × 25 modules with their quiet
     * zone is 33, and it is 33 that the ten millimetres of the caption were chosen against: a type
     * whose URL spilled into a version 3 would still fit the square —the drawing divides by the
     * symbol's own module count— and would print at 0,270 mm a module instead of 0,303, which is the
     * one number the printed folio of the ticket was about.
     */
    @Test
    fun `every seeded type is a version two symbol of twenty-five modules`() {
        val withoutUrl = seeded.filter { it.numistaUrl == null }
        assertTrue(withoutUrl.isEmpty(), "fichas sin URL: ${withoutUrl.map { it.id }}")

        val versions = seeded.mapNotNull { numistaQr(it.numistaUrl)?.width }.distinct()

        assertEquals(listOf(25), versions, "no todos los tipos sembrados son versión 2")
        assertEquals(33, numistaQr(paquillos)!!.qrModulesWithQuietZone)
    }

    /**
     * And todas son la URL corta de Numista, que es la única razón por la que 25 módulos bastan.
     *
     * El límite es 32 y no «lo que mide hoy la más larga»: 32 bytes es exactamente lo que cabe en una
     * versión 2 al nivel L, así que es **la** invariante — la página larga,
     * `.../catalogue/pieces1885.html`, son 49 y no cabría. Fijar el máximo de hoy convertiría este
     * test en un chivato de lo que hay en `data/`, y curar una moneda con un id de siete dígitos lo
     * rompería sin que nada estuviese mal.
     */
    @Test
    fun `the url the code carries is numista's short one and not the catalogue page`() {
        val urls = seeded.mapNotNull { it.numistaUrl }

        assertEquals(seeded.size, urls.size)
        assertTrue(urls.all { it.startsWith("https://es.numista.com/") })
        assertTrue(urls.all { it.length <= 32 }, "no cabe en una versión 2: ${urls.maxBy { it.length }}")
    }

    @Test
    fun `a member no numista type backs gets no code at all`() {
        assertNull(numistaQr(null))
        assertNull(numistaQr(""))
        assertNull(numistaQr("   "))
    }

    /**
     * The runs are the row, exactly: nothing added, nothing dropped, nothing overlapping.
     *
     * The drawing paints runs instead of modules — for the size of the PDF, not for the seams — so an
     * off-by-one here would be a code with a column of white through it. And a QR is designed to
     * survive that, which is what makes it the kind of bug that only shows up on somebody else's phone.
     */
    @Test
    fun `the runs a row is drawn as are that row's dark modules`() {
        val code = numistaQr(paquillos)!!

        for (row in 0 until code.height) {
            val runs = code.qrRuns(row)
            assertEquals(
                (0 until code.width).filter { code.get(it, row) },
                runs.flatMap { it.toList() },
                "la fila $row no se dibuja como es",
            )
            assertTrue(
                runs.zipWithNext().all { (left, right) -> left.last + 1 < right.first },
                "dos tramos de la fila $row se tocan en vez de ser uno",
            )
        }
    }
}
