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
 * The words a family can be made **entirely** of and still name nothing: articles, the two or
 * three conjunctions, and the genitive prepositions where a commercial name gets cut off. The
 * list is closed and it is short on purpose — it is not a guess about which families are ugly.
 *
 * Both languages the fichas arrive in are here (the client asks for `lang=es`, and Numista serves
 * whatever the contributor wrote when there is no translation), plus the neighbours a Royal Mint
 * or a Monnaie de Paris name can begin with.
 */
private val FUNCTION_WORDS = setOf(
    // English
    "the", "a", "an", "and", "of",
    // Spanish
    "el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "y", "e",
    // French
    "le", "les", "du", "des", "et",
    // Portuguese
    "o", "os", "as", "do", "da", "dos", "das",
    // Italian
    "il", "lo", "gli", "di",
    // German
    "der", "die", "das", "und",
)

/**
 * Whether a family is a field somebody started typing and did not finish, like the «The» that
 * N#596807 declares (#404).
 *
 * A family with no word of its own is not a name: it is the first keystrokes of one. Measured on
 * 11 August 2026 against Numista itself, «The» is a real series of the catalogue —
 * `catalogue/series.php?id=13367` — holding exactly one type, the 2 Pounds of one ounce whose
 * range The Royal Mint sells as *The Declaration of Independence*, so what the contributor lost
 * was the rest of the words and not the series. Printing it verbatim gave the collector a card
 * called «The» over a single coin, and the residue holds that piece more honestly than a card
 * does until Numista is fixed and somebody asks for the ficha again (ADR 0025).
 *
 * Two limits keep this from deciding what a good name is:
 *
 * - **Every** word must be a function word. «The Royal Tudor Beasts» and «Noah's Ark» keep their
 *   cards, because they say something besides the article.
 * - An initialism is a name and not a fragment. `SML` is a family of the seeded cache and `UN`
 *   could be another tomorrow, so a family written in full caps is never a placeholder.
 *
 * Measured over the seeded snapshot the day it was written: 858 fichas carry a family, 64 distinct
 * ones, and not one of them is caught by this.
 */
fun isPlaceholderFamily(family: String): Boolean {
    val words = family.split(Regex("\\s+")).filter(String::isNotEmpty)
    if (words.isEmpty() || family == family.uppercase()) return false
    return words.all { word -> word.trim { !it.isLetter() }.lowercase() in FUNCTION_WORDS }
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
