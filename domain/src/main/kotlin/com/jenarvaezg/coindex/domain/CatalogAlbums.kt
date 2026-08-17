package com.jenarvaezg.coindex.domain

/**
 * The album of every curated catalog, built once per assembly (#537).
 *
 * **The invariant «one card and its plate come out of the same album» stops being prose here.** It
 * used to be argued in a comment and guaranteed by nobody: the index built an album to divide its
 * card by, the plate built a second one to draw its casillas from, and the two agreed only as long
 * as nobody edited one of the six places that called [buildCollectionCatalogAlbum]. Now there is one
 * album per catalog per assembly and every reader is handed **that instance** — a card's ratio and
 * its plate's «Progreso» are literally the same four counters, not two readings that match.
 *
 * It is built against one [PiecesByType] index rather than the whole inventory, so an assembly costs
 * a walk of the catalogued members instead of `members × pieces` once per reader.
 *
 * A catalog it does not hold is a catalog the assembly never saw, which is a collection assembled by
 * one curation being read against another. [get] says so with a null rather than quietly building a
 * seventh album, because that null is a wiring mistake and not a state of the world.
 */
data class CatalogAlbums(
    private val byCatalogId: Map<String, CollectionCatalogAlbum> = emptyMap(),
) {
    operator fun get(catalog: CollectionCatalog): CollectionCatalogAlbum? = byCatalogId[catalog.id]

    /** The same reading for a caller holding an id and not the file, which is what a screen has. */
    fun of(catalogId: String): CollectionCatalogAlbum? = byCatalogId[catalogId]

    companion object {
        fun over(
            catalogs: List<CollectionCatalog>,
            items: List<CollectedItem>,
        ): CatalogAlbums {
            val pieces = piecesByType(items)
            return CatalogAlbums(
                catalogs.associate { catalog ->
                    catalog.id to buildCollectionCatalogAlbum(catalog, pieces)
                },
            )
        }
    }
}
