package com.jenarvaezg.coindex.data.photos

/**
 * The catalog pictures of one coin side, at the two sizes Numista publishes: `…-original.jpg`,
 * the contributor's scan, and `…-180.jpg`, the thumbnail, 180 pixels on its longest side.
 */
data class CoinPhoto(val thumbnail: String? = null, val picture: String? = null) {
    /**
     * The URLs to try for this side, best first. The thumbnail leads: a plate cell is small, and a
     * sheet asking for every original at once gets `503`s from Numista's edge. The original is the
     * fallback, so a missing or refused thumbnail still shows a coin (#67).
     */
    val candidates: List<String> = listOfNotNull(thumbnail, picture).distinct()

    /** Whether this side has any picture at all to ask for. */
    val hasPicture: Boolean = candidates.isNotEmpty()
}

/**
 * Catalog picture URLs for one type, kept out of the domain and loaded straight from Numista by
 * Coil.
 */
data class TypeImages(
    val obverse: CoinPhoto = CoinPhoto(),
    val reverse: CoinPhoto = CoinPhoto(),
)
