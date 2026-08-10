package com.jenarvaezg.coindex.domain

/**
 * The year a piece is placed by (ADR 0026 §9).
 *
 * Matching a slot still reads [CollectedItem.recordedYear] (engraved / Hijri when that is what the
 * coin says). Placement reads the Gregorian one first, so a ½ Dirham of 1316 lands in 1899 and the
 * axis does not stretch to the fourteenth century. Pieces with neither inherit the type's earliest
 * year (#326), which is what keeps the undated Portuguese escudos on the arc at all.
 *
 * **Zero is not a year.** Numista stores `0` on undated medals; treating it as a placement would
 * open the axis on year 0 and paint two thousand years of bare cardboard before 1780.
 *
 * In the domain and no longer beside the axis that first needed it: the year axis places a casilla by
 * this and «Las cifras» draws its arc of 1.756 years by this, and two copies of the rule are two
 * screens that can come to disagree about the same coin one tap apart.
 */
fun placementYear(item: CollectedItem, meta: TypeMeta?): Int? =
    listOfNotNull(item.gregorianYear, item.recordedYear, meta?.minYear)
        .firstOrNull { it > 0 }
