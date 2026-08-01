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

/** The per-collector comparison between a followed proposal and its curated catalog. */
data class CollectionCatalogAlbum(
    val catalogId: String,
    val name: String,
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
}

fun buildCollectionCatalogAlbum(
    catalog: CollectionCatalog,
    items: List<CollectedItem>,
): CollectionCatalogAlbum = CollectionCatalogAlbum(
    catalogId = catalog.id,
    name = catalog.name,
    members = catalog.members.map { member ->
        val status = when {
            // Never inspect inventory for a member the Numista-backed inventory cannot represent.
            member.isUnlisted -> CollectionCatalogMemberStatus.Unlisted
            // Never `Missing`, by contract: an unstruck slot is not a hole in the collection.
            member.isAnnounced -> CollectionCatalogMemberStatus.NotYetIssued
            else -> {
                val ownedItems = items
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
