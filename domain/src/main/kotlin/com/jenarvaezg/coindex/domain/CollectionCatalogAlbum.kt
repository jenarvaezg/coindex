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
}

fun buildCollectionCatalogAlbum(
    catalog: CollectionCatalog,
    items: List<CollectedItem>,
): CollectionCatalogAlbum = CollectionCatalogAlbum(
    catalogId = catalog.id,
    name = catalog.name,
    members = catalog.members.map { member ->
        val ownedItems = items
            .filter { item -> catalog.memberMatches(member, item) }
            .map { item -> ItemRef(item.id, item.typeId, item.quantity) }
        val status = if (ownedItems.isEmpty()) {
            CollectionCatalogMemberStatus.Missing
        } else {
            CollectionCatalogMemberStatus.Owned(
                quantity = ownedItems.fold(0) { total, item -> saturatingAdd(total, item.quantity) },
                items = ownedItems,
            )
        }
        CollectionCatalogAlbumMember(member, status)
    },
)
