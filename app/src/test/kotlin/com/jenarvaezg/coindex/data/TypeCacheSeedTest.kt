package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.TypeMetaEntity
import com.jenarvaezg.coindex.data.numista.NumistaTypeDto
import com.jenarvaezg.coindex.data.seed.SeedReport
import com.jenarvaezg.coindex.data.seed.TypeCacheSeed
import com.jenarvaezg.coindex.domain.Finish
import com.jenarvaezg.coindex.domain.inferFinish
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The seeded cache is how a plate draws the designs the collector is missing: nobody syncs a coin
 * they don't own, so a curated type absent from the seed stays blank on a fresh install. Curating a
 * catalog without extending the seed is the easy mistake this class catches.
 */
class TypeCacheSeedTest {
    private val json = Json { ignoreUnknownKeys = true }

    /** The APK's `versionCode`: any value works, only a bump matters. */
    private val version = 78

    private fun seed(
        dao: FakeTypeMetaDao,
        values: NamedValues = FakeNamedValues(),
        versionCode: Int = version,
    ) = TypeCacheSeed(dao, values, versionCode) { TypeCacheFile.read() }

    /** A cached type is never re-fetched: new curated fichas only arrive with the seed (#67). */
    @Test
    fun `a cache from an older release is topped up with the types curated since`() = runTest {
        val dao = FakeTypeMetaDao()
        seed(dao).topUp(curatedTypeIds.toSet())
        // The phone of a collector who installed before the 1000 escudos were curated.
        val stale = dao.rows.value.filterNot { it.typeId in escudosTypeIds }
        dao.rows.value = stale

        val added = seed(dao).topUp(curatedTypeIds.toSet()).added

        assertTrue(added > 0, "no se ha añadido ninguna ficha")
        assertTrue(
            dao.rows.value.map { it.typeId }.containsAll(escudosTypeIds),
            "siguen faltando fichas de los 1000 escudos",
        )
    }

    /**
     * The snapshot is megabytes of JSON: a repeat launch of the same APK settles on the
     * `cachedTypeIds` column and one stored integer.
     */
    @Test
    fun `a cache that already has every curated type is left alone and never parses the asset`() =
        runTest {
            val dao = FakeTypeMetaDao()
            val values = FakeNamedValues()
            seed(dao, values).topUp(curatedTypeIds.toSet())
            val before = dao.rows.value

            val untouched = TypeCacheSeed(dao, values, version) { error("no debería leerse el snapshot") }

            assertEquals(SeedReport(added = 0, overwritten = 0), untouched.topUp(curatedTypeIds.toSet()))
            assertEquals(before, dao.rows.value)
        }

    /** How a ficha corrected in the seed reaches phones that already cached it (#606). */
    @Test
    fun `a new version writes its snapshot over the ficha that was cached`() = runTest {
        val dao = FakeTypeMetaDao()
        val values = FakeNamedValues()
        seed(dao, values).topUp(curatedTypeIds.toSet())
        val typeId = escudosTypeIds.first()
        dao.overwrite(stale(typeId))

        val report = seed(dao, values, versionCode = version + 1).topUp(curatedTypeIds.toSet())

        val written = dao.rows.value.first { it.typeId == typeId }
        assertEquals(snapshot[typeId.toString()]?.get("title")?.jsonPrimitive?.contentOrNull, written.title)
        assertTrue(report.overwritten > 0, "la siembra nueva no ha pisado nada")
        assertEquals(0, report.added, "y no ha añadido nada, porque no faltaba nada")
    }

    /**
     * The applied version is recorded, so relaunching the same APK doesn't undo the refresh gesture
     * of ADR 0025.
     */
    @Test
    fun `a ficha refreshed after the update survives until the next one`() = runTest {
        val dao = FakeTypeMetaDao()
        val values = FakeNamedValues()
        seed(dao, values).topUp(curatedTypeIds.toSet())
        val refreshed = stale(escudosTypeIds.first())
        dao.overwrite(refreshed)

        seed(dao, values).topUp(curatedTypeIds.toSet())

        assertEquals(refreshed, dao.rows.value.first { it.typeId == refreshed.typeId })
    }

    @Test
    fun `the first install only adds`() = runTest {
        val report = seed(FakeTypeMetaDao()).topUp(curatedTypeIds.toSet())

        assertTrue(report.added > 0)
        assertEquals(0, report.overwritten)
    }

    private fun stale(typeId: Int) = TypeMetaEntity(
        typeId = typeId,
        title = "lo que decía la ficha vieja",
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

    private val escudosTypeIds: List<Int> =
        SHIPPED_CURATION.catalogs
            .first { it.id == "portugal-1000-escudos-plata-500" }
            .members.mapNotNull { it.numistaTypeId }

    private val snapshot: Map<String, JsonObject> =
        json.parseToJsonElement(TypeCacheFile.read()).jsonObject
            .mapValues { (_, element) -> element.jsonObject }

    /**
     * Every type id the curated files name: catalogs, groupings and commemorative programmes, the
     * latter even where no catalog claims them (ADR 0022). An announced member's `design_type_id`
     * is left out: it is another variant, and seeding it would fill the cell with the wrong coin.
     */
    private val curatedTypeIds: List<Int> =
        SHIPPED_CURATION.catalogs
            .flatMap { catalog -> catalog.members.mapNotNull { it.numistaTypeId } } +
            SHIPPED_CURATION.groupings.flatMap { it.typeIds } +
            SHIPPED_CURATION.programmes
                .flatMap { programme -> programme.members.map { it.numistaTypeId } }

    @Test
    fun `the seed covers every type the curated files name`() {
        val absent = curatedTypeIds.distinct().filter { snapshot[it.toString()] == null }
        assertTrue(absent.isEmpty(), "tipos curados sin ficha en la caché sembrada: $absent")
    }

    /**
     * `TypeCacheSeed` silently drops a row it cannot decode, and a row with no picture leaves the
     * card as blank as a missing row.
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

    /**
     * `inferFinish` lee el dorado de `composition.text`, el único campo que separa una libra dorada
     * de otra que no lo es (#573), y una regla de cadenas literales deja de funcionar en silencio
     * si Numista cambia la redacción. Un tipo nuevo con oro en la siembra pone esto en rojo: hay
     * que mirar si es una moneda de oro o una dorada.
     */
    @Test
    fun `every seeded composition that names gold is read as the alloy or the coating it is`() {
        val goldWords = listOf("oro", "gold")
        val byFinish = snapshot.entries
            .mapNotNull { (id, ficha) ->
                val body = ficha.jsonObject
                val composition = body["composition"]?.jsonObject?.get("text")
                    ?.jsonPrimitive?.contentOrNull
                    ?: return@mapNotNull null
                if (goldWords.none { composition.lowercase().contains(it) }) return@mapNotNull null
                val finish = inferFinish(
                    body["title"]?.jsonPrimitive?.contentOrNull,
                    body["series"]?.jsonPrimitive?.contentOrNull,
                    composition,
                )
                id.toInt() to finish
            }
            .sortedBy { it.first }

        assertEquals(
            listOf(
                // La onza de koala con el detalle resaltado en oro de 24 quilates.
                42_672 to Finish.Gilded,
                // El Bitcoin de 2025 chapado, aunque el título no lo diga.
                440_309 to Finish.Gilded,
                // Las dos acuñadas en oro: la aleación no es un acabado.
                304_649 to null,
                448_512 to null,
            ).sortedBy { it.first },
            byFinish,
        )
    }
}
