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
 * Numista object classes for something struck and ownable that is not money. `Monedas de
 * colección` and `Monedas no circulantes` stay out: full members carry them (Equilibrium's
 * N#356004 and N#477907; most seeded fichas are non-circulating), so the curator decides.
 *
 * Spanish because every ficha is fetched with `lang=es` (`NumistaClient`,
 * `scripts/seed-type-cache.py`); the suite pins this vocabulary so a wording drift cannot turn the
 * net into a silent no-op.
 *
 * Not to be confused with [ObjectClass], which reads the coarse `category` for the chip of Coins.
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
 * Like [metalDeviations], it runs in the suite and never in [CollectionCatalog.validate], which
 * stops the app. There is no "shown but not counted" member status (#89), so a pattern a curator
 * puts in a catalog is a full member, and this only says "look at it"; `variant_note` silences it.
 *
 * It catches the accidental intruder: weight searches with `st=all` return essais next to coins,
 * like the two 1874 Venezuelan essais (N#352550, N#352551) a person dropped in #55.
 *
 * Groupings are skipped: they assert no coverage, and have no member to write an exception on.
 *
 * @param objectClassByType Numista's `type` per type id, from the seeded cache
 */
@SuiteOnly
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
@SuiteOnly
fun thingsThatAreNotMoney(): Set<String> = THINGS_THAT_ARE_NOT_MONEY
