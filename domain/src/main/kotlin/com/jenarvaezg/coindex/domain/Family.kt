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
 * Presentation-only alias for a resolved proposal family. Never enters the proposal variant
 * key, grouping or persisted dispositions.
 */
fun collectionProposalFamilyLabel(family: String): String = when {
    isTechnicalFamily(family) ->
        "Sistema monetario ${family.removePrefix("System ")}"
    else -> curatedFamilyLabel(family)
}

private fun curatedFamilyLabel(family: String): String = when (family) {
    "SML" -> "Silver Maple Leaf"
    // The collector calls them paquillos; the key stays the coin's catalog name.
    "100 Pesetas de Franco" -> "Paquillos · 100 pesetas de Franco"
    "Red Data Book" -> "Libro Rojo de Rusia"
    "Serie de monedas de plata obtenidas a valor facial" ->
        "Monedas españolas de plata a valor facial"
    "Lunar ounce" -> "Rwanda Lunar Ounce"
    "Nautical Ounce" -> "Rwanda Nautical Ounce"
    else -> family
}
