package com.jenarvaezg.coindex.ui

/** A coin's album name: the value on the first range and its subject on the second. */
data class CoinName(
    val denomination: String,
    val theme: String?,
) {
    /** Plain-text form for surfaces that cannot preserve the two typographic ranges. */
    val text: String get() = listOfNotNull(denomination, theme).joinToString(" / ")
}

private val quotedNickname = Regex("[\"“”«]([^\"“”»]+)[\"“”»]")
private val nestedAside = Regex("\\([^()]*\\)")
private val silverWeightTail = Regex(
    """(?i)^(?:\d+(?:/\d+)?|¼|½|¾)\s*oz(?:\s+fine)?\s+silver$""",
)
private val portraitTail = Regex(
    """(?i)^(?:\d+(?:st|nd|rd|th)|first|second|third|fourth)\s+portrait$""",
)
private val bullionTail = Regex("""(?i)^.*\bbullion coin(?:age)?\b.*$""")
private val proseTails = setOf(
    "silver",
    "silver proof",
    "proof",
)

/** Derives the two ranges of an album cartouche from Numista's full title. */
fun coinName(title: String): CoinName {
    val raw = title.trim()
    val nicknameMatch = quotedNickname.find(raw)
    val nickname = nicknameMatch?.groupValues?.get(1)?.trim().takeUnless { it.isNullOrEmpty() }
    val openParenthesis = raw.indexOf('(')
    val closeParenthesis = raw.lastIndexOf(')')
    val hasParenthetical = openParenthesis >= 0 && closeParenthesis > openParenthesis
    val parenthetical = if (hasParenthetical) {
        raw.substring(openParenthesis + 1, closeParenthesis)
    } else {
        ""
    }
    val outside = if (hasParenthetical) {
        raw.removeRange(openParenthesis, closeParenthesis + 1).trim()
    } else {
        raw
    }
    val withoutNickname = quotedNickname.replace(outside, "").trim()
    val head = withoutNickname.split(" - ").map(String::trim).filter(String::isNotEmpty)
    val denomination = quotedNickname.find(outside)
        ?.let { outside.substring(0, it.range.first).trim() }
        ?.takeIf(String::isNotBlank)
        ?: head.firstOrNull().orEmpty().ifBlank { raw }
    val intermediate = head.drop(1)
        .map(::cleanCandidate)
        .filter(String::isNotBlank)
        .joinToString(" - ")
        .ifBlank { null }
    val parenthesized = parenthetical
        .split(Regex("""\s+-\s+|[;,]"""))
        .asSequence()
        .map(::cleanCandidate)
        .firstOrNull(String::isNotBlank)
    val theme = when {
        nickname != null -> nickname
        parenthesized == null -> intermediate
        parenthesized.equals("Euro", ignoreCase = true) && intermediate != null -> intermediate
        else -> parenthesized
    }
    return CoinName(denomination, theme)
}

private fun cleanCandidate(value: String): String {
    val candidate = nestedAside.replace(value, "").trim(' ', '-', '·', ';', ',')
    val noise = candidate.lowercase() in proseTails ||
        bullionTail.matches(candidate) || silverWeightTail.matches(candidate) ||
        portraitTail.matches(candidate)
    return candidate.takeUnless { noise }.orEmpty()
}
