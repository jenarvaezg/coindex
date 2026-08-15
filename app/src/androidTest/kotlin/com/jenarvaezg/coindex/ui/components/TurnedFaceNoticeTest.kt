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
 * A hole that comes round to a face this phone does not have says so (#509).
 *
 * The URLs are unreachable on purpose, which is exactly the state the audit of 14 August 2026 hit
 * with the prefetch pending: the photograph exists in the catalogue — all 848 types carry both
 * faces — and simply is not here yet. What the ticket refused is the mute disc that leaves behind.
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
        // A lámina asks for `printedPhoto(side.other)` and gets a `CoinPhoto` whichever way, so a
        // casilla can turn onto a face that has no candidate to try: nothing ever settles there,
        // and without this the disc would stay mute for good rather than for a while.
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
        // The third acceptance criterion of the ticket, and the reason the notice is not a dead end:
        // whatever the far face turned out to be, the way back is the same tap.
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
     * Waits for the load to give up, which is the thing being tested and is genuinely asynchronous.
     *
     * On a phone with no network Coil fails as soon as there is no socket to open, which is the
     * case the ticket is about; here it is a DNS lookup for a domain that cannot resolve, and that
     * takes as long as it takes.
     */
    private fun awaitNotice() = compose.waitUntil(GIVES_UP_MILLIS) {
        compose.onAllNodesWithText(FACE_NOT_DOWNLOADED).fetchSemanticsNodes().isNotEmpty()
    }

    private companion object {
        /** Room for a failing lookup on an emulator, not a budget the app is held to. */
        const val GIVES_UP_MILLIS = 10_000L

        /** The casilla of a lámina (`PlateScreen`), which is where the audit found the mute disc. */
        val HOLE = 104.dp
    }
}
