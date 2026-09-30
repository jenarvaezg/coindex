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

    /** The `versionCode` of the APK under test; any number does, as long as a bump is a bump. */
    private val version = 78

    private fun seed(
        dao: FakeTypeMetaDao,
        values: NamedValues = FakeNamedValues(),
        versionCode: Int = version,
    ) = TypeCacheSeed(dao, values, versionCode) { TypeCacheFile.read() }

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

        val added = seed(dao).topUp(curatedTypeIds.toSet()).added

        assertTrue(added > 0, "no se ha añadido ninguna ficha")
        assertTrue(
            dao.rows.value.map { it.typeId }.containsAll(escudosTypeIds),
            "siguen faltando fichas de los 1000 escudos",
        )
    }

    /**
     * The second launch of the same APK reads two cheap things and returns.
     *
     * The snapshot is 2,4 MB of JSON and it is parsed on the starts that have something to do. With
     * every curated type cached **and** this version's snapshot already written down, there is
     * nothing: the `cachedTypeIds` column and one integer out of a preferences file settle it.
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

    /**
     * A new APK writes its snapshot over the ficha that was there, which is the whole of #606.
     *
     * Until this, a corrected ficha had no route to the two phones that exist: the curator re-seeds
     * it, the asset travels in the APK, and `insertIfAbsent` ignored the conflict. The nine Peruvian
     * fichas of #603 would have needed nine gestures on a card nobody had reason to press.
     */
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
     * And a ficha the collector refreshed **after** the update stays his until the next one.
     *
     * The gesture of ADR 0025 is one type, one consulta, over a card where he has already seen the
     * error; a seed that undid it on the next launch would make it pointless. The version is what
     * protects it: this snapshot has been applied here, and it is not applied twice.
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

    /** A first install has nothing to overwrite, and says so rather than claiming 1.089 writes. */
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
     * Every type id the curated files name: catalogs, groupings and commemorative programmes.
     *
     * An announced member names none. Its `design_type_id` is not one either: that is the same
     * design in **another** variant, so seeding it here would fill the cell with a coin the
     * catalog does not claim.
     *
     * A programme's members count even where no catalog claims them (ADR 0022): the 25 escudos of
     * 1977 and 1983 are in no catalog and are exactly the coins «1 de 3» says are missing.
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

    /**
     * El vocabulario del chapado, fijado contra las fichas que de verdad se publican (#573).
     *
     * `inferFinish` lee el dorado de `composition.text` porque es el único campo que separa las
     * quince libras redondas doradas de las treinta y dos que no lo son —los títulos dicen «Silver
     * Proof» en las cuarenta y siete—, y una regla de agujas literales se pudre en silencio si
     * Numista cambia la redacción: se convertiría en un no-op y nadie se enteraría, que es la misma
     * lección que obligó a fijar aparte el vocabulario de `objectClassDeviations`.
     *
     * Así que la premisa se mide aquí: de las 1.089 fichas sembradas, cuatro nombran el oro en la
     * composición y cada una cae del lado que le toca. Un quinto tipo con oro que llegue con la
     * próxima siembra pone esto en rojo, y eso es lo que se quiere — que alguien mire si es una
     * moneda de oro o una dorada.
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
                // El Bitcoin de 2025 chapado, cuya casilla ya decía en prosa que el título calla.
                440_309 to Finish.Gilded,
                // Y las dos que están **hechas** de oro, que no son un acabado de nadie.
                304_649 to null,
                448_512 to null,
            ).sortedBy { it.first },
            byFinish,
        )
    }
}
