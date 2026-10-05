package com.jenarvaezg.coindex.domain

/** One member whose Numista ficha says a different metal than its catalog declares. */
data class MetalDeviation(
    val catalogId: String,
    val memberId: String,
    val numistaTypeId: Int,
    val declared: Metal,
    val observed: Metal,
) {
    override fun toString(): String =
        "$catalogId/$memberId (Numista $numistaTypeId): declara ${metalCode(declared)}, " +
            "la ficha dice ${metalCode(observed)}"
}

/**
 * Finds the members whose metal contradicts the one their catalog declares.
 *
 * Not part of [CollectionCatalog.validate], which stops the app: the catalog declares the
 * collection's variant, not each member's, and the curator's judgement outranks the physical check.
 * In the suite it catches accidental intruders, like the gold twentieth-ounce once filed in the
 * silver Kookaburra catalog (#63); a [CollectionCatalogMember.variantNote] silences it per member.
 *
 * @param compositionByType Numista's `composition.text` per type, from the seeded cache
 */
@SuiteOnly
fun metalDeviations(
    catalogs: List<CollectionCatalog>,
    compositionByType: Map<Int, String?>,
): List<MetalDeviation> = catalogs.flatMap { catalog ->
    val declared = catalog.metal ?: return@flatMap emptyList()
    catalog.members.mapNotNull { member ->
        val typeId = member.numistaTypeId ?: return@mapNotNull null
        if (member.variantNote != null) return@mapNotNull null
        // A type nobody cached says nothing; the seed test is what makes that a failure.
        val observed = inferMetal(compositionByType[typeId]) ?: return@mapNotNull null
        if (observed == declared) return@mapNotNull null
        MetalDeviation(catalog.id, member.id, typeId, declared, observed)
    }
}
