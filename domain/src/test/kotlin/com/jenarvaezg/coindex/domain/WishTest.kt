package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val NOW = 1_786_400_000_000L

/**
 * What a marked casilla is, and when it stops being one (ADR 0029).
 *
 * The two halves worth pinning are the **key** and the **death**. The key is three facts and not one
 * type, because a date run repeats its type across years and an issue run across issues, so a mark
 * keyed on the type alone would be a mark over a whole plate — which is what §2 refuses. The death is
 * `memberMatches` and not a second reading of the inventory, so «dies measured» cannot disagree with
 * the album about the same coin.
 */
class WishTest {
    /** The three facts, and the first issue where the file declares several. */
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
        // A casilla whose file names no issue is keyed on the two facts it has, and null is «none»
        // rather than a zero — the sentinel lives in the table and nowhere else.
        assertEquals(
            WishKey(typeId = 20, year = 1_966, issueId = null),
            member.copy(numistaIssueIds = emptyList()).wishKey(),
        )
    }

    /**
     * A coin the app cannot name cannot be looked for, which is the recorte ADR 0029 accepts.
     *
     * An announced member has no Numista type — its `design_type_id` is the design in another variant
     * and is never consulted — so there is nothing to key a mark on, and no price to ask for either.
     */
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

    /**
     * The years of one date run are **different** casillas, which is the whole reason the key is not a
     * type: 419 of the father's unpriced holes live in date runs.
     */
    @Test
    fun `two years of one date run are two different marks`() {
        val run = dateRun("kooka", 2_010..2_012, typeId = 30)
        val keys = run.members.mapNotNull { it.wishKey() }

        assertEquals(3, keys.distinct().size)
        assertEquals(listOf(2_010, 2_011, 2_012), keys.map { it.year })
    }

    /**
     * A wish dies when its own casilla fills, and not when any coin of the type arrives.
     *
     * `memberMatches` is what decides it, which is the rule that fills the casilla on the plate: on a
     * date run it requires the year recorded on the piece, so buying the 2010 leaves the mark on the
     * 2011 exactly where it was. The row is **not deleted** — if he sells it the wish comes back, which
     * is probably right and is zero measured cases.
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

    /** The list is read newest first: the last casilla marked is the one being hunted. */
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

    /**
     * A mark no curated file claims any more is dropped from the reading and kept in the table.
     *
     * What names a casilla is the file, so a catalog retired by an app update leaves the row with
     * nothing to draw. Counting it would make the door say «7» and open on six.
     */
    @Test
    fun `a mark no catalog claims is not read`() {
        val orphan = Wish(WishKey(typeId = 999, year = 1_900, issueId = null), markedAt = NOW)

        assertTrue(wishedSlots(listOf(orphan), listOf(dateRun("kooka", 2_010..2_011, 30)), emptyList()).isEmpty())
    }

    /**
     * On an issue run the mark carries the issue, because that is what tells the casillas apart.
     *
     * The six stars of the 100 pesetas of Franco share a type **and** a year, so a wish keyed on either
     * would cover all six — and only the star the collector is missing is the one they are looking for.
     */
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
