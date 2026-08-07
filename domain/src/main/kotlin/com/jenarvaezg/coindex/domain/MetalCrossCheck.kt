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
 * Deliberately **not** part of [CollectionCatalog.validate], which stops the app at startup. What
 * a catalog declares is the variant of the collection, not an assertion about each member, and the
 * curator's judgement outranks the physical check: a list that makes more sense with seven silver
 * coins and one of cupronickel keeps the cupronickel one. A fatal check would make Numista's
 * `composition.text` a veto over curation, which is exactly backwards.
 *
 * So it lives in the test suite, where it catches the accidental intruder — the twentieth-ounce of
 * **gold** that sat in the Kookaburra catalog as if it were the silver ounce of 2009 (#63) — and
 * is silenced, member by member, by a [CollectionCatalogMember.variantNote] in prose.
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
