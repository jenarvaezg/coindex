package com.jenarvaezg.coindex.domain

/**
 * The cut of the shelf window: a plate of this many casillas or more is not one (ADR 0030 §1).
 *
 * It is a rule about **what a plate can say** and the sibling of `HOLE_THRESHOLD_SLOTS`: there, a
 * plate of fifty-one holes has no cost of completion but a reproach; here, a plate of twenty zeros has
 * nothing to show a collector who came to look at what exists. Exclusive, so twenty is out.
 */
const val SHOWCASE_MAX_SLOTS: Int = 20

/**
 * One curated catalog the collector owns nothing of, drawn as a plate they can walk into (ADR 0030).
 *
 * It carries the album and not only the catalog because everything the shelf and the plate ask of it —
 * how many casillas, which coin faces the tile, what a tasación would cost — is the album's, and
 * building it twice is how a tile and its plate come to disagree about the same twelve holes.
 *
 * There is **no ratio** and there is nothing to store: the population is derived from the evidence on
 * every read, exactly like the fraction on a card is (ADR 0021 §7 as amended).
 */
data class ShowcasePlate(
    val catalog: CollectionCatalog,
    val album: CollectionCatalogAlbum,
) {
    /**
     * The casillas the cut counts, which is the divisor the plate itself divides by.
     *
     * Announced and unlisted members are outside it for the reason they are outside every other
     * measurement: money cannot buy the first and the app cannot see the second.
     */
    val slots: Int get() = album.issuedMembers()
}

/**
 * This catalog as a plate of the shelf window, or null if it is not one.
 *
 * The two conditions of ADR 0030 §1 and no third: **no evidence** — the same fact `resolvePlate` opens
 * a plate by, so a catalog cannot be both the collector's and the window's — and **fewer than
 * [SHOWCASE_MAX_SLOTS] measurable casillas**.
 *
 * Asked of one catalog rather than only of the shelf, because the two callers need the same answer
 * about the same file: the shelf, which draws the twenty, and the plate, which has to know whether this
 * particular one opens with no evidence at all.
 */
fun showcasePlate(
    catalog: CollectionCatalog,
    items: List<CollectedItem>,
    evidencedCatalogIds: Set<String>,
): ShowcasePlate? {
    if (catalog.id in evidencedCatalogIds) return null
    val album = buildCollectionCatalogAlbum(catalog, items)
    val slots = album.issuedMembers()
    // Zero is not a small window, it is no window: a file of announcements has no plate to open.
    if (slots == 0 || slots >= SHOWCASE_MAX_SLOTS) return null
    return ShowcasePlate(catalog, album)
}

/**
 * The whole shelf window, in the order it opens: fewest casillas first (ADR 0030 §8).
 *
 * The default order is not «by cost of entering», which is what #282 chose while the pass still valued
 * on its own: with the tasación in the collector's hands the shelf is born with no amount at all, and
 * an order that ranks twenty absences ranks nothing. Fewest first is the same reading as the cut — the
 * plate closest to being a plate leads — and the tie is broken by the id so that two plates of three
 * casillas cannot swap places between two readings of the same files.
 */
fun showcasePlates(
    catalogs: List<CollectionCatalog>,
    items: List<CollectedItem>,
    evidencedCatalogIds: Set<String>,
): List<ShowcasePlate> = catalogs
    .mapNotNull { catalog -> showcasePlate(catalog, items, evidencedCatalogIds) }
    .sortedWith(compareBy({ it.slots }, { it.catalog.id }))
