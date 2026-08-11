package com.jenarvaezg.coindex.ui.print

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.ui.COMPLETE_STAMP_WORD
import com.jenarvaezg.coindex.ui.screens.printDensity
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The rubber stamp on paper: the word reaches the PDF, and the Progress row is not what carries it.
 *
 * Drawing lives in millimetres under [printDensity], so this pins the ink itself rather than the
 * page packer — a complete heading says «completa», an incomplete one does not (#371).
 */
@RunWith(AndroidJUnit4::class)
class PrintedCompletionStampInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aCompleteHeadingCarriesTheStamp() {
        compose.setContent {
            CoindexTheme {
                CompositionLocalProvider(LocalDensity provides printDensity) {
                    PrintedCompletionStamp(heading = PrintHeading.Masthead, ratio = "22/22")
                }
            }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription(COMPLETE_STAMP_WORD).assertIsDisplayed()
        compose.onNodeWithText(COMPLETE_STAMP_WORD.uppercase()).assertIsDisplayed()
        compose.onNodeWithText("22 / 22").assertIsDisplayed()
        val ratio = compose.onNodeWithText("22 / 22").fetchSemanticsNode().boundsInRoot
        val word = compose.onNodeWithText(COMPLETE_STAMP_WORD.uppercase())
            .fetchSemanticsNode().boundsInRoot
        assertTrue("el ratio invade COMPLETA: $ratio / $word", ratio.bottom < word.top)
    }

    @Test
    fun aSlimHeadingOfASharedFolioStillCarriesTheStamp() {
        compose.setContent {
            CoindexTheme {
                CompositionLocalProvider(LocalDensity provides printDensity) {
                    PrintedCompletionStamp(heading = PrintHeading.Slim, ratio = "22/22")
                }
            }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription(COMPLETE_STAMP_WORD).assertIsDisplayed()
        compose.onNodeWithText(COMPLETE_STAMP_WORD.uppercase()).assertIsDisplayed()
        compose.onNodeWithText("22 / 22").assertIsDisplayed()
        val ratio = compose.onNodeWithText("22 / 22").fetchSemanticsNode().boundsInRoot
        val word = compose.onNodeWithText(COMPLETE_STAMP_WORD.uppercase())
            .fetchSemanticsNode().boundsInRoot
        assertTrue("el ratio slim invade COMPLETA: $ratio / $word", ratio.bottom < word.top)
    }
}
