package com.jenarvaezg.coindex.domain

import java.text.Collator
import java.util.Locale

/**
 * Issued members owned over issued members catalogued (ADR 0021 §6). Nothing is stored per card
 * (ADR 0021 §7), so this ratio alone tells a collection in progress from a finished one. The
 * denominator is what the app can measure ([CollectionCatalogAlbum.issuedMembers]): announced and
 * unlisted members never count against the collector.
 */
data class CoverageRatio(val owned: Int, val issued: Int) {
    init {
        require(issued > 0) { "a coverage ratio needs a measurable denominator, got $issued" }
        require(owned in 0..issued) { "owned $owned is not inside 0..$issued" }
    }

    val value: Double get() = owned.toDouble() / issued

    val missing: Int get() = issued - owned

    /**
     * Every measurable member owned. Not called complete: an open series claims no completeness
     * (ADR 0020), and next year the same catalog may say 22/23.
     */
    val nothingMissing: Boolean get() = owned == issued
}

/** The owned coin shown inside one index card's die-cut hole. */
data class IndexCover(
    val typeId: Int,
    val printedSide: PrintedSide,
)

/**
 * One card of the index: every species of collection in one list, sorted by one comparator (ADR
 * 0021 §2). No section or provenance label tells them apart; what a card does depends only on
 * whether it has an issue list, that is, a [coverage] (ADR 0021 §3).
 */
sealed interface IndexCard {
    /** The card-sized name: the curated `short_name`, or Numista's raw family verbatim (§4). */
    val name: String

    /** Null when the collection has no issue list, and therefore no ratio to offer. */
    val coverage: CoverageRatio?

    /** The country, or null when it cannot be named with certainty (see [Issuers]). */
    val issuer: String?

    val distinctTypes: Int

    val quantity: Int

    /** The first owned emission in album order, on the face the album prints. */
    val cover: IndexCover?

    /** A collection derived from the pieces the collector owns right now (ADR 0007). */
    data class Derived(
        override val name: String,
        override val coverage: CoverageRatio?,
        override val issuer: String?,
        val collection: DerivedCollection,
        /**
         * The catalog whose plate this card can open right now, or null when there is none to
         * open: the same conditions `resolvePlate` applies, so a dead action is never drawn.
         */
        val plateCatalogId: String?,
        override val cover: IndexCover? = null,
        /**
         * Whether the catalog behind this card declares its series still issued (ADR 0020), or
         * null where no catalog names the collection. Never printed on the card (ADR 0021 §3);
         * only the index shelf reads it.
         */
        val seriesStatus: SeriesStatus? = null,
    ) : IndexCard {
        override val distinctTypes: Int get() = collection.distinctTypes
        override val quantity: Int get() = collection.quantity
        val key: VariantKey get() = collection.key()
    }

    /**
     * A box the collector enumerated by hand (ADR 0021 §2, §11). It holds only owned pieces, so it
     * has no gaps and no ratio, and sorts among the no-ratio cards.
     */
    data class Box(
        override val name: String,
        override val issuer: String?,
        val box: OwnGroupingView,
        override val cover: IndexCover? = null,
    ) : IndexCard {
        override val coverage: CoverageRatio? get() = null
        override val distinctTypes: Int get() = box.distinctTypes
        override val quantity: Int get() = box.quantity
    }
}

/**
 * The one order of the whole first level: `(has ratio ↓, ratio ↓, denominator ↓, name ↑)` (ADR
 * 0021 §6). A ratio and a piece count are incomparable, so `has ratio` comes first, as a sort level
 * rather than a headed block (ADR 0021 §7). The denominator puts `22/22` before `2/2`; the name
 * breaks ties.
 *
 * A function rather than a constant because a [Collator] is not thread safe.
 */
internal fun indexOrder(): Comparator<IndexCard> {
    val names = cardNameOrder()
    return compareByDescending<IndexCard> { it.coverage != null }
        .thenByDescending { it.coverage?.value ?: 0.0 }
        .thenByDescending { it.coverage?.issued ?: 0 }
        .thenBy(names) { it.name }
}

/**
 * Spanish alphabetical order, so «Álbum» sorts at the head rather than after `Z` as raw UTF-16
 * would. Card names are in Spanish by rule (ADR 0021 §4).
 */
private fun cardNameOrder(): Comparator<String> {
    val collator = Collator.getInstance(Locale.forLanguageTag("es"))
    return Comparator { left, right -> collator.compare(left, right) }
}

/**
 * Builds the single list of the first level from the seeds that ship with the app. Built once per
 * process, like [CollectionTitles]; only [build] sees the collector's inventory.
 */
class CollectionIndex(
    catalogs: List<CollectionCatalog>,
    groupings: List<CuratedGrouping>,
    private val titles: CollectionTitles,
) {
    private val catalogsByKey: Map<VariantKey, CollectionCatalog> =
        catalogs.associateBy { it.key() }

    /** The country a curated grouping declares; a catalog carries its own in [catalogsByKey]. */
    private val groupingIssuers: Map<String, String> =
        groupings.associate { grouping -> grouping.family to grouping.issuerCode }

    /**
     * Takes the whole snapshot rather than its `items` and `typeMeta` again (#217), so the index
     * and the derivation cannot be handed different inventories.
     */
    fun build(
        snapshot: CollectionSnapshot,
        derivation: CollectionDerivation,
        boxes: List<OwnGroupingView>,
        /**
         * The assembly's albums, where a card's ratio comes from (#537); handed in so the card and
         * its plate divide by the same album.
         */
        albums: CatalogAlbums,
    ): List<IndexCard> {
        val items = snapshot.items
        val issuers = Issuers(snapshot.typeMeta)
        // Every name at once, because two cards reading alike is a fact about the pair (#565).
        val names = titles.of(derivation.derivedCollections.map { it.key() })
        val cards = derivation.derivedCollections.map { collection ->
            val key = collection.key()
            val catalog = catalogsByKey[key]
            val album = catalog?.let { albums[it] }
            IndexCard.Derived(
                name = names.getValue(key),
                coverage = album?.coverage(),
                issuer = issuers.of(
                    declaredCode = declaredIssuerCode(key),
                    pieces = derivation.itemsByKey[key].orEmpty(),
                ),
                collection = collection,
                plateCatalogId = catalog?.takeIf { it.isEvidencedBy(items) }?.id,
                cover = if (catalog == null) {
                    derivation.itemsByKey[key].orEmpty().firstOwnedCover()
                } else {
                    album?.firstOwnedCover(catalog.printedSide)
                },
                seriesStatus = catalog?.seriesStatus,
            )
        } + boxes.map { box ->
            IndexCard.Box(
                name = box.name,
                issuer = issuers.of(declaredCode = null, pieces = box.items),
                box = box,
                cover = box.items.firstOwnedCover(),
            )
        }
        return cards.sortedWith(indexOrder())
    }

    /**
     * The country the curated file declares, or null for cards no file names. Looked up by the same
     * keys [CollectionTitles] uses: a catalog by its whole variant key, a grouping by family (ADR
     * 0013).
     */
    private fun declaredIssuerCode(key: VariantKey): String? =
        catalogsByKey[key]?.issuerCode ?: groupingIssuers[key.family]
}

/**
 * The coin a card shows: the first emission the collector owns, on the face the album prints. The
 * plate reads the same [firstOwnedIndex], so the coin that leaves the card lands in its casilla
 * (ADR 0026 §3).
 */
private fun CollectionCatalogAlbum.firstOwnedCover(printedSide: PrintedSide): IndexCover? {
    val member = firstOwnedIndex()?.let { members[it] } ?: return null
    val owned = member.status as? CollectionCatalogMemberStatus.Owned ?: return null
    return owned.items.firstOrNull()?.let { item -> IndexCover(item.typeId, printedSide) }
}

private fun List<CollectedItem>.firstOwnedCover(): IndexCover? =
    firstOrNull { it.quantity > 0 }?.let { item ->
        IndexCover(item.typeId, PrintedSide.Reverse)
    }

/**
 * Who issued a collection, for the eyebrow of its card.
 *
 * A curated file's `issuer_code` wins (ADR 0021 §9), so a curated card names its country even when
 * its pieces' types are not cached. Without a file the pieces decide: two issuers, or one unknown,
 * leave the eyebrow empty, which is why unknowns stay in the list.
 *
 * Open gap (#170): this still reads the catalog header, so a card whose only piece is a Niue
 * Equilibrium says «Tokelau». `CollectionCatalog.issuerCodes()` is what it should ask.
 *
 * Names come from the type cache, keyed by Numista's issuer codes (in French, like
 * `afrique_du_sud`), through [cardCountry] for the codes whose label is not a country (ADR 0023).
 */
internal class Issuers(private val typeMeta: TypeMetaIndex) {
    private val namesByCode: Map<String, String> = buildMap {
        for (meta in typeMeta.values) {
            val code = meta.issuerCode ?: continue
            val name = meta.issuerName ?: continue
            putIfAbsent(code, name)
        }
    }

    fun of(declaredCode: String?, pieces: List<CollectedItem>): String? {
        declaredCode?.let { code -> cardCountry(code, namesByCode[code])?.let { return it } }
        return pieces
            .map { piece -> typeMeta[piece.typeId]?.country }
            .distinct()
            .singleOrNull()
    }
}
