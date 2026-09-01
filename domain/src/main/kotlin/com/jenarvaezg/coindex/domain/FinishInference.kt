package com.jenarvaezg.coindex.domain

private val LUNAR_COLOURS = listOf(
    "blue", "golden", "lilac", "purple", "red", "teal", "white", "yellow",
)

/**
 * How a composition says the coin was coated in gold, and how it says it is made of it.
 *
 * A coating always names an action — plated, chapado, highlighted — and that verb is the whole
 * rule: «Oro 999,9» names gold and no action, so it is an alloy and not a finish. `gilded` and
 * `gilt` carry both halves in one word and stand alone.
 *
 * Measured on 1 September 2026 over the 1,089 seeded fichas and the 44 pound sterling ones fetched
 * for #573: three wordings appear and no other, «(with selective gold plating)» in the eleven
 * gilded pounds, «chapado en oro» in N#440309 and «(highlighted in 24-carat gold)» in the koala.
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
 * It has to outrank them, and that is the finding of #573: the fifteen gilded round pounds are
 * titled «Silver Proof» **word for word** like the thirty-two of `uk-1-libra-plata-proof`, same 9,5
 * g and same silver, so a title-only rule keys them as the plate's own variant and stands a second
 * card next to it. What separates them is a field — `Silver (.925) (with selective gold plating)`
 * against `Silver (.925)`, measured in all forty-four fichas, eleven and thirty-three with neither
 * a false positive nor a false negative — and the curator already ruled which finish wins when both
 * are true: «Son `Gilded`, no `Proof`». There is no composite Proof gilded and this is why one is
 * not needed.
 *
 * The reverse description was the other candidate the issue proposed and the seeded cache refuses
 * it: «un fénix **dorado**» (N#511576, N#519836), «la inserción **dorada** … Au 999» (N#80877), «La
 * sala de conciertos **dorada**» (N#9165). A description of a face describes the drawing, and gold
 * leaf on the coin and gold on the thing drawn are the same sentence. The comments are worse still
 * — ten of the thirty-two pounds that do belong mention the 2008 gilded edition without being it.
 *
 * The composition is the field that cannot say that, because it is about the material and nothing
 * else. Which is why [inferMetal] reads the head of this same string and drops what follows the
 * bracket: the head answers what the coin is made of, and what it drops answers how it was
 * finished. Neither reading is complete without the other, and a coin **of** gold is not a gilded
 * one — so the dominant metal is the guard rail, and no needle has to be spelled as an exception.
 *
 * That guard reads what is written **before** the coating word rather than the whole string, and
 * that is not a detail: [inferMetal] resolves its needles in order and `oro` precedes `cobre`, so
 * «Cobre chapado en oro» asked whole comes back gold and the gilding would be dropped as an alloy.
 * What a coin is made of is what the composition says before it says what was laid on top.
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
