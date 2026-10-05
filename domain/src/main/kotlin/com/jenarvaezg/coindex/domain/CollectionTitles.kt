package com.jenarvaezg.coindex.domain

/**
 * What each collection is called on a card (#22).
 *
 * The name lives in the curated file that defines the variant (ADR 0016): a catalog's or a curated
 * grouping's `short_name`, matched on the whole variant key for a catalog and on the family alone
 * for a grouping (ADR 0013). What no file claims falls through to [familyLabel]. The sources are
 * constant for the life of the seeds; [of] resolves a whole index at once because collisions
 * depend on which cards appear together (#565).
 */
class CollectionTitles(
    catalogs: List<CollectionCatalog>,
    groupings: List<CuratedGrouping>,
) {
    private val byKey: Map<VariantKey, String> =
        catalogs.associate { catalog -> catalog.key() to catalog.shortName }

    private val byFamily: Map<String, String> =
        groupings.associate { grouping -> grouping.family to grouping.shortName }

    /**
     * The name of every card of one index, resolved together so no two cards read alike.
     *
     * A collision is a property of the pair: «5 francs Semeuse» named both the 1963 circulation
     * coin and the 1960 essai piéfort (#565), and a card shows only photo, name and count (ADR 0026
     * §12). Curation cannot fix that case, since no catalog names either. A grouping claims a
     * family that may hold two variant keys, so curated names need this too; only a catalog, which
     * owns a whole key, is immune.
     */
    fun of(keys: List<VariantKey>): Map<VariantKey, String> {
        val plain = keys.associateWith(::nameOf)
        val shared = plain.values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        return plain.mapValues { (key, name) ->
            if (name in shared) disambiguate(name, key) else name
        }
    }

    /**
     * Every name a curated file claims, which a new box must avoid (ADR 0021 §4). Raw Numista
     * families of uncurated cards are not included: they move with the inventory, and a later
     * collision is left visible rather than policed (ADR 0021 §11).
     */
    fun curatedNames(): Set<String> = byKey.values.toSet() + byFamily.values.toSet()

    private fun nameOf(key: VariantKey): String =
        byKey[key] ?: byFamily[key.family] ?: familyLabel(key.family)

    /**
     * What is added to a name two cards share: the weight that splits them, in the key's unit.
     * Weight has split a family in every collision seen so far, and as a figure it adds no
     * vocabulary to the card (ADR 0026 §12). Two cards differing only in finish or metal would
     * still read alike; no such pair exists yet. A set has no weight (ADR 0012) and keeps its name.
     */
    private fun disambiguate(name: String, key: VariantKey): String =
        key.weightMillioz?.let { weight -> "$name · ${ounceLabel(weight)}" } ?: name
}
