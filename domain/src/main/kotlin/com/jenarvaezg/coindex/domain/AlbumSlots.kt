package com.jenarvaezg.coindex.domain

/**
 * One measurable casilla of an evidenced plate, with everything about *where it falls* resolved.
 *
 * The album says whether the collector has this casilla; this says the same thing plus the two
 * second readings an axis of the shelf needs to place it — the country of the member (#170) and the
 * year of the casilla (ADR 0026 §9). Both are read here once, off the album the plate itself draws,
 * so the country axis and the year axis stop deciding what fills a casilla and only group and order
 * what they are handed (#538).
 *
 * **A reading beside the album and not a richer album member**, which was the other place it could
 * have gone. Three reasons, and the first is the one that decides it:
 *
 * - **A ratio may not depend on the ficha cache.** Resolving a country needs [TypeMeta], so putting
 *   it on [CollectionCatalogAlbumMember] would make [buildCollectionCatalogAlbum] take the cache as
 *   an input — and then «2 de 22» would be a number that moves when a sync brings a ficha. The four
 *   counters of #218 are about the inventory and nothing else.
 * - **The plate does not ask.** The album travels inside `PlateResult.Available` and into
 *   `PlateSubject`, where the country is the header's job and the year is already written on the
 *   member. Nobody there would read either field.
 * - **What is measurable is a smaller population than what a plate draws.** A plate paints its
 *   announced and unlisted members too (they are said in prose, ADR 0021); an axis paints only what
 *   can be owned or missed, so a list of them is a different list and not an annotation of the same.
 *
 * The status itself is **not** re-derived: [owned] comes from the album instance the assembly built,
 * which is what makes a hole on the country axis and the hole on its plate the same hole rather than
 * two readings that agree (#537).
 */
data class AlbumSlot(
    val catalogId: String,
    val memberId: String,
    /**
     * The Numista type this casilla answers with, when the curated file names one.
     *
     * The member's own type, else the single type of a plate that names exactly one — a casilla with
     * no type of its own can only be answered for by the plate's, and only when there is no doubt
     * about which that is.
     */
    val typeId: Int?,
    /** Owned, or the other half of the fork: a hole. Nothing else reaches this list. */
    val owned: Boolean,
    /**
     * Pieces behind the casilla — the album's own counter — and zero for a hole.
     *
     * Never zero on an owned casilla, and not because this floors it: a row recorded with no pieces
     * fills nothing (`CollectionCatalog.memberMatches`), so an owned casilla has at least one behind
     * it by the time the album says so.
     */
    val quantity: Int,
    /** The member's country, cured (ADR 0023, #170) — null when neither code nor ficha names one. */
    val country: String?,
    /** The year the casilla stands on, or null where nothing names one. Never zero or negative. */
    val year: Int?,
)

/**
 * Every measurable casilla of every evidenced plate, in the order the curated files are read in.
 *
 * Built once per assembly for the reason [CatalogAlbums] is: two surfaces walking the plates
 * themselves is how the country axis came to reimplement the evidence filter, the Owned/Missing fork
 * and the year of a casilla, each in its own file (#538).
 *
 * **Evidence is applied here and not by the caller.** A plate the collector owns nothing of cannot be
 * opened at all (`PlateUnavailable.NoEvidence`), so a casilla of one is not a seat anybody can act on
 * — and leaving the filter to whoever reads the list is exactly the duplication this replaces. The
 * shelf window of ADR 0030 is the population that *is* made of unevidenced plates, and it reads its
 * albums directly rather than through an axis.
 *
 * `Unlisted` and `NotYetIssued` stay out: one has no Numista type to answer with and the other is a
 * coin no money can buy yet, so neither is a seat on any axis.
 */
internal fun albumSlots(
    catalogs: List<CollectionCatalog>,
    albums: CatalogAlbums,
    typeMeta: TypeMetaIndex,
    evidencedCatalogIds: Set<String>,
): List<AlbumSlot> = buildList {
    for (catalog in catalogs) {
        if (catalog.id !in evidencedCatalogIds) continue
        // The assembly's album (#537) and never one built here: the casilla this resolves is the
        // same casilla the plate draws, and a null is a wiring mistake rather than a plate to skip.
        val album = albums[catalog] ?: continue
        // The type of a plate that names exactly one, which is the only case where the file itself
        // can answer for a member carrying no type or no year. `singleOrNull` over every mention and
        // not over the distinct ones on purpose: a date run repeats one type across twenty years, and
        // the year of one of its casillas is the year the file wrote on it and never the type's floor.
        val onlyType = catalog.members.mapNotNull { it.numistaTypeId }.singleOrNull()
        for (albumMember in album.members) {
            val status = albumMember.status
            val owned = status is CollectionCatalogMemberStatus.Owned
            if (!owned && status !is CollectionCatalogMemberStatus.Missing) continue
            val member = albumMember.member
            add(
                AlbumSlot(
                    catalogId = catalog.id,
                    memberId = member.id,
                    typeId = member.numistaTypeId ?: onlyType,
                    owned = owned,
                    // The album's counter and not a second sum of its pieces (#218).
                    quantity = (status as? CollectionCatalogMemberStatus.Owned)?.quantity ?: 0,
                    country = catalog.countryOf(member, typeMeta),
                    year = slotYear(member, onlyType, typeMeta),
                ),
            )
        }
    }
}

/**
 * The year a casilla stands on: the year on the coin, else the floor of a single-type plate.
 *
 * Date-run members carry the year of the casilla, and that is the whole answer for every catalog that
 * ships — an issued member must declare its year to load at all. The fallback is for the plate that
 * names one type and no year, whose casilla would otherwise stand nowhere and leave no ghost.
 *
 * A year that is not positive is not a year an axis can paint, so it arrives as no year at all rather
 * than as a seat somewhere before the era.
 */
private fun slotYear(
    member: CollectionCatalogMember,
    onlyType: Int?,
    typeMeta: TypeMetaIndex,
): Int? = (member.year ?: onlyType?.let { typeMeta[it]?.minYear })?.takeIf { it > 0 }
