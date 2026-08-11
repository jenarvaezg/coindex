package com.jenarvaezg.coindex.ui.print

import com.jenarvaezg.coindex.data.photos.CoinPhoto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The folio a shared lámina gets, which is the one it fills (#431).
 *
 * The PNG stopped being a drawing of its own and became the printed page, so the blank third an A4
 * leaves under a short plate is the one thing left to take off it. What these pin is that the trim
 * is **measured**: the rejilla floors its rows at one, so anything that feels for the height by
 * shortening the paper until the cells stop fitting is told they still fit long after they have
 * fallen off the bottom edge.
 */
class TrimmedFolioTest {
    private val a4 = printGeometry(NotebookOptions())

    private fun section(casillas: Int, faces: Int = 1) = PrintSection(
        eyebrow = "COINDEX · CATÁLOGO CURADO",
        title = "Fuertes · Venezuela",
        subtitle = null,
        facts = listOf("Progreso" to "8 de 12"),
        source = "Numista",
        cells = (0 until casillas).map { index ->
            PrintCell(
                curatedLabel = "19${'0' + index % 10}",
                state = "Tengo",
                footnote = null,
                diameterMm = 37f,
                faces = List(faces) { CoinPhoto(thumbnail = "https://example.test/$index.jpg") },
                filled = true,
            )
        },
    )

    @Test
    fun `the folio loses the height it was not drawing on, and nothing else`() {
        val plate = section(casillas = 12)
        val trimmed = a4.trimmedToContent(plate)

        assertTrue(trimmed.heightMm < a4.heightMm, "el folio no se ha recortado")
        // The width is the notebook's and stays: same rejilla, same columns, same coin at 1:1.
        assertEquals(a4.widthMm, trimmed.widthMm)
        assertEquals(a4.marginMm, trimmed.marginMm)
        assertEquals(a4.footMm, trimmed.footMm)
        // What is left is exactly the block and the two things around it that are not the rejilla.
        assertEquals(
            plate.grid(a4).blockHeightMm(plate.cells.size) + a4.marginMm * 2 + a4.footMm,
            trimmed.heightMm,
        )
    }

    /**
     * The plate of one casilla, which is fifteen of the father's and the case that caught the bug.
     *
     * Felt for rather than measured, the answer was 57 mm — a masthead over an empty strip, because
     * `fitCount` floors at one row and reported a rejilla that fitted a coin into a folio shorter
     * than the coin. Measured, the casilla is still on the page.
     */
    @Test
    fun `a plate of one casilla keeps its casilla`() {
        val plate = section(casillas = 1)
        val page = printPages(listOf(plate), a4.trimmedToContent(plate)).single()

        assertEquals(1, page.blocks.sumOf { it.cells.size })
        assertTrue(
            page.geometry.heightMm > a4.marginMm * 2 + a4.footMm + page.geometry.headingMm,
            "el folio no da ni para la banda de la cabecera: la casilla se sale del papel",
        )
    }

    @Test
    fun `a page that fills its folio keeps the paper it was counted on`() {
        // Sixty casillas do not fit on one A4, so there is no blank to take off it.
        val long = section(casillas = 60)

        assertEquals(a4.heightMm, a4.trimmedToContent(long).heightMm)
    }

    /**
     * The trim repacks and never resizes, so the page that comes back is the page the notebook would
     * have printed on that paper — every casilla on it, and only one page.
     */
    @Test
    fun `trimming a measured page leaves the same casillas on one page`() {
        val plate = section(casillas = 12)
        val page = printPages(listOf(plate), a4).single()
        val trimmed = assertNotNull(page.trimmedToContent())

        assertEquals(12, trimmed.blocks.sumOf { it.cells.size })
        assertTrue(trimmed.geometry.heightMm < page.geometry.heightMm)
    }

    /**
     * What the export waits for before it captures, which used to be `sheetImageCount` and is now
     * the page's own count: faces and not cells (#230). A plate printing one declared side asks for
     * one picture per casilla; both faces asks for two, and a hole in either is a hole to report.
     */
    @Test
    fun `the wait counts faces and not casillas`() {
        assertEquals(12, printPages(listOf(section(12)), a4).single().photographs)
        assertEquals(24, printPages(listOf(section(12, faces = 2)), a4).single().photographs)
        // A cell drawn with no face asks for nothing: the checklist of #231 waits for no picture.
        assertEquals(0, printPages(listOf(section(12, faces = 0)), a4).single().photographs)
    }
}
