package com.jenarvaezg.coindex

import com.jenarvaezg.coindex.data.CatalogFiles
import com.jenarvaezg.coindex.data.GroupingFiles
import com.jenarvaezg.coindex.data.numista.CollectedItemDto
import com.jenarvaezg.coindex.data.numista.NumistaTypeDto
import com.jenarvaezg.coindex.data.seed.typeMetaEntity
import com.jenarvaezg.coindex.data.toDomain
import com.jenarvaezg.coindex.data.toEntity
import com.jenarvaezg.coindex.domain.CatalogSeeds
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionDerivation
import com.jenarvaezg.coindex.domain.CuratedGrouping
import com.jenarvaezg.coindex.domain.GroupingSeeds
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.TypeMetaIndex
import com.jenarvaezg.coindex.domain.UnclassifiedItem
import com.jenarvaezg.coindex.domain.buildCollectionCatalogAlbum
import com.jenarvaezg.coindex.domain.deriveCollection
import com.jenarvaezg.coindex.ui.unclassifiedReasonLabel
import java.io.File
import kotlin.test.Test
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assume.assumeTrue

/**
 * Prints what the app would show for a real collection snapshot, run through the shipped domain.
 *
 * A field session used to mean transcribing «Sin clasificar» card by card off a phone screen.
 * That answers slowly and, worse, approximately: only [deriveCollection] knows the family
 * precedence of ADR 0012 and ADR 0013, the weight normalization against curated weights, and
 * the inferred finish. A listing rebuilt by hand — or reimplemented in a script — reports
 * orphans the app does not have, which is the one error this project cannot afford.
 *
 * Inert without `COINDEX_FIELD_SNAPSHOT`, so the suite stays green and offline for everyone
 * else. Point it at a directory holding a `collected_items.json` captured by
 * `scripts/record-fixture.py --user-id`, which refuses to write one inside the repository: a
 * private collection never becomes a committed fixture.
 *
 *     COINDEX_FIELD_SNAPSHOT=/private/tmp/coindex-privado/padre \
 *     COINDEX_FIELD_TYPES=/private/tmp/coindex-privado/types \
 *       ./gradlew :app:testDebugUnitTest --tests '*FieldReportTest*' --rerun
 *
 * **`--rerun` no es opcional al cambiar de colección**: la variable de entorno no es una entrada
 * declarada de la tarea, así que cambiarla sola deja la tarea `UP-TO-DATE` y el XML anterior en
 * su sitio. Sin ella se lee el informe de una captura creyendo que es el de la otra — el mismo
 * falso verde de #66, con otra cara.
 *
 * `COINDEX_FIELD_TYPES` is optional and holds `type_<id>_es.json` captures for types the seeded
 * cache lacks. Leaving them out reports those pieces as missing metadata, which is a state a
 * phone only shows until it syncs — so it would invent orphans nobody has.
 *
 * Gradle swallows stdout, so read the report from the test result:
 *
 *     app/build/test-results/testDebugUnitTest/TEST-*FieldReportTest.xml
 */
class FieldReportTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `field report`() {
        val snapshot = System.getenv(SNAPSHOT_VARIABLE)
        assumeTrue("sin $SNAPSHOT_VARIABLE: informe de campo omitido", snapshot != null)
        val directory = File(checkNotNull(snapshot))

        val items = readItems(File(directory, "collected_items.json"))
        val typeMeta = readTypeMeta(System.getenv(TYPES_VARIABLE))
        val catalogs = CatalogSeeds.parseAll(CatalogFiles.all())
        val groupings = GroupingSeeds.parseAll(GroupingFiles.all())

        val derivation = deriveCollection(items, typeMeta, catalogs, groupings)

        println(header(directory, items, typeMeta, catalogs, groupings))
        println(derivedCollectionReport(derivation, catalogs, items))
        println(unclassifiedReport(derivation.unclassified, typeMeta))
        println(unpublishedReport(items, typeMeta))
    }

    /** The same two hops the sync makes: Numista DTO to row, row to domain. */
    private fun readItems(file: File): List<CollectedItem> {
        require(file.exists()) { "falta la captura ${file.absolutePath}" }
        val text = file.readText()
        val dtos = json.decodeFromString(CollectedItemsResponse.serializer(), text).items
        // The issue id lives only in the untouched body, exactly as on the phone.
        val raw = json.parseToJsonElement(text).jsonObject["items"] as? JsonArray
        return dtos.mapIndexedNotNull { index, dto ->
            dto.toEntity(raw?.getOrNull(index)?.toString() ?: "{}", 0L)?.toDomain()
        }
    }

    /** The shipped type cache, plus any deliberate captures for what it misses. */
    private fun readTypeMeta(extraDirectory: String?): TypeMetaIndex {
        val cache = json.parseToJsonElement(File(TYPE_CACHE).readText()).jsonObject
        val seeded = cache.entries.mapNotNull { (typeIdText, element) ->
            val raw = element as? JsonObject ?: return@mapNotNull null
            val typeId = typeIdText.toIntOrNull() ?: return@mapNotNull null
            decode(typeId, raw)
        }
        val extra = File(extraDirectory ?: "").listFiles().orEmpty()
            .filter { it.name.startsWith("type_") && it.name.endsWith(".json") }
            .mapNotNull { file ->
                val typeId = file.name.removePrefix("type_").substringBefore('_').toIntOrNull()
                val raw = json.parseToJsonElement(file.readText()).jsonObject
                typeId?.let { decode(it, raw) }
            }
        return (seeded + extra).associateBy { it.id }
    }

    private fun decode(typeId: Int, raw: JsonObject): TypeMeta? {
        val dto = runCatching {
            json.decodeFromJsonElement(NumistaTypeDto.serializer(), raw)
        }.getOrNull() ?: return null
        return typeMetaEntity(typeId, dto, raw.toString(), 0L).toDomain()
    }

    private fun header(
        directory: File,
        items: List<CollectedItem>,
        typeMeta: TypeMetaIndex,
        catalogs: List<CollectionCatalog>,
        groupings: List<CuratedGrouping>,
    ): String = buildString {
        appendLine("== INFORME DE CAMPO: ${directory.name} ==")
        appendLine("filas: ${items.size}")
        appendLine("piezas: ${items.sumOf { it.quantity }}")
        appendLine("tipos distintos: ${items.map { it.typeId }.distinct().size}")
        appendLine("fichas de tipo disponibles: ${typeMeta.size}")
        appendLine("catálogos: ${catalogs.size} · agrupaciones curadas: ${groupings.size}")
    }

    /**
     * Every card, saying whether a curated catalog claims it — and if none does, the types
     * it is made of, which is what a curation ticket needs to start from.
     */
    private fun derivedCollectionReport(
        derivation: CollectionDerivation,
        catalogs: List<CollectionCatalog>,
        allItems: List<CollectedItem>,
    ): String = buildString {
        val catalogsByKey = catalogs.associateBy { it.key() }
        appendLine()
        appendLine("== ÍNDICE DE COLECCIONES (${derivation.derivedCollections.size}) ==")
        for (collection in derivation.derivedCollections) {
            val key = collection.key()
            val catalog = catalogsByKey[key]
            val coverage = catalog?.let {
                val album = buildCollectionCatalogAlbum(it, allItems)
                "CATÁLOGO ${it.id} ${album.ownedMembers()}/${album.issuedMembers()}"
            } ?: "sin catálogo"
            appendLine(
                "· ${collection.family} | ${weightLabel(collection.weightMillioz)} | " +
                    "${collection.finish?.name?.lowercase() ?: "—"} | " +
                    "${collection.distinctTypes} tipos, " +
                    "${collection.quantity} piezas | $coverage",
            )
            if (catalog == null) {
                val types = derivation.itemsByKey[key].orEmpty()
                    .map { it.typeId }
                    .distinct()
                    .sorted()
                    .joinToString(", ") { "N#$it" }
                appendLine("    tipos: $types")
            }
        }
    }

    private fun unclassifiedReport(
        unclassified: List<UnclassifiedItem>,
        typeMeta: TypeMetaIndex,
    ): String = buildString {
        appendLine()
        appendLine("== SIN CLASIFICAR (${unclassified.size}) ==")
        for (orphan in unclassified) {
            val item = orphan.item
            val meta = typeMeta[item.typeId]
            val rows = if (orphan.rowCount > 1) " · ${orphan.rowCount} filas" else ""
            appendLine(
                "· N#${item.typeId} ${item.title ?: meta?.title ?: "?"} " +
                    "(${item.issuerCode ?: "?"}, ${item.recordedYear ?: "sin fecha"}) " +
                    "x${orphan.quantity}$rows",
            )
            appendLine("    ${unclassifiedReasonLabel(orphan.reason)}")
            appendLine(
                "    familia Numista: ${meta?.family ?: "ninguna"} · " +
                    "peso: ${meta?.weightOz ?: "ninguno"} oz",
            )
        }
    }

    /**
     * The types with no year at all, which is the offline trace of an unpublished Numista page.
     *
     * A referee has to publish a submission before it becomes publicly visible, and can also ask
     * for editing or delete it outright — but the API serves the draft meanwhile, with every field
     * exactly as the contributor left it. Measured over the two collections and the seeded cache,
     * a missing `min_year` picked out the three unpublished pages and nothing else. It is a trace,
     * not the state itself: an undated medal that Numista did publish lands here too, and the
     * answer for it is simply «published, dated nowhere».
     *
     * Worth its own section because the damage is invisible from the outside. A draft with no
     * family piles up in «Sin clasificar» like any orphan, but one with a half-typed family — the
     * `series: "The"` of N#596807 — becomes a card named after the typo, and only the collector
     * who owns that piece ever sees it.
     */
    private fun unpublishedReport(items: List<CollectedItem>, typeMeta: TypeMetaIndex): String =
        buildString {
            val undated = items
                .map { it.typeId }
                .distinct()
                .sorted()
                .mapNotNull { typeId -> typeMeta[typeId] }
                .filter { it.minYear == null && it.maxYear == null }
            appendLine()
            appendLine("== FICHAS SIN AÑO: POSIBLE PÁGINA SIN PUBLICAR (${undated.size}) ==")
            if (undated.isEmpty()) {
                appendLine("· ninguna")
                return@buildString
            }
            appendLine(
                "Comprueba cada una en numista.com: «This page has not been published yet» " +
                    "significa que no es verificable, así que no puede entrar en un catálogo " +
                    "ni en una agrupación, y merece su issue en el repo.",
            )
            for (meta in undated) {
                val family = meta.family
                val symptom = when {
                    family == null -> "sin familia: cae en «Sin clasificar» como cualquier huérfana"
                    else -> "familia «$family»: sale en una tarjeta con ese nombre"
                }
                appendLine("· N#${meta.id} ${meta.title ?: "?"}")
                appendLine("    $symptom")
            }
        }

    private fun weightLabel(weightMillioz: Int?): String =
        weightMillioz?.let { "${it / 1000.0} oz" } ?: "conjunto"

    private companion object {
        const val SNAPSHOT_VARIABLE = "COINDEX_FIELD_SNAPSHOT"
        const val TYPES_VARIABLE = "COINDEX_FIELD_TYPES"
        const val TYPE_CACHE = "../data/numista-type-cache.json"
    }
}

@Serializable
private data class CollectedItemsResponse(val items: List<CollectedItemDto> = emptyList())
