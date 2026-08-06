package com.jenarvaezg.coindex.domain

/**
 * The ten issuer codes whose Numista label is not the name of a country in Spanish, and what a card
 * says instead.
 *
 * Numista does not write countries: it writes **issuing entities with their period of validity**,
 * which is why `russie` arrives as «Federación de Rusia (1991-presente)» and `rome` as «Romano,
 * Imperio (27 a. C. - 395 d. C.)». Both are correct as catalogue data — the parenthesis is what
 * tells the Russian Federation apart from `ancienne_urss`, and that distinction is real — so this
 * is not something to fix on numista.com. It is ours at the moment we paint it.
 *
 * **A table of corrections and not a catalogue of countries.** The seeded cache serves 40 issuer
 * codes and 31 of them already read as a country in Spanish; keeping «Venezuela» here would
 * duplicate in Kotlin what the ficha says right, and a coin from a country nobody owns yet would
 * label its card with no line added. What lives here is the exception, and the ficha is the default.
 *
 * **Why a table and not a heuristic.** Cutting at the first `(` and un-inverting on the comma is one
 * line of code over a third party's prose, and it gets each of these subtly wrong: «Federación de
 * Rusia» is still not the name of a country, «Imperio Romano» carries Numista's capital inside it,
 * and «Alemania, República Federal de» either loses its tail or keeps a scope definition on a line
 * of identity. Nine strings the curator wrote are worth more than a mechanism that guesses.
 *
 * **What each correction says depends on what the entity is** (ADR 0023): a country served with its
 * period, or inverted for an index, gives its common Spanish name — the Russian Federation *is*
 * today's Russia — while a state that is nobody's country any more keeps its own name, which is why
 * `russia-empire` is «Imperio ruso» and not «Rusia», beside the `ancienne_urss` the ficha already
 * calls «Unión Soviética». `allemagne-pre1945` is «Alemania» because that is what Numista itself
 * calls it: «Alemania (1871-1948)» is a country with a period, not the name of another state.
 *
 * **The tenth is here for a third reason: the language** (#257). `new_south_wales` arrives as «New
 * South Wales» even with `lang=es`, which is a clean label — no period of validity, no inversion, 15
 * characters — and therefore the one vice [readsAsACountry] cannot see. It is the only English label
 * among the 25 issuer codes the curated files declare, measured over the shipped cache, and ADR 0021
 * §4 asks for Spanish. It takes its own name rather than «Australia» by the second rule above: in
 * 1813 it was a British colony and today it is a state, so it is nobody's country and «Nueva Gales
 * del Sur» is what it is called.
 */
private val curedCountries: Map<String, String> = mapOf(
    "allemagne" to "Alemania",
    "allemagne-pre1945" to "Alemania",
    "chine" to "China",
    "democratic_republic_congo_period" to "República Democrática del Congo",
    "haiti" to "Haití",
    "new_south_wales" to "Nueva Gales del Sur",
    "republique_dominicaine" to "República Dominicana",
    "rome" to "Imperio romano",
    "russia-empire" to "Imperio ruso",
    "russie" to "Rusia",
)

/**
 * The country a card's eyebrow says, given the issuer code and the name Numista serves for it.
 *
 * Reading the raw name through a function rather than storing the cured one is the bargain the metal,
 * the finish and [objectClassOf] already take: a correction made today reaches fichas cached long ago
 * without an API call, and [TypeMeta.issuerName] stays what Numista actually said.
 *
 * The code answers on its own where the table has it, so a curated file naming `haiti` labels its
 * card whether or not a Haitian ficha has reached the phone yet (ADR 0021 §9). Everywhere else the
 * ficha is the only authority, and a country the curator has never had to correct is printed as it
 * came — the same deal [familyLabel] gives an uncurated family.
 */
fun cardCountry(issuerCode: String?, numistaName: String?): String? =
    issuerCode?.let { code -> curedCountries[code] } ?: numistaName

/**
 * Whether a label reads as the name of a country rather than as one of Numista's issuing entities.
 *
 * The two vices of that prose are the **period of validity** —«Haití (1804-presente)»— and the
 * **inversion of an index** —«China, República Popular»—, and both are what ADR 0021 §4 keeps off a
 * line of identity. Length is the third: the eyebrow is set in small caps above a `short_name` capped
 * at 40 characters (#163), and a country that outruns the name below it does not fit on a card.
 *
 * **The language is a fourth vice this cannot see** (#257). «New South Wales» has none of the three
 * and is still not Spanish, so the net passes it and only a curator reading the ficha catches it. It
 * is not made a fourth clause because detecting a language is not one line of code over a third
 * party's prose, and guessing at that prose is what ADR 0023 refused; the table carries the finding
 * instead.
 *
 * It is the rule of the net over what ships (ADR 0023) and of the one migration that net implies, not
 * a filter any card is dropped by: a card always says what it knows, and a label that fails here is a
 * country waiting to be cured.
 */
fun readsAsACountry(label: String): Boolean =
    '(' !in label && ',' !in label && label.length <= COUNTRY_NAME_CEILING

/** The `short_name` ceiling of #163, borrowed: the eyebrow sits above a name capped there. */
private const val COUNTRY_NAME_CEILING = 40

/** The corrected codes, for the net that checks them against the cache that ships (ADR 0023). */
fun curedIssuerCodes(): Set<String> = curedCountries.keys
