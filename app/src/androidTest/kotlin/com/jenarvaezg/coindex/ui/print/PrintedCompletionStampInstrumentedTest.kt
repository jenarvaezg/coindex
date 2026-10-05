package com.jenarvaezg.coindex.ui.print

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.ui.COMPLETE_STAMP_WORD
import com.jenarvaezg.coindex.ui.recordInto
import com.jenarvaezg.coindex.ui.screens.printDensity
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import com.jenarvaezg.coindex.ui.theme.Paper
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A square of paper wide enough to hold the tilted stamp with margin on every side. */
private const val SHEET_MM = 40f

/**
 * The rubber stamp on paper (#371): the word reaches the PDF on the stamp itself, not through the
 * Progress row. Drawing is in millimetres under [printDensity], so this checks the ink, not the
 * page packer.
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

    /**
     * #476: the tests above read the semantics tree, which stayed green while the stamp printed an
     * empty frame. This replays the export path (a [Picture] onto a bitmap) and looks for ink in
     * the middle of the frame, where «n / n» and «COMPLETA» are.
     */
    @Test
    fun theInkOfTheStampReachesTheMiddleOfItsFrame() {
        val picture = Picture()
        compose.setContent {
            CoindexTheme {
                CompositionLocalProvider(LocalDensity provides printDensity) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(Dp(SHEET_MM))
                            .background(Paper.paper)
                            .recordInto(picture),
                    ) {
                        PrintedCompletionStamp(heading = PrintHeading.Masthead, ratio = "19/19")
                    }
                }
            }
        }
        compose.waitForIdle()

        val sheet = Bitmap.createBitmap(
            picture.width,
            picture.height,
            Bitmap.Config.ARGB_8888,
        )
        Canvas(sheet).drawPicture(picture)

        // The middle third: inside the double rule whatever the tilt and clear of both frames, so
        // only the two lines of type can ink it.
        val third = picture.width / 3
        val paper = sheet.getPixel(1, 1)
        var inked = 0
        for (x in third until third * 2) {
            for (y in third until third * 2) {
                if (sheet.getPixel(x, y) != paper) inked += 1
            }
        }

        assertTrue("el sello imprime un marco vacío: $inked píxeles de tinta", inked > 0)
    }
}
