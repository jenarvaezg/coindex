package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
                    CollectionName("Fuertes", modifier = Modifier.width(123.7.dp))
                    CollectionName(
                        "Conservación de la Naturaleza",
                        modifier = Modifier.width(123.7.dp),
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
}
