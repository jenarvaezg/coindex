package com.jenarvaezg.coindex.domain

/**
 * What identifies one empty casilla, using only catalogue facts that survive a sync (ADR 0029 §1):
 * the Numista type, the year on the coin and, where the curated file names one, the Numista issue.
 * Never a `collected_items` row id, since a sync replaces those wholesale (ADR 0013).
 *
 * The type alone is not enough: a date run repeats it across years (ADR 0009) and an issue run
 * across issues (ADR 0014). This is the identity the validator compares and
 * [CollectionCatalog.memberMatches] fills by. [issueId] is the first issue the file declares, as a
 * casilla holding the curved and the straight nine of 1969 is one casilla.
 */
data class WishKey(val typeId: Int, val year: Int, val issueId: Int?)

/**
 * A casilla the collector marked on this phone, and when (ADR 0029). The app's only declarative,
 * and not the per-card one ADR 0021 §7 retired: it says «I am looking for this coin» about an empty
 * slot, dies when the slot fills, and never travels with the app. [markedAt] orders the list,
 * newest first; a wish never expires.
 */
data class Wish(val key: WishKey, val markedAt: Long)

/**
 * One marked casilla resolved against the curated shelf: the coin and the plate it belongs to. A
 * wish is stored as three numbers; the curated file supplies its name, face and plate.
 */
data class WishedSlot(
    val wish: Wish,
    val catalog: CollectionCatalog,
    val member: CollectionCatalogMember,
) {
    val key: WishKey get() = wish.key
    val typeId: Int get() = key.typeId
}

/**
 * The key of a casilla, or null for a member with no Numista type (announced or unlisted): a coin
 * the app cannot name cannot be marked (ADR 0029). Issued members always have a year
 * (`IssuedWithoutYear`).
 */
fun CollectionCatalogMember.wishKey(): WishKey? {
    val typeId = numistaTypeId ?: return null
    val year = year ?: return null
    return WishKey(typeId, year, numistaIssueIds.firstOrNull())
}

/**
 * Every live wish, resolved, newest first.
 *
 * Liveness is derived, never stored (ADR 0029 §2): a wish whose casilla filled, by the same
 * [CollectionCatalog.memberMatches] the plate uses, is hidden but not deleted.
 *
 * A wish no curated file claims is dropped from the reading and kept in the table, since a later
 * update may name it again. The same happens when a curation edit changes the casilla's key, for
 * example by declaring an issue: the old mark stops matching. The symptom, so nobody debugs it as
 * data loss: after the app update that edited the file, the count drops by one and the row leaves
 * the annex; one tap on the plate marks it again. Falling back to `(typeId, year)` was rejected:
 * the plate would then insert a second row under the new key instead of undoing the old one.
 *
 * The first catalog that claims the slot wins, the same tie as ADR 0021 §10.
 */
fun wishedSlots(
    wishes: List<Wish>,
    catalogs: List<CollectionCatalog>,
    items: List<CollectedItem>,
): List<WishedSlot> = wishes
    .sortedByDescending { it.markedAt }
    .mapNotNull { wish -> resolve(wish, catalogs) }
    .filter { slot -> items.none { item -> slot.catalog.memberMatches(slot.member, item) } }

private fun resolve(wish: Wish, catalogs: List<CollectionCatalog>): WishedSlot? =
    catalogs.firstNotNullOfOrNull { catalog ->
        catalog.members
            .firstOrNull { member -> member.wishKey() == wish.key }
            ?.let { member -> WishedSlot(wish, catalog, member) }
    }
