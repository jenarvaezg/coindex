package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.domain.PrintedSide

/**
 * The face a hole shows, and the one behind it, where no plate declares a `printed_side`: the album
 * grid of Monedas and the pieces of a collection without an issue list or of a box (ADR 0021 §9).
 * A casilla uses [printedFaces] instead (ADR 0020, #302).
 *
 * Reverse first: a commemorative is recognised by its motif, not its portrait. The second face is
 * null unless it has a photograph, so a hole never turns over onto a silhouette (ADR 0026 §3).
 */
internal fun coinAlbumFaces(images: TypeImages?): Pair<CoinPhoto?, CoinPhoto?> {
    val reverse = images?.reverse?.takeIf { it.hasPicture }
    val obverse = images?.obverse?.takeIf { it.hasPicture }
    return if (reverse != null) reverse to obverse else obverse to null
}

/**
 * The two faces of a casilla: the one its plate declares, then the other. The lámina and the annex
 * share it so the sheet a casilla opens arrives on the same face (ADR 0020, #227, #508).
 *
 * Unlike [coinAlbumFaces], a face without a photograph is kept: a hole draws the design's ghost.
 */
internal fun printedFaces(
    images: TypeImages?,
    side: PrintedSide,
): Pair<CoinPhoto?, CoinPhoto?> =
    images?.printedPhoto(side) to images?.printedPhoto(side.other)
