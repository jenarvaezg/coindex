package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.ui.DrawnCell
import com.jenarvaezg.coindex.ui.plaqueOf
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The cell of the plate on a 411 dp phone: three columns of (411 − 40 padding − 32 gutter) / 3. */
private val CELL_WIDTH = 113.dp

/** The casilla's hole; its tag hangs off it (#473). */
private val HOLE = 104.dp

/** The longest label in `data/`, at 73 characters: seven lines of Bitter if nothing stops it. */
private const val LONGEST_LABEL =
    "Iglesia de la Guarnición de Potsdam, con marca de ceca debajo (1934-1935)"

/** The name #412 was reported with: exactly three lines of Bitter. */
private const val THREE_LINE_LABEL = "V centenario de la primera vuelta al mundo"

/** A titled casilla of the 1 Bolívar row #473 was reported on. */
private const val MINTED_LATER = "1945 (acuñada en 1947)"

/** Any Numista type: it makes the tag clickable, as on real casillas. */
private const val A_TYPE = 10338

/**
 * The casilla of a plate, drawn: hole, sunken year, name (#473).
 *
 * The tag's node is its 28 dp drawing, not the 48 dp touch target, so hole-to-tag drops are read
 * straight off it. The name's node includes 6 dp of padding on each side, so names are compared box
 * to box; the ink-to-ink arithmetic is in `PlateSpacingTest`.
 */
@RunWith(AndroidJUnit4::class)
class PlateCellNameTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun theTagsOfARowShareOneBaselineWhateverTheNamesBelow() {
        compose.setContent {
            CoindexTheme {
                Row {
                    // The 1 Bolívar row of #473: two titled casillas and one whose label is its
                    // year, so it prints no name.
                    Cell(name = MINTED_LATER, year = "1945")
                    Cell(name = THREE_LINE_LABEL, year = "1954")
                    Cell(name = null, year = "1960")
                }
            }
        }

        val first = topOfText("1945")
        assertEquals(first, topOfText("1954"), 0.5f)
        assertEquals(first, topOfText("1960"), 0.5f)
    }

    @Test
    // #473: a nameless casilla used to reserve its row's empty name box, hanging its year 64 dp
    // under its coin when rows were 42 dp apart.
    fun theCasillaWithNoNameKeepsItsYearUnderItsOwnCoin() {
        compose.setContent {
            CoindexTheme {
                Row {
                    Cell(name = MINTED_LATER, year = "1945")
                    Cell(name = null, year = "1960")
                }
            }
        }

        val expected = with(compose.density) { PlateSpacing.underTheHole.toPx() }
        assertEquals(expected, underTheHole("1960"), 0.5f)
        assertEquals(expected, underTheHole("1945"), 0.5f)
    }

    @Test
    // #473: nothing inside a casilla is sized in sp, so the coin-to-year drop holds at font
    // scale 2; a growing name pushes the next row away instead of its own year down.
    fun thePlateKeepsItsProximityWhenTheCollectorEnlargesTheType() {
        compose.setContent {
            CoindexTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale = 2f),
                ) {
                    TwoRows()
                }
            }
        }

        val expected = with(compose.density) { PlateSpacing.underTheHole.toPx() }
        assertEquals(expected, underTheHole("1960"), 0.5f)
        assertTrue(
            "el año de la casilla sin nombre (${underTheHole("1960")} px bajo su moneda) alcanza " +
                "las monedas de la fila de abajo (${untilTheNextRow("1960")} px)",
            underTheHole("1960") * 2 <= untilTheNextRow("1960"),
        )
    }

    @Test
    fun aNameStaysNearerItsOwnYearThanTheRowBelow() {
        compose.setContent { CoindexTheme { TwoRows() } }

        val toItsYear = topOfText(MINTED_LATER) - bottomOfText("1945")
        val toTheNextRow = topOf("row-2") - bottomOfText(MINTED_LATER)

        assertTrue(
            "$toItsYear px hasta su año contra $toTheNextRow px hasta la fila de abajo",
            toItsYear * 2 <= toTheNextRow,
        )
    }

    @Test
    // An announced member has no year and an unclickable tag lacks the 48 dp target; the casilla
    // reserves that height anyway so its name lines up with its neighbours'.
    fun aCasillaWithNoYearStillLeavesTheTagsRoom() {
        compose.setContent {
            CoindexTheme {
                Row {
                    Cell(name = MINTED_LATER, year = "1945")
                    Cell(name = THREE_LINE_LABEL, year = null, typeId = null)
                }
            }
        }

        assertEquals(topOfText(MINTED_LATER), topOfText(THREE_LINE_LABEL), 0.5f)
    }

    @Test
    // Three lines is the casilla's limit, set by the notebook's cartouche (#412, #473).
    fun aThreeLineNameIsPrintedWhole() {
        compose.setContent {
            CoindexTheme {
                PlateCellName(THREE_LINE_LABEL, modifier = Modifier.width(CELL_WIDTH))
            }
        }

        val printed = layoutOf(THREE_LINE_LABEL)
        assertEquals(3, printed.lineCount)
        assertFalse(printed.hasVisualOverflow)
    }

    @Test
    // #348: search and accessibility still read the whole name.
    fun aTruncatedNameKeepsItsWholeTextInSemantics() {
        compose.setContent {
            CoindexTheme {
                PlateCellName(LONGEST_LABEL, modifier = Modifier.width(CELL_WIDTH))
            }
        }

        assertTrue(layoutOf(LONGEST_LABEL).hasVisualOverflow)
        compose.onNodeWithText(LONGEST_LABEL).assertExists()
    }

    /** The 1 Bolívar row and one below it, spaced as the grid spaces them. */
    @Composable
    private fun TwoRows() {
        Column(verticalArrangement = Arrangement.spacedBy(PlateSpacing.rowGap)) {
            Row {
                Cell(name = MINTED_LATER, year = "1945")
                Cell(name = null, year = "1960")
            }
            Row(modifier = Modifier.testTag("row-2")) {
                Cell(name = null, year = "1965")
            }
        }
    }

    @Composable
    private fun Cell(name: String?, year: String?, typeId: Int? = A_TYPE) {
        val label = name ?: year.orEmpty()
        PlateCell(
            cell = DrawnCell(
                id = label,
                label = label,
                numistaTypeId = typeId,
                footnote = null,
                year = year,
                owned = true,
                missing = false,
                // The tag comes from the plaque, not from `year` (#511); without one the casilla
                // draws no tag at all.
                plaque = plaqueOf(label = label, year = year, yearIsCommon = false),
            ),
            images = null,
            printedSide = PrintedSide.Obverse,
            travellingFrom = null,
            onOpenCoin = {},
            // Keyed by the year, which is how the helpers below look the casilla up.
            modifier = Modifier.width(CELL_WIDTH).testTag("cell-${year ?: name}"),
        )
    }

    private fun topOf(tag: String): Float =
        compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top

    private fun topOfText(text: String): Float =
        compose.onNodeWithText(text).fetchSemanticsNode().boundsInRoot.top

    private fun bottomOfText(text: String): Float =
        compose.onNodeWithText(text).fetchSemanticsNode().boundsInRoot.bottom

    /** From the cardboard under a coin down to the drawing of its year. */
    private fun underTheHole(year: String): Float =
        topOfText(year) - (topOf("cell-$year") + with(compose.density) { HOLE.toPx() })

    /** From that same drawing down to the coins of the row underneath. */
    private fun untilTheNextRow(year: String): Float = topOf("row-2") - bottomOfText(year)

    /** How Bitter laid the name out: line count and whether it was cut. */
    private fun layoutOf(name: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        val node = compose.onNodeWithText(name).fetchSemanticsNode()
        val layout = node.config.getOrElseNullable(SemanticsActions.GetTextLayoutResult) { null }
        requireNotNull(layout) { "«$name» no es un Text: nadie puede decir en qué líneas cayó" }
        layout.action?.invoke(results)
        return results.first()
    }
}
