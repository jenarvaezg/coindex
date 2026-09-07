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
 * The eight Numista series whose label is not the name of a collection in Spanish, and what a card
 * says instead (ADR 0031).
 *
 * It is [curedCountries] applied to the other field of the card that Numista writes, for the same
 * reason and with the same bargain: **six of these are in English**, which ADR 0021 §4 does not
 * want on a card, and the correction is a string the curator wrote rather than a mechanism that
 * guesses at a third party's prose. Measured over the collector's seeded cache on 7 September 2026:
 * 13 of his 198 types carry a real Numista family that no curated file claims, and they fall into
 * these ten series.
 *
 * **A table of corrections and not a catalogue of series.** The cache serves 64 distinct families
 * and most of them are already the name a collector would use — «The Queen's Beasts», «Vienna
 * Philharmonic», «Capitales de provincia y ciudades autónomas» — so what lives here is the
 * exception and the ficha is the default.
 *
 * **Two of the ten stay as Numista wrote them, and that is the measure of what this table is.**
 * `DC Comics` is a proper name, and `Gothic Horror` is the name of a Royal Mint range: the curated
 * files already keep a mint's own product name in its own language —«The Royal Tudor Beasts», «St
 * George and the Dragon», «Silver Britannia», «Equilibrium», «Nautical Ounce»— and a family that is
 * a range name is nothing else. What gets Spanish is the prose Numista wrote *about* a programme,
 * not the programme's name. The table cures case by case; it does not translate.
 *
 * **It cures the text and not the scope.** `Austria and its People` is Numista's umbrella over 18
 * types of three separate Münze Österreich programmes — castles, abbeys, and the legends where the
 * collector's «Charlemagne in the Untersberg» belongs (series 1582, read 7 September 2026) — so it
 * takes the translation of what Numista said and not «Leyendas de Austria», which would be a lie the
 * day a castle lands on the same card. The same restraint names the French series 10784 «Carlomagno»
 * rather than «100 francos Carlomagno»: it holds seven types and two of them are 500 francs. And the
 * anniversary framing of that label goes, because a scope definition does not belong on a line of
 * identity (ADR 0021 §4).
 *
 * `Millennium` spans four issuers —España, Gibraltar, Jamaica and Nueva Zelanda— which is why it is
 * «Milenio» and nothing more specific. `Hercules type` is the one name the source corroborates
 * itself: Numista's own page for series 9240 is written in Spanish and calls it «el tipo Hércules».
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
 * What a family reads as when no curated file names the collection.
 *
 * The six editorial aliases this used to hold died with #22: five belonged to families with a
 * catalog, and their text now lives in that file's `short_name`; the sixth, `SML`, was
 * unreachable — its six types all sit inside the maple leaf catalog, which declares another
 * family, and by ADR 0016 the catalog rules. What remains is not an alias but the formatting of
 * a generated string: a technical monetary system reaches the collector only this way (ADR 0012),
 * and the corrections of [curedFamilies], which are not aliases either — nothing curated names
 * these eight series, and a card with no file behind it is the only thing that reads them.
 *
 * Everything else is printed verbatim, in whatever language Numista wrote it. An ugly card name
 * is the visible debt of a collection nobody has curated yet, and hiding it behind a prettier
 * string in code would hide the work instead of doing it.
 *
 * A cured family is read through a function and never stored, which is the bargain [cardCountry],
 * the metal and the finish already take: `TypeMeta.family` stays what Numista said, the variant key
 * is keyed on that raw string, and so curing a label renames a card without moving a single coin
 * between cards.
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
