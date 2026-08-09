package com.jenarvaezg.coindex.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.ui.COMPLETE_STAMP_WORD
import com.jenarvaezg.coindex.ui.screens.OffScreenSheet
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What the completion stamp is, said as what is on screen and what reaches an exported sheet.
 *
 * The stamp is a **state and not an event** (ADR 0026 §3): there is nothing to remember, so there
 * is nothing here about «the first time». What is defended is the pair of rules that decide whether
 * ink falls at all — every issued member owned — and the export rule of §4, which is one line in
 * `OffScreenSheet` and this test: **the stamp travels to the PNG and the stamping does not**.
 */
@RunWith(AndroidJUnit4::class)
class CompletionStampTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun aCompleteSheetSaysTheOneWord() {
        compose.setContent {
            CoindexTheme { StampedRatio(ratio = "22/22", complete = true) }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription(COMPLETE_STAMP_WORD).assertIsDisplayed()
        // And it adds not one figure: the ratio under the ink is the one the header already had.
        compose.onNodeWithText("22/22").assertIsDisplayed()
    }

    /** Four of twenty-two enters with its eighteen ghosts and its bare ratio (#304). */
    @Test
    fun aPlateThatIsMissingEightGetsNoInk() {
        compose.setContent {
            CoindexTheme { StampedRatio(ratio = "4/22", complete = false) }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription(COMPLETE_STAMP_WORD).assertDoesNotExist()
        compose.onNodeWithText("4/22").assertIsDisplayed()
    }

    /**
     * The sheet the father shows other people carries the stamp, because it is a state — unlike the
     * gloss, which follows a sensor and stays in the app.
     */
    @Test
    fun anExportedSheetCarriesTheStampWithTheInkAlreadyDry() {
        var stamping: Stamping? = Stamping.Default
        compose.setContent {
            CoindexTheme {
                OffScreenSheet(Density(1f)) {
                    stamping = LocalStamping.current
                    StampedRatio(ratio = "22/22", complete = true)
                }
            }
        }
        compose.waitForIdle()

        // Nothing alive is provided, so no frame of the export can catch the ink in the air.
        assertNull(stamping)
        compose.onNodeWithContentDescription(COMPLETE_STAMP_WORD).assertExists()
    }

    /** A sheet of a plate that is not complete comes out with no stamp on it at all. */
    @Test
    fun anExportedSheetOfAnIncompletePlateCarriesNoStamp() {
        compose.setContent {
            CoindexTheme {
                OffScreenSheet(Density(1f)) {
                    StampedRatio(ratio = "19/20", complete = false)
                }
            }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription(COMPLETE_STAMP_WORD).assertDoesNotExist()
    }
}
