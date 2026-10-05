package com.jenarvaezg.coindex.data.prices

import com.jenarvaezg.coindex.data.db.IssuePriceReadEntity
import com.jenarvaezg.coindex.data.db.TypeIssueEntity
import com.jenarvaezg.coindex.data.db.TypeIssueReadEntity
import com.jenarvaezg.coindex.domain.CatalogAlbums
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.Curation
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.domain.ShowcasePlate
import com.jenarvaezg.coindex.domain.Wish
import com.jenarvaezg.coindex.domain.WishedSlot
import com.jenarvaezg.coindex.domain.showcasePlate
import com.jenarvaezg.coindex.domain.wishKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val NOW = 1_754_600_000_000L
private const val DAY = 24L * 60 * 60 * 1_000

/** Which issues a pass may ask Numista about (ADR 0028 §1). */
class ValuationPlanTest {
    @Test
    fun `the owned half is every distinct issue of the collection`() {
        val plan = plan(
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

    @Test
    fun `the holes of a plate within ten slots are asked for, and no others`() {
        val withinReach = dateRun("reach", years = 1_960..1_965, typeId = 10)
        val tooFar = dateRun("far", years = 1_900..1_950, typeId = 20)

        val plan = plan(
            listOf(item(id = 1, typeId = 10, issueId = 100, year = 1_960)) +
                item(id = 2, typeId = 20, issueId = 200, year = 1_900),
            Curation(catalogs = listOf(withinReach, tooFar)),
            setOf("reach", "far"),
        )

        assertEquals(listOf("reach"), plan.holes.map { it.catalogId }.distinct())
        assertEquals(listOf(1_961, 1_962, 1_963, 1_964, 1_965), plan.holes.mapNotNull { it.year })
    }

    @Test
    fun `a closed plate contributes no holes`() {
        val closed = dateRun("closed", years = 1_960..1_961, typeId = 10)

        val plan = plan(
            listOf(
                item(id = 1, typeId = 10, issueId = 100, year = 1_960),
                item(id = 2, typeId = 10, issueId = 101, year = 1_961),
            ),
            Curation(catalogs = listOf(closed)),
            setOf("closed"),
        )

        assertTrue(plan.holes.isEmpty())
    }

    /** A catalog with no evidence has no plate on screen to show a closing cost on. */
    @Test
    fun `a catalog with no evidence is not walked at all`() {
        val plan = plan(
            listOf(item(id = 1, typeId = 10, issueId = 100, year = 1_960)),
            Curation(catalogs = listOf(dateRun("reach", 1_960..1_962, typeId = 10))),
            evidencedCatalogIds = emptySet(),
        )

        assertTrue(plan.holes.isEmpty())
    }

    /** A row without prices is still an answer (ADR 0028 §4). */
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

    @Test
    fun `a price older than ninety days is asked again`() {
        val plan = ValuationPlan(owned = listOf(OwnedIssue(10, 100)), holes = emptyList())
        val quarter = read(10, 100, NOW - PRICE_LIFETIME_MILLIS - 1, hasPrices = true)

        assertEquals(listOf(OwnedIssue(10, 100)), ownedIssuesToAsk(plan, listOf(quarter), NOW))
        assertTrue(
            ownedIssuesToAsk(plan, listOf(quarter.copy(readAt = NOW - 1)), NOW).isEmpty(),
            "un precio de hoy no se vuelve a pedir",
        )
    }

    /**
     * Pins the number #561 decided; every other test reads [PRICE_LIFETIME_MILLIS] and would pass
     * at any value.
     */
    @Test
    fun `a catalog price lives the ninety days of the listing that addresses it`() {
        assertEquals(90 * DAY, PRICE_LIFETIME_MILLIS)
        assertEquals(LISTING_LIFETIME_MILLIS, PRICE_LIFETIME_MILLIS)
    }

    /** The old monthly life made the whole cold pass fall due on one day (ADR 0028 §5, #561). */
    @Test
    fun `a price of a month and a day is no longer asked again`() {
        val plan = ValuationPlan(owned = listOf(OwnedIssue(10, 100)), holes = emptyList())
        val month = read(10, 100, NOW - 31 * DAY, hasPrices = true)

        assertTrue(ownedIssuesToAsk(plan, listOf(month), NOW).isEmpty())
        assertEquals(0, valuationCallCount(plan, listOf(month), NOW))
    }

    /** An issue run declares its issues (ADR 0014), so its holes cost one call and not two. */
    @Test
    fun `a hole whose file names its issue needs no lookup`() {
        val plan = ValuationPlan(
            owned = emptyList(),
            holes = listOf(
                PlateHole("run", typeId = 10, year = 1_966, issueIds = listOf(8_508, 33_204)),
                PlateHole("dates", typeId = 20, year = 1_905),
            ),
        )

        // The first declared issue, as the plate picks: two varieties in one slot cost one of them.
        assertEquals(listOf(OwnedIssue(10, 8_508)), resolvedHoleIssues(plan, emptyList(), NOW))
        assertEquals(mapOf(20 to plan.holes.drop(1)), holeIssuesToAsk(plan, emptyList(), NOW))
    }

    /** One `/types/{id}/issues` answers every year of a type. */
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

    /** With no year there is nothing to match in a listing. */
    @Test
    fun `a hole with no year is not asked about`() {
        val plan = ValuationPlan(
            owned = emptyList(),
            holes = listOf(PlateHole("announced", typeId = 20, year = null)),
        )

        assertTrue(holeIssuesToAsk(plan, emptyList(), NOW).isEmpty())
        assertEquals(0, valuationCallCount(plan, emptyList(), NOW))
    }

    /** That is what lets the pass run on every launch. */
    @Test
    fun `with everything cached a pass costs zero calls`() {
        val plan = ValuationPlan(owned = listOf(OwnedIssue(10, 100)), holes = emptyList())

        assertEquals(0, valuationCallCount(plan, listOf(read(10, 100, NOW, true)), NOW))
    }

    /** A hole does not declare its issue; the stored listing addresses it with no lookup (#452). */
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

    /** «This type was listed» is enough to stop asking, whatever years the listing brought. */
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
     * Price and listing share a lifetime since #561 but are read on different days, so each expires
     * on its own.
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

    /**
     * The pass drops an expired listing because it is about to list the type again, and the stale
     * map would price the wrong issue first. The screen keeps it, as ADR 0028 §5 keeps expired rows
     * (#493).
     */
    @Test
    fun `an expired listing leaves the pass but stays on the screen`() {
        val reads = listOf(TypeIssueReadEntity(typeId = 20, readAt = NOW - LISTING_LIFETIME_MILLIS - 1))
        val issues = listOf(
            TypeIssueEntity(typeId = 20, issueId = 900, position = 0, year = 1_905, gregorianYear = null),
        )

        val forThePass = IssueListings.of(reads, issues, NOW)
        val heldByThePhone = IssueListings.held(reads, issues)

        assertEquals(IssueListings.EMPTY, forThePass)
        assertEquals(setOf(20), heldByThePhone.listedTypeIds)
        assertEquals(900, heldByThePhone.issueOf(PlateHole("dates", typeId = 20, year = 1_905)))
    }

    /**
     * Same rule as a [PlateHole] (#493): the plate header adds up what the pass fetched, so both
     * must address the same issue.
     */
    @Test
    fun `an empty casilla is addressed by its file first and by the listing after`() {
        val listings = IssueListings(
            listedTypeIds = setOf(20),
            issueIdByTypeAndYear = mapOf((20 to 1_905) to 900),
        )
        val member = CollectionCatalogMember(
            id = "dates-1905",
            label = "1905",
            year = 1_905,
            numistaTypeId = 20,
        )

        assertEquals(900, listings.issueOf(member))
        // The first of the file's ids: two varieties in one casilla cost one of them.
        assertEquals(
            41,
            listings.issueOf(member.copy(numistaIssueIds = listOf(41, 42))),
        )
        // An announced casilla has no type at all, so there is nothing to address a price to.
        assertNull(listings.issueOf(member.copy(numistaTypeId = null)))
    }

    /**
     * A mark is the collector asking for that number, so it overrides both the threshold and the
     * evidence filter of #282, for that slot alone (ADR 0029 §4).
     */
    @Test
    fun `a marked casilla enters the plan past the threshold and with no evidence`() {
        val tooFar = dateRun("far", years = 1_900..1_950, typeId = 20)
        val unowned = dateRun("window", years = 1_990..1_991, typeId = 30)

        val plan = plan(
            items = listOf(item(id = 1, typeId = 20, issueId = 200, year = 1_900)),
            curation = Curation(catalogs = listOf(tooFar, unowned)),
            evidencedCatalogIds = setOf("far"),
            wishes = listOf(
                wish(tooFar, "far-1905"),
                wish(unowned, "window-1990"),
            ),
        )

        assertEquals(
            listOf(PlateHole("far", 20, 1_905), PlateHole("window", 30, 1_990)),
            plan.holes,
        )
    }

    @Test
    fun `a marked casilla of a plate within reach is not asked about twice`() {
        val withinReach = dateRun("reach", years = 1_960..1_962, typeId = 10)

        val plan = plan(
            items = listOf(item(id = 1, typeId = 10, issueId = 100, year = 1_960)),
            curation = Curation(catalogs = listOf(withinReach)),
            evidencedCatalogIds = setOf("reach"),
            wishes = listOf(wish(withinReach, "reach-1961")),
        )

        assertEquals(listOf(1_961, 1_962), plan.holes.mapNotNull { it.year })
    }

    /**
     * The ceiling the gesture's «+2 consultas al mes» quotes (ADR 0029 §5). Both calls last a
     * quarter since #561, but the figure must never round a spend down.
     */
    @Test
    fun `a mark costs two calls a month, or one when its file names the issue`() {
        val dates = dateRun("dates", years = 1_900..1_902, typeId = 20)
        val declared = dates.copy(
            id = "declared",
            members = dates.members.map { it.copy(numistaIssueIds = listOf(900)) },
        )

        assertEquals(2, wishCallsPerMonth(listOf(wish(dates, "dates-1900"))))
        assertEquals(1, wishCallsPerMonth(listOf(wish(declared, "dates-1900"))))
        // Two years of one type share the listing: two marks are three calls and not four.
        assertEquals(
            3,
            wishCallsPerMonth(listOf(wish(dates, "dates-1900"), wish(dates, "dates-1901"))),
        )
        assertEquals(0, wishCallsPerMonth(emptyList()))
    }

    /**
     * The threshold of ADR 0028 §1 does not apply to a shelf-window plate, opened to see what the
     * collector lacks (ADR 0030 §3, §7); twelve slots is two over it.
     */
    @Test
    fun `tasar one plate of the shelf window asks about every casilla of it`() {
        val twelve = dateRun("lunar", years = 2_012..2_023, typeId = 30)

        val plan = showcaseValuationPlan(showcase(twelve))

        assertTrue(plan.owned.isEmpty())
        assertEquals(12, plan.holes.size)
        assertEquals(setOf("lunar"), plan.holes.mapTo(mutableSetOf()) { it.catalogId })
        // The pass's own plan leaves it out.
        assertTrue(plan(emptyList(), Curation(listOf(twelve)), emptySet()).holes.isEmpty())
    }

    /**
     * The gesture uses the pass's arithmetic (ADR 0030 §3): one `/prices` per hole plus one listing
     * per type whose file names no issue, minus what is fresh on the phone.
     */
    @Test
    fun `the gesture counts what the pass would spend, and nothing it already holds`() {
        val dates = dateRun("dates", years = 1_900..1_902, typeId = 20)
        val declared = dates.copy(
            id = "declared",
            members = dates.members.mapIndexed { at, member ->
                member.copy(numistaIssueIds = listOf(900 + at))
            },
        )

        assertEquals(4, showcaseCallCount(showcase(dates), PriceBook(), NOW))
        assertEquals(3, showcaseCallCount(showcase(declared), PriceBook(), NOW))
        // The 900 is on the phone and fresh, so two of the three are left.
        assertEquals(
            2,
            showcaseCallCount(showcase(declared), PriceBook(readAt = mapOf((20 to 900) to NOW)), NOW),
        )
        // Once expired it is asked about again: this is what «Volver a tasar» buys.
        assertEquals(
            3,
            showcaseCallCount(
                showcase(declared),
                PriceBook(readAt = mapOf((20 to 900) to NOW - PRICE_LIFETIME_MILLIS - 1)),
                NOW,
            ),
        )
    }
}

/**
 * The screen's reading of listings ignores expiry (#493), but the gesture counts what the pass will
 * spend, so an expired listing is a lookup (ADR 0030 §3). `freshListings` keeps that in the book.
 */
class ShowcaseSpendTest {
    @Test
    fun `a listing past its ninety days is counted as a lookup the pass will pay for`() {
        val dates = dateRun("dates", years = 1_900..1_902, typeId = 20)
        val issues = (1_900..1_902).mapIndexed { at, year ->
            TypeIssueEntity(20, 900 + at, at, year, year)
        }
        fun bookAt(readAt: Long): PriceBook {
            val reads = listOf(TypeIssueReadEntity(20, readAt))
            return priceBook(
                rows = emptyList(),
                spot = null,
                listings = IssueListings.held(reads, issues),
                listingReads = reads,
            )
        }

        // Fresh, the type counts as listed and only the three prices are counted.
        assertEquals(3, showcaseCallCount(showcase(dates), bookAt(NOW), NOW))
        // Expired, the screen's held reading still lists the type — and the gesture still counts the
        // fourth call, because the pass would spend it on the listing.
        assertEquals(4, showcaseCallCount(showcase(dates), bookAt(NOW - LISTING_LIFETIME_MILLIS - 1), NOW))
    }
}

/** The pass's plan, with albums built by the same call `Curation.assemble` uses (#537). */
private fun plan(
    items: List<CollectedItem>,
    curation: Curation,
    evidencedCatalogIds: Set<String>,
    wishes: List<WishedSlot> = emptyList(),
): ValuationPlan = valuationPlan(
    items = items,
    curation = curation,
    albums = CatalogAlbums.over(curation.catalogs, items),
    evidencedCatalogIds = evidencedCatalogIds,
    wishes = wishes,
)

/** The catalog as the shelf window holds it: nothing owned, so every casilla is a hole. */
private fun showcase(catalog: CollectionCatalog): ShowcasePlate = requireNotNull(
    showcasePlate(
        catalog,
        requireNotNull(CatalogAlbums.over(listOf(catalog), emptyList())[catalog]),
        emptySet(),
    ),
)

/** One marked casilla of a curated catalog, resolved as the annex resolves it. */
private fun wish(catalog: CollectionCatalog, memberId: String): WishedSlot {
    val member = catalog.members.first { it.id == memberId }
    return WishedSlot(
        wish = Wish(key = requireNotNull(member.wishKey()), markedAt = NOW),
        catalog = catalog,
        member = member,
    )
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

/** A date run of one type; the years without a piece are the holes the threshold counts. */
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
