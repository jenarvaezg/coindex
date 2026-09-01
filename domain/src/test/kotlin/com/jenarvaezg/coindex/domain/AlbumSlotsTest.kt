package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The casilla the axes of the shelf are handed (#538): measurable, placed, and counted once.
 *
 * What used to be three rules in two files of `ui/shelf` — the evidence filter, the Owned/Missing
 * fork and the second readings of the country and the year — is asserted here, on the assembly the
 * app actually runs. The axes' own tests are now about grouping and order, which is all they decide.
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
     * The fork the two axes each carried a copy of, in one place.
     *
     * `Unlisted` and `NotYetIssued` are outside it for the reason they are outside the denominator of
     * the plate (#48, #31): one has no Numista type to answer with and the other is a coin no money
     * can buy, so neither is a seat the collector could act on.
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

    /**
     * The album's counter and not a second sum of the rows behind it (#218).
     *
     * Two rows of one year is the ordinary shape of a duplicate — the father holds the 1929 twice —
     * and the casilla is one casilla holding three pieces, not two casillas.
     */
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
     * The country of the **member** and not of the catalog header (#170).
     *
     * Historia del real is issued by `mexique` and holds two New South Wales members; the axis that
     * printed the header's country is what made Nueva Gales del Sur disappear from the atlas.
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

    /**
     * A hole whose ficha has not landed still paints under its country.
     *
     * The state a phone is in between a sync arriving and the type cache filling: the member's own
     * ficha is missing, and a sibling of the same issuer answers for the name.
     */
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

    /** No code the table corrects and no ficha at all: the casilla has no country to be grouped by. */
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

    /** The year written on the casilla, which is the whole answer for every catalog that ships. */
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
     * A plate naming one type and no year stands on the type's floor, so its hole leaves a ghost.
     *
     * The fallback is deliberately narrow — one *typed member*, not one distinct type — because a
     * date run repeats its type across twenty years and each of its casillas has a year of its own.
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

    /** Nothing names a year and no ficha floors it: the casilla stands nowhere, and says so. */
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
     * The status of a casilla is the plate's own, not a second reading of the inventory (#537).
     *
     * The demonstration is a piece an issue-qualified member does not claim (ADR 0019): the row is in
     * the inventory and of the right type, and the casilla is still a hole — which is exactly what a
     * fork of its own in the UI got wrong before there was one album per assembly.
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
