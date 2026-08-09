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
}
