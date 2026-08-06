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
     * The Numista issue this piece is attached to, when the collector recorded one.
     *
     * It is what tells apart two rows the year cannot: the six issues of the 100 pesetas of
     * Franco all say 1966 and differ only by the star on the coin, which Numista files as a
     * variety of the issue rather than as a year.
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
     * Numista's `category`: `coin` or `exonumia`.
     *
     * Kept as prose and read through [objectClassOf], for the same reason `composition.text` is: the
     * split is a rule, and a rule improved later has to fix rows cached long ago. It takes no part in
     * the variant key, in the matching or in any ratio — four of the 13 exonumia in the seeded cache
     * are members of curated catalogs in full, which is why this is a filter and not a section
     * (ADR 0021 §1, #89).
     */
    val category: String? = null,
    /**
     * The coin's diameter in millimetres, Numista's `size`.
     *
     * The one measurement of a type that says nothing about what it is and everything about how it
     * prints: the exported notebook draws each coin at its real diameter (#169), so this is the
     * whole basis of a 1:1 page. It takes no part in the variant key, in the matching or in any
     * ratio — a coin is not more or less of a collection for being wide.
     */
    val sizeMillimetres: Double? = null,
    /**
     * Numista's own short URL for this type: `https://es.numista.com/1885`.
     *
     * The second field that says nothing about what the coin is and everything about how it prints
     * (see [sizeMillimetres]): the printed notebook can carry a QR per coin (#234), and the code is
     * only small enough to fit under a caption because this URL is twenty-odd characters and not the
     * forty-nine of `en.numista.com/catalogue/pieces1885.html`.
     *
     * **Numista's and not ours.** Building it from [id] would hardcode the catalogue's language and
     * its host into a string a phone camera is going to follow, and the API already hands it over —
     * in the collector's own language, because that is the language the ficha was asked in. It takes
     * no part in the variant key, in the matching or in any ratio.
     */
    val numistaUrl: String? = null,
) {
    /**
     * The country this type is from, which is what a card and a coin row paint (ADR 0023).
     *
     * [issuerName] stays Numista's own prose and this is the reading of it, so the pair never has to
     * travel together to be cured: nine of the 40 issuer codes name an issuing entity with its period
     * of validity, and `russie` is «Rusia» here and «Federación de Rusia (1991-presente)» above.
     */
    val country: String? get() = cardCountry(issuerCode, issuerName)

    /**
     * Whether this looks like a Numista page a referee has not published yet (#186).
     *
     * A submission still in review is served by the API with every field as the contributor left
     * it, so a half-typed `series` arrives as a real family. It is **not verifiable** —the referee
     * may still delete the page and take the id with it— and what is not verifiable does not become
     * a collection: the piece waits in the unclassified residue until the page is published.
     *
     * No year at all is the offline trace of that state, measured in #38 and again in #186: of the
     * 816 seeded types, the two it catches are the two unpublished ones. It is a trace and not the
     * state — a published type nobody has dated falls here too, and its destination is the residue
     * it already had for want of a family, so the false positive costs it nothing.
     */
    val looksUnpublished: Boolean get() = minYear == null && maxYear == null
}

typealias TypeMetaIndex = Map<Int, TypeMeta>
