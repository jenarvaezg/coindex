package com.jenarvaezg.coindex.domain

/**
 * One measurable casilla of an evidenced plate, with its member's country (#170) and its year (ADR
 * 0026 §9) resolved, so the country and year axes only group and sort (#538).
 *
 * A reading beside the album rather than a richer album member, because:
 * - resolving a country needs [TypeMeta], and a ratio must not move when a sync brings a ficha: the
 *   album's counters (#218) depend on the inventory alone;
 * - the plate needs neither field;
 * - an axis shows only what can be owned or missed, a smaller population than a plate draws.
 *
 * [owned] comes from the assembly's album instance, so a hole on an axis is the same hole as on its
 * plate (#537).
 */
data class AlbumSlot(
    val catalogId: String,
    val memberId: String,
    /**
     * The Numista type this casilla answers with: the member's own, or else the only type of a
     * plate that names exactly one.
     */
    val typeId: Int?,
    /** Owned, or else a hole; nothing else reaches this list. */
    val owned: Boolean,
    /**
     * The album's piece counter, zero for a hole. Never zero when owned: a row with no pieces fills
     * nothing (`CollectionCatalog.memberMatches`).
     */
    val quantity: Int,
    /** The member's country, cured (ADR 0023, #170) — null when neither code nor ficha names one. */
    val country: String?,
    /** The year the casilla stands on, or null where nothing names one. Never zero or negative. */
    val year: Int?,
)

/**
 * Every measurable casilla of every evidenced plate, in curated-file order, built once per assembly
 * so no surface re-implements the evidence filter, the Owned/Missing fork or the year (#538).
 *
 * Evidence is applied here: a plate with no evidence cannot be opened
 * (`PlateUnavailable.NoEvidence`). The shelf window (ADR 0030) reads its albums directly.
 * `Unlisted` and `NotYetIssued` stay out, since neither can be owned or missed.
 */
internal fun albumSlots(
    catalogs: List<CollectionCatalog>,
    albums: CatalogAlbums,
    typeMeta: TypeMetaIndex,
    evidencedCatalogIds: Set<String>,
): List<AlbumSlot> = buildList {
    for (catalog in catalogs) {
        if (catalog.id !in evidencedCatalogIds) continue
        // The assembly's album (#537), so this is the casilla the plate draws; a null here is a
        // wiring mistake.
        val album = albums[catalog] ?: continue
        // The type of a plate that names exactly one: the only case where the file can answer for
        // a member with no type or year. Over every mention, not distinct ones: a date run repeats
        // one type, and its casillas take the year the file wrote, never the type's floor.
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
 * The year a casilla stands on: the member's year, or else the earliest year of a single-type
 * plate. Every issued member declares a year; the fallback is for a plate that names one type and
 * no year. A non-positive year becomes null.
 */
private fun slotYear(
    member: CollectionCatalogMember,
    onlyType: Int?,
    typeMeta: TypeMetaIndex,
): Int? = (member.year ?: onlyType?.let { typeMeta[it]?.minYear })?.takeIf { it > 0 }
