package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.numista.NumistaTypeDto
import com.jenarvaezg.coindex.domain.CatalogSeeds
import com.jenarvaezg.coindex.domain.GroupingSeeds
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

    private val snapshot: Map<String, JsonObject> =
        json.parseToJsonElement(TypeCacheFile.read()).jsonObject
            .mapValues { (_, element) -> element.jsonObject }

    /** Every type id the curated files name, catalogs and groupings alike. */
    private val curatedTypeIds: List<Int> =
        CatalogSeeds.parseAll(CatalogFiles.all())
            .flatMap { catalog -> catalog.members.map { it.numistaTypeId } } +
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
