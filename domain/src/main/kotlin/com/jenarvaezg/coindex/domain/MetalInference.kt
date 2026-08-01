package com.jenarvaezg.coindex.domain

/**
 * A composition whose text names no single dominant metal, checked before any metal name.
 *
 * These are the two shapes measured in the seeded cache: the bimetallic 500 bolívares, whose text
 * describes a core and a ring, and «Cobre recubierto de cuproníquel», a copper core clad in
 * something else. Both would otherwise be read as whichever metal their text happens to say first.
 */
private val NO_DOMINANT_METAL = listOf("bimetál", "bimetal", "recubiert", "clad")

/**
 * Needle to metal, **in order**: the first match wins, so a compound name always precedes the
 * metals it contains — cupronickel before copper and nickel, aluminium bronze before aluminium.
 *
 * Numista is fetched in Spanish (`lang=es`), so the Spanish spelling leads; the English one is
 * kept because the parentheses of a Spanish ficha routinely hold English prose, and a future
 * language change should not silently empty this field.
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
    // «Nickel silver» is a copper-zinc-nickel alloy with no silver in it at all, so the word has
    // to be spent before the silver rule ever sees it.
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
 * Infers the dominant metal from Numista's `composition.text`, with auditable rules in the spirit
 * of [inferFinish] (ADR 0005): Numista has no metal field either, only prose.
 *
 * Everything in parentheses is dropped first, and that is not cosmetic. The Koala of 2016 reads
 * «Plata 999 (highlighted in 24-carat gold)»: it is a silver ounce with a gilded detail, and the
 * only part of that sentence that describes what the coin is made of is the part before the
 * bracket. The breakdown a parenthesis holds is always about the alloy the head already named.
 *
 * Returns null when nothing is recognised, which is not [Metal.Other]: «no dominant metal» is a
 * claim, and an unread text supports none.
 */
fun inferMetal(composition: String?): Metal? {
    val head = composition?.substringBefore('(')?.lowercase()?.trim() ?: return null
    if (head.isEmpty()) return null
    if (NO_DOMINANT_METAL.any { needle -> head.contains(needle) }) return Metal.Other
    return METAL_NEEDLES
        .firstOrNull { (needles, _) -> needles.any { needle -> head.contains(needle) } }
        ?.second
}
