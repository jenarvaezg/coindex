package com.jenarvaezg.coindex.domain

/** Collapses whitespace so that " Lunar   ounce " and "Lunar ounce" are one family. */
fun normalizeFamily(family: String): String? {
    val normalized = family.split(Regex("\\s+")).filter(String::isNotEmpty).joinToString(" ")
    return normalized.ifEmpty { null }
}

/**
 * Numista's technical `System YYYY[-YYYY]` families are monetary systems, not collectible
 * groupings, so a curated catalog outranks them when both name a type (ADR 0012). They are
 * still families: a piece is never dropped for having one. "System of a Down" and
 * "System 19-2001" are not technical: every dash-separated segment must be a four-digit year.
 */
fun isTechnicalFamily(family: String): Boolean {
    val period = family.removePrefix("System ")
    if (period == family || period.isEmpty()) return false
    return period.split('-').all { year -> year.length == 4 && year.all(::isAsciiDigit) }
}

/**
 * What a family reads as when no curated file names the collection.
 *
 * The six editorial aliases this used to hold died with #22: five belonged to families with a
 * catalog, and their text now lives in that file's `short_name`; the sixth, `SML`, was
 * unreachable — its six types all sit inside the maple leaf catalog, which declares another
 * family, and by ADR 0016 the catalog rules. What remains is not an alias but the formatting of
 * a generated string: a technical monetary system reaches the collector only this way (ADR 0012).
 *
 * Everything else is printed verbatim, in whatever language Numista wrote it. An ugly card name
 * is the visible debt of a collection nobody has curated yet, and hiding it behind a prettier
 * string in code would hide the work instead of doing it.
 */
fun familyLabel(family: String): String = when {
    isTechnicalFamily(family) ->
        "Sistema monetario ${family.removePrefix("System ")}"
    else -> family
}
