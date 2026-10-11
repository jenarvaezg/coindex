package com.jenarvaezg.coindex.ui.ficha

import com.jenarvaezg.coindex.domain.PrintedSide

/**
 * What a surface asks for when it opens a coin's screen (#643): the coin, the face it was showing,
 * and the casilla it was tapped in, if any.
 */
data class CoinFichaRequest(
    val typeId: Int,
    /** The face up on arrival: a casilla's plate's `printed_side`, else the reverse (ADR 0020). */
    val side: PrintedSide = PrintedSide.Reverse,
    val casilla: CasillaOrigin? = null,
)

/**
 * The casilla a coin's screen was opened from, in the catalogue's terms rather than Numista's, as a
 * wish is keyed (ADR 0029 §1).
 *
 * @param year the casilla's year, which prices a hole through the stored listing.
 * @param issueId the issue the curated file names for it, if any.
 * @param missing whether the casilla is empty: the screen then says what it would cost.
 * @param dated whether the year is what tells the casilla apart, as on a date run: the screen then
 *   opens on that date's pieces.
 */
data class CasillaOrigin(
    val year: Int?,
    val issueId: Int? = null,
    val missing: Boolean = false,
    val dated: Boolean = false,
)

/** The face as a route writes it. */
internal fun PrintedSide.routeCode(): String = when (this) {
    PrintedSide.Obverse -> "obverse"
    PrintedSide.Reverse -> "reverse"
}

/**
 * The request a coin route carries, or null without a type id. A casilla is present when its
 * `missing` flag is: year and issue are optional even then.
 */
fun coinFichaRequestOf(
    typeId: String?,
    side: String?,
    year: String?,
    issue: String?,
    missing: String?,
    dated: String?,
): CoinFichaRequest? = CoinFichaRequest(
    typeId = typeId?.toIntOrNull() ?: return null,
    side = if (side == "obverse") PrintedSide.Obverse else PrintedSide.Reverse,
    casilla = missing?.let {
        CasillaOrigin(
            year = year?.toIntOrNull(),
            issueId = issue?.toIntOrNull(),
            missing = it.toBoolean(),
            dated = dated.toBoolean(),
        )
    },
)
