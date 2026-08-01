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
)

typealias TypeMetaIndex = Map<Int, TypeMeta>
