package com.jenarvaezg.coindex.domain

/** One member whose Numista ficha classes it as something struck that is not money. */
data class ObjectClassDeviation(
    val catalogId: String,
    val memberId: String,
    val numistaTypeId: Int,
    val objectClass: String,
) {
    override fun toString(): String =
        "$catalogId/$memberId (Numista $numistaTypeId): la ficha lo clasifica como «$objectClass»"
}

/**
 * The Numista object classes for a thing that was struck, can be owned, and is not money.
 *
 * Deliberately five and not the whole table. `Monedas de colección` is **out**: two members of the
 * Equilibrium catalog carry it (N#356004 and N#477907) and they are members in full, so the class
 * says nothing there — Numista's own table calls it "depends on the declared scope", which is to
 * say the curator decides. `Monedas no circulantes` is out for the same reason at scale: 615 of the
 * 727 seeded fichas are one, which is what the two collections are made of.
 *
 * The strings are Spanish because the cache is: every ficha is fetched with `lang=es`, by the app
 * (`NumistaClient`) and by `scripts/seed-type-cache.py` alike. A literal-string net can rot into a
 * silent no-op if that wording ever drifts, so the suite pins the vocabulary separately.
 */
private val THINGS_THAT_ARE_NOT_MONEY = setOf(
    "Monedas de ensayo",
    "Monedas de fantasía",
    "Medallas",
    "Medallas conmemorativas",
    "Medallones de colección",
)

/**
 * Finds the members whose Numista ficha says they are not money at all.
 *
 * A sibling of [metalDeviations] and bound by the same rule: it lives in the test suite and
 * **never** in [CollectionCatalog.validate], which stops the app at startup. There is no member
 * status for "shown but not counted because it is not a coin" — #89 killed it, because neither
 * collection has a piece that a curated catalog miscounts and the state would have been the first
 * slot that can be filled and still stay out of the divisor. So a pattern a curator puts inside a
 * catalog is a member in full, and this check only ever says "look at it".
 *
 * What it catches is the accidental intruder, the way #63's twentieth-ounce of gold was caught: the
 * two 1874 essais of the Venezuelan silver — N#352550 and N#352551, 25 g of .900 and the very
 * module of the 22 slots — came out of the same weight enumeration that populated the catalog in
 * #55 and were dropped by a person reading. With `st=all` the weight search returns essais next to
 * coins, so the intruder is not hypothetical: it is the normal path of curating.
 *
 * It stops at catalogs. A curated grouping asserts no coverage, so an odd piece inside one
 * miscounts nothing — and it has no members to write the exception on, which would make the red
 * unsilenceable and turn this into the fatal check the domain forbids.
 *
 * @param objectClassByType Numista's `type` per type id, from the seeded cache
 */
fun objectClassDeviations(
    catalogs: List<CollectionCatalog>,
    objectClassByType: Map<Int, String?>,
): List<ObjectClassDeviation> = catalogs.flatMap { catalog ->
    catalog.members.mapNotNull { member ->
        val typeId = member.numistaTypeId ?: return@mapNotNull null
        if (member.variantNote != null) return@mapNotNull null
        // A type nobody cached says nothing; the seed test is what makes that a failure.
        val objectClass = objectClassByType[typeId] ?: return@mapNotNull null
        if (objectClass !in THINGS_THAT_ARE_NOT_MONEY) return@mapNotNull null
        ObjectClassDeviation(catalog.id, member.id, typeId, objectClass)
    }
}

/** The vocabulary [objectClassDeviations] reads, exposed so the suite can pin it against `data/`. */
fun thingsThatAreNotMoney(): Set<String> = THINGS_THAT_ARE_NOT_MONEY
