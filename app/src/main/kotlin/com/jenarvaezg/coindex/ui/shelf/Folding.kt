package com.jenarvaezg.coindex.ui.shelf

import java.text.Normalizer
import java.util.Locale

private val COMBINING_MARKS = Regex("\\p{Mn}+")

/**
 * Text as the search box compares it: lower case and stripped of accents (ADR 0021 §1).
 *
 * Accent insensitivity is not a nicety here. The corpus is written in Spanish by rule (§4) and
 * typed on a phone keyboard: «bolivar» has to find «Bolívar de Venezuela» and «Ruanda» has to find
 * itself when the collector spells it without thinking. Decomposing to NFD and dropping the
 * combining marks does it for every accent at once, rather than through a table of letters that
 * would forget «ü» the first time a German series arrived.
 */
fun fold(text: String): String =
    COMBINING_MARKS
        .replace(Normalizer.normalize(text, Normalizer.Form.NFD), "")
        .lowercase(Locale.ROOT)

/**
 * Whether a folded haystack answers a raw query.
 *
 * Every word has to appear, in any order and anywhere: «panda plata» finds the silver Panda
 * without the collector having to remember which word the card puts first. An empty query matches
 * everything, so a screen with nothing typed is never a filtered screen.
 */
fun matchesQuery(haystack: String, query: String): Boolean {
    val words = fold(query).split(' ').filter(String::isNotEmpty)
    return words.all { word -> haystack.contains(word) }
}
