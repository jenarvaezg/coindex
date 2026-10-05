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

/** A day of August 2026, so the ages a header prints are the same on every run. */
private const val NOW = 1_786_442_400_000L
private const val DAY = 24L * 60 * 60 * 1_000

/** Seven weeks back: past a month, the age gives way to the date. */
private const val VALUED = NOW - 48 * DAY

/**
 * The plate as the screen, the exported sheet and the notebook page receive it, worded once in a
 * [PlateSubject] (#218). A cell keeps only what tells it apart; what every member shares goes to
 * the heading, and the Numista type is never in a cell (#88).
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

    /** A date-run coin, matched by its type and recorded year. */
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

    @Test
    fun `a plate is handed its own heading and never a catalog to read it off`() {
        val plate = subject(dateRun)

        assertEquals("venezuela-fuertes", plate.catalogId)
        assertEquals("Fuertes · Venezuela", plate.title)
        assertEquals("https://en.numista.com/catalogue/pieces10340.html", plate.source)
        assertEquals(PrintedSide.Reverse, plate.printedSide)
    }

    /**
     * «Progreso» divides by the album alone, as the card does (#218). The album here disagrees with
     * the catalog's flags on purpose: the 1886 is issued but cannot be measured.
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

    /** The cell title already links to Numista, and the sheet is a picture, not a dump (#88). */
    @Test
    fun `a run of two types puts no identifier under any of its cells`() {
        val plate = subject(dateRun + member("1876", "1876", 1876, 48_672))

        assertEquals(listOf(null, null, null), plate.cells.map { it.footnote })
        // Nor in the heading: there is no single type to name.
        assertEquals(emptyList(), plate.entries.filter { it.first == "Tipo" })
    }

    /**
     * Real members of `outstanding-personalities-russia-2-roubles-plata-500` (#159), in different
     * years so each cell keeps its own.
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
                "Variante" to "0,804 oz",
                "Tipo" to "Numista 10340",
                "Catálogo" to "1 ago 2026",
            ),
            plate.entries,
        )
    }

    /** Los fuertes no tienen acabado que nombrar; una lámina proof sí (#409). */
    @Test
    fun `a declared finish keeps its row in the specification`() {
        val plate = subject(dateRun, owned = listOf(coin(1, 10_340, 1879)), finish = Finish.Proof)

        assertEquals(
            listOf(
                "Progreso" to "1 / 2 emisiones",
                "Variante" to "0,804 oz",
                "Acabado" to "Proof",
                "Tipo" to "Numista 10340",
                "Catálogo" to "1 ago 2026",
            ),
            plate.entries,
        )
    }

    /**
     * «1 / 2 emisiones» cuenta lo que sostiene el catálogo; «1 de 3», el programa entero, cuya
     * tercera moneda no está en ningún catálogo (ADR 0022).
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
                "Variante" to "0,804 oz",
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
                "Variante" to "0,804 oz",
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
     * Screen and sheet raise the ratio into the header and drop the row; the notebook page has no
     * header, so `entries` keeps it.
     */
    @Test
    fun `the ratio is printed once, and never twice on the same surface`() {
        val plate = subject(dateRun + unlisted, owned = listOf(coin(1, 10_340, 1879)))

        assertEquals("Progreso" to "1 / 2 emisiones", plate.entries[0])
        assertEquals("1/2", plate.ratio)
        val beside = plateEntriesBesideRatio(plate.entries)
        assertEquals(emptyList(), beside.filter { it.first == "Progreso" })
        // The unmeasurable note stays: the ratio over the title doesn't mention it.
        assertEquals("" to "1 emisión no medible", beside[0])
    }

    /**
     * Read from the inventory like the die-cut (ADR 0026 §3): `owned == issued`, nothing stored.
     */
    @Test
    fun `a plate with every issued member owned says it is complete`() {
        val complete = subject(dateRun, owned = listOf(coin(1, 10_340, 1879), coin(2, 10_340, 1886)))

        assertEquals("2/2", complete.ratio)
        assertTrue(complete.complete)
    }

    /** Completion expires: a new casilla in an open series just stops the stamp being drawn. */
    @Test
    fun `a date run that grows loses the stamp without drama`() {
        val owned = listOf(coin(1, 10_340, 1879), coin(2, 10_340, 1886))
        val grown = subject(dateRun + member("1887", "1887", 1887, 10_340), owned = owned)

        assertEquals("2/3", grown.ratio)
        assertFalse(grown.complete)
    }

    @Test
    fun `a plate with no measurable emission offers no ratio and no stamp`() {
        val plate = subject(listOf(announced))

        assertNull(plate.ratio)
        assertFalse(plate.complete)
    }

    /**
     * The rule `CollectionIndex.firstOwnedCover` picks the card's photo by; otherwise the flying
     * coin would land in colour on a ghost (#304).
     */
    @Test
    fun `the coin lands on the first casilla the collector owns`() {
        val plate = subject(dateRun, owned = listOf(coin(1, 10_340, 1886)))

        assertEquals(1, plate.landingCell)
    }

    @Test
    fun `a complete plate lands on the top of the sheet`() {
        val plate = subject(dateRun, owned = listOf(coin(1, 10_340, 1879), coin(2, 10_340, 1886)))

        assertEquals(0, plate.landingCell)
    }

    /** The tag is the handle that opens Numista, so it keeps the year (#337). */
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
        // What identifies a cell to a drawer: its key and its type.
        assertEquals(listOf("1879", "1886", "2023-rabbit", "2027-goat"), plate.cells.map { it.id })
        assertEquals(
            listOf(10_340, 10_340, null, null),
            plate.cells.map { it.numistaTypeId },
        )
    }

    /** A filled casilla has a value, not a cost, so only holes get a stamp (#493). */
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
     * Each figure dates its own reads (#594). The holes' stamps don't repeat the date, which
     * «Coste de cerrar» already says (ADR 0026 §5).
     */
    @Test
    fun `each figure of the header is handed the age of its own price`() {
        val plate = pricedSubject(
            PlateMoney(
                value = PlateValue(eur = 1_612.0, pieces = 2, catalogReadAt = VALUED),
                cost = PlateCost(eur = 84.0, holes = 1, catalogReadAt = NOW - 3 * DAY),
                holeCosts = mapOf("1886" to 84.0),
            ),
            nowMillis = NOW,
        )

        // The absolute form is pinned in `FiguresLabelsTest`; here each line gets its own read.
        assertEquals(
            "Valor actual: 1.612 € · al mayor de tres precios · Numista: ${priceAgeLabel(VALUED, NOW)}",
            plate.value,
        )
        assertEquals("Coste de cerrar: 84 € · en sin circular · Numista: hace 3 días", plate.cost)
        assertEquals(listOf(null, "84 €"), plate.cells.map { it.cost })
    }

    @Test
    fun `a plate handed no money says nothing about money anywhere`() {
        val plate = pricedSubject(PlateMoney())

        assertNull(plate.value)
        assertNull(plate.cost)
        assertFalse(plate.moneyWaiting)
        assertEquals(listOf(null, null), plate.cells.map { it.cost })
    }

    /** The line takes the figures' slot, so it never sits beside an amount (#519). */
    @Test
    fun `a plate whose market has not landed says it, and says no amount`() {
        val plate = pricedSubject(PlateMoney(waiting = true))

        assertTrue(plate.moneyWaiting)
        assertNull(plate.value)
        assertNull(plate.cost)
        assertEquals(listOf(null, null), plate.cells.map { it.cost })
    }

    /** Con un solo año, lo que distingue las casillas es el nombre (#511). */
    @Test
    fun `una lamina de un solo ano pone en la chapa lo que distingue`() {
        val plate = subject(issueRun)

        assertEquals(
            listOf(CellPlaque.Name("Estrella 66"), CellPlaque.Name("Estrella 67")),
            plate.cells.map { it.plaque },
        )
        // Lo que sube a la chapa deja de escribirse al pie.
        assertEquals(listOf(null, null), plate.cells.map { it.printedName })
        // El año se dice una vez, en la especificación.
        assertTrue(plate.entries.contains("Año" to "1966"))
    }

    @Test
    fun `una date run mantiene el ano en la chapa`() {
        val plate = subject(dateRun)

        assertEquals(
            listOf(CellPlaque.Year("1879"), CellPlaque.Year("1886")),
            plate.cells.map { it.plaque },
        )
        assertEquals(listOf(null, null), plate.cells.map { it.printedName })
    }

    /** Si el título ya es el año, no hay otro nombre que subir a la chapa. */
    @Test
    fun `una casilla titulada con su ano no cambia de chapa`() {
        assertEquals(CellPlaque.Year("1966"), plaqueOf("1966", "1966", yearIsCommon = true))
        assertEquals(CellPlaque.Name("Estrella 66"), plaqueOf("Estrella 66", "1966", yearIsCommon = true))
        assertEquals(CellPlaque.Year("1966"), plaqueOf("Estrella 66", "1966", yearIsCommon = false))
    }

    /** Una anunciada no tiene año. */
    @Test
    fun `una casilla sin ano no tiene chapa`() {
        assertNull(plaqueOf("Year of the Goat", null, yearIsCommon = false))
    }

    /** El nombre que va a la chapa llega soldado, como el que va al pie (#511). */
    @Test
    fun `la chapa suelda la cifra con su unidad`() {
        val members = listOf(
            member("25-bolivares", "25 bolívares · jaguar · 28,28 g", 1975, 37_246),
            member("50-bolivares", "50 bolívares · cachicamo gigante · 35 g", 1975, 37_247),
        )

        assertEquals(
            listOf(
                CellPlaque.Name("25 bolívares · jaguar · 28,28 g"),
                CellPlaque.Name("50 bolívares · cachicamo gigante · 35 g"),
            ),
            subject(members).cells.map { it.plaque },
        )
    }

    /** The two-casilla date run with the first one filled. */
    private fun pricedSubject(
        money: PlateMoney,
        nowMillis: Long = System.currentTimeMillis(),
    ): PlateSubject {
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
        return plateSubject(PlateResult.Available(catalog, album), money, nowMillis = nowMillis)
    }
}
