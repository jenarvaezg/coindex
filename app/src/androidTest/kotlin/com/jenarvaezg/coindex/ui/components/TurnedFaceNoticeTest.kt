package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.ui.FACE_NOT_DOWNLOADED
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val REVERSE = CoinPhoto(thumbnail = "https://example.invalid/b-180.jpg", picture = null)
private val OBVERSE = CoinPhoto(thumbnail = "https://example.invalid/a-180.jpg", picture = null)

/** A casilla whose far face is declared by the catalogue but has no photograph to load at all. */
private val NO_PICTURE = CoinPhoto(thumbnail = null, picture = null)

/**
 * A hole turned to a face this phone hasn't downloaded says so instead of showing a mute disc
 * (#509). The URLs are unreachable on purpose: the photograph exists but hasn't arrived.
 */
@RunWith(AndroidJUnit4::class)
class TurnedFaceNoticeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun aTurnedHoleWithNothingBehindItSaysTheFaceHasNotArrived() {
        compose.setContent {
            CoindexTheme {
                AlbumHole(
                    photo = OBVERSE,
                    otherSide = REVERSE,
                    modifier = Modifier.size(HOLE).testTag("hole"),
                )
            }
        }

        compose.onNodeWithText(FACE_NOT_DOWNLOADED).assertDoesNotExist()

        compose.onNodeWithTag("hole").performClick()

        awaitNotice()
        compose.onNodeWithText(FACE_NOT_DOWNLOADED).assertIsDisplayed()
    }

    @Test
    fun aFaceWithNoPhotographAtAllSaysItToo() {
        // `printedPhoto(side.other)` always returns a `CoinPhoto`, so a casilla can turn onto a
        // face with no candidate at all; nothing ever settles there.
        compose.setContent {
            CoindexTheme {
                AlbumHole(
                    photo = OBVERSE,
                    otherSide = NO_PICTURE,
                    modifier = Modifier.size(HOLE).testTag("hole"),
                )
            }
        }

        compose.onNodeWithTag("hole").performClick()

        compose.onNodeWithText(FACE_NOT_DOWNLOADED).assertIsDisplayed()
    }

    @Test
    fun pressingItAgainBringsTheRestingFaceBack() {
        compose.setContent {
            CoindexTheme {
                AlbumHole(
                    photo = OBVERSE,
                    otherSide = REVERSE,
                    modifier = Modifier.size(HOLE).testTag("hole"),
                )
            }
        }

        compose.onNodeWithTag("hole").performClick()
        awaitNotice()

        compose.onNodeWithTag("hole").performClick()

        compose.onNodeWithText(FACE_NOT_DOWNLOADED).assertDoesNotExist()
    }

    /**
     * Waits for the load to give up. Offline, Coil fails at once; here it waits on a DNS lookup for
     * a domain that can't resolve.
     */
    private fun awaitNotice() = compose.waitUntil(GIVES_UP_MILLIS) {
        compose.onAllNodesWithText(FACE_NOT_DOWNLOADED).fetchSemanticsNodes().isNotEmpty()
    }

    private companion object {
        /** Room for a failing lookup on an emulator, not a budget the app is held to. */
        const val GIVES_UP_MILLIS = 10_000L

        /** The hole size of a lámina (`PlateScreen`), where #509 was found. */
        val HOLE = 104.dp
    }
}
