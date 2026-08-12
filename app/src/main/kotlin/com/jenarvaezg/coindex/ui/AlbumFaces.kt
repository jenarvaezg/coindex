package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.data.photos.TypeImages

/**
 * Which face a hole rests on where no plate declared one, and the face waiting behind it.
 *
 * A casilla obeys the catalog's `printed_side` and asks [printedPhoto] instead (ADR 0020, #302). The
 * two surfaces that reach for this have no such declaration to obey: the album grid of Monedas,
 * whose types belong to whatever catalogs claim them, and the pieces of a collection without an
 * issue list or of a box, which by construction has no catalog at all (ADR 0021 §9).
 *
 * Reverse first, because that is the face that **is** the coin: the seeded cache calls the shield of
 * the father's 1 Bolívar the anverso and the bust the reverso, and a commemorative reads by its
 * motif rather than by its portrait.
 *
 * The second face is null unless a photograph actually exists for it, so a hole never offers a turn
 * that would land on a silhouette — and the journey of ADR 0026 §3 takes off and lands on the same
 * photograph.
 */
internal fun coinAlbumFaces(images: TypeImages?): Pair<CoinPhoto?, CoinPhoto?> {
    val reverse = images?.reverse?.takeIf { it.hasPicture }
    val obverse = images?.obverse?.takeIf { it.hasPicture }
    return if (reverse != null) reverse to obverse else obverse to null
}
