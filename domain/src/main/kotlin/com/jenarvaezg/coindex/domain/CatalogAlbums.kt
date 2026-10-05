package com.jenarvaezg.coindex.domain

/**
 * The album of every curated catalog, built once per assembly (#537) so every reader gets the same
 * instance: a card's ratio and its plate's «Progreso» are the same counters. Built against one
 * [PiecesByType] index, so an assembly costs one walk of the catalogued members.
 *
 * [get] returns null for a catalog the assembly never saw instead of building an album: that is a
 * wiring mistake (a collection read against another curation), not a state of the world.
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
