package com.jenarvaezg.coindex.domain

/** One piece as recorded in the collector's own Numista collection. */
data class CollectedItem(
    val id: Long,
    val quantity: Int,
    val typeId: Int,
    val title: String? = null,
    val issuerCode: String? = null,
    val issueYear: Int? = null,
    val gregorianYear: Int? = null,
    val grade: String? = null,
    val price: Double? = null,
    val forSwap: Boolean? = null,
    val collectionName: String? = null,
    /**
     * The Numista issue this piece is attached to, when the collector recorded one. It tells apart
     * rows the year cannot: the 100 pesetas of Franco all say 1966 and differ by the star, which
     * Numista files as a variety of the issue.
     */
    val issueId: Int? = null,
) {
    /** Year recorded on the piece; a date run never matches a piece without one. */
    val recordedYear: Int? get() = issueYear ?: gregorianYear
}

/** The slice of Numista type metadata the domain reasons about. */
data class TypeMeta(
    val id: Int,
    val title: String? = null,
    val displayTitle: String? = null,
    /** Raw Numista `series` value. Never an editorial alias. */
    val family: String? = null,
    val issuerCode: String? = null,
    /** Numista's own name for the issuer, in the collector's language: «Australia», «España». */
    val issuerName: String? = null,
    val minYear: Int? = null,
    val maxYear: Int? = null,
    val weightOz: Double? = null,
    val finish: Finish? = null,
    /** Dominant metal, inferred from `composition.text` like the finish is from the title. */
    val metal: Metal? = null,
    /**
     * Numista's `category`, `coin` or `exonumia`, kept raw and read through [objectClassOf] so an
     * improved rule fixes cached rows. A filter, not a section: it takes no part in the variant
     * key, matching or ratios, since curated catalogs include exonumia (ADR 0021 §1, #89).
     */
    val category: String? = null,
    /**
     * The diameter in millimetres, Numista's `size`; the notebook draws each coin at its real
     * diameter (#169). Not part of the variant key, matching or ratios.
     */
    val sizeMillimetres: Double? = null,
    /**
     * Numista's own short URL for this type, such as `https://es.numista.com/1885`, short enough
     * for a QR under a caption in the printed notebook (#234). Taken from the API rather than built
     * from [id], so it keeps Numista's host and the language the ficha was asked in. Not part of
     * the variant key, matching or ratios.
     */
    val numistaUrl: String? = null,
    /**
     * Millesimal fineness of the silver in this type, inferred from `composition.text` by
     * [silverFineness]. Null for a type that is not silver or names no fineness; such a piece has
     * no silver floor.
     */
    val fineness: Double? = null,
    /**
     * The thickness in millimetres, Numista's `thickness`. Often missing, so the stack figure is
     * extrapolated from the pieces that have it (`docs/ux/cifras-316.md`). Not part of the variant
     * key, matching or ratios.
     */
    val thicknessMillimetres: Double? = null,
    /**
     * Whether this is no longer legal tender, Numista's `demonetization.is_demonetized`. Null means
     * Numista does not say, which is not «still legal tender».
     */
    val demonetized: Boolean? = null,
    /**
     * Every hand that drew or engraved either face (`engravers` and `designers` of obverse and
     * reverse) as one list: a name on both faces is one hand, and Numista files the same person
     * under either key.
     */
    val hands: List<String> = emptyList(),
    /** The mints that struck this type, Numista's `mints`. */
    val mints: List<String> = emptyList(),
) {
    /**
     * The weight in grams, derived from [weightOz] rather than stored beside it: the ounce is what
     * the variant key is built on (ADR 0018).
     */
    val weightGrams: Double? get() = weightOz?.let { ounces -> ounces * GRAMS_PER_TROY_OUNCE }

    /**
     * The weight a loose coin is keyed by: Numista's grams snapped to the common bullion weights,
     * never to a weight a catalog declares (ADR 0018, #288). A curated member takes its key from
     * the file instead (ADR 0016). Null without a declared weight, which keeps the piece out of
     * every weight band.
     *
     * A property so the derivation's variant key and the shelf's loose row cannot disagree (#540).
     */
    val weightMillioz: Int? get() = weightOz?.let(::normalizeWeightMillioz)

    /**
     * The country a card and a coin row paint (ADR 0023): [issuerName] read through [cardCountry],
     * so `russie` is «Rusia» here while [issuerName] keeps «Federación de Rusia (1991-presente)».
     */
    val country: String? get() = cardCountry(issuerCode, issuerName)

    /**
     * Whether this looks like a Numista page a referee has not published yet (#186). The API serves
     * a submission in review as typed, half-typed `series` included, and the referee may still
     * delete it, so it does not become a collection: the piece waits in the unclassified residue.
     *
     * Having no year at all is the offline trace of that state (#38, #186), not the state itself:
     * a published type nobody dated also matches. [deriveCollection] applies it only to types with
     * a family.
     */
    val looksUnpublished: Boolean get() = minYear == null && maxYear == null
}

typealias TypeMetaIndex = Map<Int, TypeMeta>
