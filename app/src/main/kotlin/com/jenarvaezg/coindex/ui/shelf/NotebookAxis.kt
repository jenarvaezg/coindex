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
    ByPlate("Por lámina"),
    ByCountry("Por país"),
    ByYear("Por año"),
}

/**
 * The year a piece is placed by on an axis (ADR 0026 §9).
 *
 * Matching a slot still reads [CollectedItem.recordedYear] (engraved / Hijri when that is what the
 * coin says). Placement reads the Gregorian one first, so a ½ Dirham of 1316 lands in 1899 and the
 * axis does not stretch to the fourteenth century. Pieces with neither inherit the type's earliest
 * year (#326), which is what keeps the undated Portuguese escudos on the arc at all.
 *
 * **Zero is not a year.** Numista stores `0` on undated medals; treating it as a placement would
 * open the axis on year 0 and paint two thousand years of bare cardboard before 1780.
 */
fun placementYear(item: CollectedItem, meta: TypeMeta?): Int? =
    listOfNotNull(item.gregorianYear, item.recordedYear, meta?.minYear)
        .firstOrNull { it > 0 }
