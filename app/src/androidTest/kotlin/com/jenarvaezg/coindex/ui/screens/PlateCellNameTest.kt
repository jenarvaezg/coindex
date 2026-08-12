package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.ui.components.RecessedYearTag
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertEquals
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
    // The year hangs off the name right above it at one distance and not two (#411): the line a
    // short name leaves unused is the box's, and it no longer falls between the name and its year.
    fun theNameHandsItsYearTheSameAirWhateverItsLines() {
        compose.setContent {
            CoindexTheme {
                Row {
                    // One line of Bitter at 113 dp, out of the two the box reserves: the line it
                    // leaves unused is where the year used to float.
                    Cell(name = SHORT_LABEL, year = "1876")
                    Cell(name = LONGEST_LABEL, year = "1934")
                }
            }
        }

        val expected = with(compose.density) { PlateSpacing.insideMember.toPx() }
        assertEquals(expected, airBetween(SHORT_LABEL, "year-1876"), 0.5f)
        assertEquals(expected, airBetween(LONGEST_LABEL, "year-1934"), 0.5f)
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
    private fun Cell(name: String, year: String) {
        Column(modifier = Modifier.width(CELL_WIDTH)) {
            PlateCellName(name)
            RecessedYearTag(year, onOpen = {}, modifier = Modifier.testTag("year-$year"))
        }
    }

    private fun topOf(tag: String): Float =
        compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.top

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
