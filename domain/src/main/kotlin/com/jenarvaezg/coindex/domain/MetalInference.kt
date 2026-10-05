package com.jenarvaezg.coindex.domain

/**
 * Compositions with no single dominant metal, checked before any metal name: bimetallic coins
 * (core and ring) and clad ones («Cobre recubierto de cuproníquel»), which would otherwise read as
 * whichever metal their text names first.
 */
private val NO_DOMINANT_METAL = listOf("bimetál", "bimetal", "recubiert", "clad")

/**
 * Needle to metal, in order: the first match wins, so a compound name precedes the metals it
 * contains (cupronickel before copper and nickel).
 *
 * Fichas are fetched in Spanish (`lang=es`), so Spanish spellings lead; English ones stay because
 * a Spanish ficha's parentheses often hold English, and a language change should not empty this.
 */
private val METAL_NEEDLES: List<Pair<List<String>, Metal>> = listOf(
    // Billon is a low-grade silver alloy and the collector calls it silver (#40).
    listOf("vellón", "vellon", "billon") to Metal.Silver,
    listOf(
        "cuproníquel",
        "cuproniquel",
        "cupronickel",
        "cupro-nickel",
        "copper-nickel",
    ) to Metal.Cupronickel,
    listOf("latón", "laton", "brass") to Metal.Brass,
    listOf("bronce", "bronze") to Metal.Bronze,
    // «Nickel silver» is a copper-zinc-nickel alloy with no silver, so it must match before silver.
    listOf("nickel silver") to Metal.Copper,
    listOf("platino", "platinum") to Metal.Platinum,
    listOf("paladio", "palladium") to Metal.Palladium,
    listOf("plata", "silver") to Metal.Silver,
    listOf("oro", "gold") to Metal.Gold,
    listOf("cobre", "copper") to Metal.Copper,
    listOf("níquel", "niquel", "nickel") to Metal.Nickel,
    listOf("acero", "steel") to Metal.Steel,
    listOf("cinc", "zinc") to Metal.Zinc,
    listOf("aluminio", "aluminium", "aluminum") to Metal.Aluminium,
)

/**
 * Infers the dominant metal from Numista's `composition.text` by auditable rules, like
 * [inferFinish] (ADR 0005).
 *
 * Everything from the first parenthesis on is dropped: «Plata 999 (highlighted in 24-carat gold)»
 * is a silver coin with a gilded detail, and a parenthesis only qualifies the alloy the head names.
 *
 * Null when nothing is recognised, which is not [Metal.Other]: «no dominant metal» is a claim, and
 * an unread text supports none.
 */
fun inferMetal(composition: String?): Metal? {
    val head = composition?.substringBefore('(')?.lowercase()?.trim() ?: return null
    if (head.isEmpty()) return null
    if (NO_DOMINANT_METAL.any { needle -> head.contains(needle) }) return Metal.Other
    return METAL_NEEDLES
        .firstOrNull { (needles, _) -> needles.any { needle -> head.contains(needle) } }
        ?.second
}
