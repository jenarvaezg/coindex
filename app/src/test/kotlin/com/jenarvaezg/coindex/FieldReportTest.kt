package com.jenarvaezg.coindex

import com.jenarvaezg.coindex.data.CatalogFiles
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.GroupingFiles
import com.jenarvaezg.coindex.data.ProgrammeFiles
import com.jenarvaezg.coindex.data.db.TypeMetaEntity
import com.jenarvaezg.coindex.data.numista.CollectedItemDto
import com.jenarvaezg.coindex.data.numista.NumistaTypeDto
import com.jenarvaezg.coindex.data.seed.typeMetaEntity
import com.jenarvaezg.coindex.data.toDomain
import com.jenarvaezg.coindex.data.toEntity
import com.jenarvaezg.coindex.data.toImages
import com.jenarvaezg.coindex.domain.AssembledCollection
import com.jenarvaezg.coindex.domain.CatalogSeeds
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionSnapshot
import com.jenarvaezg.coindex.domain.Curation
import com.jenarvaezg.coindex.domain.GroupingSeeds
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.ProgrammeSeeds
import com.jenarvaezg.coindex.domain.TypeMetaIndex
import com.jenarvaezg.coindex.domain.UnclassifiedItem
import com.jenarvaezg.coindex.domain.UnclassifiedReason
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.print.grid
import com.jenarvaezg.coindex.ui.print.notebookSections
import com.jenarvaezg.coindex.ui.print.pages
import com.jenarvaezg.coindex.ui.print.printGeometry
import com.jenarvaezg.coindex.ui.print.printPages
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
 * That answers slowly and, worse, approximately: only [Curation.assemble] knows the family
 * precedence of ADR 0012 and ADR 0013, the weight normalization against curated weights, and
 * the inferred finish. A listing rebuilt by hand — or reimplemented in a script — reports
 * orphans the app does not have, which is the one error this project cannot afford.
 *
 * It calls that assembly rather than reproducing it (#217). This file used to rebuild the body of
 * `observeState()` line by line, so the report ordered its index with a second implementation of
 * the app's and nothing guaranteed the two stayed in step.
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
        val types = readTypeEntities(System.getenv(TYPES_VARIABLE))
        val typeMeta = types.associate { it.typeId to it.toDomain() }
        val curation = Curation(
            catalogs = CatalogSeeds.parseAll(CatalogFiles.all()),
            groupings = GroupingSeeds.parseAll(GroupingFiles.all()),
            programmes = ProgrammeSeeds.parseAll(ProgrammeFiles.all()),
        )

        // Sin base de datos no hay cajas propias: lo que el informe ordena es lo que sale de los
        // ficheros y del inventario, que es la mitad medible desde una captura. Lo demás es el
        // mismo ensamblaje que corre en el móvil, sin una segunda versión aquí.
        val collection = curation.assemble(CollectionSnapshot(items = items, typeMeta = typeMeta))
        val state = CollectionState(
            collection = collection,
            images = types.associate { it.typeId to it.toImages() },
            fichaFetchedAt = types.associate { it.typeId to it.fetchedAt },
        )

        println(header(directory, curation, collection))
        println(indexReport(collection))
        println(notebookReport(state, curation))
        println(unclassifiedReport(collection.unclassified, typeMeta))
        println(unpublishedReport(items, typeMeta))
    }

    /**
     * How long the printed notebook of #169 comes out for this collection, card by card.
     *
     * The only place the answer exists: the length of the notebook is not a property of `data/` —
     * it depends on which variants the collector owns, because a card with no catalog prints its
     * pieces instead of a hundred and twenty-one empty slots. Fifty-six catalogs would be a
     * hundred and one pages (`NotebookPagesTest`); a real collection is not the shelf.
     */
    private fun notebookReport(state: CollectionState, curation: Curation): String = buildString {
        // El cuaderno de hoy, que es el que la configuración por omisión produce (#228).
        val paper = printGeometry(NotebookOptions())
        val sections = notebookSections(state, state.index, curation, NotebookOptions())
        val pages = printPages(sections, paper)
        appendLine()
        appendLine("== CUADERNO IMPRESO: ${pages.size} PÁGINAS A4 (${sections.size} láminas) ==")
        appendLine("fotos que pediría: ${pages.sumOf { it.photographs }}")
        for (section in sections.sortedByDescending { it.pages(paper) }) {
            val grid = section.grid(paper)
            appendLine(
                "· ${section.pages(paper)} pág | ${section.cells.size} casillas | " +
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
     * The shipped type cache, plus any deliberate captures for what it misses.
     *
     * Kept as rows rather than as domain fichas because a row is what the phone has: it carries the
     * picture URLs the printed notebook counts, and it is the same `toDomain()` the app calls that
     * turns it into what the derivation reasons about.
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
     * The index **in the order the phone shows it** (ADR 0021 §6), each card saying its ratio — and
     * where no curated file claims it, the types it is made of, which is what a curation ticket
     * needs to start from.
     *
     * Printed through [CollectionIndex] rather than in derivation order on purpose: the comparator
     * is the thing under test here, and reimplementing the sort in the report would report an order
     * nobody's phone has. It is also the only place the whole order is measurable at once — 58 cards
     * against a real inventory, where the emulator shows the first two.
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

    /**
     * Why a piece produced no collection — and this is now the **only** place it is said.
     *
     * ADR 0021 §12 took the four reasons out of the app: «nothing is discarded silently» became
     * «nothing is discarded» once a coin had a hierarchy of its own, so the «Sin colección» chip of
     * Coins answers *which* and the *why* migrated here, which is where the curator already looks.
     * The wording came from `unclassifiedReasonLabel`, which had no reader left on screen.
     */
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
                    // Desde #186 una ficha a medias ya no inventa tarjeta: la pieza espera en el
                    // residuo hasta que se publique y su ficha se refresque.
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
