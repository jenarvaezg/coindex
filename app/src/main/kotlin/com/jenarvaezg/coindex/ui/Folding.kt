package com.jenarvaezg.coindex.ui

import java.text.Normalizer
import java.util.Locale

private val COMBINING_MARKS = Regex("\\p{Mn}+")

/**
 * Text as the search box compares it: lower case and without accents (ADR 0021 §1), so «bolivar»
 * finds «Bolívar». NFD plus dropping the combining marks covers every accent, «ü» included.
 */
fun fold(text: String): String =
    COMBINING_MARKS
        .replace(Normalizer.normalize(text, Normalizer.Form.NFD), "")
        .lowercase(Locale.ROOT)

/**
 * Whether a folded haystack contains every word of a raw query, in any order: «panda plata» finds
 * the silver Panda. An empty query matches everything.
 */
fun matchesQuery(haystack: String, query: String): Boolean {
    val words = fold(query).split(' ').filter(String::isNotEmpty)
    return words.all { word -> haystack.contains(word) }
}
