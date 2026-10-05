package com.jenarvaezg.coindex.domain

/**
 * What a struck thing is, coarsely: a coin, or something struck that is filed beside coins
 * (Numista's `category`).
 *
 * Not the net of [objectClassDeviations], which reads `type` to warn a curator. This answers the
 * collector's question with a chip: exonumia such as the FNMT's ECU pieces live inside curated
 * catalogs, which is why ADR 0021 §1 made medals a filter and not a section.
 */
enum class ObjectClass {
    Coin,
    Exonumia,
}

/** Numista's value for the exonumia side of `category`; the coin side is `coin`. */
private const val EXONUMIA = "exonumia"

/**
 * Reads Numista's `category` into the two-value split. An unrecorded category defaults to
 * [ObjectClass.Coin], which in practice only covers a type whose ficha has not arrived yet. Read
 * from [TypeMeta.category] rather than stored, so a better rule fixes cached rows.
 */
fun objectClassOf(numistaCategory: String?): ObjectClass =
    if (numistaCategory == EXONUMIA) ObjectClass.Exonumia else ObjectClass.Coin
