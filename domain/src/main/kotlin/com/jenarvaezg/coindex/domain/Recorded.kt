package com.jenarvaezg.coindex.domain

/**
 * What counts as a fact somebody actually recorded, as opposed to a field that merely exists.
 *
 * Numista's catalogue is filled in by volunteers and is full of holes, and a hole does not always
 * arrive as an absent field: it arrives as an empty string where the issuer's name goes, or as a
 * zero where the diameter goes. Whether those are data is a question about the catalogue and not
 * about JSON, which is why the answer lives here rather than inside whatever reads the ficha.
 */

/**
 * The text if somebody wrote it.
 *
 * A blank issuer name, a blank composition, a blank category: the field is present and says
 * nothing, so the coin has no issuer name rather than one called «». Printed on a card, the
 * difference is a label with nothing under it.
 */
fun recordedText(value: String?): String? = value?.takeIf(String::isNotBlank)

/**
 * The diameter if somebody measured it.
 *
 * Zero millimetres is not a very small coin, it is a field nobody filled in. The printed notebook
 * prints «Ø 40,6 mm» or nothing at all, never «Ø 0 mm».
 */
fun recordedDiameter(millimetres: Double?): Double? = millimetres?.takeIf { it > 0.0 }
