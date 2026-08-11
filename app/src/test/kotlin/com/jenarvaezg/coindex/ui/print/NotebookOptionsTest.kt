package com.jenarvaezg.coindex.ui.print

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.Curation
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.OwnGrouping
import com.jenarvaezg.coindex.domain.OwnGroupingView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Los cinco interruptores por los que sale el cuaderno, y la puerta que el #228 abre.
 *
 * La configuración no es un parámetro del pincel: el número de páginas es aritmética hecha antes de
 * dibujar nada, así que lo que se comprueba aquí es que un interruptor se convierte en milímetros y
 * que los milímetros de hoy son los que nadie eligió.
 */
class NotebookOptionsTest {
    @Test
    fun `the notebook of today is what nobody chose`() {
        val untouched = NotebookOptions()

        // Fotos sí, una cara, tamaño real sí, sin compartir página, sin QR y sin la lámina de las
        // sueltas: nadie se encuentra su cuaderno cambiado sin haberlo pedido.
        assertTrue(untouched.photographs)
        assertFalse(untouched.bothFaces)
        assertTrue(untouched.actualSize)
        assertFalse(untouched.sharePage)
        assertFalse(untouched.numistaQr)
        assertFalse(untouched.unclaimed)
    }

    /**
     * The door is open all the way: **every** configuration but the untouched one moves a millimetre.
     *
     * Each of the five became millimetres in its own ticket (#230-#234): «QR de Numista» grows the
     * caption to make room for the code (#234), «ambas caras» widens the cell to hold the second one
     * (#230), «fotos» takes the coin band away altogether (#231), «compartir página» thins the heading
     * and lets a folio hold two plates (#232), and «tamaño real» shrinks every coin and trades the
     * ruler for a number (#233). This test used to have two halves and the last ticket emptied one of
     * them — what is left is the promise #228 made from the start: the defaults are the notebook of
     * today, and everything else is a notebook the collector asked for.
     */
    @Test
    fun `every configuration but the untouched one moves a millimetre`() {
        val everyCombination = allCombinations()

        assertEquals(32, everyCombination.size, "faltan combinaciones de cinco interruptores")
        val moved = everyCombination.filter { printGeometry(it) != PrintGeometry() }
        assertEquals(
            everyCombination.filter { it != NotebookOptions() },
            moved,
            "un interruptor no mueve la geometría, o la mueve sin que nadie lo haya pedido",
        )
    }

    /**
     * The half a geometry check cannot see: two of the five never touch a cell at all.
     *
     * The configuration reaches `notebookSections` too, because what a cell *is* depends on it — both
     * faces gives it an obverse (#230), no photographs gives it neither (#231). The two that are checked
     * here are the two that are **pure geometry**: «compartir página» packs folios and thins a heading,
     * so a coin on a shared page is the same coin (#232); and «tamaño real» is a fraction the page is
     * drawn with, so a cell keeps the **real** diameter it always carried and what shrinks is the circle
     * (#233) — which is exactly what lets a scaled caption still say «33 mm». The three that do change a
     * cell are left out because changing one is precisely what they do, and `NotebookPagesTest` is where
     * that is measured.
     */
    @Test
    fun `sharing a folio and scaling the coins change no cell`() {
        val card = IndexCard.Box(
            name = "Bandeja del abuelo",
            issuer = null,
            box = OwnGroupingView(
                OwnGrouping(id = 1, name = "Bandeja del abuelo", typeIds = emptyList()),
                emptyList(),
            ),
        )
        val today = notebookSections(
            CollectionState(),
            listOf(card),
            emptyList(),
            Curation(emptyList()),
            NotebookOptions(),
        )

        val landed = { it: NotebookOptions -> it.numistaQr || it.bothFaces || !it.photographs }
        val moved = allCombinations().filterNot(landed).filter { options ->
            notebookSections(CollectionState(), listOf(card), emptyList(), Curation(emptyList()), options) != today
        }

        assertEquals(emptyList(), moved, "un interruptor de geometría ha cambiado una casilla")
    }

    @Test
    fun `the default configuration declares the geometry the notebook was measured with`() {
        val paper = printGeometry(NotebookOptions())

        assertEquals(210f, paper.widthMm)
        assertEquals(297f, paper.heightMm)
        assertEquals(PrintHeading.Masthead, paper.heading)
        assertEquals(40f, paper.headingMm)
        assertTrue(paper.printsCoins)
        // Una lámina por folio, y por tanto ninguna costura entre láminas que pagar (#232).
        assertFalse(paper.sharesPage)
        assertEquals(0f, paper.blockGapMm)
        assertEquals(14f, paper.footMm)
        assertEquals(50f, paper.rulerBarMm)
        assertEquals(16f, paper.captionMm)
        assertEquals(28f, paper.minCellWidthMm)
        // Al tamaño con el que sale del cajón, que es el 1:1 del #169: la moneda no se escala, y por
        // eso la página lleva la regla y ninguna casilla dice ningún número.
        assertEquals(1f, paper.coinScale)
        assertEquals(40.9f, paper.printedDiameterMm(40.9f))
        assertFalse(paper.printsDiameterLabel)
        // Y ningún código: sin él, el rótulo es el pie de foto entero.
        assertEquals(0f, paper.qrMm)
        assertEquals(0f, paper.qrGapMm)
        // Una cara por moneda, que es la del #169: el reverso, que es el lado que se mira.
        assertEquals(1, paper.facesPerCell)
        // 210 menos los dos márgenes; 297 menos los dos márgenes y la regla del pie, y de ahí la
        // cabecera de la única lámina que hay en el folio.
        assertEquals(180f, paper.gridWidthMm)
        assertEquals(253f, paper.contentHeightMm)
        assertEquals(213f, paper.gridHeightMm)
    }

    /**
     * What «compartir página» declares: a folio that takes more than one plate, under a thin band.
     *
     * **The band comes with the switch and is not a switch of its own** (#228 decided that, #232
     * implements it): forty millimetres of masthead per plate makes no sense once two of them share a
     * folio, and it is where most of the saving comes from — 90 pages sharing folios with the
     * masthead against 73 with this band, on the sixty plates the ticket measured.
     *
     * Nothing else moves. The coins keep their diameter, the caption keeps its sixteen millimetres
     * and the strip at the foot keeps its ruler: a folio shared by two plates is still printed at
     * 1:1, and that is the whole reason this switch is about packing and not about size.
     */
    @Test
    fun `sharing a folio thins the heading and moves nothing else`() {
        val paper = printGeometry(NotebookOptions())
        val folio = printGeometry(NotebookOptions(sharePage = true))

        assertTrue(folio.sharesPage)
        assertEquals(PrintHeading.Slim, folio.heading)
        assertEquals(14f, folio.headingMm)
        // La cabecera fina se queda con el epígrafe, una línea de título y la raya: ni subtítulo ni
        // bloque de fichas caben en catorce milímetros, y medio dato bajo una raya es peor que nada.
        assertEquals(1, folio.heading.titleLines)
        assertFalse(folio.heading.subtitle)
        assertFalse(folio.heading.facts)
        // Y la costura entre dos láminas, que sólo existe cuando hay dos.
        assertEquals(6f, folio.blockGapMm)
        assertEquals(
            paper,
            folio.copy(sharesPage = paper.sharesPage, heading = paper.heading),
        )
        // El folio no crece: lo que cambia es que la cabecera sale de él una vez por lámina y no una
        // vez por página, así que la rejilla de una lámina sola gana los veintiséis milímetros.
        assertEquals(paper.contentHeightMm, folio.contentHeightMm)
        assertEquals(239f, folio.gridHeightMm)
    }

    /**
     * Sharing a folio composes with the other three, and with «sin fotos» it thins a thin heading.
     *
     * The list of #231 already dropped the specification block and came down to twenty-eight
     * millimetres; sharing takes it to fourteen, and what it gives up on top is the subtitle and the
     * second line of the title. Two lists of a dozen lines each on one folio is what the two switches
     * together are for, and neither had to learn about the other to do it.
     */
    @Test
    fun `sharing a folio thins the list's heading too`() {
        val list = printGeometry(NotebookOptions(photographs = false))
        val shared = printGeometry(NotebookOptions(photographs = false, sharePage = true))

        assertEquals(28f, list.headingMm)
        assertEquals(14f, shared.headingMm)
        assertFalse(shared.printsCoins)
        assertEquals(7f, shared.captionMm)
        assertEquals(0f, shared.rulerBarMm)
        assertEquals(list, shared.copy(sharesPage = false, heading = list.heading))
    }

    /**
     * What the QR costs in millimetres, which is all it costs: only the caption moves.
     *
     * The page, the margins, the heading and the ruler are untouched, and so is the width of a cell:
     * beside the name would have forced a floor of 44 mm on every cell, and that is a column taken
     * away from almost every coin in the collection. The code goes under the name and grows the one
     * measure this grid has to spare.
     */
    @Test
    fun `the qr grows the caption and nothing else`() {
        val paper = printGeometry(NotebookOptions())
        val coded = printGeometry(NotebookOptions(numistaQr = true))

        // 16 mm de rótulo, 2 de aire y los 12 del código con su zona de silencio.
        assertEquals(30f, coded.captionMm)
        assertEquals(12f, coded.qrMm)
        assertEquals(2f, coded.qrGapMm)
        // Y las palabras siguen teniendo los 16 mm del #169: el código se **suma** al pie de foto, no
        // le quita sitio al rótulo. Un estado, un título de dos líneas y un año siguen cabiendo.
        assertEquals(paper.captionMm, coded.captionMm - coded.qrGapMm - coded.qrMm)
        assertEquals(
            paper,
            coded.copy(captionMm = paper.captionMm, qrMm = paper.qrMm, qrGapMm = paper.qrGapMm),
        )
        // Un módulo de 0,364 mm: 33 de ellos —25 de versión 2 y las dos zonas de silencio— en 12 mm.
        assertEquals(0.364f, coded.qrMm / 33f, 0.001f)
    }

    /**
     * What both faces cost in millimetres, which is all they cost: only the coin band moves (#230).
     *
     * The page, the margins, the heading, the ruler and the caption are untouched — the second face
     * is paid for in width, because at 1:1 the alternative is halving the diameter and that is the
     * one thing a page measured with a ruler cannot do.
     */
    @Test
    fun `both faces widen the coin band and nothing else`() {
        val paper = printGeometry(NotebookOptions())
        val doubled = printGeometry(NotebookOptions(bothFaces = true))

        assertEquals(2, doubled.facesPerCell)
        assertEquals(paper, doubled.copy(facesPerCell = paper.facesPerCell))
        // Dos onzas de 40,9 mm y la calle de 3 que las separa.
        assertEquals(40.9f, paper.coinBandWidthMm(40.9f))
        assertEquals(84.8f, doubled.coinBandWidthMm(40.9f), 0.01f)
    }

    /**
     * What «tamaño real» apagado declares: every coin at three fifths, and no ruler to check it with.
     *
     * The switch gives up the one promise #169 was built on, so what goes with the diameter is the bar
     * at the foot — a ruler nobody is going to lay a coin against protects nothing, and beside a coin
     * that is not at 1:1 it is a foot that lies. What takes its place is the diameter as a **number** in
     * every caption, which is what the list of #231 already prints for exactly the same reason.
     *
     * The floor on a cell comes down with the coins or the saving is never made: at three fifths every
     * member of the shelf prints narrower than 28 mm, so that floor would become the width of every cell
     * in the notebook. And the caption **grows** by two millimetres, which is the switch's one surprise:
     * the number needs a line of its own, because appended to the year it was the millimetres an ellipsis
     * ate on the cells of a collection with no issue list.
     */
    @Test
    fun `scaling the coins takes the ruler away and prints the diameter instead`() {
        val paper = printGeometry(NotebookOptions())
        val scaled = printGeometry(NotebookOptions(actualSize = false))

        assertEquals(0.6f, scaled.coinScale)
        // Las rusas de 33 mm salen a 19,8, y la casilla es eso más el pie de foto, ahora de 18.
        assertEquals(19.8f, scaled.printedDiameterMm(33f), 0.01f)
        assertEquals(19.8f, scaled.coinBandWidthMm(33f), 0.01f)
        assertEquals(18f, scaled.captionMm)
        assertEquals(37.8f, scaled.cellHeightMm(33f), 0.01f)
        // Ni regla ni tira: el pie de página se queda con la línea que dice de dónde salió la lámina.
        assertEquals(0f, scaled.rulerBarMm)
        assertEquals(5f, scaled.footMm)
        assertTrue(scaled.printsDiameterLabel)
        // Y el suelo de la casilla baja con las monedas: 17 mm es lo que mide «SIN EMITIR».
        assertEquals(18f, scaled.minCellWidthMm)
        assertEquals(18f, scaled.cellWidthMm(16f))
        // Nada más se mueve. La casilla de 24,5 de una onza ya no es el suelo, sino la moneda.
        assertEquals(24.54f, scaled.cellWidthMm(40.9f), 0.01f)
        assertEquals(
            paper,
            scaled.copy(
                coinScale = paper.coinScale,
                rulerBarMm = paper.rulerBarMm,
                footMm = paper.footMm,
                captionMm = paper.captionMm,
                minCellWidthMm = paper.minCellWidthMm,
            ),
        )
    }

    /**
     * A hole scales like the coin that goes in it, and so does a casilla nobody measured (#169).
     *
     * The fraction multiplies into the cell's width and height and nowhere else, so the fallback
     * diameter — the ounce a lone hole borrows when no Numista type backs it — needs to know nothing
     * about the switch: it is a real diameter like any other and comes out at three fifths of itself.
     */
    @Test
    fun `the diameter of a cell nobody measured scales like its siblings`() {
        val scaled = printGeometry(NotebookOptions(actualSize = false))

        assertEquals(40f, scaled.fallbackDiameterMm)
        assertEquals(24f, printGrid(null, scaled).printedDiameterMm, 0.01f)
        assertEquals(40f, printGrid(null, scaled).diameterMm)
        // La rejilla se mide contra el diámetro real y se dibuja al escalado, que es lo que deja a un
        // pie de foto decir «40,9 mm» de un círculo de 24,5.
        val ounces = printGrid(40.9f, scaled)
        assertEquals(40.9f, ounces.diameterMm)
        assertEquals(24.54f, ounces.printedDiameterMm, 0.01f)
    }

    /**
     * Exactly the pages printed at 1:1 carry a ruler, and every other page says the size in words.
     *
     * One fact and two ways of keeping it: #169 put on paper that a coin's size can be **checked**, and
     * a page does that with a bar the collector measures or with a number they read. Both would be a
     * caption arguing with a ruler; neither would be a coin with no size at all. So it is asked of the
     * thirty-two configurations rather than of the two that were being written at the time.
     */
    @Test
    fun `a page carries the ruler or the number, and never both or neither`() {
        allCombinations().forEach { options ->
            val paper = printGeometry(options)
            val atActualSize = options.photographs && options.actualSize

            assertEquals(atActualSize, paper.rulerBarMm > 0f, "la regla no cuadra con $options")
            assertEquals(!atActualSize, paper.printsDiameterLabel, "el número no cuadra con $options")
        }
    }

    /**
     * The scaled page composes with the other four, and none of them had to learn about it.
     *
     * The second face is paid for in width off the **printed** diameter, which is the one thing at 1:1
     * that could not be done — «ambas caras» at real size doubles a cell to 84,8 mm and takes a plate of
     * ounces to six cells a page, and at three fifths the pair costs 52 and keeps three columns. The code
     * adds its own band to whatever caption the page arrived with. And «sin fotos» wins outright: there
     * is no coin to scale, so the list is the list.
     */
    @Test
    fun `the scaled page composes with the other four`() {
        val scaled = printGeometry(NotebookOptions(actualSize = false))
        val doubled = printGeometry(NotebookOptions(actualSize = false, bothFaces = true))
        val coded = printGeometry(NotebookOptions(actualSize = false, numistaQr = true))
        val folio = printGeometry(NotebookOptions(actualSize = false, sharePage = true))

        // Dos onzas de 24,54 mm y la calle de 3 que las separa, contra los 84,8 del 1:1.
        assertEquals(52.08f, doubled.coinBandWidthMm(40.9f), 0.01f)
        assertEquals(scaled, doubled.copy(facesPerCell = scaled.facesPerCell))
        // El código se suma al rótulo escalado, sea el que sea: 18 más los 2 de aire y los 12 del QR.
        assertEquals(32f, coded.captionMm)
        assertEquals(12f, coded.qrMm)
        // Y compartir folio adelgaza la cabecera y no toca ni la escala ni el suelo de la casilla.
        assertEquals(PrintHeading.Slim, folio.heading)
        assertEquals(0.6f, folio.coinScale)
        assertEquals(18f, folio.minCellWidthMm)
        // Sin fotos no hay moneda que escalar: la lista es la lista, marcada o no la casilla.
        assertEquals(
            printGeometry(NotebookOptions(photographs = false)),
            printGeometry(NotebookOptions(photographs = false, actualSize = false)),
        )
    }

    /**
     * The code and the second face compose, which is what «cinco interruptores» buys.
     *
     * Neither knows about the other: the code grows the caption, the second face widens the cell,
     * and a notebook with both on pays for both. Models with a name were dropped precisely so the
     * collector can combine, and the live page count is what tells them what the combination costs.
     */
    @Test
    fun `the code and the second face compose without knowing about each other`() {
        val both = printGeometry(NotebookOptions(bothFaces = true, numistaQr = true))

        assertEquals(2, both.facesPerCell)
        assertEquals(30f, both.captionMm)
        assertEquals(12f, both.qrMm)
        assertEquals(
            printGeometry(NotebookOptions(numistaQr = true)),
            both.copy(facesPerCell = 1),
        )
    }

    /**
     * What «sin fotos» declares: a page of lines, and nothing on it measured against a ruler (#231).
     *
     * It is the only one of the five that changes the *shape* of the page rather than a measure of
     * it. No coin band, so a cell is its caption and a caption is one line of seven millimetres; no
     * diameter deciding the width, so the floor is the whole of it and exactly two columns fill the
     * printable band; no ruler, because there is nothing at 1:1 to catch a viewer's «ajustar a la
     * página» with; and a heading that names the plate without summarising it, since a list that says
     * «Tengo» or «Me falta» on every line has printed the coverage already.
     */
    @Test
    fun `with no photographs the page is lines, two to a row, and carries no ruler`() {
        val list = printGeometry(NotebookOptions(photographs = false))

        assertEquals(0, list.facesPerCell)
        assertFalse(list.printsCoins)
        assertEquals(7f, list.captionMm)
        assertEquals(0f, list.rulerBarMm)
        assertEquals(28f, list.headingMm)
        // La casilla es su línea, mida lo que mida la moneda: es lo que quita las cien páginas.
        assertEquals(7f, list.cellHeightMm(40.9f))
        assertEquals(7f, list.cellHeightMm(16f))
        assertEquals(0f, list.coinBandWidthMm(40.9f))
        // Media banda imprimible, así que caben dos exactas y la llenan.
        assertEquals(88.5f, list.cellWidthMm(40.9f), 0.01f)
        val grid = printGrid(40.9f, list)
        assertEquals(2, grid.columns)
        assertEquals(23, grid.rows)
        assertEquals(46, grid.cellsPerPage)
        assertEquals(list.gridWidthMm, grid.blockWidthMm, 0.01f)
        // Y la rejilla ya no la decide el diámetro: la lámina de medios y la de onzas son la misma,
        // y una casilla que nadie midió no necesita ningún diámetro de reserva para caber.
        val shape = { it: PrintGrid -> Triple(it.columns, it.rows, it.cellWidthMm) }
        assertEquals(shape(grid), shape(printGrid(16f, list)))
        assertEquals(shape(grid), shape(printGrid(null, list)))
    }

    /**
     * The code closes the line instead of sitting under it, so it costs the row five millimetres.
     *
     * On a page of coins the caption grows by the code's whole band, because the cell is read top to
     * bottom and there is a name to stack it under. A line is read left to right and has a right edge
     * going spare, so what the code costs is only the height it does not already have: seven
     * millimetres of line become the twelve of the square.
     *
     * And no air of its own: [PrintGeometry.qrGapMm] is what separates a code from a caption stacked
     * over it, which is a thing a line does not have — the row already spaces what is on it, and the
     * twelve millimetres of the square carry the symbol's own quiet zone. A gap the renderer never spends
     * would be a millimetre in the arithmetic that is nowhere on the paper.
     */
    @Test
    fun `on a page of lines the code costs the row its own height and no more`() {
        val coded = printGeometry(NotebookOptions(photographs = false, numistaQr = true))

        assertEquals(12f, coded.captionMm)
        assertEquals(12f, coded.qrMm)
        assertEquals(0f, coded.qrGapMm)
        assertEquals(12f, coded.cellHeightMm(40.9f))
        // Sólo el pie de foto se mueve: la cabecera, el pie de página y las columnas siguen igual.
        assertEquals(
            printGeometry(NotebookOptions(photographs = false)),
            coded.copy(captionMm = 7f, qrMm = 0f),
        )
        // El código más robusto reduce las filas, pero la rejilla sigue cabiendo entera en el folio.
        val bare = printGeometry(NotebookOptions(photographs = false))
        assertTrue(printGrid(40.9f, coded).rows < printGrid(40.9f, bare).rows)
    }

    /**
     * Two of the five stop being questions when the coins stop being drawn.
     *
     * With the photographs off no coin reaches the page at all, so «ambas caras» and «tamaño real»
     * have nothing to negotiate. The sheet greys them rather than leaving them ticked and inert.
     */
    @Test
    fun `with the photographs off there is no face and no size to negotiate`() {
        val bare = NotebookOptions(photographs = false)

        assertFalse(bare.offers(NotebookSwitch.BothFaces))
        assertFalse(bare.offers(NotebookSwitch.ActualSize))
        // Los otros tres siguen siendo preguntas: una lista sin fotos aún se puede compartir página
        // y llevar el QR, y las fotos se pueden volver a encender.
        assertTrue(bare.offers(NotebookSwitch.Photographs))
        assertTrue(bare.offers(NotebookSwitch.SharePage))
        assertTrue(bare.offers(NotebookSwitch.NumistaQr))

        assertTrue(NotebookSwitch.entries.all { NotebookOptions().offers(it) })
    }

    @Test
    fun `every switch reads and writes the one field it is about`() {
        NotebookSwitch.entries.forEach { switch ->
            val on = NotebookOptions().with(switch, on = true)
            val off = NotebookOptions().with(switch, on = false)

            assertTrue(on[switch], "$switch no se enciende")
            assertFalse(off[switch], "$switch no se apaga")
            NotebookSwitch.entries.filter { it != switch }.forEach { other ->
                assertEquals(NotebookOptions()[other], on[other], "$switch ha movido $other")
            }
        }
    }

    /**
     * The order of the enum is the order of the sheet, and all six of them do something now.
     *
     * `pending` was what made a grey switch honest — the issue that would make it work, named under a
     * control that could not be touched — and each ticket set its own to null: «QR de Numista» first,
     * then «ambas caras», «fotos», «compartir página» and «tamaño real» (#233). With the last one the
     * property itself went, because a field that can only be null is the same lie a grey switch was.
     *
     * «Sin colección» is last because its lámina is last (#275), and it is the only one of the six
     * that adds a page instead of rearranging the ones there are.
     */
    @Test
    fun `the seven switches are in the order the sheet draws them`() {
        assertEquals(
            listOf(
                NotebookSwitch.Photographs,
                NotebookSwitch.BothFaces,
                NotebookSwitch.ActualSize,
                NotebookSwitch.SharePage,
                NotebookSwitch.NumistaQr,
                NotebookSwitch.Unclaimed,
                NotebookSwitch.Money,
            ),
            NotebookSwitch.entries.toList(),
        )
    }

    /**
     * A single lámina or hoja cannot share a folio with another plate, and «Sin colección» is the
     * index's loose-coin plate (#401). What remains is the same how-and-what the notebook asks,
     * minus the two questions that only make sense over many cards.
     */
    @Test
    fun `a single sheet asks five switches, not the index-only ones`() {
        assertEquals(
            listOf(
                NotebookSwitch.Photographs,
                NotebookSwitch.BothFaces,
                NotebookSwitch.ActualSize,
                NotebookSwitch.NumistaQr,
                NotebookSwitch.Money,
            ),
            sheetExportSwitches(),
        )
    }

    /**
     * Packing and the loose-coin plate stay as they were stored: the sheet UI never offers them, so
     * confirming a lámina must not silently rewrite how the next full notebook will print (#401).
     */
    @Test
    fun `sheet export clears packing and the loose plate without touching the rest`() {
        val chosen = NotebookOptions(
            photographs = false,
            bothFaces = true,
            actualSize = false,
            sharePage = true,
            numistaQr = true,
            unclaimed = true,
            money = true,
        )

        assertEquals(
            chosen.copy(sharePage = false, unclaimed = false),
            chosen.forSheetExport(),
        )
    }
}

/**
 * The thirty-two configurations the **five geometry switches** can be in.
 *
 * «Sin colección» is deliberately out (#275): it decides *what* is printed and not how, so it moves
 * no millimetre and changes no cell of a card — and folding it in would double this list to say
 * nothing new about the geometry these tests are about.
 */
internal fun allCombinations(): List<NotebookOptions> = buildList {
    for (photographs in BOTH) {
        for (bothFaces in BOTH) {
            for (actualSize in BOTH) {
                for (sharePage in BOTH) {
                    for (numistaQr in BOTH) {
                        add(
                            NotebookOptions(
                                photographs = photographs,
                                bothFaces = bothFaces,
                                actualSize = actualSize,
                                sharePage = sharePage,
                                numistaQr = numistaQr,
                            ),
                        )
                    }
                }
            }
        }
    }
}

private val BOTH = listOf(false, true)
