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
import com.jenarvaezg.coindex.ui.shelf.INDEX_SEARCH_PLACEHOLDER
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Android's minimum touch target, as for the casillas (#302). */
private val MINIMUM_TARGET = 48.dp

/**
 * The aspa that empties the search box (#414). The query survives walking into a collection and
 * back (though not a relaunch, ADR 0021 §1), and clearing it used to take a backspace per letter.
 */
@RunWith(AndroidJUnit4::class)
class SearchFieldTest {
    @get:Rule
    val compose = createComposeRule()

    private fun searchFieldHolding(text: String) {
        compose.setContent {
            var query by remember { mutableStateOf(text) }
            CoindexTheme {
                SearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = INDEX_SEARCH_PLACEHOLDER,
                )
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

        compose.onNodeWithText(INDEX_SEARCH_PLACEHOLDER).assertExists()
        clearButton().assertDoesNotExist()
    }

    /** The touch box reaches 48 dp while the field stays 40 dp and the cross 16 dp (ADR 0026). */
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
