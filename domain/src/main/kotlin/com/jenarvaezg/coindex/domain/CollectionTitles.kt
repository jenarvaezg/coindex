package com.jenarvaezg.coindex.domain

/**
 * What each collection is called on a card (#22).
 *
 * The name of a collection lives in the curated file that defines it, because that file **is**
 * the variant (ADR 0016): a catalog names its own `short_name`, and so does a curated grouping.
 * The collector never renames anything — the only renameable heading in the app, the own box, is
 * empty on both phones — so the three sources this reads are constant for the lifetime of the
 * seeds. What varies is which cards are on screen together, which is why [of] is asked for a whole
 * index at once and not for a name at a time (#565).
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

    /**
     * The name of every card of one index, resolved together so that two cards cannot read alike.
     *
     * **Together and not one at a time**, because a collision is not a property of either card: the
     * five francs Semeuse of #565 are two honest names that only became a defect standing side by
     * side in the index. The father had two cards reading «5 francs Semeuse», one over the 1963
     * circulation coin and one over the 1960 essai piéfort, and the card shows the photograph, the
     * name and the count and nothing else since ADR 0026 §12 — so on screen there was nothing at
     * all to tell them apart.
     *
     * ADR 0026 §12 foresaw the residual risk and named its cure: *«if a catalog ever arrives whose
     * `short_name` omits the variant, that is a curation rule»*. This is the same risk arriving from
     * the side it did not foresee — **no catalog at all**. Curation cannot answer here: it is one
     * Numista series over two physical patterns, and the essai is «Monedas de ensayo», one of the
     * five classes `objectClassDeviations` warns about precisely for landing inside a lámina (#89).
     * Both are signed huérfanas in `data/orphans.json`, and that register does not touch the screen.
     *
     * The curated species are in the same net, and they are not safe without it: `short_name`
     * uniqueness is validated across the files, but a **grouping** claims a family and a family can
     * hold two variant keys, so one grouping can name two cards. Only a catalog is immune, because
     * it owns a whole key.
     */
    fun of(keys: List<VariantKey>): Map<VariantKey, String> {
        val plain = keys.associateWith(::nameOf)
        val shared = plain.values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        return plain.mapValues { (key, name) ->
            if (name in shared) disambiguate(name, key) else name
        }
    }

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

    private fun nameOf(key: VariantKey): String =
        byKey[key] ?: byFamily[key.family] ?: familyLabel(key.family)

    /**
     * What is added to a name two cards share: the weight that split them, in the unit the key is
     * keyed in.
     *
     * The weight and not the finish or the metal, and the limit is deliberate. Weight is what splits
     * a family in every case the two collections have ever produced — measured on 1 September 2026
     * over the 112 cards of both, where the Semeuse is the only collision there is — and it is the
     * one field of the key that is a figure and not a word, so saying it adds no vocabulary to a
     * card that ADR 0026 §12 stripped down to three things. Two cards of one family, one weight and
     * two finishes would still read alike; that pair does not exist, and inventing its wording now
     * would be a mechanism with no subject. It is the same account that keeps «plata» off every card
     * and puts «Oro» on the one card the metal distinguishes.
     *
     * A set spans denominations and has no weight (ADR 0012), so it has nothing to be told apart by
     * and keeps its bare name.
     */
    private fun disambiguate(name: String, key: VariantKey): String =
        key.weightMillioz?.let { weight -> "$name · ${ounceLabel(weight)}" } ?: name
}
