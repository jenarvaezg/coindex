package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.CollectedItemEntity
import com.jenarvaezg.coindex.data.ficha.FICHA_READING
import com.jenarvaezg.coindex.data.numista.NumistaTypeDto
import com.jenarvaezg.coindex.domain.Metal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject

/**
 * A mapper is a mapper again: the type cache row is read column by column, and only the collected
 * item still reaches into a stored response.
 *
 * There is no `fetchedAt` in this file any more. The five fields the ficha used to be parsed for on
 * every read were memoized behind `(typeId, fetchedAt)`, so two assertions about one type
 * contaminated each other unless each invented a moment of its own — nothing in the interface said
 * so, it was discovered by the failure (#221). What the body says is now `FichaTest`'s subject and
 * a column by the time it gets here.
 */
class MappersTest {
    private val lenient = Json { ignoreUnknownKeys = true }

    /** The row exactly as the sync, the refresh and the seed write it: through the one mapper. */
    private fun typeEntity(raw: String) = typeMetaEntity(
        typeId = 404_044,
        dto = runCatching { lenient.decodeFromString(NumistaTypeDto.serializer(), raw) }
            .getOrDefault(NumistaTypeDto()),
        raw = raw,
        fetchedAt = 0,
    )

    @Test
    fun `the ficha's own fields travel from the body to the columns to the card`() {
        // The cache row keeps the whole response, so the 608 seeded types already carry the
        // name Numista wrote in the collector's own language: `australie` is «Australia».
        val meta = typeEntity(Fixtures.type(404_044)).toDomain()

        assertEquals("Australia", meta.issuerName)
        assertEquals("australie", meta.issuerCode)
        assertEquals("coin", meta.category)
        assertEquals(32.6, meta.sizeMillimetres)
        assertEquals("https://es.numista.com/404044", meta.numistaUrl)
    }

    @Test
    fun `a row is stamped with the reading that wrote it`() {
        assertEquals(FICHA_READING, typeEntity(Fixtures.type(404_044)).readVersion)
    }

    /**
     * El metal se deriva **en lectura** de la prosa de `composition.text`, que ahora es columna:
     * lo que se guarda es lo que Numista escribió, nunca el veredicto de `inferMetal`, así que una
     * regla mejor sigue arreglando las fichas cacheadas hace meses sin gastar una llamada.
     */
    @Test
    fun `the metal is inferred from the stored prose, never stored itself`() {
        assertEquals(Metal.Silver, typeEntity(Fixtures.type(404_044)).toDomain().metal)
        assertEquals(
            Metal.Gold,
            typeEntity("""{"composition": {"text": "Oro 999,9"}}""").toDomain().metal,
        )
    }

    @Test
    fun `a type with nothing in its body simply has none of it`() {
        listOf("{}", "no es json", """{"composition": {}, "issuer": {"code": "australie"}}""")
            .map { typeEntity(it).toDomain() }
            .forEach { meta ->
                assertNull(meta.metal)
                assertNull(meta.issuerName)
                assertNull(meta.category)
                assertNull(meta.sizeMillimetres)
                assertNull(meta.numistaUrl)
            }
        // The code is still the one stored in its column.
        assertEquals("australie", typeEntity("""{"issuer": {"code": "australie"}}""").issuerCode)
    }

    private fun entity(raw: String) = CollectedItemEntity(
        id = 1,
        typeId = 1_885,
        quantity = 1,
        title = null,
        issuerCode = null,
        issueYear = 1966,
        gregorianYear = 1966,
        grade = null,
        price = null,
        forSwap = null,
        collectionName = null,
        raw = raw,
        syncedAt = 0,
    )

    @Test
    fun `the issue id comes from the recorded response`() {
        // The exact shape of a row, from the committed fixture rather than from memory.
        val firstItem = Json.parseToJsonElement(Fixtures.collectedItems)
            .jsonObject.getValue("items")
            .let { items -> (items as JsonArray)[0] }
            .toString()

        val item = entity(firstItem).toDomain()

        // The issue of the fixture's first row, read straight from its `raw`.
        assertEquals(63_444, item.issueId)
    }

    @Test
    fun `a row without an issue, or with unreadable json, simply has no issue`() {
        // The second fixture item is a piece recorded against no issue at all.
        val second = Json.parseToJsonElement(Fixtures.collectedItems)
            .jsonObject.getValue("items")
            .let { items -> (items as JsonArray)[1] }
            .toString()

        assertNull(entity(second).toDomain().issueId)
        assertNull(entity("{}").toDomain().issueId)
        assertNull(entity("no es json").toDomain().issueId)
        assertNull(entity("""{"issue": {"year": 1966}}""").toDomain().issueId)
        // A piece is still a piece without one.
        assertEquals(1_885, entity("{}").toDomain().typeId)
    }
}
