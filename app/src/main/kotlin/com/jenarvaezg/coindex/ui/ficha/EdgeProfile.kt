package com.jenarvaezg.coindex.ui.ficha

/** How an edge is drawn in profile under the coin (#643). */
enum class EdgePattern { Reeded, Plain, Ornamented }

/**
 * The edge as the coin's screen draws it, read from Numista's description rather than its
 * photograph, which each contributor shoots differently.
 *
 * @param heightOverWidth the strip's proportion to the photograph above it, thickness over
 *   diameter; null when either is missing, and the strip is drawn at a nominal height.
 */
data class EdgeProfile(
    val pattern: EdgePattern,
    val lettering: String?,
    val heightOverWidth: Double?,
)

/** Null when the ficha says nothing about the edge: no drawing is better than a guessed one. */
fun edgeProfile(
    description: String?,
    lettering: String?,
    thicknessMillimetres: Double?,
    diameterMillimetres: Double?,
): EdgeProfile? {
    val said = description?.takeIf(String::isNotBlank)
    val legend = lettering?.takeIf(String::isNotBlank)
    if (said == null && legend == null) return null
    val ratio = if (thicknessMillimetres != null && diameterMillimetres != null) {
        thicknessMillimetres / diameterMillimetres
    } else {
        null
    }
    return EdgeProfile(
        pattern = said?.let(::patternOf) ?: EdgePattern.Plain,
        lettering = legend,
        heightOverWidth = ratio?.takeIf { it > 0.0 },
    )
}

/**
 * The first sentence names the edge; later ones describe its details, such as a security mark or
 * the ornament between the legend's ends.
 */
private fun patternOf(description: String): EdgePattern {
    val head = description.split('.', '(', '\n', '\r').first().lowercase()
    val lettered = LETTERS.any { it in head }
    return when {
        !lettered && ORNAMENTS.any { it in head } -> EdgePattern.Ornamented
        REEDS.any { it in head } -> EdgePattern.Reeded
        else -> EdgePattern.Plain
    }
}

/** Spanish and English: half the fichas reach the phone untranslated. */
private val REEDS = listOf(
    "reed", "reeg", "milled", "serrat", "fresad", "ranura", "dentad", "carrizo", "estriad",
    "lengüeta",
)

private val ORNAMENTS = listOf("motivo", "diseño", "decor", "ornament", "relieve")

private val LETTERS = listOf("letra", "inscri", "letter")

/** A stretch of a legend, and whether it is a digit to draw upside down. */
data class LetteringRun(val text: String, val turned: Boolean = false)

/**
 * Splits a legend around Unicode's turned digits (`↊`, `↋`), which neither Bitter nor Barlow has:
 * each becomes its digit, drawn turned over.
 */
fun letteringRuns(text: String): List<LetteringRun> {
    val runs = mutableListOf<LetteringRun>()
    val plain = StringBuilder()
    for (char in text) {
        val digit = TURNED_DIGITS[char]
        if (digit == null) {
            plain.append(char)
        } else {
            if (plain.isNotEmpty()) runs += LetteringRun(plain.toString())
            plain.clear()
            runs += LetteringRun(digit, turned = true)
        }
    }
    if (plain.isNotEmpty()) runs += LetteringRun(plain.toString())
    return runs
}

private val TURNED_DIGITS = mapOf('↊' to "2", '↋' to "3")
