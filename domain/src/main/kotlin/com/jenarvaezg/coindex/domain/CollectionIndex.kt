package com.jenarvaezg.coindex.domain

import java.text.Collator
import java.util.Locale

/**
 * Issued members owned over issued members catalogued (ADR 0021 §6).
 *
 * A measured fact, and the one that replaced the collector's declaration of intent: since ADR 0021
 * §7 nothing is stored per card, so what tells a collection being pursued from one that is finished
 * is this ratio and nothing else.
 *
 * The denominator is what the app can measure — see [CollectionCatalogAlbum.issuedMembers] — so an
 * announced or unlisted member, which no money can buy and no inventory can represent, never counts
 * against the collector.
 */
data class CoverageRatio(val owned: Int, val issued: Int) {
    init {
        require(issued > 0) { "a coverage ratio needs a measurable denominator, got $issued" }
        require(owned in 0..issued) { "owned $owned is not inside 0..$issued" }
    }

    val value: Double get() = owned.toDouble() / issued

    val missing: Int get() = issued - owned

    /**
     * Every measurable member owned. Deliberately not called complete *coverage*: by ADR 0020 an
     * open series has no completeness to claim, and next January this same catalog may say 22/23.
     */
    val nothingMissing: Boolean get() = owned == issued
}

/**
 * One card of the index: one species of collection, in one list, sorted by one comparator
 * (ADR 0021 §2).
 *
 * There is no block, no section and no word of provenance telling the cases apart. What a card
 * *does* is decided by one capability — whether it has an issue list — which on this type is
 * exactly whether it carries a [coverage] (ADR 0021 §3).
 */
sealed interface IndexCard {
    /** The card-sized name: the curated `short_name`, or Numista's raw family verbatim (§4). */
    val name: String

    /** Null when the collection has no issue list, and therefore no ratio to offer. */
    val coverage: CoverageRatio?

    /** The country, unsaid when nothing can name it without claiming more than it knows. */
    val issuer: String?

    val distinctTypes: Int

    val quantity: Int

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
        /**
         * Whether the catalog behind this card declares its series still being issued (ADR 0020),
         * or null where no catalog names the collection.
         *
         * Never printed on the card — the card says what it does and nothing about its curation
         * (ADR 0021 §3) — and read only by the shelf of the index, where «cerrada» is the honest
         * answer to «what can I still finish?».
         */
        val seriesStatus: SeriesStatus? = null,
    ) : IndexCard {
        override val distinctTypes: Int get() = collection.distinctTypes
        override val quantity: Int get() = collection.quantity
        val key: VariantKey get() = collection.key()
    }

    /**
     * A box the collector enumerated by hand (ADR 0021 §2, §11).
     *
     * It only ever holds pieces you own, so it can never contain a gap: it has no ratio, and it
     * falls in the no-ratio stretch of the order without privilege.
     */
    data class Box(
        override val name: String,
        override val issuer: String?,
        val box: OwnGroupingView,
    ) : IndexCard {
        override val coverage: CoverageRatio? get() = null
        override val distinctTypes: Int get() = box.distinctTypes
        override val quantity: Int get() = box.quantity
    }
}

/**
 * The one order of the whole first level: `(has ratio ↓, ratio ↓, denominator ↓, name ↑)`
 * (ADR 0021 §6).
 *
 * `has ratio` first because «te faltan 8» and «3 monedas · 2 tipos» are incomparable magnitudes —
 * and it is a level of this comparator rather than a block with a heading, because ADR 0021 §7
 * removed three blocks and none comes back. Then the ratio, because the index is a notebook that
 * shows rather than a list of chores. Then the denominator, so `22/22` beats `2/2`. The name breaks
 * the tie, which is what finally makes the order agree with the text on the card.
 *
 * A function rather than a constant because a [Collator] is not thread safe, and one sort of sixty
 * cards is not worth sharing one.
 */
internal fun indexOrder(): Comparator<IndexCard> {
    val names = cardNameOrder()
    return compareByDescending<IndexCard> { it.coverage != null }
        .thenByDescending { it.coverage?.value ?: 0.0 }
        .thenByDescending { it.coverage?.issued ?: 0 }
        .thenBy(names) { it.name }
}

/**
 * Spanish alphabetical order, which is the only reading of «name ↑» that agrees with the card.
 *
 * Comparing the strings themselves would order by UTF-16 code unit, where every accented letter
 * lands after `Z` — «Álbum» at the very end of a list it belongs at the head of. The corpus is
 * written in Spanish by rule (ADR 0021 §4), country names included.
 */
private fun cardNameOrder(): Comparator<String> {
    val collator = Collator.getInstance(Locale.forLanguageTag("es"))
    return Comparator { left, right -> collator.compare(left, right) }
}

/**
 * Builds the single list of the first level out of the seeds that travel with the app.
 *
 * Built once per process, like [CollectionTitles], because catalogs and groupings are constant for
 * the lifetime of the seeds; only [build] sees the collector's inventory.
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
     * The snapshot arrives whole rather than as the `items` and `typeMeta` the derivation already
     * consumed (#217): the two always travel together, and threading them a second time is what
     * let a caller hand the index one inventory and the derivation another.
     */
    fun build(
        snapshot: CollectionSnapshot,
        derivation: CollectionDerivation,
        boxes: List<OwnGroupingView>,
    ): List<IndexCard> {
        val items = snapshot.items
        val issuers = Issuers(snapshot.typeMeta)
        val cards = derivation.derivedCollections.map { collection ->
            val key = collection.key()
            val catalog = catalogsByKey[key]
            IndexCard.Derived(
                name = titles.of(key),
                coverage = catalog?.let { coverageOf(it, items) },
                issuer = issuers.of(
                    declaredCode = declaredIssuerCode(key),
                    pieces = derivation.itemsByKey[key].orEmpty(),
                ),
                collection = collection,
                plateCatalogId = catalog?.takeIf { it.isEvidencedBy(items) }?.id,
                seriesStatus = catalog?.seriesStatus,
            )
        } + boxes.map { box ->
            IndexCard.Box(
                name = box.name,
                issuer = issuers.of(declaredCode = null, pieces = box.items),
                box = box,
            )
        }
        return cards.sortedWith(indexOrder())
    }

    /**
     * What the curated file says the country is, or null for the cards no file names.
     *
     * Resolved by the two keys [CollectionTitles] resolves a name with — a catalog on the whole
     * variant key, a grouping on its family alone (ADR 0013) — because it is the same file that
     * answers both questions about the same card.
     */
    private fun declaredIssuerCode(key: VariantKey): String? =
        catalogsByKey[key]?.issuerCode ?: groupingIssuers[key.family]

    private fun coverageOf(
        catalog: CollectionCatalog,
        items: List<CollectedItem>,
    ): CoverageRatio? {
        val album = buildCollectionCatalogAlbum(catalog, items)
        val issued = album.issuedMembers()
        // A catalog whose every member is announced or unlisted has nothing measurable to divide
        // by, so it offers no ratio rather than a zero one.
        if (issued == 0) return null
        return CoverageRatio(album.ownedMembers(), issued)
    }
}

/**
 * Who issued a collection, for the eyebrow of its card.
 *
 * **The file speaks when there is one** (ADR 0021 §9): every curated file declares its
 * `issuer_code`, so the card of a curated collection names its country from the curation and not
 * from the pieces that happen to be in the phone. That is what retires the silence clause where it
 * used to hurt — a card whose two pieces have one uncached type would go bare while its own file
 * knew the answer all along.
 *
 * Where no file names the collection the pieces are the only authority there is, and there the
 * clause stays: two issuers under one card, or one piece whose issuer nobody recorded, leave the
 * eyebrow unsaid. An eyebrow that covers half its card is worse than no eyebrow at all, which is
 * why the unknowns are kept in the list rather than filtered out.
 *
 * **One file cannot name one country**, and this still reads its header: Equilibrium spans Tokelau
 * and Niue, so a card whose only piece is the 2023 Niue is labelled «Tokelau» today. The catalog
 * stopped claiming a single issuer — `CollectionCatalog.issuerCodes()` is what to ask, and a
 * spanning catalog either falls through to the pieces here or goes bare. Deciding which is the
 * open half of #170.
 *
 * The name of an issuer comes from the type cache either way, keyed by the same code the files
 * declare — `afrique_du_sud`, in French, because Numista's codes are. One source for both kinds of
 * card, and [cardCountry] over it for the nine codes whose Numista label is an issuing entity with
 * its period of validity rather than a country (ADR 0023).
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
