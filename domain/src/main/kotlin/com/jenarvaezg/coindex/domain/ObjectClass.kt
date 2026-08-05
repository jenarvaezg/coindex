package com.jenarvaezg.coindex.domain

/**
 * What a struck thing is, coarsely: a coin, or something struck that is filed beside coins.
 *
 * This is Numista's own `category`, and it is deliberately **not** the five-class net of
 * [objectClassDeviations]. That one reads `type` — «Medallas», «Monedas de ensayo» — to warn a
 * curator that a member of a catalog may not be money at all, and it is silenced one member at a
 * time in prose.
 * This is the collector's question instead, and it is answered with a chip: 13 of the 829 seeded types
 * are exonumia, and **four of them live inside curated catalogs** — the two Mexican Onzas and two
 * Niue members — which is exactly why ADR 0021 §1 made medals a **filter and not a section**. A
 * «Medallas» section would have had to tear those four out of their plate.
 */
enum class ObjectClass {
    Coin,
    Exonumia,
}

/** Numista's value for the exonumia side of `category`; the coin side is `coin`. */
private const val EXONUMIA = "exonumia"

/**
 * Reads Numista's `category` into the two-value split.
 *
 * A category nobody recorded is [ObjectClass.Coin], and that is a default rather than a claim: with
 * two chips there is no third place to put it, and «Medallas y fichas» would say something about the
 * coin that the ficha does not. `category` covers 100 % of the seeded cache, so the default only ever
 * catches a type between a sync landing and its ficha arriving.
 *
 * The prose is kept in [TypeMeta.category] and read here rather than stored as the enum, on the same
 * bargain as the metal and the finish — a better rule fixes rows cached long ago without an API call.
 */
fun objectClassOf(numistaCategory: String?): ObjectClass =
    if (numistaCategory == EXONUMIA) ObjectClass.Exonumia else ObjectClass.Coin
