package com.jenarvaezg.coindex.domain

/**
 * Issuer codes whose Numista label is not a country name in Spanish, and what a card says instead
 * (ADR 0023). Numista names issuing entities with their period of validity («Federación de Rusia
 * (1991-presente)») or inverted for an index. That is correct catalogue data (the period tells
 * `russie` from `ancienne_urss`), so the cure is ours, at display time.
 *
 * Only the exceptions live here; any other code uses the ficha's label. A table and not a
 * heuristic: cutting at `(` and un-inverting at the comma still leaves «Federación de Rusia» or
 * «Alemania, República Federal de».
 *
 * A country served with its period or inverted takes its common Spanish name (`russie` is «Rusia»,
 * `ghana` is «Ghana»); a state that is nobody's country any more keeps its own name
 * (`russia-empire` is «Imperio ruso»). `allemagne-pre1945` is «Alemania» because Numista itself
 * calls it «Alemania (1871-1948)». `new_south_wales` is here for its language (#257, ADR 0021 §4):
 * Numista serves it in English even with `lang=es`, and as a former colony and today a state it is
 * «Nueva Gales del Sur».
 */
private val curedCountries: Map<String, String> = mapOf(
    "allemagne" to "Alemania",
    "allemagne-pre1945" to "Alemania",
    "chine" to "China",
    "democratic_republic_congo_period" to "República Democrática del Congo",
    "ghana" to "Ghana",
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
 * Read through a function rather than stored, like the metal, the finish and [objectClassOf], so a
 * correction reaches fichas cached long ago and [TypeMeta.issuerName] stays what Numista said. A
 * code in the table answers before any ficha of it reaches the phone (ADR 0021 §9); any other
 * country is printed as Numista sent it, as [familyLabel] does for uncurated families.
 */
fun cardCountry(issuerCode: String?, numistaName: String?): String? =
    issuerCode?.let { code -> curedCountries[code] } ?: numistaName

/**
 * The country a member was struck for, which is not always its catalog's (#170): the member's own
 * issuer code or else the catalog's, named from the member's ficha or else from any cached ficha
 * of the same issuer, so a hole whose type is not cached still gets its country. Everything goes
 * through [cardCountry], so a card, the country axis and the país chip spell a country alike.
 */
fun CollectionCatalog.countryOf(
    member: CollectionCatalogMember,
    typeMeta: TypeMetaIndex,
): String? {
    val code = issuerCodeOf(member)
    val numistaName = member.numistaTypeId?.let { typeMeta[it]?.issuerName }
        ?: typeMeta.values.firstOrNull { it.issuerCode == code }?.issuerName
    return cardCountry(code, numistaName)
}

/**
 * Whether a label reads as the name of a country rather than one of Numista's issuing entities: no
 * period of validity («Haití (1804-presente)»), no index inversion («China, República Popular»),
 * both kept off a line of identity by ADR 0021 §4, and no longer than a `short_name` (#163).
 *
 * It cannot see language (#257): «New South Wales» passes, and only a curator reading the ficha
 * catches it, so the table carries that finding. This is the rule of the suite's net over what
 * ships (ADR 0023), never a filter that drops a card.
 */
fun readsAsACountry(label: String): Boolean =
    '(' !in label && ',' !in label && label.length <= COUNTRY_NAME_CEILING

/** The `short_name` ceiling of #163, borrowed: the eyebrow sits above a name capped there. */
private const val COUNTRY_NAME_CEILING = 40

/** The corrected codes, for the net that checks them against the cache that ships (ADR 0023). */
@SuiteOnly
fun curedIssuerCodes(): Set<String> = curedCountries.keys
