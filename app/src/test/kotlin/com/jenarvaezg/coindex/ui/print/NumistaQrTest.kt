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
 * The QR printed under a coin (#234): it decodes to the promised page, and every seeded type fits
 * the version the caption was sized for. Scanning a printed folio is not covered here.
 */
class NumistaQrTest {
    private val json = Json { ignoreUnknownKeys = true }

    /** Shared by the five paquillos (N#1885). */
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

    /** Decoded from the modules, not a bitmap, so it tests the payload and not the scaling. */
    @Test
    fun `the modules decode back to the numista page they promise`() {
        val code = numistaQr(paquillos)!!

        assertEquals(paquillos, Decoder().decode(code).text)
    }

    /**
     * The 12 mm caption was sized for 33 modules (25 plus the quiet zone). A version 3 would still
     * fit the square, but at 0,324 mm a module instead of 0,364.
     */
    @Test
    fun `every seeded type is a version two symbol of twenty-five modules`() {
        val withoutUrl = seeded.filter { it.numistaUrl == null }
        assertTrue(withoutUrl.isEmpty(), "fichas sin URL: ${withoutUrl.map { it.id }}")

        val versions = seeded.mapNotNull { numistaQr(it.numistaUrl)?.width }.distinct()

        assertEquals(listOf(25), versions, "no todos los tipos sembrados son versión 2")
        assertEquals(4, QR_QUIET_MODULES)
        assertEquals(33, numistaQr(paquillos)!!.qrModulesWithQuietZone)
    }

    /**
     * 32 bytes is what a version 2 holds at level L; the catalogue page (`.../pieces1885.html`) is
     * 49. The bound is that capacity, not today's longest URL, so a longer id breaks nothing.
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
     * Runs are drawn instead of modules to keep the PDF small. An off-by-one would leave a white
     * column that error correction mostly hides, so only some phones would fail to scan it.
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
