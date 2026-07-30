package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.CollectedItemEntity
import com.jenarvaezg.coindex.data.db.TypeMetaEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject

/**
 * The issue id is read back out of the stored response, not out of a column.
 *
 * `SyncService` keeps the untouched JSON element of every row, so a piece synced long before this
 * feature existed already carries its issue — the same bargain that lets the finish be inferred
 * on read. If this ever regressed, an issue run would quietly report every star as missing.
 */
class MappersTest {
    private fun typeEntity(raw: String) = TypeMetaEntity(
        typeId = 404_044,
        title = null,
        family = null,
        issuerCode = "australie",
        minYear = null,
        maxYear = null,
        weightGrams = null,
        obverseUrl = null,
        reverseUrl = null,
        raw = raw,
        fetchedAt = 0,
    )

    @Test
    fun `the issuer's name comes from the cached type, not from a table of codes`() {
        // The cache row keeps the whole response, so the 608 seeded types already carry the
        // name Numista wrote in the collector's own language: `australie` is «Australia».
        assertEquals("Australia", typeEntity(Fixtures.type(404_044)).toDomain().issuerName)
    }

    @Test
    fun `a type with no issuer, or unreadable json, simply has no issuer name`() {
        assertNull(typeEntity("{}").toDomain().issuerName)
        assertNull(typeEntity("no es json").toDomain().issuerName)
        assertNull(typeEntity("""{"issuer": {"code": "australie"}}""").toDomain().issuerName)
        // The code is still the one stored in its column.
        assertEquals("australie", typeEntity("{}").toDomain().issuerCode)
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
