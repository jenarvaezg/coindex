package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.TypeMeta

/**
 * How the notebook sheet is ordered (ADR 0026 §9).
 *
 * Not a destination: the stain and the year axis are the same álbum sheet with another order
 * chosen in the folded shelf. The default is today's Collections — by plate — so the app still
 * opens the same and nobody has to learn anything.
 */
enum class NotebookAxis(val label: String) {
    ByPlate("por lámina"),
    ByCountry("por país"),
    ByYear("por año"),
}

/**
 * The year a piece is placed by on an axis (ADR 0026 §9).
 *
 * Matching a slot still reads [CollectedItem.recordedYear] (engraved / Hijri when that is what the
 * coin says). Placement reads the Gregorian one first, so a ½ Dirham of 1316 lands in 1899 and the
 * axis does not stretch to the fourteenth century. Pieces with neither inherit the type's earliest
 * year (#326), which is what keeps the undated Portuguese escudos on the arc at all.
 */
fun placementYear(item: CollectedItem, meta: TypeMeta?): Int? =
    item.gregorianYear ?: item.recordedYear ?: meta?.minYear
