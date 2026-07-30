package com.jenarvaezg.coindex.domain

private val LUNAR_COLOURS = listOf(
    "blue", "golden", "lilac", "purple", "red", "teal", "white", "yellow",
)

/**
 * Infers the physical finish from a Numista type title, with the composite Proof coloured
 * case resolved before either single finish. Numista exposes no stable finish field, so
 * these rules are the whole story and are deliberately auditable (ADR 0005).
 *
 * @param title the raw type title, in whatever language it was fetched
 * @param family the raw Numista `series` value, used for the two bullion series whose
 *   titles do not say "bullion"
 */
fun inferFinish(title: String?, family: String?): Finish? {
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

private fun isLunarColourVariant(loweredTitle: String, family: String?): Boolean =
    family == "Lunar Series III" &&
        LUNAR_COLOURS.any { colour -> loweredTitle.contains("year of the $colour ") }
