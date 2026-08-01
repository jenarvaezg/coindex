package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.CollectedItemEntity
import com.jenarvaezg.coindex.data.db.TypeMetaEntity
import com.jenarvaezg.coindex.domain.Metal
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
    // Distinct fetchedAt per row: the mapper reads each response once and remembers the answer,
    // and two different responses for one type are only two rows if they were fetched apart.
    private fun typeEntity(raw: String, fetchedAt: Long = 0) = TypeMetaEntity(
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
        fetchedAt = fetchedAt,
    )

    @Test
    fun `the issuer's name comes from the cached type, not from a table of codes`() {
        // The cache row keeps the whole response, so the 608 seeded types already carry the
        // name Numista wrote in the collector's own language: `australie` is «Australia».
        assertEquals(
            "Australia",
            typeEntity(Fixtures.type(404_044), fetchedAt = 5).toDomain().issuerName,
        )
    }

    @Test
    fun `a type with no issuer, or unreadable json, simply has no issuer name`() {
        assertNull(typeEntity("{}", fetchedAt = 1).toDomain().issuerName)
        assertNull(typeEntity("no es json", fetchedAt = 2).toDomain().issuerName)
        assertNull(
            typeEntity("""{"issuer": {"code": "australie"}}""", fetchedAt = 3)
                .toDomain()
                .issuerName,
        )
        // The code is still the one stored in its column.
        assertEquals("australie", typeEntity("{}", fetchedAt = 4).toDomain().issuerCode)
    }

    /**
     * El metal se deriva en lectura de `composition.text`, que ya viaja dentro de `raw`: las 723
     * fichas sembradas lo tienen desde siempre, así que meterlo en la clave (#40) no costó ni una
     * migración de la caché ni una llamada de presupuesto.
     */
    @Test
    fun `the metal is read from the stored ficha, not from a column`() {
        assertEquals(
            Metal.Silver,
            typeEntity(Fixtures.type(404_044), fetchedAt = 10).toDomain().metal,
        )
        assertEquals(
            Metal.Gold,
            typeEntity("""{"composition": {"text": "Oro 999,9"}}""", fetchedAt = 11)
                .toDomain()
                .metal,
        )
    }

    @Test
    fun `a type with no composition, or unreadable json, simply has no metal`() {
        assertNull(typeEntity("{}", fetchedAt = 12).toDomain().metal)
        assertNull(typeEntity("no es json", fetchedAt = 13).toDomain().metal)
        assertNull(
            typeEntity("""{"composition": {}}""", fetchedAt = 14).toDomain().metal,
        )
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
