package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.ui.components.RecessedYearTag
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The cell of the plate on a 411 dp phone: three columns of (411 − 40 padding − 32 gutter) / 3. */
private val CELL_WIDTH = 113.dp

/** A label of one line, and a real one: four members of `data/` are called exactly this. */
private const val SHORT_LABEL = "Onza Troy"

/** The longest label in `data/`, at 73 characters: seven lines of Bitter if nothing stops it. */
private const val LONGEST_LABEL =
    "Iglesia de la Guarnición de Potsdam, con marca de ceca debajo (1934-1935)"

/** The name #412 was reported with: 42 characters, three lines of Bitter, and not one more. */
private const val THREE_LINE_LABEL = "V centenario de la primera vuelta al mundo"

@RunWith(AndroidJUnit4::class)
class PlateCellNameTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun theTagsOfARowShareOneBaselineWhateverTheNamesAbove() {
        compose.setContent {
            CoindexTheme {
                Row {
                    // A date run cell, whose label is already its year and prints no name.
                    Cell(name = "", year = "1879")
                    // Two lines: the one Fuertes member whose label is not a year.
                    Cell(name = "1 Venezolano", year = "1876")
                    // Seven lines of Bitter if nothing stops it.
                    Cell(name = LONGEST_LABEL, year = "1934")
                }
            }
        }

        val first = topOf("year-1879")
        assertEquals(first, topOf("year-1876"), 0.5f)
        assertEquals(first, topOf("year-1934"), 0.5f)
    }

    @Test
    // A name that fills its box hands its year the 16 dp of #411, and a name that does not hands it
    // half of what it left over, because the name is centred in the box since #412. What #411 was
    // defending survives as the comparison `PlateSpacingTest` makes: even that wider gap stays well
    // inside the air that separates two members, so a year never reads as the next row's label.
    fun theAirUnderANameIsHalfOfWhatItLeftOver() {
        compose.setContent {
            CoindexTheme {
                Row {
                    // One line of Bitter at 113 dp, out of the two the box reserves.
                    Cell(name = SHORT_LABEL, year = "1876")
                    Cell(name = LONGEST_LABEL, year = "1934")
                }
            }
        }

        // Measured and not modelled: the autosize shrinks the line as well as the letter, so a name
        // of two lines at 13 sp does not fill a box reserved for two of the tallest — which is why
        // the box was ever reserved in dp (#411). What the arithmetic of `PlateSpacing` gives is the
        // **worst case**, with lines at full height, and both of these have to stay under it.
        val longName = airBetween(LONGEST_LABEL, "year-1934")
        val shortName = airBetween(SHORT_LABEL, "year-1876")
        val worst = with(compose.density) {
            PlateSpacing.insideMemberCentred(
                reserved = 2,
                used = 1,
                lineHeight = PlateSpacing.nameLine.toDp(),
            ).toPx()
        }
        val between = with(compose.density) { PlateSpacing.betweenMembers.toPx() }

        assertTrue(
            "el nombre corto ($shortName px) no cede su mitad del hueco al de dos líneas " +
                "($longName px)",
            shortName > longName,
        )
        assertTrue("$shortName px pasa del peor caso de $worst px", shortName <= worst + 0.5f)
        assertTrue("$shortName px alcanza los $between px que separan dos casillas", shortName < between)
    }

    @Test
    // The name of the report reaches the casilla whole on the third line its row reserved (#412),
    // and the same name in the two lines every plate used to reserve dies in an ellipsis.
    fun aThreeLineNameIsPrintedWholeWhenItsRowReservedThreeLines() {
        compose.setContent {
            CoindexTheme {
                PlateCellName(
                    THREE_LINE_LABEL,
                    lines = 3,
                    modifier = Modifier.width(CELL_WIDTH),
                )
            }
        }

        val printed = layoutOf(THREE_LINE_LABEL)
        assertEquals(3, printed.lineCount)
        assertFalse(printed.hasVisualOverflow)
    }

    @Test
    // The other half of the pair, and the defect as it was reported: the two lines every plate used
    // to reserve cut this very name. If Bitter ever fits it in two, the test above proves nothing.
    fun theSameNameInTwoLinesIsCutWhereTheReportSawIt() {
        compose.setContent {
            CoindexTheme {
                PlateCellName(
                    THREE_LINE_LABEL,
                    lines = 2,
                    modifier = Modifier.width(CELL_WIDTH),
                )
            }
        }

        assertTrue(layoutOf(THREE_LINE_LABEL).hasVisualOverflow)
    }

    @Test
    // The reservation is the row's and the price of the third line is paid by every casilla on it:
    // a short name keeps its year at the same distance, so the tags of the row still line up.
    fun theTagsOfARowShareOneBaselineWhenTheRowReservedAThirdLine() {
        compose.setContent {
            CoindexTheme {
                Row {
                    Cell(name = SHORT_LABEL, year = "1876", lines = 3)
                    Cell(name = THREE_LINE_LABEL, year = "2020", lines = 3)
                    Cell(name = "", year = "1934", lines = 3)
                }
            }
        }

        val first = topOf("year-1876")
        assertEquals(first, topOf("year-2020"), 0.5f)
        assertEquals(first, topOf("year-1934"), 0.5f)
    }

    @Test
    // A name that had to be cut is still the whole name to search and to accessibility (#348).
    fun aTruncatedNameKeepsItsWholeTextInSemantics() {
        compose.setContent {
            CoindexTheme {
                PlateCellName(LONGEST_LABEL, modifier = Modifier.width(CELL_WIDTH))
            }
        }

        compose.onNodeWithText(LONGEST_LABEL).assertExists()
    }

    @Composable
    private fun Cell(name: String, year: String, lines: Int = PLATE_CELL_NAME_MIN_LINES) {
        Column(modifier = Modifier.width(CELL_WIDTH)) {
            PlateCellName(name, lines = lines)
            RecessedYearTag(year, onOpen = {}, modifier = Modifier.testTag("year-$year"))
        }
    }

    private fun topOf(tag: String): Float =
        compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.top

    /** What Bitter actually did inside the box: how many lines it took, and whether it was cut. */
    private fun layoutOf(name: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        val node = compose.onNodeWithText(name).fetchSemanticsNode()
        val layout = node.config.getOrElseNullable(SemanticsActions.GetTextLayoutResult) { null }
        requireNotNull(layout) { "«$name» no es un Text: nadie puede decir en qué líneas cayó" }
        layout.action?.invoke(results)
        return results.first()
    }

    /**
     * The blank the eye sees between the last line of [name] and the ink of its tag.
     *
     * Ink to ink, which is what proximity is made of and neither node's own box: the text's
     * semantics sit inside its breathing room, and the tag's inside the target it bought with
     * `minimumInteractiveComponentSize`.
     */
    private fun airBetween(name: String, tag: String): Float =
        topOf(tag) - compose.onNodeWithText(name).fetchSemanticsNode().boundsInRoot.bottom
}
