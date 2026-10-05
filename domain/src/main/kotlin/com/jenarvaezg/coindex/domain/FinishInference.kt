package com.jenarvaezg.coindex.domain

private val LUNAR_COLOURS = listOf(
    "blue", "golden", "lilac", "purple", "red", "teal", "white", "yellow",
)

/**
 * How a composition says a coin was coated in gold. A coating names an action (plated, chapado,
 * highlighted), so «Oro 999,9» is an alloy and not a finish; `gilded` and `gilt` carry both halves
 * and stand alone. Wordings seen so far (#573): «(with selective gold plating)», «chapado en oro»
 * (N#440309) and «(highlighted in 24-carat gold)».
 */
private val GOLD_WORDS = listOf("gold", "oro")
private val COATING_WORDS = listOf("plating", "plated", "chapad", "highlighted", "baño")
private val COATING_ALONE = listOf("gilded", "gilt")

/**
 * Infers the physical finish from a Numista type title and composition, with the composite Proof
 * coloured case resolved before either single finish. Numista exposes no stable finish field, so
 * these rules are the whole story and are deliberately auditable (ADR 0005).
 *
 * @param title the raw type title, in whatever language it was fetched
 * @param family the raw Numista `series` value, used for the two bullion series whose
 *   titles do not say "bullion"
 * @param composition the raw `composition.text`, the one field that says a coin is gilded when
 *   its title does not
 */
fun inferFinish(title: String?, family: String?, composition: String?): Finish? {
    if (isGoldCoated(composition)) return Finish.Gilded
    val lowered = title?.lowercase() ?: return null
    val proof = lowered.contains("proof")
    val coloured = lowered.contains("colour") ||
        lowered.contains("color") ||
        lowered.contains("coloread") ||
        lowered.contains("coloriz") ||
        isLunarColourVariant(lowered, family)
    return when {
        proof && coloured -> Finish.ProofColoured
        proof -> Finish.Proof
        coloured -> Finish.Coloured
        lowered.contains("gild") ||
            lowered.contains("dorad") ||
            lowered.contains("chapado en oro") -> Finish.Gilded
        lowered.contains("antiqu") || lowered.contains("acabado antiguo") -> Finish.Antiqued
        lowered.contains("bullion") ||
            family == "Lunar Series III" ||
            family == "The Royal Tudor Beasts" -> Finish.Bullion
        else -> null
    }
}

/**
 * Whether the composition declares a gold coating, which outranks every reading of the title.
 *
 * The gilded round pounds are titled «Silver Proof» exactly like those of `uk-1-libra-plata-proof`,
 * with the same weight and metal; only the composition tells them apart (`Silver (.925) (with
 * selective gold plating)`), and the curator ruled them `Gilded`, not `Proof` (#573), so no
 * composite finish is needed. Reverse descriptions and comments are not used: they describe gold in
 * the drawing or mention other editions.
 *
 * A coin made of gold is not gilded, so the guard reads the metal of the text before the coating
 * word only: [inferMetal] tries `oro` before `cobre`, and «Cobre chapado en oro» read whole would
 * come back gold.
 */
private fun isGoldCoated(composition: String?): Boolean {
    val lowered = composition?.lowercase() ?: return false
    val coatingAt = (COATING_WORDS + COATING_ALONE)
        .map(lowered::indexOf)
        .filter { it >= 0 }
        .minOrNull()
        ?: return false
    val gold = COATING_ALONE.any(lowered::contains) || GOLD_WORDS.any(lowered::contains)
    return gold && inferMetal(lowered.take(coatingAt)) != Metal.Gold
}

private fun isLunarColourVariant(loweredTitle: String, family: String?): Boolean =
    family == "Lunar Series III" &&
        LUNAR_COLOURS.any { colour -> loweredTitle.contains("year of the $colour ") }
