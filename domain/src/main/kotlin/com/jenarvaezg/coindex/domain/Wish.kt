package com.jenarvaezg.coindex.domain

/**
 * What identifies one empty casilla, in the only facts that survive a sync (ADR 0029 §1).
 *
 * The three are the catalogue's and not the collection's: the Numista type, the year on the coin, and
 * the Numista issue where the curated file names one. **Not one of them is a `collected_items` row
 * id**, which is the whole reason a box is keyed the way it is (ADR 0013): row ids come from Numista
 * and are replaced wholesale on every sync, so a mark keyed on them would quietly empty itself.
 *
 * **Three and not one**, which is where ADR 0029 §1 widens what #484 wrote: a `typeId` is not a slot
 * in two of the four schema versions. A date run repeats one type across its years (ADR 0009) and an
 * issue run repeats it across its issues (ADR 0014), so a mark keyed on the type alone would cover
 * every year of the Kookaburra at once — a wish over a plate, which is exactly what the grain rule
 * refuses. It is the same identity the curated-file validator compares (`typeId to year`) and the
 * same one [CollectionCatalog.memberMatches] fills a casilla by.
 *
 * [issueId] is the **first** issue the file declares and null where it declares none, which is the
 * choice the casilla itself makes: a slot holding the curved and the straight nine of the 1969 peseta
 * is one slot, and it is filled by either.
 */
data class WishKey(val typeId: Int, val year: Int, val issueId: Int?)

/**
 * A casilla the collector marked on this phone, and when (ADR 0029).
 *
 * It is a declarative and the only one the app has, and it is not the one ADR 0021 §7 retired: it
 * says «I am looking for this coin» about a slot that is empty, and it **dies measured** when the
 * slot fills. Nothing about it is a claim on the catalogue, so it never travels with the app.
 *
 * [markedAt] is not a clock anything expires by — a wish has no lifetime — it is the one order the
 * list has: the last thing marked is the thing being hunted.
 */
data class Wish(val key: WishKey, val markedAt: Long)

/**
 * One marked casilla resolved against the curated shelf: the coin, and the plate it is a slot of.
 *
 * Resolved because a wish row on its own cannot be drawn. What names the coin, what says which face
 * to print and what says which plate it belongs to are all the curated file's, and the file is what
 * travels in the APK — so a wish is stored as three numbers and read as a member of a catalog.
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
 * The key of a casilla, or null where it cannot be wished for at all.
 *
 * Null for a member with no Numista type — an announced design, an unlisted piece — and that is the
 * recorte ADR 0029 accepts from the other side: a coin the app cannot name cannot be marked. Every
 * issued member has a year, which the validator requires (`IssuedWithoutYear`), so what is left out
 * here is exactly what has no coin behind it yet.
 */
fun CollectionCatalogMember.wishKey(): WishKey? {
    val typeId = numistaTypeId ?: return null
    val year = year ?: return null
    return WishKey(typeId, year, numistaIssueIds.firstOrNull())
}

/**
 * Every wish that is still alive, resolved and in the order they were marked, newest first.
 *
 * **Alive is derived and never stored** (ADR 0029 §2): a wish whose casilla has filled is not shown
 * and not deleted, and the question is asked of [CollectionCatalog.memberMatches] — the very rule the
 * plate fills that casilla by — so «dies measured» cannot disagree with the album about the same coin.
 *
 * A wish no curated file claims any more is **dropped from the reading and kept in the table**: what
 * names it is the file, so a catalog retired by an app update leaves the row with nothing to draw.
 * It is not deletion because a later update may name it again, and the alternative — a row that
 * cannot be drawn but is counted — is a door that says «7» and opens on six.
 *
 * The other way a row goes quiet is a **curation edit to the casilla itself**: a member that starts
 * declaring an issue it did not declare before is a different [WishKey], so the old mark stops
 * matching. It is the same silence and it is accepted for the same reason — the file is what names a
 * casilla — and it is one more thing the curator does at app-update time and can see, unlike the sync,
 * which happens every month and must never move a mark.
 *
 * The first catalog that claims the slot wins, which is the same tie ADR 0021 §10 leaves everywhere
 * else: curated multiple membership has no home and no extra view, and the coin is the same coin
 * whichever list is naming it here.
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
