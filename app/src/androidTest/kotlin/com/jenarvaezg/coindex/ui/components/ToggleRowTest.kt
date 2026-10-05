package com.jenarvaezg.coindex.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The whole line is the control, and a greyed line still reports its state (#512). The 20 dp tick
 * alone is easy to miss one-handed, and the Material switch the row used to hold took the tap on
 * its own.
 */
@RunWith(AndroidJUnit4::class)
class ToggleRowTest {
    @get:Rule
    val compose = createComposeRule()

    private fun mountRow(enabled: Boolean, initial: Boolean = false) {
        compose.setContent {
            CoindexTheme {
                var checked by remember { mutableStateOf(initial) }
                ToggleRow(
                    label = LABEL,
                    note = NOTE,
                    checked = checked,
                    enabled = enabled,
                    onCheckedChange = { checked = it },
                )
            }
        }
    }

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun theLabelCarriesTheTap() {
        mountRow(enabled = true)

        compose.onNodeWithText(LABEL).assertIsOff()
        compose.onNodeWithText(LABEL).performClick()
        compose.onNodeWithText(LABEL).assertIsOn()
        compose.onNodeWithText(LABEL).performClick()
        compose.onNodeWithText(LABEL).assertIsOff()
    }

    @Test
    fun aGreyedRowReportsItsStateAndRefusesTheTap() {
        mountRow(enabled = false, initial = true)

        compose.onNodeWithText(LABEL).assertIsNotEnabled()
        compose.onNodeWithText(LABEL).performClick()
        // Greyed, it still reports the configuration: the note says why it can't move, the tick
        // which way it is.
        compose.onNodeWithText(LABEL).assertIsOn()
        compose.onNodeWithText(NOTE).assertExists()
    }

    private companion object {
        // The real panel's words: «Fotos» and the note shown when the switch is moot.
        const val LABEL = "Fotos"
        const val NOTE = "Sin fotos no hay nada que ajustar"
    }
}
