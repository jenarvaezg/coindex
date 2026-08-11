package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Pixel 7 three-column card width used by the height cartouche test. */
private val CARD_WIDTH = 123.7.dp

@RunWith(AndroidJUnit4::class)
class CollectionNameTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun shortAndTwoLineNamesReserveTheSameHeight() {
        compose.setContent {
            CoindexTheme {
                Column {
                    CollectionName("Fuertes", modifier = Modifier.width(CARD_WIDTH))
                    CollectionName(
                        "Conservación de la Naturaleza",
                        modifier = Modifier.width(CARD_WIDTH),
                    )
                }
            }
        }

        val shortHeight = compose.onNodeWithText("Fuertes")
            .fetchSemanticsNode()
            .boundsInRoot
            .height
        val twoLineHeight = compose.onNodeWithText("Conservación de la Naturaleza")
            .fetchSemanticsNode()
            .boundsInRoot
            .height

        assertEquals(twoLineHeight, shortHeight, 0.5f)
    }

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    // #405: raw family names wrap at words (or an existing hyphen), never mid-grapheme.
    fun aLongFamilyNameDoesNotBreakMidWord() {
        val name = "Ibero-American"
        var layout: TextLayoutResult? = null

        compose.setContent {
            CoindexTheme {
                CollectionName(
                    name,
                    modifier = Modifier.width(CARD_WIDTH),
                    onTextLayout = { layout = it },
                )
            }
        }

        compose.waitForIdle()
        val result = checkNotNull(layout) { "CollectionName never reported a layout" }
        for (line in 1 until result.lineCount) {
            val breakAt = result.getLineStart(line)
            val before = name[breakAt - 1]
            assertTrue(
                "line $line starts mid-word at index $breakAt ('…$before|${name[breakAt]}…')",
                before.isWhitespace() || before == '-',
            )
        }
    }
}
