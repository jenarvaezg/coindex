package com.jenarvaezg.coindex.domain

/**
 * The commemorative programmes every curated catalog touches, standings included, built once per
 * assembly (#539).
 *
 * **The sibling of [CatalogAlbums], and it exists for the same reason one step further on.** #537
 * gave the album one instance per assembly so a card and its plate could not divide by two different
 * denominators; the programmes stayed behind, and a plate resolved them again on every read — which
 * on paper meant the notebook re-derived thirteen programmes against the whole inventory once per
 * printed card, sixty-seven times for the father's collection. The reading never changed between
 * those sixty-seven answers, because nothing it is made of changes without a new snapshot: the
 * curated files are constant for the life of the process and the inventory is the assembly's own.
 *
 * **Built off one walk of the inventory and not one per catalog.** The owned types are gathered once
 * and every programme's progress is counted against that set, so an assembly costs a walk of the
 * pieces plus a walk of the catalogued members, instead of `catalogs × pieces`.
 *
 * **A catalog it does not hold reads as no programme, and that is not the null [CatalogAlbums.get]
 * has.** The two absences are different facts: an album is what a plate divides by, so its absence is
 * a wiring mistake that has to be said out loud, while a standing is a *second reading* beside the
 * plate and never part of its denominator (ADR 0022) — the honest answer for a catalog no programme
 * names is «ninguno», and it is the answer for forty-seven of the forty-nine files. The wiring
 * mistake is still caught, and caught first: `resolvePlate` asks for the album before it asks for
 * this.
 */
data class CatalogProgrammes(
    private val byCatalogId: Map<String, List<ProgrammeStanding>> = emptyMap(),
) {
    /** The programmes this catalog touches, in file order, each with the collector's progress. */
    operator fun get(catalog: CollectionCatalog): List<ProgrammeStanding> =
        byCatalogId[catalog.id].orEmpty()

    companion object {
        /**
         * The standings of every catalog, over one inventory.
         *
         * **File order survives**, because it is what the plate prints: the programmes are walked in
         * the order they were loaded and filtered, never grouped by the type that matched them, so
         * two programmes naming the same coin cannot swap places between two readings of the same
         * files.
         *
         * Only the catalogs that touch something are keyed, so the map is two entries and not forty
         * nine of an empty list — see [get] for why the missing key is an answer and not a hole.
         */
        fun over(
            catalogs: List<CollectionCatalog>,
            programmes: List<CommemorativeProgramme>,
            items: List<CollectedItem>,
        ): CatalogProgrammes {
            if (programmes.isEmpty()) return CatalogProgrammes()
            // The progress of a programme is over the **programme** and not over any catalog
            // (ADR 0022), so it does not depend on which catalog is asking and is counted once.
            val owned = ownedTypeIds(items)
            val standings = programmes.map { ProgrammeStanding(it, it.progressOver(owned)) }
            return CatalogProgrammes(
                buildMap {
                    for (catalog in catalogs) {
                        val types = catalog.members.mapNotNullTo(mutableSetOf()) { it.numistaTypeId }
                        val touching = standings.filter { standing ->
                            standing.programme.typeIds.any { it in types }
                        }
                        if (touching.isNotEmpty()) put(catalog.id, touching)
                    }
                },
            )
        }
    }
}
