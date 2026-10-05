package com.jenarvaezg.coindex.domain

/** Reference to one owned piece backing a catalog member. */
data class ItemRef(
    val itemId: Long,
    val typeId: Int,
    val quantity: Int,
)

sealed interface CollectionCatalogMemberStatus {
    data class Owned(val quantity: Int, val items: List<ItemRef>) : CollectionCatalogMemberStatus

    data object Missing : CollectionCatalogMemberStatus

    /**
     * Struck and sold, but Numista has no published type for it (#48). Every inventory row comes
     * from Numista with a type, so the app cannot know whether the collector owns it; `Missing`
     * would claim knowledge it does not have.
     */
    data object Unlisted : CollectionCatalogMemberStatus

    /**
     * Named by the issuer and not yet struck, so no money can buy it (#31). As `Missing` it would
     * be a «me falta» the collector cannot act on.
     */
    data object NotYetIssued : CollectionCatalogMemberStatus
}

data class CollectionCatalogAlbumMember(
    val member: CollectionCatalogMember,
    val status: CollectionCatalogMemberStatus,
)

/**
 * The per-collector comparison between a followed collection and its curated catalog.
 *
 * Every emission of a plate is counted here and nowhere else (#218): the card's `CoverageRatio` and
 * the plate's «Progreso» read the same counters, and since #537 the same instance, which
 * [CatalogAlbums] builds once per catalog per assembly.
 *
 * It carries no catalog id or name; it always travels next to its catalog (in
 * `PlateResult.Available`, into `PlateSubject`).
 */
data class CollectionCatalogAlbum(
    val members: List<CollectionCatalogAlbumMember>,
) {
    fun ownedMembers(): Int =
        members.count { it.status is CollectionCatalogMemberStatus.Owned }

    /**
     * The denominator of the plate: what the app can measure from the Numista-backed inventory.
     * Announced and unlisted members are stated in prose, never as a divisor.
     */
    fun issuedMembers(): Int =
        members.count { member ->
            member.status is CollectionCatalogMemberStatus.Owned ||
                member.status is CollectionCatalogMemberStatus.Missing
        }

    fun announcedMembers(): Int =
        members.count { it.status is CollectionCatalogMemberStatus.NotYetIssued }

    /** Struck and sold, but outside the divisor: Numista has no type to measure it by. */
    fun unlistedMembers(): Int =
        members.count { it.status is CollectionCatalogMemberStatus.Unlisted }
}

/**
 * What this collector has of this catalog, as the one ratio the whole app divides by. Null when
 * every member is announced or unlisted: nothing measurable, so no ratio rather than a zero one.
 *
 * Next to the counters rather than in the index because the plate's completion stamp reads it
 * too (ADR 0026 §3), so card and stamp cannot disagree.
 */
fun CollectionCatalogAlbum.coverage(): CoverageRatio? {
    val issued = issuedMembers()
    if (issued == 0) return null
    return CoverageRatio(ownedMembers(), issued)
}

/**
 * The first member of this album the collector owns, or null where there is none. The index takes
 * its card's photograph from here and the plate the casilla that coin flies to (ADR 0026 §3). It
 * must be the first owned member, not the first member, or the coin would land on a hole.
 */
fun CollectionCatalogAlbum.firstOwnedIndex(): Int? =
    members.indexOfFirst { it.status is CollectionCatalogMemberStatus.Owned }.takeIf { it >= 0 }

/**
 * The collector's pieces keyed by Numista type. A member is filled only by pieces of its own type,
 * so building an album reads a few rows per key instead of walking the inventory.
 */
typealias PiecesByType = Map<Int, List<CollectedItem>>

/**
 * The pieces of one inventory as the index an album reads. `groupBy` keeps row order, so a
 * member's first owned piece is the same row as when walking the list, and the coin a card shows
 * does not move.
 */
fun piecesByType(items: List<CollectedItem>): PiecesByType = items.groupBy { it.typeId }

/**
 * One album, built against the whole inventory, for a caller holding one catalog and one list (a
 * test, a curator's script). Everything the app draws comes from [CatalogAlbums], which indexes
 * once for every catalog.
 */
fun buildCollectionCatalogAlbum(
    catalog: CollectionCatalog,
    items: List<CollectedItem>,
): CollectionCatalogAlbum = buildCollectionCatalogAlbum(catalog, piecesByType(items))

internal fun buildCollectionCatalogAlbum(
    catalog: CollectionCatalog,
    pieces: PiecesByType,
): CollectionCatalogAlbum = CollectionCatalogAlbum(
    members = catalog.members.map { member ->
        val status = when {
            // Never inspect inventory for a member the Numista-backed inventory cannot represent.
            member.isUnlisted -> CollectionCatalogMemberStatus.Unlisted
            // Never `Missing`, by contract: an unstruck slot is not a hole in the collection.
            member.isAnnounced -> CollectionCatalogMemberStatus.NotYetIssued
            else -> {
                val ownedItems = member.numistaTypeId
                    ?.let { typeId -> pieces[typeId] }
                    .orEmpty()
                    .filter { item -> catalog.memberMatches(member, item) }
                    .map { item -> ItemRef(item.id, item.typeId, item.quantity) }
                if (ownedItems.isEmpty()) {
                    CollectionCatalogMemberStatus.Missing
                } else {
                    CollectionCatalogMemberStatus.Owned(
                        quantity = ownedItems.fold(0) { total, item ->
                            saturatingAdd(total, item.quantity)
                        },
                        items = ownedItems,
                    )
                }
            }
        }
        CollectionCatalogAlbumMember(member, status)
    },
)
