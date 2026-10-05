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

/** A face the catalogue holds but the phone can't reach, as on a plate off wifi. */
private val UNREACHABLE = CoinPhoto(thumbnail = "https://example.invalid/a-180.jpg", picture = null)

/** A face Numista has no picture for: nothing to download, so nothing to report. */
private val NO_PICTURE = CoinPhoto(thumbnail = null, picture = null)

/**
 * A hole at rest whose photograph did not arrive says so, and only then (#510). The URL is
 * unreachable on purpose: the picture exists but isn't on this phone, which must not look like a
 * picture still on its way.
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
        // The mark is about a download, not the catalogue: nothing was ever asked for here.
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
        // The mark is a still state, not motion, so it travels to paper (ADR 0026 §4, ADR 0029 §7).
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
     * Waits for the load to give up. Offline, Coil fails at once; here it waits on a DNS lookup for
     * a domain that can't resolve.
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
