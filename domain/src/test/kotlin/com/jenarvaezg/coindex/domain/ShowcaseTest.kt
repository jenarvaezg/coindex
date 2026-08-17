package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Which curated catalogs the shelf window of «Explorar» is made of (ADR 0030 §1).
 *
 * Two facts decide it and nothing else does: **no evidence**, and **fewer than twenty measurable
 * casillas**. The cut is about what a plate can say — at twenty slots a plate of zeros stops being a
 * shelf window and becomes a catalogue nobody asked for — so what it counts is the divisor the plate
 * itself divides by, and never the members of the file.
 */
class ShowcaseTest {
    /** The cut is exclusive, which is what «fewer than twenty» means where it is measured. */
    @Test
    fun `a catalog with no evidence is in the window below twenty casillas and out at twenty`() {
        val nineteen = dateRun("britannia", 2_000..2_018)
        val twenty = dateRun("panda", 2_000..2_019)

        assertEquals(19, showcasePlate(nineteen, albumOf(nineteen), emptySet())?.slots)
        assertNull(showcasePlate(twenty, albumOf(twenty), emptySet()))
    }

    /**
     * Evidence takes a catalog out of the window, and it is the same evidence that opens its plate.
     *
     * The window is «what you do **not** collect», so the moment a sync brings one coin of it the plate
     * becomes the collector's own — with its ratio, its two figures of money and its «Exportar».
     */
    @Test
    fun `a catalog the collector owns something of is not in the window`() {
        val run = dateRun("kooka", 2_010..2_012)
        val owned = listOf(CollectedItem(id = 1, quantity = 1, typeId = TYPE_ID, issueYear = 2_010))

        assertNull(showcasePlate(run, albumOf(run, owned), setOf("kooka")))
        // And with the evidence gone it comes back: nothing is stored about the window.
        assertNotNull(showcasePlate(run, albumOf(run), emptySet()))
    }

    /**
     * The cut counts the **divisor** and not the file's members.
     *
     * An announced casilla is not something money can buy and an unlisted one cannot be measured at
     * all, so neither is a slot of the window: a plate of nineteen buyable coins plus two announcements
     * is a plate of nineteen, and pushing it out over a design nobody has struck would be the divisor
     * rule read backwards.
     */
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

    /** Every casilla of a plate in the window is empty, which is what «you own none of it» means. */
    @Test
    fun `every casilla of a plate in the window is a hole`() {
        val kooka = dateRun("kooka", 2_010..2_012)

        val plate = assertNotNull(showcasePlate(kooka, albumOf(kooka), emptySet()))

        assertTrue(plate.album.members.all { it.status is CollectionCatalogMemberStatus.Missing })
        assertEquals(0, plate.album.ownedMembers())
    }

    /**
     * The default order is fewest casillas first (ADR 0030 §8).
     *
     * It is the same «what can be said» reading as the cut itself: the plate closest to being a plate
     * comes first. The order #282 chose — by cost of entering — cannot be the default, because with the
     * tasación in the collector's hands the shelf is born with no amount at all.
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

    /** A catalog with nothing measurable in it is not a shelf window: there is no plate to open. */
    @Test
    fun `a catalog with no measurable casilla is not in the window`() {
        val onlyAnnounced = catalog("soon", listOf(announced("soon-2027", 2_027)))

        assertNull(showcasePlate(onlyAnnounced, albumOf(onlyAnnounced), emptySet()))
    }
}

private const val TYPE_ID = 30

/**
 * The album the assembly would carry for this catalog (#537).
 *
 * Through [CatalogAlbums] and not straight to the builder, because that is the door the window reads
 * its albums by: a test that built one of its own would be the seventh place they are built.
 */
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
