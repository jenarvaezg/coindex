package com.jenarvaezg.coindex.domain

/**
 * What one phone holds, and the only input the domain has: the inventory as last synced, the
 * fichas cached for it and the collector's own boxes (ADR 0021 §11). Everything a screen shows is
 * derived from these by [Curation.assemble]; nothing is stored per collection or per card (ADR
 * 0021 §7).
 */
data class CollectionSnapshot(
    val items: List<CollectedItem> = emptyList(),
    val typeMeta: TypeMetaIndex = emptyMap(),
    val ownGroupings: List<OwnGrouping> = emptyList(),
)

/**
 * Everything the domain derives from one snapshot. [items] and [typeMeta] are the snapshot's own
 * lists, carried along so every reader sees the same pieces behind each derived collection.
 */
data class AssembledCollection(
    val items: List<CollectedItem> = emptyList(),
    /**
     * The first level, as one list in one order (ADR 0021 §2, §6): curated catalogs, curated
     * groupings and the collector's own boxes, already sorted by the index comparator.
     */
    val index: List<IndexCard> = emptyList(),
    val derivedCollections: List<DerivedCollection> = emptyList(),
    val unclassified: List<UnclassifiedItem> = emptyList(),
    val typeMeta: TypeMetaIndex = emptyMap(),
    /**
     * The album of every curated catalog, built once against this inventory (#537), so a card's
     * ratio, its plate's casillas and the shelf window tile read the same album.
     */
    val albums: CatalogAlbums = CatalogAlbums(),
    /**
     * The commemorative programmes each catalog touches, with the collector's progress (#539).
     * Built once per assembly, since programme files are constant for the process (ADR 0022);
     * resolving them per plate made the notebook re-derive every programme per printed card.
     */
    val programmeStandings: CatalogProgrammes = CatalogProgrammes(),
    /** Catalogs the collector owns at least one official type of (plate reachability). */
    val evidencedCatalogIds: Set<String> = emptySet(),
    /**
     * Every measurable casilla of every evidenced plate, resolved once (#538). The country and year
     * axes group and sort these (ADR 0026 §9) instead of rebuilding them from the curated files.
     */
    val slots: List<AlbumSlot> = emptyList(),
    /** The pieces behind each derived collection, for the screen that opens one. */
    val itemsByKey: Map<VariantKey, List<CollectedItem>> = emptyMap(),
    /**
     * The emission label of each row whose year does not tell it apart, by row id (#225). By row,
     * not by type, because these rows share one type: the 100 pesetas of Franco all say 1966 and
     * only the star tells them apart.
     */
    val emissionLabels: Map<Long, String> = emptyMap(),
    /** The collector's own boxes, which hold only pieces they own (ADR 0021 §11). */
    val ownGroupings: List<OwnGroupingView> = emptyList(),
) {
    /**
     * Which collections claim which coin, resolved once per assembly (#540).
     *
     * Derived from [index] and [itemsByKey] rather than passed in, so no assembly, hand-built test
     * ones included, can carry claims that contradict its own index. Lazy because Coins asks for it
     * several times per read and it walks the whole inventory.
     */
    val claims: CoinClaims by lazy { coinClaimsOf(index, itemsByKey) }

    fun derivedCollectionFor(key: VariantKey): DerivedCollection? =
        derivedCollections.firstOrNull { it.key() == key }
}

/**
 * The curated files that ship with the app, tied together once.
 *
 * They are constant for the life of the process, so what depends only on them (the card names of
 * #22, the index comparator of ADR 0021 §6) is built here once. The collector's snapshot comes in
 * through [assemble], which the app's repository and the field report of #21 share, so a count has
 * one definition.
 *
 * Constructing a curation validates it (#545): rules that span two species are checked here. Files
 * come in through [load], and each side of the seam brings its own [CuratedFiles].
 */
class Curation(
    val catalogs: List<CollectionCatalog>,
    val groupings: List<CuratedGrouping> = emptyList(),
    /** Commemorative programmes (ADR 0022): a second reading, never a card and never a family. */
    val programmes: List<CommemorativeProgramme> = emptyList(),
) {
    init {
        requireDistinctShortNames(catalogs, groupings)
    }

    /** What each collection is called on a card (#22). */
    val titles: CollectionTitles = CollectionTitles(catalogs, groupings)

    /** The one list of the first level, built from the same constant seeds (ADR 0021 §6). */
    private val index: CollectionIndex = CollectionIndex(catalogs, groupings, titles)

    /** The catalogs keyed on Numista issues, which are the only ones that can name an emission. */
    private val issueRuns: List<CollectionCatalog> = catalogs.filter { it.isIssueRun }

    /** The single entry to the domain: one snapshot in, everything the screens read out. */
    fun assemble(snapshot: CollectionSnapshot): AssembledCollection {
        val items = snapshot.items
        val typeMeta = snapshot.typeMeta
        val derivation = deriveCollection(items, typeMeta, catalogs, groupings)
        val boxes = buildOwnGroupingViews(snapshot.ownGroupings, items)
        // Every catalog, not only those with a card: the shelf window shows catalogs the collector
        // owns nothing of.
        val albums = CatalogAlbums.over(catalogs, items)
        val evidencedCatalogIds = catalogs
            .filter { catalog -> catalog.isEvidencedBy(items) }
            .mapTo(mutableSetOf()) { it.id }
        return AssembledCollection(
            items = items,
            index = index.build(snapshot, derivation, boxes, albums),
            albums = albums,
            // Every catalog again: a shelf window plate also shows the collector's standing in a
            // programme, which belongs to the collector and not to that plate (ADR 0030, ADR 0022).
            programmeStandings = CatalogProgrammes.over(catalogs, programmes, items),
            derivedCollections = derivation.derivedCollections,
            unclassified = derivation.unclassified,
            typeMeta = typeMeta,
            evidencedCatalogIds = evidencedCatalogIds,
            slots = albumSlots(catalogs, albums, typeMeta, evidencedCatalogIds),
            itemsByKey = derivation.itemsByKey,
            ownGroupings = boxes,
            emissionLabels = emissionLabelsOf(items),
        )
    }

    /**
     * The emission label of each row an issue run matches (ADR 0019); other rows keep their year.
     * Two catalogs claiming one issue already fail in `CatalogSeeds.parseAll`, so this does not
     * arbitrate between them.
     */
    private fun emissionLabelsOf(items: List<CollectedItem>): Map<Long, String> {
        if (issueRuns.isEmpty()) return emptyMap()
        return buildMap {
            for (item in items) {
                issueRuns.firstNotNullOfOrNull { it.emissionLabelFor(item) }
                    ?.let { label -> put(item.id, label) }
            }
        }
    }

    /**
     * Every Numista type the curated files name: the set a plate can be asked to draw, and so the
     * set the type cache must hold.
     *
     * A non-issued member names none, and its `design_type_id` stays out: it is the design in
     * another variant and would seed the cell with the wrong coin. Programme members count (ADR
     * 0022), even those no catalog claims, because the plate shows the programme's missing coins.
     */
    fun curatedTypeIds(): Set<Int> = buildSet {
        catalogs.forEach { catalog ->
            catalog.members.forEach { member -> member.numistaTypeId?.let(::add) }
        }
        groupings.forEach { addAll(it.typeIds) }
        programmes.forEach { programme ->
            programme.members.forEach { member -> add(member.numistaTypeId) }
        }
    }

    companion object {
        /**
         * The one door curated files come through: parse each species, then validate the whole
         * (#545). It fails loudly: a silently dropped catalog reads on the phone as «me falta».
         */
        fun load(files: CuratedFiles): Curation = Curation(
            catalogs = CatalogSeeds.parseAll(files.readAll(CuratedSpecies.Catalogs)),
            groupings = GroupingSeeds.parseAll(files.readAll(CuratedSpecies.Groupings)),
            programmes = ProgrammeSeeds.parseAll(files.readAll(CuratedSpecies.Programmes)),
        )

        /**
         * One species, sorted by file name so asset and directory listings load the same curation.
         * No files is a build that lost a directory, and fails: both sides ship all three species.
         */
        private fun CuratedFiles.readAll(
            species: CuratedSpecies,
        ): List<Pair<String, String>> = read(species).sortedBy { (fileName, _) -> fileName }.ifEmpty {
            throw CatalogSeedException("no hay ficheros curados en `${species.directory}`")
        }
    }
}

/**
 * Rejects a `short_name` shared by a catalog and a curated grouping (#22). Each species checks its
 * own while parsing, but the index draws both side by side (#12). Programmes are not cards, so
 * they stay out (ADR 0022).
 */
private fun requireDistinctShortNames(
    catalogs: List<CollectionCatalog>,
    groupings: List<CuratedGrouping>,
) {
    val catalogNames = catalogs.associateBy { it.shortName }
    for (grouping in groupings) {
        val catalog = catalogNames[grouping.shortName] ?: continue
        throw CatalogSeedException(
            "`short_name` `${grouping.shortName}` is claimed by both catalog `${catalog.id}` " +
                "and grouping `${grouping.id}`",
        )
    }
}
