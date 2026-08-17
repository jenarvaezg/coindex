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
     * This coin was struck and sold, but Numista has no published type for it (#48).
     *
     * The app cannot know whether the collector owns it because every inventory row comes from
     * Numista and therefore has a type. Calling it `Missing` would claim knowledge we do not have.
     */
    data object Unlisted : CollectionCatalogMemberStatus

    /**
     * The issuer named this one and has not struck it, so no money can buy it (#31).
     *
     * The third state the spec §1 promised and the §5 golden table required, and which never
     * made it into the Android port. Without it an announced member reads as `Missing`, which
     * is a «me falta» the collector cannot act on.
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
 * **Every emission of a plate is counted here and nowhere else** (#218). The card's `CoverageRatio`
 * and the plate's «Progreso» are the same sentence about the same collection, so they read the same
 * four counters; counting the catalog's own member flags a second time in the UI made two rules for
 * one number that agreed only as long as nobody touched [buildCollectionCatalogAlbum].
 *
 * Since #537 they read the same **instance** and not merely the same rule: [CatalogAlbums] builds one
 * album per catalog per assembly and hands it to the index, the shelf window and the plate alike.
 *
 * It carries neither the catalog's id nor its name: it travels next to the catalog that produced it
 * — inside `PlateResult.Available`, into `PlateSubject` — and a second copy of a name is a second
 * thing to keep in step.
 */
data class CollectionCatalogAlbum(
    val members: List<CollectionCatalogAlbumMember>,
) {
    fun ownedMembers(): Int =
        members.count { it.status is CollectionCatalogMemberStatus.Owned }

    /**
     * The denominator of the plate: what the app can measure from the Numista-backed inventory.
     *
     * Announced and unlisted members are both unmeasurable by the app. Their existence is said in
     * prose, not as a divisor.
     */
    fun issuedMembers(): Int =
        members.count { member ->
            member.status is CollectionCatalogMemberStatus.Owned ||
                member.status is CollectionCatalogMemberStatus.Missing
        }

    fun announcedMembers(): Int =
        members.count { it.status is CollectionCatalogMemberStatus.NotYetIssued }

    /** Struck and sold, and outside the divisor because Numista has no type to measure it by. */
    fun unlistedMembers(): Int =
        members.count { it.status is CollectionCatalogMemberStatus.Unlisted }
}

/**
 * What this collector has of this catalog, as the one ratio the whole app divides by.
 *
 * Null for a catalog whose every member is announced or unlisted: there is nothing measurable to
 * divide by, so it offers no ratio rather than a zero one.
 *
 * It lives next to the counters and not inside the index because the index is no longer the only
 * caller (ADR 0026 §3): the completion stamp is **read from the inventory like the die-cut**, and
 * reading it from a second rule is how the card's ratio in rust and the stamp on the plate would
 * come to disagree about the same collection one tap apart.
 */
fun CollectionCatalogAlbum.coverage(): CoverageRatio? {
    val issued = issuedMembers()
    if (issued == 0) return null
    return CoverageRatio(ownedMembers(), issued)
}

/**
 * The first member of this album the collector actually owns, or null where there is none.
 *
 * **One rule with two readers, so they cannot disagree about one coin** (ADR 0026 §3): the index
 * takes the photograph of its card from here, and the plate takes the casilla the coin flies to.
 * The rule is «the first *owned* member» and not «the first member» — the card of the 1 Bolívar
 * would otherwise show the 1879 the father does not have, and the journey would land a coin in full
 * colour on the hole where that same coin is a ghost.
 */
fun CollectionCatalogAlbum.firstOwnedIndex(): Int? =
    members.indexOfFirst { it.status is CollectionCatalogMemberStatus.Owned }.takeIf { it >= 0 }

/**
 * The collector's pieces keyed by the Numista type they are of.
 *
 * The index every album is built against, and the reason building one is not a walk of the whole
 * inventory: a member is filled by a piece **of its own type** and by no other, so the rows worth
 * asking about are the handful under one key rather than the 229 of the phone.
 */
typealias PiecesByType = Map<Int, List<CollectedItem>>

/**
 * The pieces of one inventory as the index an album reads.
 *
 * `groupBy` keeps the order the rows arrived in, which is what keeps an album built through the index
 * identical to one built by walking the list: the first owned piece of a member is the same row, so
 * the coin a card shows and the casilla it flies to do not move.
 */
fun piecesByType(items: List<CollectedItem>): PiecesByType = items.groupBy { it.typeId }

/**
 * One album, built against the whole inventory.
 *
 * The door for a caller holding one catalog and one list — a test, a curator's script — where
 * indexing 229 rows to read three of them is not worth the line. Everything the app draws comes out
 * of [CatalogAlbums] instead, which indexes once for every catalog it holds.
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
