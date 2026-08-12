package com.jenarvaezg.coindex.ui.print

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.ui.screens.NotebookPageSheet
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The two names #412 was reported with, and the longest Spanish one of the same catalog. */
private const val REPORTED = "V centenario de la primera vuelta al mundo"
private const val ALSO_REPORTED = "Academia General del Aire y del Espacio"
private const val LONGEST_SPANISH = "Crucero de instrucción Juan Sebastián de Elcano"

/**
 * The 2 euros of `spain-face-value-18g`, whose 25,75 mm fall back to the 28 mm floor: the narrowest
 * cell the notebook draws, and the coin the report's names belong to.
 */
private const val TWO_EUROS_MM = 25.75f

/** The longest label in `data/`, at 73 characters, which no surface prints whole. */
private const val LONGEST =
    "Iglesia de la Guarnición de Potsdam, con marca de ceca debajo (1934-1935)"

/**
 * A name the screen prints whole is never an ellipsis on the paper (#412).
 *
 * That parity is the whole of this file. The screen was given a third line for these very names, and
 * the paper cannot be given one: the sixteen millimetres of [PrintGeometry.captionMm] are what the
 * page count was computed from, so a taller caption is a longer notebook. What the paper has instead
 * is resolution — 2,9 mm of serif is a comfortable caption at 300 dpi and 13 sp was already the
 * screen's floor — so here the ladder of #348 does the work the third line does on the glass.
 *
 * Every shape of page is measured, because each one takes millimetres from somewhere else: the code
 * (#478), the checklist with no photographs (#231), the scaled page whose caption has to say the real
 * diameter in words (#233).
 */
@RunWith(AndroidJUnit4::class)
class PrintedCaptionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun theNamesOfTheReportReachTheNarrowestCellWhole() {
        showPage(NotebookOptions())

        assertWhole(REPORTED)
        assertWhole(ALSO_REPORTED)
        assertWhole(LONGEST_SPANISH)
    }

    @Test
    fun theNamesOfTheReportSurviveTheCode() {
        showPage(NotebookOptions(numistaQr = true))

        assertWhole(REPORTED)
        assertWhole(ALSO_REPORTED)
    }

    @Test
    // «Sin fotos» is the other shape of cell: one line, with the state taking 17 mm of it (#231).
    fun theNamesOfTheReportSurviveTheChecklist() {
        showPage(NotebookOptions(photographs = false))

        assertWhole(REPORTED)
        assertWhole(ALSO_REPORTED)
    }

    @Test
    // «Tamaño real» off shrinks the coin and not the caption, which then has to print the measure.
    fun theNamesOfTheReportSurviveAScaledPage() {
        showPage(NotebookOptions(actualSize = false))

        assertWhole(REPORTED)
        assertWhole(ALSO_REPORTED)
    }

    @Test
    // The parity works the other way too, and this is where it is honest: the 73 characters of the
    // Potsdam Garrison Church are five lines on the screen and an ellipsis there, so an ellipsis on
    // the paper is the same page saying the same thing. It is not what the ladder promises to save.
    fun theLongestLabelOfAllIsCutOnPaperJustAsItIsOnScreen() {
        showPage(NotebookOptions(actualSize = false))

        assertTrue("el de 73 caracteres ya cabe: revisa la paridad", isCut(LONGEST))
    }

    private fun showPage(options: NotebookOptions) {
        val geometry = printGeometry(options)
        val section = PrintSection(
            eyebrow = "COINDEX · CATÁLOGO CURADO",
            title = "Los nombres largos",
            subtitle = null,
            facts = listOf("Progreso" to "4 de 4"),
            source = "Numista",
            cells = listOf(REPORTED, ALSO_REPORTED, LONGEST_SPANISH, LONGEST).map { label ->
                PrintCell(
                    curatedLabel = label,
                    state = "Tengo",
                    footnote = "2020",
                    diameterMm = TWO_EUROS_MM,
                    faces = listOf(CoinPhoto()),
                    filled = true,
                    numistaUrl = "https://en.numista.com/catalogue/pieces19880.html",
                )
            },
        )
        val page = printPages(listOf(section), geometry).first()
        compose.setContent { CoindexTheme { NotebookPageSheet(page, onImageSettled = {}) } }
        compose.waitForIdle()
    }

    private fun assertWhole(name: String) {
        assertFalse("«$name» sale cortado del papel", isCut(name))
    }

    /** Cut means Bitter ran out of ladder: something was clipped or ellipsized off the last line. */
    private fun isCut(name: String): Boolean {
        val results = mutableListOf<TextLayoutResult>()
        val node = compose.onNodeWithText(name).fetchSemanticsNode()
        val layout = node.config.getOrElseNullable(SemanticsActions.GetTextLayoutResult) { null }
        requireNotNull(layout) { "«$name» no llegó al papel" }
        layout.action?.invoke(results)
        return results.first().hasVisualOverflow
    }
}
