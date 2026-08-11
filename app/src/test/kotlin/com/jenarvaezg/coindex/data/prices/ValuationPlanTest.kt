package com.jenarvaezg.coindex.data.prices

import com.jenarvaezg.coindex.data.db.IssuePriceReadEntity
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.Curation
import com.jenarvaezg.coindex.domain.SeriesStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val NOW = 1_754_600_000_000L

/**
 * Which issues a pass may ask Numista about (ADR 0028 §1).
 *
 * The threshold is the part worth pinning, and it is **not a saving**: a plate with 51 holes does not have
 * a cost of completion, it has a reproach of 51 slots, so the number that came back could not be shown.
 */
class ValuationPlanTest {
    /** Every piece that carries an issue, once, however many rows share it. */
    @Test
    fun `the owned half is every distinct issue of the collection`() {
        val plan = valuationPlan(
            listOf(
                item(id = 1, typeId = 10, issueId = 100),
                item(id = 2, typeId = 10, issueId = 100),
                item(id = 3, typeId = 10, issueId = 101),
                // No issue recorded: nothing to address a price to.
                item(id = 4, typeId = 11, issueId = null),
            ),
            Curation(catalogs = emptyList()),
            emptySet(),
        )

        assertEquals(listOf(OwnedIssue(10, 100), OwnedIssue(10, 101)), plan.owned)
        assertTrue(plan.holes.isEmpty())
    }

    /**
     * A plate ten slots or fewer from closing has its holes valued; one further out does not.
     *
     * Over his 49 plates the cut takes 28 of them and 138 holes, and it falls clean: what stays outside are
     * the bullion runs where he owns a single coin, which is exactly where a cost of completion would be
     * the reproach.
     */
    @Test
    fun `the holes of a plate within ten slots are asked for, and no others`() {
        val withinReach = dateRun("reach", years = 1_960..1_965, typeId = 10)
        val tooFar = dateRun("far", years = 1_900..1_950, typeId = 20)

        val plan = valuationPlan(
            listOf(item(id = 1, typeId = 10, issueId = 100, year = 1_960)) +
                item(id = 2, typeId = 20, issueId = 200, year = 1_900),
            Curation(catalogs = listOf(withinReach, tooFar)),
            setOf("reach", "far"),
        )

        assertEquals(listOf("reach"), plan.holes.map { it.catalogId }.distinct())
        assertEquals(listOf(1_961, 1_962, 1_963, 1_964, 1_965), plan.holes.mapNotNull { it.year })
    }

    /** A plate with nothing missing has nothing to cost, and leaves by the same clause. */
    @Test
    fun `a closed plate contributes no holes`() {
        val closed = dateRun("closed", years = 1_960..1_961, typeId = 10)

        val plan = valuationPlan(
            listOf(
                item(id = 1, typeId = 10, issueId = 100, year = 1_960),
                item(id = 2, typeId = 10, issueId = 101, year = 1_961),
            ),
            Curation(catalogs = listOf(closed)),
            setOf("closed"),
        )

        assertTrue(plan.holes.isEmpty())
    }

    /**
     * Only the plates that are **open** are walked.
     *
     * A catalog with no evidence has no plate to put a cost of completion in the header of, so asking about
     * its slots would spend the budget on a screen the collector cannot reach.
     */
    @Test
    fun `a catalog with no evidence is not walked at all`() {
        val plan = valuationPlan(
            listOf(item(id = 1, typeId = 10, issueId = 100, year = 1_960)),
            Curation(catalogs = listOf(dateRun("reach", 1_960..1_962, typeId = 10))),
            evidencedCatalogIds = emptySet(),
        )

        assertTrue(plan.holes.isEmpty())
    }

    /**
     * An issue Numista answered for is not asked again, **including when it had no price**.
     *
     * That second half is what makes three states out of two: without the row, those 19 issues of his 223
     * would be asked for again on every single pass, for ever (ADR 0028 §4).
     */
    @Test
    fun `an issue already answered for is not asked again, priced or not`() {
        val plan = ValuationPlan(
            owned = listOf(OwnedIssue(10, 100), OwnedIssue(10, 101), OwnedIssue(10, 102)),
            holes = emptyList(),
        )
        val reads = listOf(
            read(10, 100, NOW, hasPrices = true),
            read(10, 101, NOW, hasPrices = false),
        )

        assertEquals(listOf(OwnedIssue(10, 102)), ownedIssuesToAsk(plan, reads, NOW))
    }

    /** Thirty days later it is asked again — and the row it replaces was still being shown until now. */
    @Test
    fun `a price older than thirty days is asked again`() {
        val plan = ValuationPlan(owned = listOf(OwnedIssue(10, 100)), holes = emptyList())
        val month = read(10, 100, NOW - PRICE_LIFETIME_MILLIS - 1, hasPrices = true)

        assertEquals(listOf(OwnedIssue(10, 100)), ownedIssuesToAsk(plan, listOf(month), NOW))
        assertTrue(
            ownedIssuesToAsk(plan, listOf(month.copy(readAt = NOW - 1)), NOW).isEmpty(),
            "un precio de hoy no se vuelve a pedir",
        )
    }

    /**
     * A hole whose curated file names its issues costs **one** call and not two.
     *
     * An issue run declares them (ADR 0014), so there is nothing to look up: the saving is real and it is
     * the reason the two halves are read apart.
     */
    @Test
    fun `a hole whose file names its issue needs no lookup`() {
        val plan = ValuationPlan(
            owned = emptyList(),
            holes = listOf(
                PlateHole("run", typeId = 10, year = 1_966, issueIds = listOf(8_508, 33_204)),
                PlateHole("dates", typeId = 20, year = 1_905),
            ),
        )

        // The first of the declared issues, which is the same choice the plate makes: a slot holding two
        // varieties of one issue is one slot, and closing it costs one of them.
        assertEquals(listOf(OwnedIssue(10, 8_508)), resolvedHoleIssues(plan, emptyList(), NOW))
        assertEquals(mapOf(20 to plan.holes.drop(1)), holeIssuesToAsk(plan, emptyList(), NOW))
    }

    /**
     * The lookups are grouped by type, because a plate's holes are years of one type nine times out of ten
     * and one `/types/{id}/issues` answers all of them.
     */
    @Test
    fun `the lookups are one per type and not one per hole`() {
        val plan = ValuationPlan(
            owned = emptyList(),
            holes = listOf(
                PlateHole("dates", typeId = 20, year = 1_904),
                PlateHole("dates", typeId = 20, year = 1_905),
                PlateHole("dates", typeId = 21, year = 1_906),
            ),
        )

        val lookups = holeIssuesToAsk(plan, emptyList(), NOW)

        assertEquals(setOf(20, 21), lookups.keys)
        assertEquals(2, lookups.getValue(20).size)
        // Two lookups plus three prices: the upper bound the settings line would print.
        assertEquals(5, valuationCallCount(plan, emptyList(), NOW))
    }

    /** A hole with no year has nothing to match a listing against, so it is not asked about. */
    @Test
    fun `a hole with no year is not asked about`() {
        val plan = ValuationPlan(
            owned = emptyList(),
            holes = listOf(PlateHole("announced", typeId = 20, year = null)),
        )

        assertTrue(holeIssuesToAsk(plan, emptyList(), NOW).isEmpty())
        assertEquals(0, valuationCallCount(plan, emptyList(), NOW))
    }

    /** With everything on the phone the pass costs nothing, which is what «every launch» is bought with. */
    @Test
    fun `with everything cached a pass costs zero calls`() {
        val plan = ValuationPlan(owned = listOf(OwnedIssue(10, 100)), holes = emptyList())

        assertEquals(0, valuationCallCount(plan, listOf(read(10, 100, NOW, true)), NOW))
    }

    /**
     * A type whose listing is on the phone is not listed again — which is the whole of #452.
     *
     * The hole does not declare its issue, so before the listing was stored there was no way to tell
     * its cached price from an unasked one: `hole.issueIds.none { it in fresh }` is `true` over an
     * empty list, so the type was listed again on every pass, for ever. Measured over the father's
     * collection that was 102 listings and 111 prices on every cold start, 213 of the 1.999 calls
     * Numista let him make in August.
     */
    @Test
    fun `a type already listed is not listed again`() {
        val plan = ValuationPlan(
            owned = emptyList(),
            holes = listOf(PlateHole("dates", typeId = 20, year = 1_905)),
        )
        val listings = IssueListings(
            listedTypeIds = setOf(20),
            issueIdByTypeAndYear = mapOf((20 to 1_905) to 900),
        )

        assertTrue(holeIssuesToAsk(plan, emptyList(), NOW, listings).isEmpty())
        // The price is still owed, and now it is addressable without spending the listing.
        assertEquals(
            listOf(OwnedIssue(20, 900)),
            resolvedHoleIssues(plan, emptyList(), NOW, listings),
        )
        assertEquals(1, valuationCallCount(plan, emptyList(), NOW, listings))
    }

    /**
     * With the listing stored **and** the price fresh, the hole costs nothing at all.
     *
     * This is the assertion the old `with everything cached` one could not make: it only ever covered
     * the owned half, and the holes were the half that bled.
     */
    @Test
    fun `with the listing stored and the price fresh a hole costs zero calls`() {
        val plan = ValuationPlan(
            owned = emptyList(),
            holes = listOf(PlateHole("dates", typeId = 20, year = 1_905)),
        )
        val listings = IssueListings(
            listedTypeIds = setOf(20),
            issueIdByTypeAndYear = mapOf((20 to 1_905) to 900),
        )

        assertEquals(0, valuationCallCount(plan, listOf(read(20, 900, NOW, true)), NOW, listings))
    }

    /**
     * A year the listing does not have costs nothing either, once the listing has been stored.
     *
     * It used to cost the lookup for ever: the KDoc of `askHoles` called that «not a datum», and over
     * a plate whose curated file names a year Numista has no issue for it was a lookup every pass.
     * «This type was listed» is a datum about *this phone*, which is all that is needed to stop.
     */
    @Test
    fun `a year the stored listing does not have is not looked up again`() {
        val plan = ValuationPlan(
            owned = emptyList(),
            holes = listOf(PlateHole("dates", typeId = 20, year = 1_904)),
        )
        val listings = IssueListings(
            listedTypeIds = setOf(20),
            issueIdByTypeAndYear = mapOf((20 to 1_905) to 900),
        )

        assertTrue(holeIssuesToAsk(plan, emptyList(), NOW, listings).isEmpty())
        assertTrue(resolvedHoleIssues(plan, emptyList(), NOW, listings).isEmpty())
        assertEquals(0, valuationCallCount(plan, emptyList(), NOW, listings))
    }

    /**
     * An expired price is asked again without listing the type a second time.
     *
     * The two clocks are different on purpose: a price is the market and expires in thirty days, and
     * «which issue is the 1905 of this type» is the catalogue and does not.
     */
    @Test
    fun `an expired hole price is re-asked but the listing is not`() {
        val plan = ValuationPlan(
            owned = emptyList(),
            holes = listOf(PlateHole("dates", typeId = 20, year = 1_905)),
        )
        val listings = IssueListings(
            listedTypeIds = setOf(20),
            issueIdByTypeAndYear = mapOf((20 to 1_905) to 900),
        )
        val expired = read(20, 900, NOW - PRICE_LIFETIME_MILLIS - 1, hasPrices = true)

        assertTrue(holeIssuesToAsk(plan, listOf(expired), NOW, listings).isEmpty())
        assertEquals(
            listOf(OwnedIssue(20, 900)),
            resolvedHoleIssues(plan, listOf(expired), NOW, listings),
        )
    }
}

private fun read(typeId: Int, issueId: Int, readAt: Long, hasPrices: Boolean) =
    IssuePriceReadEntity(typeId, issueId, readAt, hasPrices)

private fun item(id: Long, typeId: Int, issueId: Int?, year: Int? = null) = CollectedItem(
    id = id,
    quantity = 1,
    typeId = typeId,
    issueYear = year,
    issueId = issueId,
)

/**
 * A date run of one type over a range of years.
 *
 * The years the collector has no piece for are the holes, which is what the threshold counts.
 */
private fun dateRun(id: String, years: IntRange, typeId: Int): CollectionCatalog =
    CollectionCatalog(
        schemaVersion = 2,
        id = id,
        name = id,
        shortName = id,
        family = id,
        issuerCode = "espagne",
        seriesStatus = SeriesStatus.Closed,
        source = "https://en.numista.com/catalogue/pieces1.html",
        updatedAt = "2026-08-10",
        members = years.map { year ->
            CollectionCatalogMember(
                id = "$id-$year",
                label = year.toString(),
                year = year,
                numistaTypeId = typeId,
            )
        },
    )
