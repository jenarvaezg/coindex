package com.jenarvaezg.coindex.domain

/**
 * The year a piece is placed by on the year axis and in «Las cifras» (ADR 0026 §9): the Gregorian
 * year first, then the recorded one, then the type's earliest year (#326), so a ½ Dirham of 1316
 * lands in 1899. Matching a slot still reads [CollectedItem.recordedYear].
 *
 * Zero is skipped: Numista stores `0` on undated medals, which would open the axis on year 0.
 */
fun placementYear(item: CollectedItem, meta: TypeMeta?): Int? =
    listOfNotNull(item.gregorianYear, item.recordedYear, meta?.minYear)
        .firstOrNull { it > 0 }
