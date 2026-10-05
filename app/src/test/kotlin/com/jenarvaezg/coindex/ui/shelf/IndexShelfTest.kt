package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.AssembledCollection
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.CoverageRatio
import com.jenarvaezg.coindex.domain.DerivedCollection
import com.jenarvaezg.coindex.domain.Finish
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.VariantKey
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The Collections shelf. Its default sort is ADR 0021 §6's comparator, applied in the domain, and
 * every other sort is built on that list, so the UI layer never defines the index order again.
 */
class IndexShelfTest {
    private val facts = indexFacts(ShelfFixtures.state)

    private fun names(shelf: IndexShelf, query: String = "") =
        shelf.narrow(facts, query).map { it.name }

    @Test
    fun `the default order is the domain's, untouched`() {
        assertEquals(
            listOf("Britannia", "Bolívar de Venezuela", "Las mexicanas"),
            names(IndexShelf()),
        )
    }

    @Test
    fun `alphabetical is Spanish alphabetical, so the accent does not go after Z`() {
        assertEquals(
            listOf("Bolívar de Venezuela", "Britannia", "Las mexicanas"),
            names(IndexShelf(sort = IndexSort.Alphabetical)),
        )
    }

    @Test
    fun `a box is unweighed rather than light, so it sits at the bottom of Mas pesadas`() {
        assertEquals(
            listOf("Britannia", "Bolívar de Venezuela", "Las mexicanas"),
            names(IndexShelf(sort = IndexSort.Heaviest)),
        )
    }

    @Test
    fun `Menos completas puts the ratios the other way round and leaves the box last`() {
        assertEquals(
            listOf("Bolívar de Venezuela", "Britannia", "Las mexicanas"),
            names(IndexShelf(sort = IndexSort.LeastComplete)),
        )
    }

    @Test
    fun `Alta mas reciente reads the highest Numista row id, which is all there is`() {
        // The box holds row 5, the Bolívar rows 1 and 2, Britannia row 9.
        assertEquals(
            listOf("Britannia", "Las mexicanas", "Bolívar de Venezuela"),
            names(IndexShelf(sort = IndexSort.RecentlyAdded)),
        )
    }

    @Test
    fun `the estado chips are the ratio and its absence, and nothing about curation`() {
        assertEquals(listOf("Britannia"), names(IndexShelf(status = PlateStatus.Complete)))
        assertEquals(
            listOf("Bolívar de Venezuela"),
            names(IndexShelf(status = PlateStatus.PartlyDone)),
        )
        assertEquals(listOf("Las mexicanas"), names(IndexShelf(status = PlateStatus.NoPlate)))
    }

    @Test
    fun `a box has no series, so no series chip offers it`() {
        assertEquals(listOf("Bolívar de Venezuela"), names(IndexShelf(series = SeriesStatus.Closed)))
        assertEquals(listOf("Britannia"), names(IndexShelf(series = SeriesStatus.Open)))

        val counts = indexFacetCounts(facts, IndexShelf(), "")
        assertEquals(3, counts.series.total)
        assertEquals(2, counts.series.byValue.values.sum())
    }

    @Test
    fun `where a collection starts is its earliest coin, not its catalog`() {
        assertEquals(
            listOf("Bolívar de Venezuela"),
            names(IndexShelf(startsIn = StartBand.BeforeFifty)),
        )
        assertEquals(listOf("Las mexicanas"), names(IndexShelf(startsIn = StartBand.FiftyToNinetyNine)))
        assertEquals(listOf("Britannia"), names(IndexShelf(startsIn = StartBand.SinceTwoThousand)))
    }

    @Test
    fun `the search finds a card by its variant as well as by its name`() {
        assertEquals(listOf("Bolívar de Venezuela"), names(IndexShelf(), query = "bolivar"))
        assertEquals(listOf("Britannia"), names(IndexShelf(), query = "bullion"))
    }

    @Test
    fun `a facet counts with its own choice dropped`() {
        val counts = indexFacetCounts(facts, IndexShelf(status = PlateStatus.Complete), "")

        assertEquals(3, counts.status.total)
        assertEquals(1, counts.status.of(PlateStatus.NoPlate))
        // Every other facet sees only the one card the shelf leaves.
        assertEquals(1, counts.issuer.total)
        assertEquals(listOf("Reino Unido" to 1), counts.issuer.issuers())
    }

    @Test
    fun `the country of a box is the country of its pieces, and it filters like any other`() {
        assertEquals(listOf("Las mexicanas"), names(IndexShelf(issuer = "México")))
    }

    /**
     * «Serie» is the exception: a box declares no series, and a chip inventing one would bring back
     * the provenance label ADR 0021 §2 removed.
     */
    @Test
    fun `every chip row adds up to its own total, except the one a box cannot answer`() {
        val counts = indexFacetCounts(facts, IndexShelf(), "")

        assertEquals(counts.weight.total, counts.weight.byValue.values.sum())
        assertEquals(counts.startsIn.total, counts.startsIn.byValue.values.sum())
        assertEquals(counts.status.total, counts.status.byValue.values.sum())
        assertEquals(1, counts.weight.of(OunceBand.Spanning))
        assertEquals(counts.series.total - 1, counts.series.byValue.values.sum())
    }

    /** #413: «Sin fecha · 0» is hidden once another chip leaves nobody undated, as País does. */
    @Test
    fun `a start chip that would leave nobody is not offered`() {
        val counts = indexFacetCounts(facts, IndexShelf(issuer = "Venezuela"), "")

        assertEquals(
            listOf(StartBand.BeforeFifty to 1),
            counts.startsIn.populatedIn(StartBand.entries),
        )
        assertEquals(0, counts.startsIn.of(StartBand.Unknown))
    }

    /**
     * The país chip and the country-axis header both read member countries, not the card's (#415).
     * ADR 0023 keeps `autriche` («Austria») and `autriche-habsbourg` («Imperio austríaco») apart.
     */
    @Test
    fun `país chips use member countries, so Austria and Imperio austríaco stay distinct and filterable`() {
        val philharmonic = plateCard(
            name = "Vienna Philharmonic",
            issuer = "Austria",
            catalogId = "austria-vienna-philharmonic-1oz-bullion",
            key = VariantKey("Vienna Philharmonic bullion anual", 1_000, Finish.Bullion, Metal.Silver),
        )
        val historia = plateCard(
            name = "Historia del real",
            issuer = "México",
            catalogId = "historia-del-real",
            key = VariantKey("Historia del real", 870, null, Metal.Silver),
        )
        val state = CollectionState(
            AssembledCollection(
                items = listOf(
                    CollectedItem(id = 1, quantity = 1, typeId = PHILHARMONIC_TYPE),
                    CollectedItem(id = 2, quantity = 1, typeId = THALER_TYPE),
                ),
                index = listOf(philharmonic, historia),
                typeMeta = mapOf(
                    PHILHARMONIC_TYPE to TypeMeta(
                        id = PHILHARMONIC_TYPE,
                        issuerCode = "autriche",
                        issuerName = "Austria",
                        minYear = 2008,
                    ),
                    THALER_TYPE to TypeMeta(
                        id = THALER_TYPE,
                        issuerCode = "autriche-habsbourg",
                        issuerName = "Imperio austríaco",
                        minYear = 1780,
                    ),
                    REAL_TYPE to TypeMeta(
                        id = REAL_TYPE,
                        issuerCode = "mexique",
                        issuerName = "México",
                        minYear = 1791,
                    ),
                ),
                itemsByKey = mapOf(
                    philharmonic.collection.key() to listOf(
                        CollectedItem(id = 1, quantity = 1, typeId = PHILHARMONIC_TYPE),
                    ),
                    historia.collection.key() to listOf(
                        CollectedItem(id = 2, quantity = 1, typeId = THALER_TYPE),
                    ),
                ),
                evidencedCatalogIds = setOf(philharmonic.plateCatalogId!!, historia.plateCatalogId!!),
            ),
        )
        val catalogs = listOf(
            catalog(
                id = "austria-vienna-philharmonic-1oz-bullion",
                issuer = "autriche",
                members = listOf(member("2008", PHILHARMONIC_TYPE, 2008)),
            ),
            catalog(
                id = "historia-del-real",
                issuer = "mexique",
                members = listOf(
                    member("thaler", THALER_TYPE, 1780, issuerCode = "autriche-habsbourg"),
                    member("real", REAL_TYPE, 1791),
                ),
            ),
        )
        val facts = indexFacts(state, catalogs)
        val counts = indexFacetCounts(facts, IndexShelf(), "")

        assertEquals(1, counts.issuer.of("Austria"))
        assertEquals(1, counts.issuer.of("Imperio austríaco"))
        assertEquals(1, counts.issuer.of("México"))
        assertEquals(
            listOf("Historia del real"),
            IndexShelf(issuer = "Imperio austríaco").narrow(facts, "").map { it.name },
        )
        assertEquals(
            listOf("Vienna Philharmonic"),
            IndexShelf(issuer = "Austria").narrow(facts, "").map { it.name },
        )
    }

    private fun plateCard(
        name: String,
        issuer: String,
        catalogId: String,
        key: VariantKey,
    ) = IndexCard.Derived(
        name = name,
        coverage = CoverageRatio(owned = 1, issued = 1),
        issuer = issuer,
        collection = DerivedCollection(
            family = key.family,
            weightMillioz = key.weightMillioz,
            finish = key.finish,
            metal = key.metal,
            distinctTypes = 1,
            quantity = 1,
        ),
        plateCatalogId = catalogId,
        seriesStatus = SeriesStatus.Closed,
    )

    private fun catalog(
        id: String,
        issuer: String,
        members: List<CollectionCatalogMember>,
    ) = CollectionCatalog(
        schemaVersion = 1,
        id = id,
        name = id,
        shortName = id,
        family = id,
        weightMillioz = 1_000,
        finish = Finish.Bullion,
        metal = Metal.Silver,
        issuerCode = issuer,
        seriesStatus = SeriesStatus.Closed,
        source = "test",
        updatedAt = "2026-08-11",
        members = members,
    )

    private fun member(
        id: String,
        typeId: Int,
        year: Int,
        issuerCode: String? = null,
    ) = CollectionCatalogMember(
        id = id,
        label = id,
        year = year,
        numistaTypeId = typeId,
        issuerCode = issuerCode,
    )

    companion object {
        private const val PHILHARMONIC_TYPE = 9_165
        private const val THALER_TYPE = 7_393
        private const val REAL_TYPE = 18_852
    }
}
