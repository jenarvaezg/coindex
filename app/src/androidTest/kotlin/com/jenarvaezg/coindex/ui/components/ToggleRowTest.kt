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
 * The whole line is the control, and a greyed line is still a line (#512).
 *
 * The tick this row draws is 20 dp of a phone held in one hand, which is the measured miss the
 * filter shelf fixed by taking the whole line; the KDoc of [ToggleRow] promised that and the
 * Material switch it used to hold took the tap on its own. So the promise is the test.
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
        // Still reporting the configuration, which is the other half of what grey owes: the note
        // says why it cannot move, and the tick says which way it is.
        compose.onNodeWithText(LABEL).assertIsOn()
        compose.onNodeWithText(NOTE).assertExists()
    }

    private companion object {
        // The words of the panel this row is drawn in, so a reading of the test is a reading of the
        // card: «Fotos» with the note the configuration puts under a switch it has made moot.
        const val LABEL = "Fotos"
        const val NOTE = "Sin fotos no hay nada que ajustar"
    }
}
