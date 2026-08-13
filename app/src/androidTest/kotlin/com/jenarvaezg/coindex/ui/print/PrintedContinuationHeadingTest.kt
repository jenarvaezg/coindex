package com.jenarvaezg.coindex.ui.print

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.ui.screens.NotebookPageSheet
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The Australian ounce, which is the coin whose rows the repeated masthead was costing. */
private const val OUNCE_MM = 40.9f

/**
 * What the second page of a plate draws, which is its name and not its specification (#480).
 *
 * The arithmetic is `PrintGeometryTest`'s and `NotebookPagesTest`'s; this is the other half of the same
 * decision — the band's height and its contents are one value precisely so the brush cannot disagree
 * with the count, and here the brush is asked. A continuation page that still drew the specification
 * into fourteen millimetres would be half a fact under a rule, and one that dropped the name would be a
 * folio nobody can file: on paper there is no scrolling back to find out which collection this is.
 *
 * The fourth row is what the band buys, so it is checked **against the edge of the paper** and not as a
 * count: the grid is clipped to the folio, so a row the packer placed and the page could not hold would
 * simply be drawn off the bottom and vanish.
 */
@RunWith(AndroidJUnit4::class)
class PrintedContinuationHeadingTest {
    @get:Rule
    val compose = createComposeRule()

    private val plate = PrintSection(
        eyebrow = "COINDEX · CATÁLOGO CURADO",
        title = "Kookaburra de la Perth Mint",
        subtitle = "Onza de plata · 1 oz · Bullion",
        facts = listOf("Progreso" to "0 de 28", "Metal" to "Plata"),
        source = "Numista",
        cells = (1..28).map { number ->
            PrintCell(
                curatedLabel = "Casilla $number",
                state = "Me falta",
                footnote = "19${number % 100}",
                diameterMm = OUNCE_MM,
                faces = listOf(CoinPhoto()),
                filled = false,
            )
        },
    )

    private val pages = printPages(listOf(plate), printGeometry(NotebookOptions()))

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun theFirstPageDrawsTheWholeMasthead() {
        show(0)

        compose.onNodeWithText(plate.title).assertExists()
        compose.onNodeWithText(plate.subtitle!!).assertExists()
        compose.onNodeWithText("Progreso").assertExists()
        compose.onNodeWithText("Metal").assertExists()
        // Doce onzas bajo los cuarenta milímetros que lo dicen todo, y ni una más.
        compose.onNodeWithText("Casilla 12").assertExists()
        compose.onNodeWithText("Casilla 13").assertDoesNotExist()
    }

    @Test
    fun thePageThatContinuesItDrawsTheNameAndNotTheSpecification() {
        show(1)

        // El nombre sigue: es lo que el #232 dejó escrito y lo que la banda fina existe para decir.
        compose.onNodeWithText(plate.title).assertExists()
        compose.onNodeWithText(printedPageOfSection(2, pages.size)).assertExists()
        // Y lo que se cae es la especificación repetida y el subtítulo, enteros y no a medias.
        compose.onNodeWithText("Progreso").assertDoesNotExist()
        compose.onNodeWithText("Metal").assertDoesNotExist()
        compose.onNodeWithText("Plata").assertDoesNotExist()
        compose.onNodeWithText(plate.subtitle!!).assertDoesNotExist()
    }

    @Test
    fun theRowThatTheThinBandBuysStaysOnThePaper() {
        // Dos folios y no tres: doce onzas bajo el masthead y las dieciséis restantes bajo el nombre.
        assertEquals(2, pages.size)
        assertEquals(16, pages[1].cells.size)

        show(1)

        // La cuarta fila es la que el masthead repetido costaba, y se dibuja dentro del folio: el
        // rótulo de la última casilla acaba por encima del canto de la hoja, márgenes y pie incluidos.
        val paper = with(compose.density) { pages[1].geometry.heightMm.dp.toPx() }
        listOf("Casilla 25", "Casilla 28").forEach { label ->
            val bounds = compose.onNodeWithText(label).fetchSemanticsNode().boundsInRoot
            assertTrue("«$label» se sale del folio: ${bounds.bottom} de $paper", bounds.bottom <= paper)
        }
    }

    private fun show(page: Int) {
        compose.setContent { CoindexTheme { NotebookPageSheet(pages[page], onImageSettled = {}) } }
        compose.waitForIdle()
    }
}
