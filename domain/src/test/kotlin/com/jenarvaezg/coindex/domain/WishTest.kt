package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val NOW = 1_786_400_000_000L

/**
 * What a marked casilla is, and when it stops being one (ADR 0029). The key is three facts, since
 * a mark keyed on the type alone would cover a whole plate (§2). A mark dies through
 * `memberMatches`, so it cannot disagree with the album about the same coin.
 */
class WishTest {
    /** With several issues declared, the key takes the first. */
    @Test
    fun `a casilla is keyed by its type, its year and the issue its file declares`() {
        val member = CollectionCatalogMember(
            id = "franco-1966-star67",
            label = "Estrella 67",
            year = 1_966,
            numistaTypeId = 20,
            numistaIssueIds = listOf(8_508, 33_204),
        )

        assertEquals(WishKey(typeId = 20, year = 1_966, issueId = 8_508), member.wishKey())
        // No issue in the file: null rather than zero, since the sentinel lives only in the table.
        assertEquals(
            WishKey(typeId = 20, year = 1_966, issueId = null),
            member.copy(numistaIssueIds = emptyList()).wishKey(),
        )
    }

    /** An announced member's `design_type_id` is never consulted, so it has no key (ADR 0029). */
    @Test
    fun `a casilla with no Numista type cannot be marked`() {
        val announced = CollectionCatalogMember(
            id = "beast-announced",
            label = "Seymour Panther",
            year = 2_026,
            status = MemberStatus.Announced,
            source = "https://example.test/announced",
            sourceNote = "anunciada",
            designTypeId = 99,
        )

        assertNull(announced.wishKey())
    }

    @Test
    fun `two years of one date run are two different marks`() {
        val run = dateRun("kooka", 2_010..2_012, typeId = 30)
        val keys = run.members.mapNotNull { it.wishKey() }

        assertEquals(3, keys.distinct().size)
        assertEquals(listOf(2_010, 2_011, 2_012), keys.map { it.year })
    }

    /**
     * Decided by `memberMatches`, so on a date run the piece's year counts. The row is kept: if the
     * piece is sold, the wish comes back.
     */
    @Test
    fun `a wish dies when its own casilla fills and not when a sibling does`() {
        val run = dateRun("kooka", 2_010..2_011, typeId = 30)
        val marks = run.members.map { member ->
            Wish(key = requireNotNull(member.wishKey()), markedAt = NOW)
        }
        val bought2010 = listOf(
            CollectedItem(id = 1, quantity = 1, typeId = 30, issueYear = 2_010),
        )

        val alive = wishedSlots(marks, listOf(run), bought2010)

        assertEquals(listOf(2_011), alive.map { it.member.year })
        // And with nothing owned, both are alive.
        assertEquals(2, wishedSlots(marks, listOf(run), emptyList()).size)
    }

    /** Newest first: the last casilla marked is the one being hunted. */
    @Test
    fun `the list is ordered by the mark and not by the catalog`() {
        val run = dateRun("kooka", 2_010..2_012, typeId = 30)
        val marks = run.members.mapIndexed { position, member ->
            Wish(key = requireNotNull(member.wishKey()), markedAt = NOW + position)
        }

        assertEquals(
            listOf(2_012, 2_011, 2_010),
            wishedSlots(marks, listOf(run), emptyList()).map { it.member.year },
        )
    }

    /** Kept in the table but not read, or the door would say «7» and open on six. */
    @Test
    fun `a mark no catalog claims is not read`() {
        val orphan = Wish(WishKey(typeId = 999, year = 1_900, issueId = null), markedAt = NOW)

        assertTrue(wishedSlots(listOf(orphan), listOf(dateRun("kooka", 2_010..2_011, 30)), emptyList()).isEmpty())
    }

    /** The stars of Franco's 100 pesetas share type and year; only the issue tells them apart. */
    @Test
    fun `an issue run keeps its marks apart by issue`() {
        val run = issueRun("franco", typeId = 20, year = 1_966, issues = listOf(8_508, 33_204))
        val marks = run.members.map { Wish(requireNotNull(it.wishKey()), NOW) }
        // The 8.508 arrives; the other star is still wanted.
        val owned = listOf(
            CollectedItem(id = 1, quantity = 1, typeId = 20, issueYear = 1_966, issueId = 8_508),
        )

        val alive = wishedSlots(marks, listOf(run), owned)

        assertEquals(listOf(33_204), alive.map { it.key.issueId })
    }
}

private fun dateRun(id: String, years: IntRange, typeId: Int): CollectionCatalog = catalog(
    id = id,
    schemaVersion = 2,
    members = years.map { year ->
        CollectionCatalogMember(
            id = "$id-$year",
            label = year.toString(),
            year = year,
            numistaTypeId = typeId,
        )
    },
)

private fun issueRun(
    id: String,
    typeId: Int,
    year: Int,
    issues: List<Int>,
): CollectionCatalog = catalog(
    id = id,
    schemaVersion = 5,
    members = issues.map { issueId ->
        CollectionCatalogMember(
            id = "$id-$issueId",
            label = "Estrella $issueId",
            year = year,
            numistaTypeId = typeId,
            numistaIssueIds = listOf(issueId),
        )
    },
)

private fun catalog(
    id: String,
    schemaVersion: Int,
    members: List<CollectionCatalogMember>,
): CollectionCatalog = CollectionCatalog(
    schemaVersion = schemaVersion,
    id = id,
    name = id,
    shortName = id,
    family = id,
    issuerCode = "espagne",
    seriesStatus = SeriesStatus.Closed,
    source = "https://en.numista.com/catalogue/pieces1.html",
    updatedAt = "2026-08-14",
    members = members,
)
