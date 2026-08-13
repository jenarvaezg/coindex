package com.jenarvaezg.coindex.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.ui.shelf.SEARCH_CLEAR_LABEL
import com.jenarvaezg.coindex.ui.shelf.SEARCH_PLACEHOLDER
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Android's minimum, the same one the casillas of the #302 map are measured against. */
private val MINIMUM_TARGET = 48.dp

/**
 * The aspa that empties the search box (#414).
 *
 * The query does not survive a launch (ADR 0021 §1) but it does survive walking into a collection
 * and back, so a word typed once keeps narrowing the shelf until it is deleted — and deleting it was
 * one backspace per letter.
 */
@RunWith(AndroidJUnit4::class)
class SearchFieldTest {
    @get:Rule
    val compose = createComposeRule()

    private fun searchFieldHolding(text: String) {
        compose.setContent {
            var query by remember { mutableStateOf(text) }
            CoindexTheme {
                SearchField(value = query, onValueChange = { query = it })
            }
        }
    }

    private fun clearButton() = compose.onNodeWithContentDescription(SEARCH_CLEAR_LABEL)

    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    @Test
    fun theAspaIsOfferedOnlyWhileThereIsSomethingToClear() {
        searchFieldHolding("")

        clearButton().assertDoesNotExist()
    }

    @Test
    fun tappingTheAspaEmptiesTheBox() {
        searchFieldHolding("The")

        compose.onNodeWithText("The").assertExists()
        clearButton().performClick()

        compose.onNodeWithText(SEARCH_PLACEHOLDER).assertExists()
        clearButton().assertDoesNotExist()
    }

    /**
     * Android's 48 dp, bought without the 40 dp field growing to it (ADR 0026).
     *
     * The ink is the field's own height and the drawn cross is 16 dp of that, so what is measured
     * here is the **touch** box and not the drawing: the same distinction the year tag of the #302
     * map is measured by, and the reason neither of them pretends its ink is its target.
     */
    @Test
    fun theAspaIsTappableWellBeyondItsStroke() {
        searchFieldHolding("The")

        clearButton().assertHeightIsAtLeast(SEARCH_FIELD_HEIGHT)
        val minimum = with(compose.density) { MINIMUM_TARGET.toPx() }
        val target = clearButton().fetchSemanticsNode().touchBoundsInRoot
        assertTrue("${target.width} × ${target.height}", target.width >= minimum)
        assertTrue("${target.width} × ${target.height}", target.height >= minimum)
    }
}
