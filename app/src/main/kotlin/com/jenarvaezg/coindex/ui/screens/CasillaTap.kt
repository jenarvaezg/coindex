package com.jenarvaezg.coindex.ui.screens

import com.jenarvaezg.coindex.domain.WishKey
import com.jenarvaezg.coindex.ui.CellPlaque
import com.jenarvaezg.coindex.ui.DrawnCell
import com.jenarvaezg.coindex.ui.ficha.CasillaOrigin

/** What a tap anywhere on a plate's casilla does: one target at a time (ADR 0026 §3). */
internal sealed interface CasillaTap {
    /** [casilla] is what the coin's screen learns from where it was tapped (#643). */
    data class Open(val typeId: Int, val casilla: CasillaOrigin) : CasillaTap

    data class Mark(val key: WishKey) : CasillaTap

    data object None : CasillaTap
}

/**
 * Outside the marking mode a casilla opens its coin's sheet; inside it, only an empty casilla with
 * a wish key takes the tap, to mark (ADR 0029 §1, §5). An announced member has no ficha to open.
 */
internal fun casillaTap(cell: DrawnCell, picking: Boolean): CasillaTap = when {
    picking -> cell.wishKey?.takeIf { cell.missing }?.let(CasillaTap::Mark) ?: CasillaTap.None
    else -> cell.numistaTypeId?.let { CasillaTap.Open(it, cell.origin()) } ?: CasillaTap.None
}

/** The casilla in the catalogue's terms, as its mark is keyed (ADR 0029 §1). */
private fun DrawnCell.origin(): CasillaOrigin = CasillaOrigin(
    year = wishKey?.year ?: year?.toIntOrNull(),
    issueId = wishKey?.issueId,
    missing = missing,
    dated = plaque is CellPlaque.Year,
)
