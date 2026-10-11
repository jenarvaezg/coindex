package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.ui.TURN_THE_COIN_OVER
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val OBVERSE = CoinPhoto(thumbnail = "https://example.invalid/a-180.jpg", picture = null)
private val REVERSE = CoinPhoto(thumbnail = "https://example.invalid/b-180.jpg", picture = null)

/**
 * What takes a tap in the pieces a casilla is drawn with: a hole turns the coin over only when it
 * is handed a second face, which only the coin's sheet does, and the year tag is a label
 * (ADR 0026 §3). The URLs are unreachable on purpose: only the targets matter here.
 */
@RunWith(AndroidJUnit4::class)
class CasillaTargetsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun theHoleAnnouncesWhatPressingItDoes() {
        compose.setContent {
            CoindexTheme {
                AlbumHole(
                    photo = OBVERSE,
                    otherSide = REVERSE,
                    modifier = Modifier.size(104.dp).testTag("hole"),
                )
            }
        }

        val click = compose.onNodeWithTag("hole")
            .fetchSemanticsNode()
            .config[SemanticsActions.OnClick]
        assertEquals(TURN_THE_COIN_OVER, click.label)
    }

    @Test
    // The off-screen sheet is never handed the other side, so an exported PNG can't show a turned
    // coin.
    fun aHoleWithoutASecondFaceTakesNoTap() {
        compose.setContent {
            CoindexTheme {
                AlbumHole(photo = OBVERSE, modifier = Modifier.size(104.dp).testTag("hole"))
            }
        }

        val node = compose.onNodeWithTag("hole").fetchSemanticsNode()
        assertFalse(node.config.contains(SemanticsActions.OnClick))
    }

    /** The whole casilla opens the sheet, so its tag takes no tap of its own. */
    @Test
    fun theYearTagTakesNoTap() {
        compose.setContent {
            CoindexTheme {
                RecessedYearTag(year = "1960", modifier = Modifier.testTag("tag"))
            }
        }

        val node = compose.onNodeWithTag("tag").fetchSemanticsNode()
        assertFalse(node.config.contains(SemanticsActions.OnClick))
    }
}
