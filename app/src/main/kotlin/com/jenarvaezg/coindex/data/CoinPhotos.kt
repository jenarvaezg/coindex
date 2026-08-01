package com.jenarvaezg.coindex.data

/**
 * The catalog pictures of one coin side, at the two sizes Numista publishes for it.
 *
 * Numista serves every photograph twice: `…-original.jpg`, which is the contributor's own
 * scan and weighs a couple hundred kilobytes, and `…-180.jpg`, the thumbnail, whose longest
 * side is 180 pixels. Both were always in the ficha; only the original was ever kept.
 */
data class CoinPhoto(val thumbnail: String? = null, val picture: String? = null) {
    /**
     * The URLs to try for this side, best first.
     *
     * The thumbnail leads because a plate cell is about a centimetre wide and a sheet of
     * nineteen issues asks for thirty-eight photographs at once: the originals added up to
     * eight megabytes in one burst, which Numista's edge answered with `503`. The original
     * stays behind it as the fallback, so a side whose thumbnail is missing or refused is
     * still a coin rather than a hole (issue #67).
     */
    val candidates: List<String> = listOfNotNull(thumbnail, picture).distinct()

    /** Whether this side has any picture at all to ask for. */
    val hasPicture: Boolean = candidates.isNotEmpty()
}

/**
 * Catalog picture URLs for one type. Kept out of the domain, which stays free of anything
 * presentational, and loaded straight from Numista by Coil — there is no proxy any more.
 */
data class TypeImages(
    val obverse: CoinPhoto = CoinPhoto(),
    val reverse: CoinPhoto = CoinPhoto(),
)
