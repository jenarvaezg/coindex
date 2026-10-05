package com.jenarvaezg.coindex.domain

/**
 * The commemorative programmes every curated catalog touches, standings included, built once per
 * assembly (#539) like [CatalogAlbums]: resolving them per plate made the notebook re-derive every
 * programme once per printed card. Owned types are gathered in one walk, so an assembly costs a
 * walk of the pieces plus one of the catalogued members.
 *
 * A catalog it does not hold reads as no programme, unlike the null of [CatalogAlbums.get]: an
 * album is what a plate divides by, so its absence is a wiring mistake, while a standing is a
 * second reading beside the plate (ADR 0022) and most catalogs touch none. `resolvePlate` asks for
 * the album first, so the wiring mistake is still caught.
 */
data class CatalogProgrammes(
    private val byCatalogId: Map<String, List<ProgrammeStanding>> = emptyMap(),
) {
    /** The programmes this catalog touches, in file order, each with the collector's progress. */
    operator fun get(catalog: CollectionCatalog): List<ProgrammeStanding> =
        byCatalogId[catalog.id].orEmpty()

    companion object {
        /**
         * The standings of every catalog, over one inventory. Programmes keep file order, which the
         * plate prints, so two programmes naming the same coin cannot swap places. Only catalogs
         * that touch a programme are keyed (see [get]).
         */
        fun over(
            catalogs: List<CollectionCatalog>,
            programmes: List<CommemorativeProgramme>,
            items: List<CollectedItem>,
        ): CatalogProgrammes {
            if (programmes.isEmpty()) return CatalogProgrammes()
            // A programme's progress is over the programme, not any catalog (ADR 0022), so it is
            // counted once.
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
