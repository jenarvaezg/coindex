package com.jenarvaezg.coindex.ui.print

import com.jenarvaezg.coindex.data.photos.CoinPhoto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * A shared lámina is its printed page trimmed to its content (#431). The trim is measured: the
 * rejilla floors its rows at one, so shrinking the paper until cells stop fitting cuts them off.
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
        // Same width, so the same rejilla and the coin still at 1:1.
        assertEquals(a4.widthMm, trimmed.widthMm)
        assertEquals(a4.marginMm, trimmed.marginMm)
        assertEquals(a4.footMm, trimmed.footMm)
        // The block plus margins and foot.
        assertEquals(
            plate.grid(a4).blockHeightMm(plate.cells.size, a4.heading) + a4.marginMm * 2 + a4.footMm,
            trimmed.heightMm,
        )
    }

    /** The case that caught the bug: `fitCount` floors at one row, so a guessed trim lost it. */
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
        // Sixty casillas overflow one A4, so there is no blank to trim.
        val long = section(casillas = 60)

        assertEquals(a4.heightMm, a4.trimmedToContent(long).heightMm)
    }

    /** The trim repacks and never resizes. */
    @Test
    fun `trimming a measured page leaves the same casillas on one page`() {
        val plate = section(casillas = 12)
        val page = printPages(listOf(plate), a4).single()
        val trimmed = assertNotNull(page.trimmedToContent())

        assertEquals(12, trimmed.blocks.sumOf { it.cells.size })
        assertTrue(trimmed.geometry.heightMm < page.geometry.heightMm)
    }

    /** The photographs the export waits for before it captures (#230). */
    @Test
    fun `the wait counts faces and not casillas`() {
        assertEquals(12, printPages(listOf(section(12)), a4).single().photographs)
        assertEquals(24, printPages(listOf(section(12, faces = 2)), a4).single().photographs)
        // No face, nothing to wait for (#231).
        assertEquals(0, printPages(listOf(section(12, faces = 0)), a4).single().photographs)
    }
}
