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

/** The hole a casilla is drawn around, and what its tag hangs off since #473. */
private val HOLE = 104.dp

/** The longest label in `data/`, at 73 characters: seven lines of Bitter if nothing stops it. */
private const val LONGEST_LABEL =
    "Iglesia de la Guarnición de Potsdam, con marca de ceca debajo (1934-1935)"

/** The name #412 was reported with: 42 characters, three lines of Bitter, and not one more. */
private const val THREE_LINE_LABEL = "V centenario de la primera vuelta al mundo"

/** One of the two titled casillas of the 1 Bolívar, and the row #473 was reported on. */
private const val MINTED_LATER = "1945 (acuñada en 1947)"

/** Any Numista type: what it buys the casilla is a tag that takes a click, like the real ones. */
private const val A_TYPE = 10338

/**
 * The casilla of a plate, drawn: hole, sunken year, name (#473).
 *
 * Two things a reader of these numbers has to know, both measured on the device rather than assumed.
 * The tag's node is its **drawing** — 28 dp of sunken cardboard — and not the 48 dp target it bought
 * with `minimumInteractiveComponentSize`, so the drop from a hole to a tag is read straight off it.
 * The name's node, on the other hand, **includes its own breathing room**, so what is compared here
 * is box to box: the ink is 6 dp further in on each side, which only widens the margins these tests
 * assert. The ink-to-ink arithmetic is `PlateSpacingTest`'s.
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
                    // The row of the 1 Bolívar the ticket was reported on: two titled casillas and
                    // one whose label is already its year and prints no name at all.
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
    // #473 itself: the casilla with no name used to reserve its row's whole name box, empty, so its
    // year hung 64 dp under its coin against the 42 dp that separated two rows. Now every tag of the
    // plate is the same short drop under its own hole, and that is what makes them line up.
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
    // The other half of #473, and the one no width of `rowGap` could have bought: the box a name
    // reserved was measured against the density and grew with the collector's type while the gap
    // between rows stayed put. Nothing inside a casilla is measured in sp any more, so the drop from
    // a coin to its year is the same at font scale 2 as at 1 — and a name that grows pushes the
    // *next row* away instead of pushing its own year down.
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
    // A name is a gloss on the year above it, and the row below has to stay further away than that.
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
    // An announced member has no year, and a listed one whose tag takes no click does not buy the
    // 48 dp target the rest have. Either would pull its name up against the hole while its
    // neighbours' stayed down, so the casilla reserves that height whether or not it fills it.
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
    // Three lines is what a casilla prints, and the reason is the notebook's cartouche and no longer
    // the cardboard hanging over the neighbours of its row (#412, #473).
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
    // And a name that had to be cut is still the whole name to search and to accessibility (#348).
    fun aTruncatedNameKeepsItsWholeTextInSemantics() {
        compose.setContent {
            CoindexTheme {
                PlateCellName(LONGEST_LABEL, modifier = Modifier.width(CELL_WIDTH))
            }
        }

        assertTrue(layoutOf(LONGEST_LABEL).hasVisualOverflow)
        compose.onNodeWithText(LONGEST_LABEL).assertExists()
    }

    /** The row of the 1 Bolívar with the row underneath it, spaced as the grid spaces them. */
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
                // What the tag says is the subject's rule and no longer the `year` field (#511):
                // a casilla built without a plaque wears nothing sunk into its cardboard, which is
                // not the casilla any of these measurements is about.
                plaque = plaqueOf(label = label, year = year, yearIsCommon = false),
            ),
            images = null,
            printedSide = PrintedSide.Obverse,
            travellingFrom = null,
            onOpenCoin = {},
            // Keyed by the year, which is what the tag under the hole prints and what the
            // measurements below look the casilla up by.
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

    /** What Bitter actually did: how many lines it took, and whether it was cut. */
    private fun layoutOf(name: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        val node = compose.onNodeWithText(name).fetchSemanticsNode()
        val layout = node.config.getOrElseNullable(SemanticsActions.GetTextLayoutResult) { null }
        requireNotNull(layout) { "«$name» no es un Text: nadie puede decir en qué líneas cayó" }
        layout.action?.invoke(results)
        return results.first()
    }
}
