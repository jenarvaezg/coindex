package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The casillas the shelf's axes receive (#538): measurable, placed and counted once, asserted on
 * the assembly the app runs. The axes' own tests only cover grouping and order.
 */
class AlbumSlotsTest {
    @Test
    fun `a plate with no evidence has no casilla on any axis`() {
        val slots = assemble(
            catalogs = listOf(dateRun(id = "fuertes", typeId = FUERTE, years = 1929..1931)),
            items = emptyList(),
            typeMeta = mapOf(FUERTE to meta(FUERTE, "venezuela", "Venezuela")),
        ).slots

        assertEquals(emptyList(), slots, "una lámina que no se puede abrir no tiene asientos")
    }

    /**
     * `Unlisted` and `NotYetIssued` stay out, as they do from the plate's denominator (#48, #31):
     * the collector can't act on either.
     */
    @Test
    fun `only what can be owned or missed becomes a casilla`() {
        val catalog = catalog(
            id = "mixta",
            issuer = "venezuela",
            schemaVersion = 2,
            members = listOf(
                member("1929", FUERTE, 1929),
                member("1930", FUERTE, 1930),
                member("sin-ficha", null, 1931, status = MemberStatus.Unlisted),
                member("2027", FUERTE, 2027, status = MemberStatus.Announced),
            ),
        )
        val slots = assemble(
            catalogs = listOf(catalog),
            items = listOf(item(id = 1, typeId = FUERTE, year = 1929)),
            typeMeta = mapOf(FUERTE to meta(FUERTE, "venezuela", "Venezuela")),
        ).slots

        assertEquals(listOf("1929", "1930"), slots.map { it.memberId })
        assertEquals(listOf(true, false), slots.map { it.owned })
        assertEquals(listOf(1, 0), slots.map { it.quantity })
    }

    /** The album's counter, not a second sum (#218): two rows of one year are one casilla. */
    @Test
    fun `an owned casilla counts every piece behind it, and a hole counts none`() {
        val slots = assemble(
            catalogs = listOf(dateRun(id = "fuertes", typeId = FUERTE, years = 1929..1930)),
            items = listOf(
                item(id = 1, typeId = FUERTE, year = 1929, quantity = 2),
                item(id = 2, typeId = FUERTE, year = 1929, quantity = 1),
            ),
            typeMeta = mapOf(FUERTE to meta(FUERTE, "venezuela", "Venezuela")),
        ).slots

        assertEquals(listOf(true, false), slots.map { it.owned })
        assertEquals(listOf(3, 0), slots.map { it.quantity })
    }

    /**
     * Not the header's country (#170): Historia del real is issued by `mexique` and holds two New
     * South Wales members.
     */
    @Test
    fun `a casilla falls in the member's country, cured`() {
        val catalog = catalog(
            id = "historia",
            issuer = "mexique",
            members = listOf(
                member("nsw", NSW, 1813, issuerCode = "new_south_wales"),
                member("real", REAL, 1791),
            ),
        )
        val slots = assemble(
            catalogs = listOf(catalog),
            items = listOf(item(id = 1, typeId = REAL, year = 1791)),
            typeMeta = mapOf(
                NSW to meta(NSW, "new_south_wales", "New South Wales"),
                REAL to meta(REAL, "mexique", "México"),
            ),
        ).slots

        assertEquals(listOf("Nueva Gales del Sur", "México"), slots.map { it.country })
    }

    /** Before the type cache fills, a sibling of the same issuer names the country. */
    @Test
    fun `a casilla whose ficha is missing borrows the name of its issuer`() {
        val catalog = catalog(
            id = "fuertes",
            issuer = "venezuela",
            members = listOf(member("1929", FUERTE, 1929), member("1930", UNCACHED, 1930)),
        )
        val slots = assemble(
            catalogs = listOf(catalog),
            items = listOf(item(id = 1, typeId = FUERTE, year = 1929)),
            typeMeta = mapOf(FUERTE to meta(FUERTE, "venezuela", "Venezuela")),
        ).slots

        assertEquals(listOf("Venezuela", "Venezuela"), slots.map { it.country })
    }

    @Test
    fun `a casilla with neither a cured code nor a ficha has no country`() {
        val catalog = catalog(
            id = "huerfana",
            issuer = "sin-emisor",
            members = listOf(member("a", FUERTE, 1929), member("b", UNCACHED, 1930)),
        )
        val slots = assemble(
            catalogs = listOf(catalog),
            items = listOf(item(id = 1, typeId = FUERTE, year = 1929)),
            typeMeta = mapOf(FUERTE to TypeMeta(id = FUERTE, issuerCode = "sin-emisor")),
        ).slots

        assertTrue(slots.all { it.country == null })
    }

    @Test
    fun `a date run puts each casilla on its own year`() {
        val slots = assemble(
            catalogs = listOf(dateRun(id = "fuertes", typeId = FUERTE, years = 1929..1931)),
            items = listOf(item(id = 1, typeId = FUERTE, year = 1930)),
            typeMeta = mapOf(FUERTE to meta(FUERTE, "venezuela", "Venezuela", minYear = 1900)),
        ).slots

        assertEquals(listOf(1929, 1930, 1931), slots.map { it.year })
    }

    /**
     * Its hole leaves a ghost. Narrow on purpose: one typed member, not one distinct type, since a
     * date run repeats its type and each of its casillas has its own year.
     */
    @Test
    fun `a single-type plate with no year on the member stands on the type's floor`() {
        val catalog = catalog(
            id = "onza",
            issuer = "mexique",
            members = listOf(member("unica", ONZA, year = null)),
        )
        val slots = assemble(
            catalogs = listOf(catalog),
            items = listOf(item(id = 1, typeId = ONZA, year = 1978)),
            typeMeta = mapOf(ONZA to meta(ONZA, "mexique", "México", minYear = 1978)),
        ).slots

        assertEquals(listOf(1978), slots.map { it.year })
        assertEquals(listOf(ONZA), slots.map { it.typeId })
    }

    @Test
    fun `a casilla nobody dates has no year rather than a seat before the era`() {
        val catalog = catalog(
            id = "onza",
            issuer = "mexique",
            members = listOf(member("unica", ONZA, year = null), member("otra", REAL, year = null)),
        )
        val slots = assemble(
            catalogs = listOf(catalog),
            items = listOf(item(id = 1, typeId = ONZA, year = 1978)),
            typeMeta = mapOf(
                ONZA to meta(ONZA, "mexique", "México"),
                REAL to meta(REAL, "mexique", "México", minYear = 1791),
            ),
        ).slots

        assertTrue(slots.all { it.year == null }, "dos tipos: la lámina no puede prestar el suyo")
        assertNull(slots.first { it.memberId == "otra" }.year)
    }

    /**
     * Not a second reading of the inventory (#537): a piece of the right type that an
     * issue-qualified member does not claim (ADR 0019) leaves the casilla a hole.
     */
    @Test
    fun `a casilla says what its plate says about the same coin`() {
        val catalog = catalog(
            id = "eagle",
            issuer = "etats-unis",
            members = listOf(
                member("2025", EAGLE, 2025, issueIds = listOf(1)),
                member("2026", EAGLE, 2026, issueIds = listOf(2)),
            ),
        )
        val collection = assemble(
            catalogs = listOf(catalog),
            items = listOf(item(id = 1, typeId = EAGLE, year = 2025, issueId = 1)),
            typeMeta = mapOf(EAGLE to meta(EAGLE, "etats-unis", "Estados Unidos")),
        )

        val album = checkNotNull(collection.albums.of("eagle"))
        assertEquals(CoverageRatio(owned = 1, issued = 2), album.coverage())
        assertEquals(listOf(true, false), collection.slots.map { it.owned })
    }

    private fun assemble(
        catalogs: List<CollectionCatalog>,
        items: List<CollectedItem>,
        typeMeta: TypeMetaIndex,
    ): AssembledCollection = Curation(catalogs = catalogs)
        .assemble(CollectionSnapshot(items = items, typeMeta = typeMeta))

    private fun item(
        id: Long,
        typeId: Int,
        year: Int,
        quantity: Int = 1,
        issueId: Int? = null,
    ) = CollectedItem(
        id = id,
        quantity = quantity,
        typeId = typeId,
        issueYear = year,
        issueId = issueId,
    )

    private fun meta(id: Int, code: String, name: String, minYear: Int? = null) = TypeMeta(
        id = id,
        issuerCode = code,
        issuerName = name,
        minYear = minYear,
    )

    private fun catalog(
        id: String,
        issuer: String,
        members: List<CollectionCatalogMember>,
        schemaVersion: Int = 1,
    ) = CollectionCatalog(
        schemaVersion = schemaVersion,
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
        updatedAt = "2026-08-30",
        members = members,
    )

    private fun dateRun(id: String, typeId: Int, years: IntRange) = catalog(
        id = id,
        issuer = "venezuela",
        schemaVersion = 2,
        members = years.map { year -> member("$year", typeId, year) },
    )

    private fun member(
        id: String,
        typeId: Int?,
        year: Int?,
        issuerCode: String? = null,
        status: MemberStatus = MemberStatus.Issued,
        issueIds: List<Int> = emptyList(),
    ) = CollectionCatalogMember(
        id = id,
        label = id,
        year = year,
        numistaTypeId = typeId,
        numistaIssueIds = issueIds,
        status = status,
        issuerCode = issuerCode,
    )

    private companion object {
        const val FUERTE = 10
        const val UNCACHED = 11
        const val NSW = 20
        const val REAL = 21
        const val ONZA = 30
        const val EAGLE = 40
    }
}
