package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogAlbum
import com.jenarvaezg.coindex.domain.CollectionCatalogAlbumMember
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.CollectionCatalogMemberStatus
import com.jenarvaezg.coindex.domain.CommemorativeProgramme
import com.jenarvaezg.coindex.domain.CommemorativeProgrammeMember
import com.jenarvaezg.coindex.domain.Finish
import com.jenarvaezg.coindex.domain.ItemRef
import com.jenarvaezg.coindex.domain.MemberStatus
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.domain.ProgrammeProgress
import com.jenarvaezg.coindex.domain.ProgrammeStanding
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.domain.buildCollectionCatalogAlbum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The plate as its three drawers receive it — the screen, the exported sheet, the notebook page.
 *
 * Everything below is read off one [PlateSubject] because that is what shipped for #218: the plate
 * is worded once and consumed whole, so a fact that used to be recomputed in three places can no
 * longer be recomputed differently in one of them.
 *
 * What a cell has left to say once the heading has said the rest is the older half of this file. A
 * date run repeats one type across years, so «1879 · Numista 10340» under a cell titled 1879 was the
 * same two facts twice, twenty-one times over. An issue run repeats the year as well. Whatever every
 * member shares belongs in the heading; the cell keeps only what tells it apart — except the Numista
 * type, which is never in a cell at all (issue #88): it heads the plate when every cell is that type,
 * and otherwise the plate simply does not say it.
 */
class PlateSubjectTest {
    private fun member(id: String, label: String, year: Int, typeId: Int) =
        CollectionCatalogMember(id = id, label = label, year = year, numistaTypeId = typeId)

    private val dateRun = listOf(
        member("1879", "1879", 1879, 10_340),
        member("1886", "1886", 1886, 10_340),
    )

    private val issueRun = listOf(
        member("estrella-66", "Estrella 66", 1966, 1_885),
        member("estrella-67", "Estrella 67", 1966, 1_885),
    )

    private val typeRun = listOf(
        member("2011-koala", "Koala - Silver Bullion Coin", 2011, 25_340),
        member("2012-koala", "Koala Silver Bullion Coin", 2012, 32_572),
    )

    private val unlisted = CollectionCatalogMember(
        id = "2023-rabbit",
        label = "Year of the Rabbit",
        year = 2023,
        status = MemberStatus.Unlisted,
        source = "https://www.perthmint.com/year-of-the-rabbit/",
        sourceNote = "Acuñada y vendida; Numista no tiene una ficha publicada.",
    )

    private val announced = CollectionCatalogMember(
        id = "2027-goat",
        label = "Year of the Goat",
        status = MemberStatus.Announced,
        source = "https://www.perthmint.com/lunar-series-iii/",
        sourceNote = "La ceca anunció el diseño, pero aún no lo ha emitido.",
    )

    private fun catalog(
        members: List<CollectionCatalogMember>,
        finish: Finish? = null,
    ) = CollectionCatalog(
        schemaVersion = 2,
        id = "venezuela-fuertes",
        name = "Fuertes · Venezuela",
        shortName = "Fuertes",
        issuerCode = "venezuela",
        family = "Fuertes de Venezuela",
        weightMillioz = 804,
        finish = finish,
        seriesStatus = SeriesStatus.Closed,
        closedNote = "La plata venezolana se acabó en 1965.",
        source = "https://en.numista.com/catalogue/pieces10340.html",
        updatedAt = "2026-08-01",
        members = members,
    )

    /** One coin of a date run, which is matched by its type and the year it was recorded with. */
    private fun coin(id: Long, typeId: Int, year: Int) =
        CollectedItem(id = id, quantity = 1, typeId = typeId, issueYear = year)

    /** The plate the three drawers get, built the way production builds it. */
    private fun subject(
        members: List<CollectionCatalogMember>,
        owned: List<CollectedItem> = emptyList(),
        programmes: List<ProgrammeStanding> = emptyList(),
        finish: Finish? = null,
    ): PlateSubject {
        val catalog = catalog(members, finish)
        return plateSubject(
            PlateResult.Available(
                catalog = catalog,
                album = buildCollectionCatalogAlbum(catalog, owned),
                programmes = programmes,
            ),
        )
    }

    /**
     * The heading of the plate, the file the sheet is exported as and the face the notebook prints
     * come off the catalog once, so no drawer has to reach past what it was handed.
     */
    @Test
    fun `a plate is handed its own heading and never a catalog to read it off`() {
        val plate = subject(dateRun)

        assertEquals("venezuela-fuertes", plate.catalogId)
        assertEquals("Fuertes · Venezuela", plate.title)
        assertEquals("https://en.numista.com/catalogue/pieces10340.html", plate.source)
        assertEquals(PrintedSide.Reverse, plate.printedSide)
    }

    /**
     * The divisor of «Progreso» is the album's and only the album's (#218).
     *
     * It used to be counted a second time off the catalog's own member flags, which agreed with the
     * album by accident: `Owned ∪ Missing` are exactly the members that are neither announced nor
     * unlisted. The album here disagrees on purpose — the 1886 is a casilla the catalog calls issued
     * and the inventory cannot measure — and the plate follows the album, because the card's ratio
     * did, and the same collection cannot count one way on the card and another one tap later.
     */
    @Test
    fun `the plate divides by the album, which is what the card divided by`() {
        val catalog = catalog(dateRun)
        val album = CollectionCatalogAlbum(
            listOf(
                CollectionCatalogAlbumMember(
                    dateRun[0],
                    CollectionCatalogMemberStatus.Owned(
                        quantity = 1,
                        items = listOf(ItemRef(itemId = 1, typeId = 10_340, quantity = 1)),
                    ),
                ),
                CollectionCatalogAlbumMember(dateRun[1], CollectionCatalogMemberStatus.Unlisted),
            ),
        )

        val plate = plateSubject(PlateResult.Available(catalog, album))

        assertEquals(1, album.issuedMembers())
        assertEquals("Progreso" to "1 / 1 emisiones", plate.entries[0])
        assertEquals("" to "1 emisión no medible", plate.entries[1])
    }

    @Test
    fun `a date run says its type once and never repeats the year it is titled with`() {
        val plate = subject(dateRun)

        assertEquals("Tipo" to "Numista 10340", plate.entries.single { it.first == "Tipo" })
        assertEquals(emptyList(), plate.entries.filter { it.first == "Año" })
        assertNull(plate.cells[0].footnote)
    }

    @Test
    fun `an issue run shares its year too, so the cell keeps only its label`() {
        val plate = subject(issueRun)

        assertEquals("Tipo" to "Numista 1885", plate.entries.single { it.first == "Tipo" })
        assertEquals("Año" to "1966", plate.entries.single { it.first == "Año" })
        assertNull(plate.cells[0].footnote)
    }

    @Test
    fun `a catalog of distinct types keeps the year in every cell, never the type`() {
        val plate = subject(typeRun)

        assertEquals(emptyList(), plate.entries.filter { it.first == "Tipo" })
        assertEquals(emptyList(), plate.entries.filter { it.first == "Año" })
        assertEquals(listOf("2011", "2012"), plate.cells.map { it.footnote })
    }

    /**
     * A date run that outgrew its single type — the fuertes gained the 1876 Venezuelan, N#48672,
     * next to twenty-one cells of N#10340 — used to hand every one of the twenty-two cells a
     * footnote, twenty-one of them repeating the same identifier (issue #88). A type identifier is
     * not what a plate says under a coin: on screen the cell title already links to its Numista
     * page, and the exported sheet is a picture, not a database dump.
     */
    @Test
    fun `a run of two types puts no identifier under any of its cells`() {
        val plate = subject(dateRun + member("1876", "1876", 1876, 48_672))

        assertEquals(listOf(null, null, null), plate.cells.map { it.footnote })
        // And it does not reappear in the heading either: two types are not one type.
        assertEquals(emptyList(), plate.entries.filter { it.first == "Tipo" })
    }

    /**
     * The 121 cells of the Russian personalities said 121 different identifiers, and not one of
     * them was an exception to a norm: there was no norm to be the exception to. Real members —
     * of `outstanding-personalities-russia-2-roubles-plata-500` since #159 split the series by
     * fineness — three years apart so the year stays theirs.
     */
    @Test
    fun `a catalog where every cell is its own type says no identifier either`() {
        val plate = subject(
            listOf(
                member("1994-i-a-krylov", "I.A. Krylov", 1994, 28_934),
                member("1995-s-a-yesenin", "S.A. Yesenin", 1995, 28_930),
                member("1996-f-m-dostoyevsky", "F.M. Dostoyevsky", 1996, 70_074),
            ),
        )

        assertEquals(listOf("1994", "1995", "1996"), plate.cells.map { it.footnote })
    }

    @Test
    fun `what every cell shares moves into the specification of the plate`() {
        val plate = subject(dateRun, owned = listOf(coin(1, 10_340, 1879)))

        assertEquals(
            listOf(
                "Progreso" to "1 / 2 emisiones",
                "Peso" to "0,804 oz",
                "Tipo" to "Numista 10340",
                "Catálogo" to "1 ago 2026",
            ),
            plate.entries,
        )
    }

    /**
     * Y un acabado declarado sí es una fila (#409). La especificación de los fuertes no la tiene
     * porque los fuertes no tienen acabado que nombrar; la de una lámina proof la tiene entera, que es
     * lo único que se pretendía conservar al callar el hueco.
     */
    @Test
    fun `a declared finish keeps its row in the specification`() {
        val plate = subject(dateRun, owned = listOf(coin(1, 10_340, 1879)), finish = Finish.Proof)

        assertEquals(
            listOf(
                "Progreso" to "1 / 2 emisiones",
                "Peso" to "0,804 oz",
                "Acabado" to "Proof",
                "Tipo" to "Numista 10340",
                "Catálogo" to "1 ago 2026",
            ),
            plate.entries,
        )
    }

    /**
     * El programa conmemorativo entra en la especificación **después** del progreso de la lámina
     * y sin mezclarse con él (ADR 0022): «1 / 2 emisiones» cuenta lo que el catálogo sostiene, y
     * «1 de 3» cuenta el programa entero, cuya tercera moneda no está en ningún catálogo.
     */
    @Test
    fun `a programme is a second line and never touches the plate progress`() {
        val standing = ProgrammeStanding(
            programme = CommemorativeProgramme(
                schemaVersion = 1,
                id = "portugal-1977-alexandre-herculano",
                name = "Serie Alexandre Herculano 1977 · Portugal",
                shortName = "Serie Alexandre Herculano 1977",
                issuerCode = "portugal",
                year = 1977,
                source = "https://example.org/serie-1977",
                sourceNote = "Carteira de tres monedas.",
                updatedAt = "2026-08-04",
                members = listOf(
                    CommemorativeProgrammeMember("2,50 escudos", 6_071),
                    CommemorativeProgrammeMember("5 escudos", 10_126),
                    CommemorativeProgrammeMember("25 escudos", 7_338),
                ),
            ),
            progress = ProgrammeProgress(owned = 1, total = 3),
        )

        val plate = subject(
            dateRun,
            owned = listOf(coin(1, 10_340, 1879)),
            programmes = listOf(standing),
        )

        assertEquals(
            listOf(
                "Progreso" to "1 / 2 emisiones",
                "Programa" to "Serie Alexandre Herculano 1977 · 1 de 3",
                "Peso" to "0,804 oz",
                "Tipo" to "Numista 10340",
                "Catálogo" to "1 ago 2026",
            ),
            plate.entries,
        )
    }

    @Test
    fun `a plate whose cells differ in everything adds nothing to its specification`() {
        val plate = subject(
            typeRun,
            owned = listOf(coin(1, 25_340, 2011), coin(2, 32_572, 2012)),
        )

        assertEquals(
            listOf(
                "Progreso" to "2 / 2 emisiones",
                "Peso" to "0,804 oz",
                "Catálogo" to "1 ago 2026",
            ),
            plate.entries,
        )
    }

    @Test
    fun `the shared year of an issue run is a fact about the plate`() {
        val plate = subject(issueRun)

        assertEquals("Año" to "1966", plate.entries[plate.entries.size - 2])
    }

    @Test
    fun `an unlisted year prevents a different year becoming common`() {
        val plate = subject(issueRun + unlisted)

        assertEquals("Tipo" to "Numista 1885", plate.entries.single { it.first == "Tipo" })
        assertEquals(emptyList(), plate.entries.filter { it.first == "Año" })
        assertEquals("2023", plate.cells.last().footnote)
    }

    @Test
    fun `unlisted emissions stay outside progress and are explained in prose`() {
        val plate = subject(
            dateRun + unlisted + announced,
            owned = listOf(coin(1, 10_340, 1879)),
        )

        assertEquals("Progreso" to "1 / 2 emisiones", plate.entries[0])
        assertEquals("" to "1 anunciada", plate.entries[1])
        assertEquals("" to "1 emisión no medible", plate.entries[2])
    }

    /**
     * The two drawers that head the plate with the figure itself take the row out from under it.
     *
     * The printed notebook does not, which is why this is a function and not a shape of `entries`:
     * a page of the cuaderno has no header to raise a ratio into, so «Progreso · 1 / 2 emisiones»
     * is the only place it says it.
     */
    @Test
    fun `the ratio is printed once, and never twice on the same surface`() {
        val plate = subject(dateRun + unlisted, owned = listOf(coin(1, 10_340, 1879)))

        assertEquals("Progreso" to "1 / 2 emisiones", plate.entries[0])
        assertEquals("1/2", plate.ratio)
        val beside = plateEntriesBesideRatio(plate.entries)
        assertEquals(emptyList(), beside.filter { it.first == "Progreso" })
        // What the progress brought with it stays: it is not the ratio, and the figure over the
        // title deliberately says nothing about an emission the app cannot measure.
        assertEquals("" to "1 emisión no medible", beside[0])
    }

    /**
     * The stamp is read from the inventory like the die-cut (ADR 0026 §3), so it is the album's
     * `owned == issued` and nothing else — no date, no flag, nothing remembered.
     */
    @Test
    fun `a plate with every issued member owned says it is complete`() {
        val complete = subject(dateRun, owned = listOf(coin(1, 10_340, 1879), coin(2, 10_340, 1886)))

        assertEquals("2/2", complete.ratio)
        assertTrue(complete.complete)
    }

    /**
     * Completing expires: `issuedMembers` leaves announced members out of the divisor, so the day
     * the curator turns an announced year into a real casilla the same catalog reads 2 of 3 and
     * the stamp is simply not drawn. 33 of the 74 catalogs are open series.
     */
    @Test
    fun `a date run that grows loses the stamp without drama`() {
        val owned = listOf(coin(1, 10_340, 1879), coin(2, 10_340, 1886))
        val grown = subject(dateRun + member("1887", "1887", 1887, 10_340), owned = owned)

        assertEquals("2/3", grown.ratio)
        assertFalse(grown.complete)
    }

    /** A catalog with nothing measurable divides by nothing, so it heads itself with no figure. */
    @Test
    fun `a plate with no measurable emission offers no ratio and no stamp`() {
        val plate = subject(listOf(announced))

        assertNull(plate.ratio)
        assertFalse(plate.complete)
    }

    /**
     * Where the coin of the index card lands, which is the first casilla this collector **owns**
     * and not the first of the catalog (#304).
     *
     * It is the same rule `CollectionIndex.firstOwnedCover` picks the card's photograph by, and it
     * has to be: the card of the 1 Bolívar shows the 1945 he has, and flying it to the 1879 would
     * land a coin in full colour on a hole where it is a ghost.
     */
    @Test
    fun `the coin lands on the first casilla the collector owns`() {
        val plate = subject(dateRun, owned = listOf(coin(1, 10_340, 1886)))

        assertEquals(1, plate.landingCell)
    }

    /** A complete sheet lands on its own first casilla, so the ceremony falls where the eye is. */
    @Test
    fun `a complete plate lands on the top of the sheet`() {
        val plate = subject(dateRun, owned = listOf(coin(1, 10_340, 1879), coin(2, 10_340, 1886)))

        assertEquals(0, plate.landingCell)
    }

    /**
     * The footnote goes silent when the plate already said the year; the tag does not, because it
     * is not a note but the handle that opens Numista (#337). Twelve casillas of four shipped
     * catalogs live in this gap, and without the year they would have nothing to press.
     */
    @Test
    fun `the tag keeps the year the footnote drops`() {
        val plate = subject(issueRun)

        assertNull(plate.cells[0].footnote)
        assertEquals(listOf("1966", "1966"), plate.cells.map { it.year })
    }

    @Test
    fun `a date run titles its cells with the very year its tag carries`() {
        val plate = subject(dateRun)

        assertEquals(plate.cells.map { it.label }, plate.cells.map { it.year })
    }

    @Test
    fun `an announced casilla has no year to press`() {
        val plate = subject(dateRun + announced)

        assertNull(plate.cells.last().year)
        assertNull(plate.cells.last().numistaTypeId)
    }

    @Test
    fun `only a Missing member is drawn as a ghost`() {
        val members = dateRun + unlisted + announced
        val catalog = catalog(members)
        val album = CollectionCatalogAlbum(
            listOf(
                CollectionCatalogAlbumMember(
                    members[0],
                    CollectionCatalogMemberStatus.Owned(
                        quantity = 2,
                        items = listOf(ItemRef(itemId = 1, typeId = 10_340, quantity = 2)),
                    ),
                ),
                CollectionCatalogAlbumMember(members[1], CollectionCatalogMemberStatus.Missing),
                CollectionCatalogAlbumMember(members[2], CollectionCatalogMemberStatus.Unlisted),
                CollectionCatalogAlbumMember(members[3], CollectionCatalogMemberStatus.NotYetIssued),
            ),
        )

        val plate = plateSubject(PlateResult.Available(catalog, album))

        assertEquals(listOf(true, false, false, false), plate.cells.map { it.owned })
        assertEquals(listOf(false, true, false, false), plate.cells.map { it.missing })
        // What identifies a cell to a drawer: its key, its title and the type behind it.
        assertEquals(listOf("1879", "1886", "2023-rabbit", "2027-goat"), plate.cells.map { it.id })
        assertEquals(
            listOf(10_340, 10_340, null, null),
            plate.cells.map { it.numistaTypeId },
        )
    }

    /**
     * The two figures of money reach a drawer already worded, and the stamp of a hole with them (#493).
     *
     * A stamp lands on the hole and never on a full casilla: a casilla that is filled has no cost, it
     * has a value, and that one is the header's.
     */
    @Test
    fun `a plate is handed both figures of money and the price inside each hole`() {
        val plate = pricedSubject(
            PlateMoney(
                value = PlateValue(eur = 1_612.0, pieces = 2),
                cost = PlateCost(eur = 84.0, holes = 1),
                holeCosts = mapOf("1886" to 84.0),
            ),
        )

        assertEquals("Valor actual: 1.612 € · al mayor de tres precios", plate.value)
        assertEquals("Coste de cerrar: 84 € · en sin circular", plate.cost)
        assertEquals(listOf(null, "84 €"), plate.cells.map { it.cost })
    }

    /**
     * And with no money to say, no drawer can print any of it — which is the export with the switch
     * off, the plate whose market has not landed, and every test in this file that says nothing about
     * money at all.
     */
    @Test
    fun `a plate handed no money says nothing about money anywhere`() {
        val plate = pricedSubject(PlateMoney())

        assertNull(plate.value)
        assertNull(plate.cost)
        assertFalse(plate.moneyWaiting)
        assertEquals(listOf(null, null), plate.cells.map { it.cost })
    }

    /**
     * And when the reason is the market, the header says so instead of the two figures (#519).
     *
     * The line stands **in the figures' slot**, so it can never be true beside one of them: what a
     * plate says of its money is either the amounts or that they have not arrived.
     */
    @Test
    fun `a plate whose market has not landed says it, and says no amount`() {
        val plate = pricedSubject(PlateMoney(waiting = true))

        assertTrue(plate.moneyWaiting)
        assertNull(plate.value)
        assertNull(plate.cost)
        assertEquals(listOf(null, null), plate.cells.map { it.cost })
    }

    /** The two-casilla date run with the first one filled, which is the plate a cost is said of. */
    private fun pricedSubject(money: PlateMoney): PlateSubject {
        val catalog = catalog(dateRun)
        val album = CollectionCatalogAlbum(
            listOf(
                CollectionCatalogAlbumMember(
                    dateRun[0],
                    CollectionCatalogMemberStatus.Owned(
                        quantity = 2,
                        items = listOf(ItemRef(itemId = 1, typeId = 10_340, quantity = 2)),
                    ),
                ),
                CollectionCatalogAlbumMember(dateRun[1], CollectionCatalogMemberStatus.Missing),
            ),
        )
        return plateSubject(PlateResult.Available(catalog, album), money)
    }
}
