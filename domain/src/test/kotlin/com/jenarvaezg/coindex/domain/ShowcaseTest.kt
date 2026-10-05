package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Which curated catalogs make up the «Explorar» shelf window (ADR 0030 §1): no evidence and fewer
 * than twenty measurable casillas, counted by the plate's divisor and not by the file's members.
 */
class ShowcaseTest {
    @Test
    fun `a catalog with no evidence is in the window below twenty casillas and out at twenty`() {
        val nineteen = dateRun("britannia", 2_000..2_018)
        val twenty = dateRun("panda", 2_000..2_019)

        assertEquals(19, showcasePlate(nineteen, albumOf(nineteen), emptySet())?.slots)
        assertNull(showcasePlate(twenty, albumOf(twenty), emptySet()))
    }

    /** The same evidence that opens its plate. */
    @Test
    fun `a catalog the collector owns something of is not in the window`() {
        val run = dateRun("kooka", 2_010..2_012)
        val owned = listOf(CollectedItem(id = 1, quantity = 1, typeId = TYPE_ID, issueYear = 2_010))

        assertNull(showcasePlate(run, albumOf(run, owned), setOf("kooka")))
        // And with the evidence gone it comes back: nothing is stored about the window.
        assertNotNull(showcasePlate(run, albumOf(run), emptySet()))
    }

    /** The cut counts the plate's divisor, not the file's members. */
    @Test
    fun `announced and unlisted casillas do not count towards the cut`() {
        val members = dateRunMembers("beasts", 2_000..2_018) + listOf(
            announced("beasts-2027", 2_027),
            announced("beasts-2028", 2_028),
            unlisted("beasts-1998", 1_998),
        )

        val beasts = catalog("beasts", members)

        val plate = showcasePlate(beasts, albumOf(beasts), emptySet())

        assertEquals(19, plate?.slots)
        assertEquals(22, plate?.album?.members?.size)
    }

    @Test
    fun `every casilla of a plate in the window is a hole`() {
        val kooka = dateRun("kooka", 2_010..2_012)

        val plate = assertNotNull(showcasePlate(kooka, albumOf(kooka), emptySet()))

        assertTrue(plate.album.members.all { it.status is CollectionCatalogMemberStatus.Missing })
        assertEquals(0, plate.album.ownedMembers())
    }

    /**
     * ADR 0030 §8. Not by cost of entering, as #282 chose: with the tasación in the collector's
     * hands, the shelf starts with no amounts.
     */
    @Test
    fun `the window is ordered by casillas, fewest first`() {
        val catalogs = listOf(
            dateRun("britannia", 2_000..2_014),
            dateRun("kooka", 2_010..2_012),
            dateRun("libertad", 2_000..2_007),
        )

        val window = showcasePlates(catalogs, CatalogAlbums.over(catalogs, emptyList()), emptySet())

        assertEquals(listOf("kooka", "libertad", "britannia"), window.map { it.catalog.id })
        assertEquals(listOf(3, 8, 15), window.map { it.slots })
    }

    @Test
    fun `a catalog with no measurable casilla is not in the window`() {
        val onlyAnnounced = catalog("soon", listOf(announced("soon-2027", 2_027)))

        assertNull(showcasePlate(onlyAnnounced, albumOf(onlyAnnounced), emptySet()))
    }
}

private const val TYPE_ID = 30

/** Built through [CatalogAlbums], as the window reads its albums, and not by the builder (#537). */
private fun albumOf(
    catalog: CollectionCatalog,
    items: List<CollectedItem> = emptyList(),
): CollectionCatalogAlbum = requireNotNull(CatalogAlbums.over(listOf(catalog), items)[catalog])

private fun dateRunMembers(id: String, years: IntRange): List<CollectionCatalogMember> =
    years.map { year ->
        CollectionCatalogMember(
            id = "$id-$year",
            label = year.toString(),
            year = year,
            numistaTypeId = TYPE_ID,
        )
    }

private fun dateRun(id: String, years: IntRange): CollectionCatalog =
    catalog(id, dateRunMembers(id, years))

private fun announced(id: String, year: Int): CollectionCatalogMember = CollectionCatalogMember(
    id = id,
    label = "Anunciada $year",
    year = year,
    status = MemberStatus.Announced,
    source = "https://example.test/announced",
    sourceNote = "anunciada",
    designTypeId = 99,
)

private fun unlisted(id: String, year: Int): CollectionCatalogMember = CollectionCatalogMember(
    id = id,
    label = "No listada $year",
    year = year,
    status = MemberStatus.Unlisted,
    source = "https://example.test/unlisted",
    sourceNote = "sin tipo en Numista",
)

private fun catalog(
    id: String,
    members: List<CollectionCatalogMember>,
): CollectionCatalog = CollectionCatalog(
    schemaVersion = 2,
    id = id,
    name = id,
    shortName = id,
    family = id,
    issuerCode = "australie",
    seriesStatus = SeriesStatus.Closed,
    source = "https://en.numista.com/catalogue/pieces1.html",
    updatedAt = "2026-08-14",
    members = members,
)
