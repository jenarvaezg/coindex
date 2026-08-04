package com.jenarvaezg.coindex.domain

/**
 * What each collection is called on a card, resolved once per process (#22).
 *
 * The name of a collection lives in the curated file that defines it, because that file **is**
 * the variant (ADR 0016): a catalog names its own `short_name`, and so does a curated grouping.
 * The collector never renames anything — the only renameable heading in the app, the own box, is
 * empty on both phones — so this index is constant for the lifetime of the seeds.
 *
 * A catalog is matched on the whole variant key and a grouping on its family alone, which is all
 * a grouping supplies (ADR 0013). What no file claims falls through to
 * [familyLabel] and reads as Numista wrote it.
 */
class CollectionTitles(
    catalogs: List<CollectionCatalog>,
    groupings: List<CuratedGrouping>,
) {
    private val byKey: Map<VariantKey, String> =
        catalogs.associate { catalog -> catalog.key() to catalog.shortName }

    private val byFamily: Map<String, String> =
        groupings.associate { grouping -> grouping.family to grouping.shortName }

    fun of(key: VariantKey): String =
        byKey[key] ?: byFamily[key.family] ?: familyLabel(key.family)

    fun of(collection: DerivedCollection): String = of(collection.key())
}
