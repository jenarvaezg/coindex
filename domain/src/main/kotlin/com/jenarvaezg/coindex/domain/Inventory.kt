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
    /**
     * Millesimal fineness of the silver in this type, inferred from `composition.text` like the
     * metal is (`silverFineness`).
     *
     * A rule read on the way in and not the prose itself, exactly like [metal]: the silver floor of a
     * piece is its **fine** silver, and a .835 coin is 16,5 % copper. Null for a type that is not
     * silver, and also for the two fichas that say «Plata» and no number — a piece with no declared
     * fineness has no floor rather than a floor of one.
     */
    val fineness: Double? = null,
    /**
     * The coin's thickness in millimetres, Numista's `thickness`.
     *
     * The only measurement of the collection that is **extrapolated** rather than measured: it is
     * missing in a third of the types, so the ladder of the stack says «unos 94 cm» over the pieces
     * that have one, scaled to all of them (`docs/ux/cifras-316.md`). It takes no part in the variant
     * key, in the matching or in any ratio.
     */
    val thicknessMillimetres: Double? = null,
    /**
     * Whether this is still money anywhere, Numista's `demonetization.is_demonetized`.
     *
     * Null is «Numista does not say», which is 2 % of his types and is not «still legal tender»: the
     * figure at the margin is a percentage of the whole collection and declares nothing about the rows
     * nobody filled in.
     */
    val demonetized: Boolean? = null,
    /**
     * Every hand that drew or engraved either face: `engravers` and `designers` of obverse and
     * reverse, as one set.
     *
     * One list and not four, because the figure it feeds is «246 were engraved by the same hand» and
     * a name signing both faces is one hand and not two. Numista files the same person under either
     * key depending on who typed the ficha.
     */
    val hands: List<String> = emptyList(),
    /** The mints that struck this type, Numista's `mints`. */
    val mints: List<String> = emptyList(),
) {
    /**
     * The type's weight in grams, which is what Numista publishes and what mass is measured in.
     *
     * Derived from [weightOz] rather than stored beside it: the ounce is what the variant key is
     * built on (ADR 0018), and two columns for one measurement is two things that can disagree.
     */
    val weightGrams: Double? get() = weightOz?.let { ounces -> ounces * GRAMS_PER_TROY_OUNCE }

    /**
     * The weight this type is keyed by when no curated file weighs it: Numista's own grams, snapped
     * to the common bullion weights and to nothing a catalog declares (ADR 0018, #288).
     *
     * It is the reading of a **loose** coin, and that is the whole of its authority: a catalog is
     * authoritative over its own members' variant (ADR 0016), and a member never comes through here
     * — its key comes from the file. Null where the ficha declares no weight, which keeps such a
     * piece out of every weight band instead of parking it under a figure nobody measured.
     *
     * A property of the type and not a call each caller makes, because the two callers are the
     * variant key of the derivation and the loose row the shelf draws, and the same coin weighing
     * one thing in its card's key and another in the «Sin colección» list is precisely the
     * disagreement #288 closed (#540).
     */
    val weightMillioz: Int? get() = weightOz?.let(::normalizeWeightMillioz)

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
