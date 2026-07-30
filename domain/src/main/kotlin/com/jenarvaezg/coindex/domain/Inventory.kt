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
    val minYear: Int? = null,
    val maxYear: Int? = null,
    val weightOz: Double? = null,
    val finish: Finish? = null,
)

typealias TypeMetaIndex = Map<Int, TypeMeta>
