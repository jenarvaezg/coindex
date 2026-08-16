package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.ui.PHOTO_NOT_DOWNLOADED
import com.jenarvaezg.coindex.ui.screens.OffScreenSheet
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A face the catalogue does hold, on a phone that cannot reach it — the state of a plate off wifi. */
private val UNREACHABLE = CoinPhoto(thumbnail = "https://example.invalid/a-180.jpg", picture = null)

/** A face Numista has no picture for at all: there is nothing to bring and nothing to say. */
private val NO_PICTURE = CoinPhoto(thumbnail = null, picture = null)

/**
 * A hole at rest whose photograph did not arrive says so, and only then (#510).
 *
 * The URL is unreachable on purpose, which is the state the audit of 14 August 2026 found with the
 * prefetch pending: the picture exists in the catalogue and is simply not on this phone. What the
 * ticket refused is that this look identical to a picture that is still on its way.
 */
@RunWith(AndroidJUnit4::class)
class PhotoNotDownloadedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun aHoleAtRestWhosePhotographDidNotArriveSaysSoWithoutBeingTurned() {
        compose.setContent {
            CoindexTheme {
                AlbumHole(photo = UNREACHABLE, modifier = Modifier.size(HOLE))
            }
        }

        awaitMark()
        compose.onNodeWithContentDescription(PHOTO_NOT_DOWNLOADED).assertIsDisplayed()
    }

    @Test
    fun aTypeWithNoPictureInNumistaKeepsTheStandInDisc() {
        // The second acceptance criterion: the mark is about a download and not about a catalogue.
        // Nothing was ever asked for here, so nothing is being waited for either.
        compose.setContent {
            CoindexTheme {
                AlbumHole(photo = NO_PICTURE, modifier = Modifier.size(HOLE))
            }
        }

        compose.waitForIdle()
        compose.onNodeWithContentDescription(PHOTO_NOT_DOWNLOADED).assertDoesNotExist()
    }

    @Test
    fun theMarkTravelsToPaper() {
        // ADR 0026 §4 as ADR 0029 §7 reads it: what is still travels, and «alive» is what follows
        // the finger, the sensor or the navigation. The mark is a state and does none of the three
        // — and a plate exported with no pictures says why it is empty instead of eleven mute discs.
        compose.setContent {
            CoindexTheme {
                OffScreenSheet(Density(1f)) {
                    AlbumHole(photo = UNREACHABLE, modifier = Modifier.size(HOLE))
                }
            }
        }

        awaitMark()
        compose.onNodeWithContentDescription(PHOTO_NOT_DOWNLOADED).assertExists()
    }

    /**
     * Waits for the load to give up, which is the thing being tested and is genuinely asynchronous.
     *
     * On a phone with no network Coil fails as soon as there is no socket to open; here it is a DNS
     * lookup for a domain that cannot resolve, and that takes as long as it takes.
     */
    private fun awaitMark() = compose.waitUntil(GIVES_UP_MILLIS) {
        compose.onAllNodesWithContentDescription(PHOTO_NOT_DOWNLOADED)
            .fetchSemanticsNodes().isNotEmpty()
    }

    private companion object {
        /** Room for a failing lookup on an emulator, not a budget the app is held to. */
        const val GIVES_UP_MILLIS = 10_000L

        /** The casilla of a lámina, of a card and of the ficha: one hole, one size (#370). */
        val HOLE = 104.dp
    }
}
