package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.AssembledCollection
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CoverageRatio
import com.jenarvaezg.coindex.domain.DerivedCollection
import com.jenarvaezg.coindex.domain.Finish
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.OwnGrouping
import com.jenarvaezg.coindex.domain.OwnGroupingView
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.VariantKey
import com.jenarvaezg.coindex.domain.gramsToOunces

/**
 * A collection small enough to reason about and shaped like the real one.
 *
 * The four coins cover the four cases the two shelves have to answer for: a type held twice, a medal
 * that lives inside a collection, a complete plate, and a coin no collection claims at all. The three
 * cards cover the two species of ADR 0021 §2 plus the ratio's absence.
 */
internal object ShelfFixtures {
    const val FUERTE = 100
    const val ONZA_MEXICANA = 200
    const val BRITANNIA = 300
    const val UNCACHED = 400

    private val typeMeta = mapOf(
        FUERTE to TypeMeta(
            id = FUERTE,
            displayTitle = "Bolívar de 25 g",
            issuerName = "Venezuela",
            minYear = 1929,
            weightOz = gramsToOunces(25.0),
            metal = Metal.Silver,
            category = "coin",
        ),
        ONZA_MEXICANA to TypeMeta(
            id = ONZA_MEXICANA,
            displayTitle = "1 Onza",
            issuerName = "México",
            minYear = 1978,
            weightOz = gramsToOunces(33.625),
            metal = Metal.Silver,
            category = "exonumia",
        ),
        BRITANNIA to TypeMeta(
            id = BRITANNIA,
            displayTitle = "Britannia",
            issuerName = "Reino Unido",
            minYear = 2020,
            weightOz = gramsToOunces(31.103),
            finish = Finish.Bullion,
            metal = Metal.Silver,
            category = "coin",
        ),
        // Cached by nobody: the state a phone is in between a sync arriving and the fichas landing.
        UNCACHED to null,
    ).filterValues { it != null }.mapValues { (_, meta) -> checkNotNull(meta) }

    /** Two rows of the same type on purpose: Coins collapses them into one coin held three times. */
    private val items = listOf(
        item(id = 1, typeId = FUERTE, quantity = 2),
        item(id = 2, typeId = FUERTE, quantity = 1),
        item(id = 5, typeId = ONZA_MEXICANA, quantity = 1),
        item(id = 9, typeId = BRITANNIA, quantity = 1),
        // The sibling row an issue-qualified catalog does not claim (ADR 0019): the shape of the
        // father's American Silver Eagle N#298883, whose 2026 issue no member names.
        item(id = 10, typeId = BRITANNIA, quantity = 1),
        item(id = 12, typeId = UNCACHED, quantity = 1),
    )

    private val fuertesKey = VariantKey("Bolívar de Venezuela", 804, null, Metal.Silver)
    private val britanniaKey = VariantKey("Britannia", 1_000, Finish.Bullion, Metal.Silver)

    private val fuertes = IndexCard.Derived(
        name = "Bolívar de Venezuela",
        coverage = CoverageRatio(owned = 1, issued = 22),
        issuer = "Venezuela",
        collection = DerivedCollection(
            family = fuertesKey.family,
            weightMillioz = 804,
            finish = null,
            metal = Metal.Silver,
            distinctTypes = 1,
            quantity = 3,
        ),
        plateCatalogId = "venezuela-bolivar",
        seriesStatus = SeriesStatus.Closed,
    )

    private val britannia = IndexCard.Derived(
        name = "Britannia",
        coverage = CoverageRatio(owned = 1, issued = 1),
        issuer = "Reino Unido",
        collection = DerivedCollection(
            family = britanniaKey.family,
            weightMillioz = 1_000,
            finish = Finish.Bullion,
            metal = Metal.Silver,
            distinctTypes = 1,
            quantity = 1,
        ),
        plateCatalogId = "reino-unido-britannia",
        seriesStatus = SeriesStatus.Open,
    )

    private val box = IndexCard.Box(
        name = "Las mexicanas",
        issuer = "México",
        box = OwnGroupingView(
            grouping = OwnGrouping(id = 7, name = "Las mexicanas", typeIds = listOf(ONZA_MEXICANA)),
            items = items.filter { it.typeId == ONZA_MEXICANA },
        ),
    )

    /**
     * The index in the order the domain comparator leaves it (ADR 0021 §6): the complete ratio, the
     * partial one, and the box in the no-ratio stretch.
     */
    val state = CollectionState(
        AssembledCollection(
            items = items,
            index = listOf(britannia, fuertes, box),
            typeMeta = typeMeta,
            itemsByKey = mapOf(
                fuertesKey to items.filter { it.typeId == FUERTE },
                // Only row 9. Row 10 is the same type and produced no collection, which is exactly
                // what `deriveCollection` does with an issue no member claims.
                britanniaKey to items.filter { it.id == 9L },
            ),
            ownGroupings = listOf(box.box),
        ),
    )

    /**
     * The same collection after the Mexican onza was dropped from the box.
     *
     * Dropping a type does not touch the piece (ADR 0013, ADR 0021 §10): the inventory is untouched
     * and only the membership goes, which is what makes a box a second reading rather than a move.
     */
    val stateWithoutTheBox = state.copy(
        collection = state.collection.copy(
            index = state.index.filterNot { it is IndexCard.Box },
            ownGroupings = emptyList(),
        ),
    )

    private fun item(id: Long, typeId: Int, quantity: Int) =
        CollectedItem(id = id, quantity = quantity, typeId = typeId)
}
