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
 * The completion stamp is a state, not an event (ADR 0026 §3): ink falls only when every issued
 * member is owned, and an exported sheet carries the stamp but not the stamping (§4).
 */
@RunWith(AndroidJUnit4::class)
class CompletionStampTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun aCompleteSheetSaysTheOneWord() {
        compose.setContent {
            CoindexTheme { StampedRatio(ratio = "22/22", complete = true, fall = rememberInkFall(true)) }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription(COMPLETE_STAMP_WORD).assertIsDisplayed()
        // The ratio under the ink is the header's own; the stamp adds no figure.
        compose.onNodeWithText("22/22").assertIsDisplayed()
    }

    /** 4 of 22 shows its bare ratio and no ink (#304). */
    @Test
    fun aPlateThatIsMissingEightGetsNoInk() {
        compose.setContent {
            CoindexTheme { StampedRatio(ratio = "4/22", complete = false, fall = rememberInkFall(false)) }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription(COMPLETE_STAMP_WORD).assertDoesNotExist()
        compose.onNodeWithText("4/22").assertIsDisplayed()
    }

    /** The stamp is a state, so it travels; the gloss follows a sensor and stays in the app. */
    @Test
    fun anExportedSheetCarriesTheStampWithTheInkAlreadyDry() {
        var stamping: Stamping? = Stamping.Default
        compose.setContent {
            CoindexTheme {
                OffScreenSheet(Density(1f)) {
                    stamping = LocalStamping.current
                    StampedRatio(ratio = "22/22", complete = true, fall = rememberInkFall(true))
                }
            }
        }
        compose.waitForIdle()

        // No `Stamping` is provided, so the export never catches the ink mid-fall.
        assertNull(stamping)
        compose.onNodeWithContentDescription(COMPLETE_STAMP_WORD).assertExists()
    }

    @Test
    fun anExportedSheetOfAnIncompletePlateCarriesNoStamp() {
        compose.setContent {
            CoindexTheme {
                OffScreenSheet(Density(1f)) {
                    StampedRatio(
                        ratio = "19/20",
                        complete = false,
                        fall = rememberInkFall(false),
                    )
                }
            }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription(COMPLETE_STAMP_WORD).assertDoesNotExist()
    }
}
