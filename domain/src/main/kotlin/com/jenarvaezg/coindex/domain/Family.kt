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
 * Words a family can consist of entirely and still name nothing: articles, a few conjunctions and
 * the genitive prepositions where a commercial name gets cut off. A short, closed list in the
 * languages fichas arrive in (the client asks for `lang=es`, but untranslated fields come as the
 * contributor wrote them) and those a mint's range name can begin with.
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
 * Whether a family is a field somebody started typing and did not finish, like the «The» of
 * N#596807 (#404): a real Numista series (`catalogue/series.php?id=13367`) whose contributor lost
 * the rest of *The Declaration of Independence*. Such a piece waits in the unclassified residue
 * until Numista is fixed and the ficha is asked for again (ADR 0025).
 *
 * Two limits keep this from judging names:
 * - every word must be a function word, so «The Royal Tudor Beasts» keeps its card;
 * - a family in full caps is an initialism (`SML`, `UN`), never a placeholder.
 */
fun isPlaceholderFamily(family: String): Boolean {
    val words = family.split(Regex("\\s+")).filter(String::isNotEmpty)
    if (words.isEmpty() || family == family.uppercase()) return false
    return words.all { word -> word.trim { !it.isLetter() }.lowercase() in FUNCTION_WORDS }
}

/**
 * Numista series whose label is not the name of a collection in Spanish, and what a card says
 * instead (ADR 0031). Like [curedCountries], these are corrections the curator wrote, only for
 * the exceptions: most families already read as a collector would name them.
 *
 * Proper names and a mint's own range names stay as Numista wrote them (`DC Comics`, `Gothic
 * Horror`), just as the curated files keep «Silver Britannia». What gets Spanish is Numista's prose
 * about a programme.
 *
 * A correction cures the text, not the scope: `Austria and its People` covers three Münze
 * Österreich programmes (series 1582), so it is «Austria y su pueblo» and not «Leyendas de
 * Austria»; series 10784 holds 100 and 500 francs, so it is «Carlomagno»; and an anniversary
 * framing goes, since a scope definition does not belong on a line of identity (ADR 0021 §4).
 * `Millennium` spans four issuers, hence «Milenio». Numista's own Spanish page for series 9240
 * says «el tipo Hércules».
 */
private val curedFamilies: Map<String, String> = mapOf(
    "100 francs Egalité - La Fayette" to "100 francos La Fayette",
    "1190e anniversaire du couronnement de Charlemagne (800-1990)." to "Carlomagno",
    "Austria and its People" to "Austria y su pueblo",
    "Charlemagne - Mounted Knight" to "Carlomagno · el caballero",
    "Contemporary Urban Art" to "Arte urbano contemporáneo",
    "French regions" to "Euros de las regiones francesas",
    "Hercules type" to "Tipo Hércules",
    "Millennium" to "Milenio",
)

/**
 * What a family reads as when no curated file names the collection: a technical monetary system
 * formatted (ADR 0012), a correction from [curedFamilies], or else the family verbatim in whatever
 * language Numista wrote it. An ugly card name is the visible debt of an uncurated collection; the
 * fix is a curated `short_name` (#22), not an alias in code.
 *
 * Read through a function and never stored, like [cardCountry]: `TypeMeta.family` stays raw and the
 * variant key is keyed on it, so curing a label renames a card without moving coins between cards.
 */
fun familyLabel(family: String): String = when {
    curedFamilies.containsKey(family) -> curedFamilies.getValue(family)
    isTechnicalFamily(family) ->
        "Sistema monetario ${family.removePrefix("System ")}"
    else -> family
}

/** The corrected families, for the net that checks them against the cache that ships (ADR 0031). */
@SuiteOnly
fun curedFamilyLabels(): Map<String, String> = curedFamilies
