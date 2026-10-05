package com.jenarvaezg.coindex.domain

/**
 * A plate of this many casillas or more stays out of the shelf window (ADR 0030 §1); exclusive, so
 * twenty is out. Like `HOLE_THRESHOLD_SLOTS`, a rule about what a plate can usefully show.
 */
const val SHOWCASE_MAX_SLOTS: Int = 20

/**
 * One curated catalog the collector owns nothing of, drawn as a plate they can walk into (ADR
 * 0030). It carries the album so the tile and its plate read the same casillas. No ratio and
 * nothing stored: the population is derived from the evidence on every read (ADR 0021 §7).
 */
data class ShowcasePlate(
    val catalog: CollectionCatalog,
    val album: CollectionCatalogAlbum,
) {
    /**
     * The casillas the cut counts, the same divisor the plate uses: announced and unlisted members
     * are outside it, as in every other measurement.
     */
    val slots: Int get() = album.issuedMembers()
}

/**
 * This catalog as a plate of the shelf window, or null if it is not one. The two conditions of ADR
 * 0030 §1: no evidence (the fact `resolvePlate` opens a plate by, so a catalog is never both the
 * collector's and the window's) and fewer than [SHOWCASE_MAX_SLOTS] measurable casillas. Asked per
 * catalog because the plate also needs to know whether it opens without evidence.
 */
fun showcasePlate(
    catalog: CollectionCatalog,
    /**
     * The album the assembly built for this catalog (#537), the one the plate draws, so the tile
     * and the plate count the same casillas.
     */
    album: CollectionCatalogAlbum,
    evidencedCatalogIds: Set<String>,
): ShowcasePlate? {
    if (catalog.id in evidencedCatalogIds) return null
    val slots = album.issuedMembers()
    // Zero is no window: a file of announcements has no plate to open.
    if (slots == 0 || slots >= SHOWCASE_MAX_SLOTS) return null
    return ShowcasePlate(catalog, album)
}

/**
 * The whole shelf window, fewest casillas first (ADR 0030 §8): the smallest plate leads, the same
 * reading as the cut. The id breaks ties so the order is stable across reads.
 */
fun showcasePlates(
    catalogs: List<CollectionCatalog>,
    albums: CatalogAlbums,
    evidencedCatalogIds: Set<String>,
): List<ShowcasePlate> = catalogs
    .mapNotNull { catalog ->
        albums[catalog]?.let { album -> showcasePlate(catalog, album, evidencedCatalogIds) }
    }
    .sortedWith(compareBy({ it.slots }, { it.catalog.id }))
