package com.jenarvaezg.coindex.domain

/**
 * What one phone holds right now, and the only input the domain has.
 *
 * Three lists and nothing else: the inventory as it was last synced, the fichas cached for it, and
 * the boxes the collector typed (ADR 0021 §11). Everything a screen shows is derived from these by
 * [Curation.assemble] — collections are never stored, and since ADR 0021 §7 nothing at all is
 * stored per card.
 */
data class CollectionSnapshot(
    val items: List<CollectedItem> = emptyList(),
    val typeMeta: TypeMetaIndex = emptyMap(),
    val ownGroupings: List<OwnGrouping> = emptyList(),
)

/**
 * Everything the domain derives from one snapshot, assembled in one place.
 *
 * The snapshot travels through with it — [items] and [typeMeta] are the same lists that went in —
 * because a derived collection is a summary, and every reader that opens one needs the pieces
 * behind it. Two readings of one inventory that disagreed on how many pieces there are would be
 * two truths, which is exactly what having a single assembly prevents.
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
     * The album of every curated catalog, built once against this inventory (#537).
     *
     * It travels with the assembly for the reason [items] does, and one step further: a card's ratio,
     * the casillas of its plate and the tile of the shelf window are not three readings that have to
     * agree about one collection, they are one album read three times.
     */
    val albums: CatalogAlbums = CatalogAlbums(),
    /**
     * The commemorative programmes each catalog touches, with the collector's progress (#539).
     *
     * Here for the reason [albums] is, and it is the same sentence one reading further on: a plate
     * used to resolve its programmes on every read, which meant the notebook re-derived thirteen
     * programmes against the whole inventory once per printed card. Nothing it is made of changes
     * between two reads of one snapshot — the programme files are constant for the life of the
     * process (ADR 0022) — so the reading belongs to the assembly and not to whoever opens a plate.
     */
    val programmeStandings: CatalogProgrammes = CatalogProgrammes(),
    /** Catalogs the collector owns at least one official type of (plate reachability). */
    val evidencedCatalogIds: Set<String> = emptySet(),
    /** The pieces behind each derived collection, for the screen that opens one. */
    val itemsByKey: Map<VariantKey, List<CollectedItem>> = emptyMap(),
    /**
     * What to call each piece whose year names nothing, by the **id of the row** (#225).
     *
     * By row and not by type, because the rows this answers for are rows of one type: the 100
     * pesetas of Franco all say 1966 and Numista 1885, and the star is the only thing left that
     * tells them apart. Resolved here once, so whoever draws a piece is handed the label with it
     * instead of having to know that some collections have one to ask for.
     */
    val emissionLabels: Map<Long, String> = emptyMap(),
    /** The collector's own boxes, which hold only pieces they own (ADR 0021 §11). */
    val ownGroupings: List<OwnGroupingView> = emptyList(),
) {
    fun derivedCollectionFor(key: VariantKey): DerivedCollection? =
        derivedCollections.firstOrNull { it.key() == key }
}

/**
 * The curated files that travel with the app, tied together once.
 *
 * The three species are constant for the lifetime of the process, so what is built from them alone
 * — the card names of #22 and the index comparator of ADR 0021 §6 — is built once here rather than
 * re-derived per read. What varies is the collector's snapshot, and [assemble] is the one door it
 * comes through: the app's repository and the field report of #21 read the same assembly, so a
 * count can no longer have two definitions depending on who asked.
 */
class Curation(
    val catalogs: List<CollectionCatalog>,
    val groupings: List<CuratedGrouping> = emptyList(),
    /** Commemorative programmes (ADR 0022): a second reading, never a card and never a family. */
    val programmes: List<CommemorativeProgramme> = emptyList(),
) {
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
        // Every catalog and not only the ones with a card: the shelf window is made of the catalogs
        // this collector owns nothing of, and it reads its albums from here like everyone else.
        val albums = CatalogAlbums.over(catalogs, items)
        return AssembledCollection(
            items = items,
            index = index.build(snapshot, derivation, boxes, albums),
            albums = albums,
            // Every catalog again, and for a second reason on top of the shelf window's: a plate of
            // the twenty carries the collector's standing in a programme too, because their progress
            // in it is theirs and not that plate's (ADR 0030, ADR 0022).
            programmeStandings = CatalogProgrammes.over(catalogs, programmes, items),
            derivedCollections = derivation.derivedCollections,
            unclassified = derivation.unclassified,
            typeMeta = typeMeta,
            evidencedCatalogIds = catalogs
                .filter { catalog -> catalog.isEvidencedBy(items) }
                .mapTo(mutableSetOf()) { it.id },
            itemsByKey = derivation.itemsByKey,
            ownGroupings = boxes,
            emissionLabels = emissionLabelsOf(items),
        )
    }

    /**
     * The emission each row belongs to, where a catalog keyed on issues names one (ADR 0019).
     *
     * Only an issue run has anything to add, and a row it does not match keeps its year: this is a
     * map of the few rows that need one, not of every piece. Two catalogs claiming the same issue
     * is a curation error `CatalogSeeds.parseAll` refuses to load at all, so the shipped shelf can
     * never have one to arbitrate between — and arbitrating here, on a shelf assembled by hand in a
     * test, would be a second rule about which catalog owns an issue.
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
     * Every Numista type the curated files name, which is exactly the set a plate can be asked
     * to draw and therefore the set the type cache has to hold.
     *
     * An announced member names none, and its `design_type_id` is not one either: that is the
     * design in another variant, and putting it here would seed the cell with the wrong coin.
     *
     * A programme's members count too (ADR 0022), including the ones no catalog claims: the plate
     * names the programme beside its rows, and the 25 escudos of 1977 and 1983 are exactly the
     * coins a collector with «1 de 3» is missing.
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
}
