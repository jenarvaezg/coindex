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

    /**
     * Every name a curated file claims, which is what a new box has to avoid (ADR 0021 §4).
     *
     * The uniqueness of `short_name` across the index is already validated at startup for the files
     * themselves; this is the same set offered to the one name the collector types. It does **not**
     * include the raw Numista families of the cards no file names: those move with the inventory, and
     * ADR 0021 §11 is explicit that a collision arriving later is not policed — it is visible in the
     * index, and the box is undone with one tap.
     */
    fun curatedNames(): Set<String> = byKey.values.toSet() + byFamily.values.toSet()
}
