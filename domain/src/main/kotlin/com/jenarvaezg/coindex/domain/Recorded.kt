package com.jenarvaezg.coindex.domain

/**
 * What counts as a fact somebody recorded. Numista's volunteer catalogue often leaves a hole as an
 * empty string or a zero rather than an absent field; whether that is data is a catalogue question,
 * so it is answered here and not in the JSON reader.
 */

/** The text if somebody wrote it: a blank issuer name means none, not one called «». */
fun recordedText(value: String?): String? = value?.takeIf(String::isNotBlank)

/** The diameter if somebody measured it: zero is an empty field, so never «Ø 0 mm». */
fun recordedDiameter(millimetres: Double?): Double? = millimetres?.takeIf { it > 0.0 }
