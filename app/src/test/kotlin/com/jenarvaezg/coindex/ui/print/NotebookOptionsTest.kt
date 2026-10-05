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
 * Las opciones del cuaderno (#228). Las páginas se cuentan antes de dibujar, así que se comprueba
 * que cada interruptor se traduce en milímetros y que el cuaderno por omisión no cambia.
 */
class NotebookOptionsTest {
    @Test
    fun `the notebook of today is what nobody chose`() {
        val untouched = NotebookOptions()

        // Nadie se encuentra el cuaderno cambiado sin haberlo pedido.
        assertTrue(untouched.photographs)
        assertFalse(untouched.bothFaces)
        assertTrue(untouched.actualSize)
        assertFalse(untouched.sharePage)
        assertFalse(untouched.numistaQr)
        assertFalse(untouched.unclaimed)
    }

    /**
     * One ticket per switch: «ambas caras» #230, «fotos» #231, «compartir página» #232, «tamaño
     * real» #233, «QR de Numista» #234.
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
     * These two are pure geometry: packing (#232) and the drawing scale (#233), so a cell keeps its
     * real diameter. QR, both faces and no photographs do change cells (`NotebookPagesTest`).
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
        // Una lámina por folio, sin costura entre láminas (#232).
        assertFalse(paper.sharesPage)
        assertEquals(0f, paper.blockGapMm)
        assertEquals(14f, paper.footMm)
        assertEquals(50f, paper.rulerBarMm)
        assertEquals(16f, paper.captionMm)
        assertEquals(28f, paper.minCellWidthMm)
        // A 1:1 (#169): la página lleva la regla y ninguna casilla imprime el diámetro.
        assertEquals(1f, paper.coinScale)
        assertEquals(40.9f, paper.printedDiameterMm(40.9f))
        assertFalse(paper.printsDiameterLabel)
        // Sin QR.
        assertEquals(0f, paper.qrMm)
        assertEquals(0f, paper.qrGapMm)
        // Una cara por moneda (#169): el reverso, el lado que se mira.
        assertEquals(1, paper.facesPerCell)
        // 210 menos los márgenes; 297 menos los márgenes y la regla del pie; menos la cabecera.
        assertEquals(180f, paper.gridWidthMm)
        assertEquals(253f, paper.contentHeightMm)
        assertEquals(213f, paper.gridHeightMm)
    }

    /**
     * The thin band comes with the switch, not as a switch of its own (#228, #232): a 40 mm
     * masthead per plate makes no sense on a shared folio. Coins stay at 1:1.
     */
    @Test
    fun `sharing a folio thins the heading and moves nothing else`() {
        val paper = printGeometry(NotebookOptions())
        val folio = printGeometry(NotebookOptions(sharePage = true))

        assertTrue(folio.sharesPage)
        assertEquals(PrintHeading.Slim, folio.heading)
        assertEquals(14f, folio.headingMm)
        // Epígrafe, una línea de título y la raya: ni subtítulo ni fichas caben en 14 mm.
        assertEquals(1, folio.heading.titleLines)
        assertFalse(folio.heading.subtitle)
        assertFalse(folio.heading.facts)
        // La costura entre láminas, que sólo existe al compartir folio.
        assertEquals(6f, folio.blockGapMm)
        assertEquals(
            paper,
            folio.copy(sharesPage = paper.sharesPage, heading = paper.heading),
        )
        // El folio no crece; la rejilla de una lámina sola gana los 26 mm de cabecera.
        assertEquals(paper.contentHeightMm, folio.contentHeightMm)
        assertEquals(239f, folio.gridHeightMm)
    }

    /** The list heading of #231 goes from 28 mm to 14, losing the subtitle and a title line. */
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
     * Since #478 the QR sits beside the caption when the cell is wide enough; only a narrower cell
     * grows to stack it underneath. Nothing else moves.
     */
    @Test
    fun `the qr costs a cell nothing where the cell has width to spare`() {
        val paper = printGeometry(NotebookOptions())
        val coded = printGeometry(NotebookOptions(numistaQr = true))
        val doubled = printGeometry(NotebookOptions(numistaQr = true, bothFaces = true))

        // El rótulo conserva los 16 mm del #169.
        assertEquals(16f, coded.captionMm)
        assertEquals(12f, coded.qrMm)
        assertEquals(2f, coded.qrGapMm)
        assertEquals(paper, coded.copy(qrMm = paper.qrMm, qrGapMm = paper.qrGapMm))
        // Módulos de 0,364 mm: 33 (25 de versión 2 más la zona de silencio) en 12 mm.
        assertEquals(0.364f, coded.qrMm / 33f, 0.001f)

        // Una cara: la onza de 40,9 no deja sitio al lado, así que lo apila y paga 14 mm.
        assertFalse(coded.qrBesideCaption(40.9f))
        assertEquals(paper.cellHeightMm(40.9f) + 14f, coded.cellHeightMm(40.9f), 0.01f)
        // Dos caras: 84,8 mm de casilla, y el código no cuesta nada.
        assertTrue(doubled.qrBesideCaption(40.9f))
        assertEquals(
            printGeometry(NotebookOptions(bothFaces = true)).cellHeightMm(40.9f),
            doubled.cellHeightMm(40.9f),
            0.01f,
        )
        // Dos caras de 16 mm son 35 mm de casilla: el código va debajo.
        assertFalse(doubled.qrBesideCaption(16f))
    }

    /** With the QR on, coins stay at 1:1 and round, the invariant of #169 (#478). */
    @Test
    fun `the code never touches the coin`() {
        val paper = printGeometry(NotebookOptions(bothFaces = true))
        val coded = printGeometry(NotebookOptions(bothFaces = true, numistaQr = true))

        // Los 1000 escudos de Portugal (40,1 mm), la lámina que abrió el #478.
        assertEquals(40.1f, coded.printedDiameterMm(40.1f), 0.001f)
        assertEquals(paper.printedDiameterMm(40.1f), coded.printedDiameterMm(40.1f), 0.001f)
        assertEquals(paper.coinBandWidthMm(40.1f), coded.coinBandWidthMm(40.1f), 0.001f)
        assertEquals(paper.cellWidthMm(40.1f), coded.cellWidthMm(40.1f), 0.001f)
        assertEquals(1f, coded.coinScale)
        // La casilla mide 56,1 mm en vez de 70,1: tres filas por folio en vez de dos.
        assertEquals(56.1f, coded.cellHeightMm(40.1f), 0.01f)
        assertEquals(70.1f, coded.cellHeightMm(40.1f) + 14f, 0.01f)
    }

    /**
     * The QR's band is reserved on both sides so the name stays centred under the coin (#478), and
     * what is left must reach the caption floor, [PrintGeometry.minCellWidthMm].
     */
    @Test
    fun `the code sits beside the caption from fifty-six millimetres of cell`() {
        val coded = printGeometry(NotebookOptions(numistaQr = true, bothFaces = true))

        // 28 de rótulo y dos bandas de 14: la casilla tiene que llegar a 56 mm.
        assertEquals(56f, coded.minCellWidthMm + 2 * (coded.qrMm + coded.qrGapMm))
        assertFalse(coded.qrBesideCaption(26f))
        assertTrue(coded.qrBesideCaption(27f))
        // Sin código no hay nada que colocar.
        assertFalse(printGeometry(NotebookOptions(bothFaces = true)).qrBesideCaption(40.9f))
        // En una página de líneas el código cierra la línea (#231).
        val list = printGeometry(NotebookOptions(photographs = false, numistaQr = true))
        assertFalse(list.qrBesideCaption(40.9f))
        assertFalse(list.qrCostsHeight(40.9f))
    }

    /** At 1:1 the second face can only be paid for in width (#230). */
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
     * «Tamaño real» off (#233): coins at three fifths. The ruler goes, since next to a scaled coin
     * it would lie, and each caption prints the diameter on a line of its own (+2 mm): appended to
     * the year it got ellipsized. The cell floor drops too, or every scaled cell would be 28 mm.
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
        // Sin regla: el pie de página se queda sólo con la fuente.
        assertEquals(0f, scaled.rulerBarMm)
        assertEquals(5f, scaled.footMm)
        assertTrue(scaled.printsDiameterLabel)
        // El suelo baja a 18 mm: «SIN EMITIR» mide 17.
        assertEquals(18f, scaled.minCellWidthMm)
        assertEquals(18f, scaled.cellWidthMm(16f))
        // La onza escalada (24,5 mm) ya pasa del suelo. Nada más se mueve.
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

    /** The fallback diameter of a hole with no Numista type scales like a real one (#169). */
    @Test
    fun `the diameter of a cell nobody measured scales like its siblings`() {
        val scaled = printGeometry(NotebookOptions(actualSize = false))

        assertEquals(40f, scaled.fallbackDiameterMm)
        assertEquals(24f, printGrid(null, scaled).printedDiameterMm, 0.01f)
        assertEquals(40f, printGrid(null, scaled).diameterMm)
        // Medida al diámetro real y dibujada al escalado: dice «40,9 mm» bajo un círculo de 24,5.
        val ounces = printGrid(40.9f, scaled)
        assertEquals(40.9f, ounces.diameterMm)
        assertEquals(24.54f, ounces.printedDiameterMm, 0.01f)
    }

    /** A coin's size can be checked on paper (#169), by exactly one of the two means. */
    @Test
    fun `a page carries the ruler or the number, and never both or neither`() {
        allCombinations().forEach { options ->
            val paper = printGeometry(options)
            val atActualSize = options.photographs && options.actualSize

            assertEquals(atActualSize, paper.rulerBarMm > 0f, "la regla no cuadra con $options")
            assertEquals(!atActualSize, paper.printsDiameterLabel, "el número no cuadra con $options")
        }
    }

    @Test
    fun `the scaled page composes with the other four`() {
        val scaled = printGeometry(NotebookOptions(actualSize = false))
        val doubled = printGeometry(NotebookOptions(actualSize = false, bothFaces = true))
        val coded = printGeometry(NotebookOptions(actualSize = false, numistaQr = true))
        val folio = printGeometry(NotebookOptions(actualSize = false, sharePage = true))

        // Dos onzas de 24,54 mm y la calle de 3 que las separa, contra los 84,8 del 1:1.
        assertEquals(52.08f, doubled.coinBandWidthMm(40.9f), 0.01f)
        assertEquals(scaled, doubled.copy(facesPerCell = scaled.facesPerCell))
        // El QR deja el rótulo escalado en 18 mm y sólo alarga la casilla estrecha (#478).
        assertEquals(18f, coded.captionMm)
        assertEquals(12f, coded.qrMm)
        assertEquals(37.8f + 14f, coded.cellHeightMm(33f), 0.01f)
        // Compartir folio no toca la escala ni el suelo de la casilla.
        assertEquals(PrintHeading.Slim, folio.heading)
        assertEquals(0.6f, folio.coinScale)
        assertEquals(18f, folio.minCellWidthMm)
        // Sin fotos no hay moneda que escalar.
        assertEquals(
            printGeometry(NotebookOptions(photographs = false)),
            printGeometry(NotebookOptions(photographs = false, actualSize = false)),
        )
    }

    /**
     * They meet only in [PrintGeometry.cellHeightMm], where the width of the second face makes the
     * QR free (#478).
     */
    @Test
    fun `the code and the second face compose without knowing about each other`() {
        val both = printGeometry(NotebookOptions(bothFaces = true, numistaQr = true))

        assertEquals(2, both.facesPerCell)
        assertEquals(16f, both.captionMm)
        assertEquals(12f, both.qrMm)
        assertEquals(
            printGeometry(NotebookOptions(numistaQr = true)),
            both.copy(facesPerCell = 1),
        )
    }

    /**
     * «Sin fotos» (#231): each cell is a 7 mm line. No ruler, since nothing is at 1:1, and a 28 mm
     * heading without facts, since every line already says «Tengo» or «Me falta».
     */
    @Test
    fun `with no photographs the page is lines, two to a row, and carries no ruler`() {
        val list = printGeometry(NotebookOptions(photographs = false))

        assertEquals(0, list.facesPerCell)
        assertFalse(list.printsCoins)
        assertEquals(7f, list.captionMm)
        assertEquals(0f, list.rulerBarMm)
        assertEquals(28f, list.headingMm)
        // La casilla es su línea, mida lo que mida la moneda.
        assertEquals(7f, list.cellHeightMm(40.9f))
        assertEquals(7f, list.cellHeightMm(16f))
        assertEquals(0f, list.coinBandWidthMm(40.9f))
        // Media banda imprimible: caben dos exactas.
        assertEquals(88.5f, list.cellWidthMm(40.9f), 0.01f)
        val grid = printGrid(40.9f, list)
        assertEquals(2, grid.columns)
        assertEquals(23, grid.rows)
        assertEquals(46, grid.cellsPerPage)
        assertEquals(list.gridWidthMm, grid.blockWidthMm, 0.01f)
        // El diámetro ya no decide la rejilla, ni hace falta el de reserva.
        val shape = { it: PrintGrid -> Triple(it.columns, it.rows, it.cellWidthMm) }
        assertEquals(shape(grid), shape(printGrid(16f, list)))
        assertEquals(shape(grid), shape(printGrid(null, list)))
    }

    /**
     * The QR closes the line on the right, so the row grows from 7 mm to the square's 12. No
     * [PrintGeometry.qrGapMm]: that gap only separates a stacked code from its caption.
     */
    @Test
    fun `on a page of lines the code costs the row its own height and no more`() {
        val coded = printGeometry(NotebookOptions(photographs = false, numistaQr = true))

        assertEquals(12f, coded.captionMm)
        assertEquals(12f, coded.qrMm)
        assertEquals(0f, coded.qrGapMm)
        assertEquals(12f, coded.cellHeightMm(40.9f))
        // Sólo se mueve el pie de foto.
        assertEquals(
            printGeometry(NotebookOptions(photographs = false)),
            coded.copy(captionMm = 7f, qrMm = 0f),
        )
        // Líneas más altas, menos filas por folio.
        val bare = printGeometry(NotebookOptions(photographs = false))
        assertTrue(printGrid(40.9f, coded).rows < printGrid(40.9f, bare).rows)
    }

    /** The sheet greys them out rather than leaving them ticked and inert. */
    @Test
    fun `with the photographs off there is no face and no size to negotiate`() {
        val bare = NotebookOptions(photographs = false)

        assertFalse(bare.offers(NotebookSwitch.BothFaces))
        assertFalse(bare.offers(NotebookSwitch.ActualSize))
        // Una lista sin fotos aún puede compartir folio y llevar QR.
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

    /** «Sin colección» follows the layout switches because its lámina prints last (#275). */
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

    /** A single lámina can't share a folio, and «Sin colección» is an index-wide plate (#401). */
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

    /** Only this export drops them; the stored options for the next notebook keep them (#401). */
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
 * The 32 combinations of the five geometry switches. «Sin colección» (#275) and the money switch
 * are left out: they decide what is printed, not how.
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
