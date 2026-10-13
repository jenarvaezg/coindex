package com.jenarvaezg.coindex.ui.ficha

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.TypeCacheFile
import com.jenarvaezg.coindex.data.ficha.FichaBodies
import com.jenarvaezg.coindex.data.ficha.PieceReading
import com.jenarvaezg.coindex.data.ficha.readFichaDetails
import com.jenarvaezg.coindex.data.prices.PriceBook
import com.jenarvaezg.coindex.data.prices.PriceKey
import com.jenarvaezg.coindex.domain.AssembledCollection
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.domain.SilverSpot
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.gramsToOunces
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.DrawnCell
import com.jenarvaezg.coindex.ui.PlateSubject
import com.jenarvaezg.coindex.ui.shelf.CoinClaim
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * What the coin's screen says, as a value (#643). The pieces are invented in the shape of the
 * father's: his rows and amounts are private.
 */
class CoinFichaSubjectTest {
    @Test
    fun `the fuertes opened from the casilla of 1929 enter on their pieces, 1929 chosen`() {
        val subject = fuertes(request(casilla = CasillaOrigin(year = 1929, dated = true)))

        val pieces = subject.section<PiecesSection>()
        assertTrue(pieces.open, "abierta desde una casilla de un date run, entra abierta")
        assertEquals("1929", pieces.selected)
        assertEquals(listOf("1879", "1886", "1929", "1935", "1936"), pieces.dates.map { it.label })
        assertTrue(pieces.dates.single { it.key == "1929" }.casilla)
        assertEquals("×2", pieces.dates.single { it.key == "1929" }.quantity)
        assertTrue(pieces.pieces.single { it.dateKey == "1929" }.here)

        // Every other section enters closed.
        assertTrue(subject.sections.filterNot { it is PiecesSection }.none { it.open })
    }

    @Test
    fun `opened from monedas, every section is closed and the first date is chosen`() {
        val subject = fuertes(request())

        assertTrue(subject.sections.none { it.open })
        assertEquals("1879", subject.section<PiecesSection>().selected)
    }

    @Test
    fun `many rows are summed up by date, and the title counts pieces`() {
        val pieces = fuertes(request()).section<PiecesSection>()

        assertEquals("Tus piezas · 6", pieces.title)
        assertEquals("6 piezas en 5 fechas · 1879 – 1936", pieces.summary)
    }

    @Test
    fun `three rows or fewer are listed whole, and summed up one by one`() {
        val subject = fuertes(request(), items = FUERTES.take(2))

        val pieces = subject.section<PiecesSection>()
        assertEquals(emptyList(), pieces.dates)
        assertEquals("1879 A vf · 1886 f", pieces.summary)
        assertEquals("Tus piezas · 2", pieces.title)
        val one = fuertes(request(), items = FUERTES.take(1))
        assertEquals("Tu pieza", one.section<PiecesSection>().title)
    }

    @Test
    fun `each piece prints its three prices, marks the greatest and says why a rung is empty`() {
        val pieces = fuertes(request()).section<PiecesSection>().pieces

        val bought = pieces.single { it.itemId == 1L }
        assertEquals(
            listOf("Catálogo de Numista", "Lo que pagaste", "Lo que vale su plata"),
            bought.rungs.map { it.label },
        )
        assertEquals(FichaPageLabels.NUMISTA_HAS_NO_PRICE, bought.rungs[0].why)
        assertEquals("250 €", bought.rungs[1].amount)
        assertTrue(bought.rungs[1].greatest)
        assertEquals("22,5 g finos · plata: 54,39 €/oz · hoy 11:52", bought.rungs[2].source)
        assertEquals(1, bought.rungs.count { it.greatest })

        val priced = pieces.single { it.itemId == 3L }
        assertEquals("Catálogo de Numista en xf", priced.rungs[0].label)
        assertEquals("60 €", priced.rungs[0].amount)
        assertEquals("Numista: hoy", priced.rungs[0].source)
        assertTrue(priced.rungs[0].greatest)
        assertEquals(FichaPageLabels.NOT_PAID, priced.rungs[1].why)

        // Never asked is not «no price».
        val unasked = pieces.single { it.itemId == 2L }
        assertEquals(FichaPageLabels.NUMISTA_NOT_ASKED, unasked.rungs[0].why)
    }

    @Test
    fun `a piece says when it was bought, its mint letter, its mintage and its notes`() {
        val bought = fuertes(request()).section<PiecesSection>().pieces.single { it.itemId == 1L }

        assertEquals("1879 · A", bought.heading)
        assertEquals("vf", bought.grade)
        assertEquals("pagaste 250 € el 3 ago 2026 · tirada de 117.000 · para cambio", bought.facts)
        assertEquals("Numista: con punto", bought.issueComment)
        assertEquals("«De la feria»", bought.note)
    }

    /** A lot carries one price for the row; the rung compares one piece (ADR 0026 §10). */
    @Test
    fun `a row of several pieces says what was paid for all of them, and the rung for one`() {
        val lot =
            fuerte(id = 9, year = 1929, issueId = 3, grade = "xf", price = 100.0, quantity = 2)

        val piece = fuertes(request(), items = listOf(lot)).section<PiecesSection>().pieces.single()

        assertEquals("pagaste 100 € por las 2", piece.facts)
        assertEquals("50 €", piece.rungs.single { it.label == FichaPageLabels.PAID }.amount)
    }

    /** A ficha with no composition says nothing about silver, so the rung can't say «no lleva». */
    @Test
    fun `an unknown metal is not called silverless`() {
        val unknown = coinFichaSubject(
            state = state(FUERTE.copy(metal = null, fineness = null), FUERTES.take(1)),
            request = request(),
            bodies = bodies(10_340),
            here = null,
            origin = null,
            book = PriceBook(spot = SPOT),
            settled = true,
            nowMillis = NOW,
        )

        val silver = unknown.section<PiecesSection>().pieces.single().rungs.last()
        assertEquals(FichaPageLabels.NO_FINE_SILVER, silver.why)
    }

    /** ADR 0028 §7: the figure is the sum of the greatest, and no source is totalled. */
    @Test
    fun `the coin's figure is the sum of the greatest, and pieces that disagree say so`() {
        val subject = fuertes(request())

        val value = assertNotNull(subject.value)
        assertTrue(value.startsWith("Valor actual: "), value)
        assertTrue(value.endsWith(" · al mayor de tres precios"), value)
        assertTrue(subject.sections.none { "pagaste" in it.summary || "Numista" in it.summary })
    }

    @Test
    fun `before the market lands there is no figure and no rung`() {
        val subject = fuertes(request(), settled = false)

        assertNull(subject.value)
        assertTrue(subject.section<PiecesSection>().pieces.all { it.rungs.isEmpty() })
    }

    @Test
    fun `a hole says what it lacks and what it would cost, in its two rungs`() {
        val subject = coinFichaSubject(
            state = state(UNICORN, items = emptyList()),
            request = CoinFichaRequest(
                typeId = 127_263,
                side = PrintedSide.Reverse,
                casilla = CasillaOrigin(year = 2018, issueId = 555, missing = true),
            ),
            bodies = bodies(127_263),
            here = null,
            origin = null,
            book = PriceBook(
                prices = mapOf(PriceKey(127_263, 555, "unc") to 156.0),
                spot = SPOT,
                readAt = mapOf((127_263 to 555) to NOW),
            ),
            settled = true,
            nowMillis = NOW,
        )

        assertTrue(subject.missing)
        assertFalse(subject.owned)
        assertEquals("Te costaría: 156 € · en sin circular", subject.cost)
        assertNull(subject.value)
        assertNull(subject.sectionOrNull<PiecesSection>())
        val cost = subject.section<CostSection>()
        assertEquals(
            listOf("Catálogo de Numista en unc", "Lo que vale su plata"),
            cost.rungs.map { it.label },
        )
        assertTrue(cost.rungs[0].greatest)
    }

    /**
     * No collection claims a hole by its pieces (#635), but the lámina it was opened from holds its
     * casilla: the screen names it and draws it in miniature, the casilla marked (#643).
     */
    @Test
    fun `a hole opened from its lamina names it and draws it with the casilla marked`() {
        val beasts = CardDestination.Plate("queens-beasts")
        val subject = coinFichaSubject(
            state = state(UNICORN, items = emptyList()),
            request = CoinFichaRequest(
                typeId = 127_263,
                casilla = CasillaOrigin(year = 2018, missing = true),
            ),
            bodies = bodies(127_263),
            here = beasts,
            origin = OriginPlate(name = "The Queen's Beasts", plate = queensBeasts()),
            book = PriceBook(),
            settled = true,
            nowMillis = NOW,
        )

        val claims = subject.section<ClaimsSection>()
        assertEquals("En esta colección", claims.title)
        assertEquals(
            listOf(FichaClaim(CoinClaim("The Queen's Beasts", beasts), here = true)),
            claims.claims,
        )
        val strip = assertNotNull(claims.strip)
        assertEquals(listOf(false, true), strip.cells.map { it.here })
        assertEquals("1/2", strip.count)
        assertEquals("The Queen's Beasts · 1/2", claims.summary)
    }

    @Test
    fun `a hole opened from monedas or the wish list names no collection`() {
        val subject = coinFichaSubject(
            state = state(UNICORN, items = emptyList()),
            request = CoinFichaRequest(
                typeId = 127_263,
                casilla = CasillaOrigin(year = 2018, missing = true),
            ),
            bodies = bodies(127_263),
            here = null,
            origin = null,
            book = PriceBook(),
            settled = true,
            nowMillis = NOW,
        )

        assertNull(subject.sectionOrNull<ClaimsSection>())
    }

    /** The edge is read, not drawn: Numista's thicknesses can't hold a drawing to scale (#644). */
    @Test
    fun `the edge is text among the faces, with its legend and its photograph`() {
        val police = coinFichaSubject(
            state = state(POLICE, items = emptyList()),
            request = CoinFichaRequest(401_375),
            bodies = bodies(401_375),
            here = null,
            origin = null,
            book = PriceBook(),
            settled = true,
            nowMillis = NOW,
        )

        val edge = police.section<FacesSection>().faces.single { it.heading == "Canto" }
        assertEquals("2 ★ ★ ↊ ★ ★ 2 ★ ★ ↊ ★ ★ 2 ★ ★ ↊ ★ ★", edge.lettering)
        assertTrue(edge.description!!.startsWith("Letras sobre lengüeta."), edge.description)
        assertNotNull(edge.photo?.picture)
        assertEquals("2,2 mm", police.section<SpecSection>().rows.toMap()["Grosor"])
    }

    @Test
    fun `the specification prints only the rows the ficha has`() {
        val spec = fuertes(request()).section<SpecSection>()

        assertEquals("La moneda", spec.title)
        assertEquals("Plata 900 · 25 g · Casa de la Moneda de París", spec.summary)
        val rows = spec.rows.toMap()
        assertEquals("5 bolívares", rows["Valor facial"])
        assertEquals("Bolívar (1879-2007)", rows["Moneda"])
        assertEquals("1879 – 1936", rows["Años"])
        assertEquals("22,5 g · 0,72 oz", rows["Plata fina"])
        assertEquals("desmonetizada el 31 dic 2011", rows["Curso legal"])
        assertEquals("de moneda", rows["Orientación"])
        // Never «grosor sin dato»: a row only when the ficha has the datum.
        assertNull(rows["Grosor"])
        assertNull(rows["Tema"])
    }

    @Test
    fun `the faces section carries both legends and summarises the first`() {
        val faces = fuertes(request()).section<FacesSection>()

        assertEquals(listOf("Anverso", "Reverso", "Canto"), faces.faces.map { it.heading })
        assertTrue(faces.summary.startsWith("★ ESTADOS UNIDOS DE VENEZUELA"), faces.summary)
    }

    @Test
    fun `numista's comments are summed up in plain text and kept as html`() {
        val comments = fuertes(request()).section<CommentsSection>()

        assertTrue(comments.summary.startsWith("Y# 24.1, NCV# mv5bs-aa:"), comments.summary)
        assertFalse("<" in comments.summary)
        assertTrue(comments.html.startsWith("<p>"))
    }

    private fun fuertes(
        request: CoinFichaRequest,
        items: List<CollectedItem> = FUERTES,
        settled: Boolean = true,
    ): CoinFichaSubject = coinFichaSubject(
        state = state(FUERTE, items),
        request = request,
        bodies = bodies(10_340, pieces = mapOf(1L to BOUGHT)),
        here = null,
        origin = null,
        book = PriceBook(
            prices = mapOf(PriceKey(10_340, 3, "xf") to 60.0),
            spot = SPOT,
            // Issue 1 was asked and Numista had no price; issue 2 was never asked.
            readAt = mapOf((10_340 to 1) to NOW, (10_340 to 3) to NOW),
        ),
        settled = settled,
        nowMillis = NOW,
    )

    /** Two casillas, the lion owned and the unicorn empty. */
    private fun queensBeasts() = PlateSubject(
        catalogId = "queens-beasts",
        title = "The Queen's Beasts 2 oz",
        source = "https://example.org",
        printedSide = PrintedSide.Reverse,
        entries = emptyList(),
        cells = listOf(
            casilla("lion", 127_000, "2016", missing = false),
            casilla("unicorn", 127_263, "2018", missing = true),
        ),
        ratio = "1/2",
        complete = false,
        landingCell = 0,
    )

    private fun casilla(id: String, typeId: Int, year: String, missing: Boolean) =
        DrawnCell(id, id, typeId, footnote = null, year, owned = !missing, missing = missing)

    private fun request(casilla: CasillaOrigin? = null) =
        CoinFichaRequest(typeId = 10_340, side = PrintedSide.Reverse, casilla = casilla)

    private fun state(meta: TypeMeta, items: List<CollectedItem>) = CollectionState(
        collection = AssembledCollection(items = items, typeMeta = mapOf(meta.id to meta)),
    )

    private fun bodies(typeId: Int, pieces: Map<Long, PieceReading> = emptyMap()) = FichaBodies(
        details = readFichaDetails(seed[typeId.toString()]!!.toString()),
        pieces = pieces,
    )

    private inline fun <reified T : FichaSection> CoinFichaSubject.section(): T =
        assertNotNull(sectionOrNull<T>(), "falta la sección ${T::class.simpleName}")

    private inline fun <reified T : FichaSection> CoinFichaSubject.sectionOrNull(): T? =
        sections.filterIsInstance<T>().singleOrNull()

    private companion object {
        val seed by lazy { Json.parseToJsonElement(TypeCacheFile.read()).jsonObject }

        val ZONE: ZoneId = ZoneId.systemDefault()
        val NOW: Long =
            LocalDate.of(2026, 10, 11).atTime(11, 52).atZone(ZONE).toInstant().toEpochMilli()
        val SPOT = SilverSpot(eurPerTroyOunce = 54.39, readAtMillis = NOW)

        val FUERTE = TypeMeta(
            id = 10_340,
            title = "5 Bolívares",
            issuerCode = "venezuela",
            issuerName = "Venezuela",
            minYear = 1879,
            maxYear = 1936,
            weightOz = gramsToOunces(25.0),
            metal = Metal.Silver,
            category = "coin",
            sizeMillimetres = 37.2,
            fineness = 0.9,
            demonetized = true,
            mints = listOf("Casa de la Moneda de París", "United States Mint of Philadelphia"),
        )

        val UNICORN = TypeMeta(
            id = 127_263,
            title = "5 Pounds - Elizabeth II (Unicorn of Scotland; 2 oz Fine Silver)",
            issuerCode = "royaume-uni",
            weightOz = gramsToOunces(62.42),
            metal = Metal.Silver,
            fineness = 0.9999,
            sizeMillimetres = 38.61,
            thicknessMillimetres = 6.0,
        )

        val POLICE = TypeMeta(
            id = 401_375,
            title = "2 Euros (National Police)",
            issuerCode = "espagne",
            weightOz = gramsToOunces(8.5),
            metal = Metal.Other,
            sizeMillimetres = 25.75,
            thicknessMillimetres = 2.2,
        )

        val FUERTES = listOf(
            fuerte(id = 1, year = 1879, issueId = 1, grade = "vf", price = 250.0, forSwap = true),
            fuerte(id = 2, year = 1886, issueId = 2, grade = "f"),
            fuerte(id = 3, year = 1929, issueId = 3, grade = "xf", quantity = 2),
            fuerte(id = 4, year = 1935, issueId = 4, grade = "xf"),
            fuerte(id = 5, year = 1936, issueId = 5, grade = "au"),
        )

        val BOUGHT = PieceReading(
            mintLetter = "A",
            acquiredOn = LocalDate.of(2026, 8, 3),
            mintage = 117_000,
            issueComment = "con punto",
            privateComment = "De la feria",
        )

        fun fuerte(
            id: Long,
            year: Int,
            issueId: Int,
            grade: String,
            price: Double? = null,
            quantity: Int = 1,
            forSwap: Boolean = false,
        ) = CollectedItem(
            id = id,
            quantity = quantity,
            typeId = 10_340,
            issueYear = year,
            grade = grade,
            price = price,
            forSwap = forSwap,
            issueId = issueId,
        )
    }
}
