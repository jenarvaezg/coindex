package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.TypeMetaEntity
import com.jenarvaezg.coindex.data.numista.NumistaTypeDto
import com.jenarvaezg.coindex.data.seed.TypeCacheSeed
import com.jenarvaezg.coindex.domain.CatalogSeeds
import com.jenarvaezg.coindex.domain.GroupingSeeds
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The seeded cache is what lets a plate draw the designs the collector is **missing**: nobody
 * syncs a coin they do not own, so a type absent from the seed stays blank on a fresh install
 * until someone spends their own budget on it.
 *
 * That makes the seed a silent dependency of every curated file, and curating a new catalog
 * without extending it is the easy mistake — it cost three blank cards on the 500 escudos and
 * another sixteen on the 1000 escudos and Lunar III. This test is the check that was missing.
 */
class TypeCacheSeedTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun seed(dao: FakeTypeMetaDao) = TypeCacheSeed(dao) { TypeCacheFile.read() }

    /**
     * The snapshot used to be a **first-install** gift: it was only written into an empty cache,
     * so every catalog curated afterwards shipped its fichas in the asset and none of them ever
     * reached a phone that already had the app. A cached type is never re-fetched either, so the
     * missing fichas had no second route in and their cells stayed silhouettes for good — most
     * of the plate reported with 7 pictures out of 19 (issue #67).
     */
    @Test
    fun `a cache from an older release is topped up with the types curated since`() = runTest {
        val dao = FakeTypeMetaDao()
        seed(dao).topUp(curatedTypeIds.toSet())
        // The phone of a collector who installed before the 1000 escudos were curated.
        val stale = dao.rows.value.filterNot { it.typeId in escudosTypeIds }
        dao.rows.value = stale

        val added = seed(dao).topUp(curatedTypeIds.toSet())

        assertTrue(added > 0, "no se ha añadido ninguna ficha")
        assertTrue(
            dao.rows.value.map { it.typeId }.containsAll(escudosTypeIds),
            "siguen faltando fichas de los 1000 escudos",
        )
    }

    @Test
    fun `a cache that already has every curated type is left alone and never parses the asset`() =
        runTest {
            val dao = FakeTypeMetaDao()
            seed(dao).topUp(curatedTypeIds.toSet())
            val before = dao.rows.value

            val untouched = TypeCacheSeed(dao) { error("no debería leerse el snapshot") }

            assertEquals(0, untouched.topUp(curatedTypeIds.toSet()))
            assertEquals(before, dao.rows.value)
        }

    /** A ficha the collector paid API budget to sync outranks the one that ships in the APK. */
    @Test
    fun `topping up never overwrites a type that is already cached`() = runTest {
        val dao = FakeTypeMetaDao()
        val synced = TypeMetaEntity(
            typeId = escudosTypeIds.first(),
            title = "sincronizado",
            family = null,
            issuerCode = null,
            minYear = null,
            maxYear = null,
            weightGrams = null,
            obverseUrl = null,
            reverseUrl = null,
            raw = "{}",
            fetchedAt = 1,
        )
        dao.insertIfAbsent(synced)

        seed(dao).topUp(curatedTypeIds.toSet())

        assertEquals(synced, dao.rows.value.first { it.typeId == synced.typeId })
    }

    private val escudosTypeIds: List<Int> =
        CatalogSeeds.parseAll(CatalogFiles.all())
            .first { it.id == "portugal-1000-escudos-plata-500" }
            .members.mapNotNull { it.numistaTypeId }

    private val snapshot: Map<String, JsonObject> =
        json.parseToJsonElement(TypeCacheFile.read()).jsonObject
            .mapValues { (_, element) -> element.jsonObject }

    /**
     * Every type id the curated files name, catalogs and groupings alike.
     *
     * An announced member names none. Its `design_type_id` is not one either: that is the same
     * design in **another** variant, so seeding it here would fill the cell with a coin the
     * catalog does not claim.
     */
    private val curatedTypeIds: List<Int> =
        CatalogSeeds.parseAll(CatalogFiles.all())
            .flatMap { catalog -> catalog.members.mapNotNull { it.numistaTypeId } } +
            GroupingSeeds.parseAll(GroupingFiles.all()).flatMap { it.typeIds }

    @Test
    fun `the seed covers every type the curated files name`() {
        val absent = curatedTypeIds.distinct().filter { snapshot[it.toString()] == null }
        assertTrue(absent.isEmpty(), "tipos curados sin ficha en la caché sembrada: $absent")
    }

    /**
     * `TypeCacheSeed` drops a row it cannot decode without saying so, and a row with no picture
     * seeds a card as empty as no row at all — both fail exactly like the hole above.
     */
    @Test
    fun `every seeded type decodes into a card with two faces`() {
        val broken = curatedTypeIds.distinct().mapNotNull { typeId ->
            val raw = snapshot[typeId.toString()] ?: return@mapNotNull null
            val dto = runCatching { json.decodeFromJsonElement(NumistaTypeDto.serializer(), raw) }
                .getOrNull() ?: return@mapNotNull "$typeId: no decodifica"
            when {
                dto.id != typeId -> "$typeId: la ficha dice id ${dto.id}"
                dto.title.isNullOrBlank() -> "$typeId: sin título"
                dto.obverse?.picture == null && dto.obverse?.thumbnail == null ->
                    "$typeId: sin anverso"
                dto.reverse?.picture == null && dto.reverse?.thumbnail == null ->
                    "$typeId: sin reverso"
                else -> null
            }
        }
        assertEquals(emptyList(), broken)
    }
}
