package com.jenarvaezg.coindex

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.SHIPPED_CURATION
import com.jenarvaezg.coindex.data.db.TypeMetaEntity
import com.jenarvaezg.coindex.data.numista.CollectedItemDto
import com.jenarvaezg.coindex.data.numista.NumistaTypeDto
import com.jenarvaezg.coindex.data.typeMetaEntity
import com.jenarvaezg.coindex.data.toDomain
import com.jenarvaezg.coindex.data.toEntity
import com.jenarvaezg.coindex.data.toImages
import com.jenarvaezg.coindex.domain.AssembledCollection
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionSnapshot
import com.jenarvaezg.coindex.domain.Curation
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.TypeMetaIndex
import com.jenarvaezg.coindex.domain.UnclassifiedItem
import com.jenarvaezg.coindex.domain.UnclassifiedReason
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.print.grid
import com.jenarvaezg.coindex.ui.print.notebookSections
import com.jenarvaezg.coindex.ui.print.pagesAlone
import com.jenarvaezg.coindex.ui.print.printGeometry
import com.jenarvaezg.coindex.ui.print.printPages
import com.jenarvaezg.coindex.ui.shelf.unclaimedFacts
import java.io.File
import kotlin.test.Test
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assume.assumeTrue

/**
 * Prints what the app would show for a real collection snapshot, through the shipped
 * [Curation.assemble] rather than a reimplementation of it (#217): a listing rebuilt elsewhere
 * reports orphans the app does not have.
 *
 * Inert without `COINDEX_FIELD_SNAPSHOT`, so the suite stays green and offline. Point it at a
 * directory holding a `collected_items.json` captured by `scripts/record-fixture.py --user-id`,
 * which refuses to write inside the repository.
 *
 *     COINDEX_FIELD_SNAPSHOT=/private/tmp/coindex-privado/padre \
 *     COINDEX_FIELD_TYPES=/private/tmp/coindex-privado/types \
 *       ./gradlew :app:testDebugUnitTest --tests '*FieldReportTest*' --rerun
 *
 * `--rerun` is required when switching collections: the environment variable is not a declared
 * task input, so Gradle would report `UP-TO-DATE` and leave the previous report in place (#66).
 *
 * `COINDEX_FIELD_TYPES` is optional and holds `type_<id>_es.json` captures for types the seeded
 * cache lacks; without them those pieces show as missing metadata, which a synced phone never does.
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
        val types = readTypeEntities(System.getenv(TYPES_VARIABLE))
        val typeMeta = types.associate { it.typeId to it.toDomain() }
        val curation = SHIPPED_CURATION

        // Sin base de datos no hay cajas propias: sólo ficheros curados e inventario.
        val collection = curation.assemble(CollectionSnapshot(items = items, typeMeta = typeMeta))
        val state = CollectionState(
            collection = collection,
            images = types.associate { it.typeId to it.toImages() },
            fichaFetchedAt = types.associate { it.typeId to it.fetchedAt },
        )

        println(header(directory, curation, collection))
        println(indexReport(collection))
        println(programmesReport(curation, items))
        println(notebookReport(state, curation))
        println(unclassifiedReport(collection.unclassified, typeMeta))
        println(unpublishedReport(items, typeMeta))
    }

    /**
     * Length of the printed notebook (#169) for this collection, card by card. It depends on what
     * the collector owns, since a card with no catalog prints its pieces rather than slots.
     */
    private fun notebookReport(state: CollectionState, curation: Curation): String = buildString {
        // La configuración por omisión (#228).
        val paper = printGeometry(NotebookOptions())
        val sections = notebookSections(state, state.index, emptyList(), curation, NotebookOptions())
        val pages = printPages(sections, paper)
        appendLine()
        appendLine("== CUADERNO IMPRESO: ${pages.size} PÁGINAS A4 (${sections.size} láminas) ==")
        appendLine("fotos que pediría: ${pages.sumOf { it.photographs }}")
        // Lo que añade encender «Sin colección» (#275): monedas fuera de toda lámina y sus páginas.
        val loose = unclaimedFacts(state).map { it.piece }
        val whole = printPages(
            notebookSections(state, state.index, loose, curation, NotebookOptions(unclaimed = true)),
            paper,
        )
        appendLine(
            "con «sin colección»: ${whole.size} páginas · ${loose.size} monedas sueltas " +
                "(+${whole.size - pages.size} páginas)",
        )
        for (section in sections.sortedByDescending { it.pagesAlone(paper) }) {
            val grid = section.grid(paper)
            appendLine(
                "· ${section.pagesAlone(paper)} pág | ${section.cells.size} casillas | " +
                    "Ø ${grid.diameterMm} mm | " +
                    "${grid.columns}×${grid.rows} | ${section.title}",
            )
        }
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

    /**
     * The shipped type cache plus any captures for what it misses, kept as rows because rows carry
     * the picture URLs the printed notebook counts.
     */
    private fun readTypeEntities(extraDirectory: String?): List<TypeMetaEntity> {
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
        return seeded + extra
    }

    private fun decode(typeId: Int, raw: JsonObject): TypeMetaEntity? {
        val dto = runCatching {
            json.decodeFromJsonElement(NumistaTypeDto.serializer(), raw)
        }.getOrNull() ?: return null
        return typeMetaEntity(typeId, dto, raw.toString(), 0L)
    }

    private fun header(
        directory: File,
        curation: Curation,
        collection: AssembledCollection,
    ): String = buildString {
        val items = collection.items
        appendLine("== INFORME DE CAMPO: ${directory.name} ==")
        appendLine("filas: ${items.size}")
        appendLine("piezas: ${items.sumOf { it.quantity }}")
        appendLine("tipos distintos: ${items.map { it.typeId }.distinct().size}")
        appendLine("fichas de tipo disponibles: ${collection.typeMeta.size}")
        appendLine(
            "catálogos: ${curation.catalogs.size} · " +
                "agrupaciones curadas: ${curation.groupings.size}",
        )
    }

    /**
     * Commemorative programmes (ADR 0022) produce no card, so their `owned / total` only shows in
     * the specification block of a plate that touches them, if any (#387).
     */
    private fun programmesReport(
        curation: Curation,
        items: List<CollectedItem>,
    ): String = buildString {
        appendLine()
        appendLine("== PROGRAMAS CONMEMORATIVOS (${curation.programmes.size}) ==")
        curation.programmes.forEach { programme ->
            val progress = programme.progress(items)
            appendLine("· ${programme.shortName} | ${progress.owned} de ${progress.total}")
        }
    }

    /**
     * The index in the phone's order (ADR 0021 §6), taken from [CollectionIndex] rather than
     * re-sorted here. Cards no curated file claims list their types, to start a curation from.
     */
    private fun indexReport(collection: AssembledCollection): String = buildString {
        val index = collection.index
        appendLine()
        appendLine("== ÍNDICE DE COLECCIONES (${index.size}) ==")
        for ((position, card) in index.withIndex()) {
            val derived = (card as? IndexCard.Derived)?.collection
            val ratio = card.coverage
                ?.let { "${it.owned}/${it.issued}" }
                ?: "sin lista de emisiones"
            appendLine(
                "${position + 1}. $ratio | ${card.name} | ${card.issuer ?: "—"} | " +
                    "${weightLabel(derived?.weightMillioz)} | " +
                    "${derived?.finish?.name?.lowercase() ?: "—"} | " +
                    "${card.distinctTypes} tipos, ${card.quantity} piezas",
            )
            if (card.coverage == null && derived != null) {
                val types = collection.itemsByKey[derived.key()].orEmpty()
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
            appendLine("    ${reasonLine(orphan.reason)}")
            appendLine(
                "    familia Numista: ${meta?.family ?: "ninguna"} · " +
                    "peso: ${meta?.weightOz ?: "ninguno"} oz",
            )
        }
    }

    /** Why a piece produced no collection; the app no longer says it anywhere (ADR 0021 §12). */
    private fun reasonLine(reason: UnclassifiedReason): String = when (reason) {
        UnclassifiedReason.MissingTypeMetadata ->
            "Ficha del tipo sin descargar: se completará en el próximo sincronizado."
        UnclassifiedReason.NoFamilyOrCatalog ->
            "Sin familia en Numista y sin catálogo curado que la referencie: candidata a catálogo."
        UnclassifiedReason.IssueNotClaimedByCatalog ->
            "Sin una emisión de Numista incluida en los catálogos curados de este tipo."
        UnclassifiedReason.UnpublishedType ->
            "Ficha aún sin publicar en Numista: hasta que un revisor la valide, sus datos no " +
                "forman colección."
        is UnclassifiedReason.UnknownWeight ->
            "«${reason.family}» sin peso en Numista: no se puede identificar la variante física."
    }

    /**
     * Types with no year at all: the offline trace of an unpublished Numista page, whose draft the
     * API serves as the contributor left it. Only a trace, since a published undated medal lands
     * here too.
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
                    // Una ficha a medias no forma tarjeta hasta publicarse y refrescarse (#186).
                    else -> "familia «$family» a medias: cae en «Sin clasificar» y no forma tarjeta"
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
