package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.ui.TURN_THE_COIN_OVER
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val OBVERSE = CoinPhoto(thumbnail = "https://example.invalid/a-180.jpg", picture = null)
private val REVERSE = CoinPhoto(thumbnail = "https://example.invalid/b-180.jpg", picture = null)

/** Android's minimum touch target, as in the #302 map. */
private val MINIMUM_TARGET = 48.dp

/**
 * The two targets of a casilla: the hole turns the coin over and the year opens the coin's sheet
 * (#508). The URLs are unreachable on purpose: only the targets and their size matter here.
 */
@RunWith(AndroidJUnit4::class)
class CasillaTargetsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun aCasillaHasTwoTargetsAndBothAreBigEnoughToPress() {
        var opened = 0
        compose.setContent {
            CoindexTheme {
                Column {
                    AlbumHole(
                        photo = OBVERSE,
                        otherSide = REVERSE,
                        modifier = Modifier.size(104.dp).testTag("hole"),
                    )
                    RecessedYearTag(
                        year = "1960",
                        onOpen = { opened += 1 },
                        modifier = Modifier.testTag("tag"),
                    )
                }
            }
        }

        val minimum = with(compose.density) { MINIMUM_TARGET.toPx() }
        listOf("hole", "tag").forEach { tag ->
            val target = compose.onNodeWithTag(tag).fetchSemanticsNode().touchBoundsInRoot
            assertTrue("$tag: ${target.width} × ${target.height}", target.width >= minimum)
            assertTrue("$tag: ${target.width} × ${target.height}", target.height >= minimum)
        }

        compose.onNodeWithTag("tag").performClick()
        assertEquals(1, opened)
        compose.onNodeWithTag("hole").performClick()
        // Turning the coin over doesn't trigger the year's action.
        assertEquals(1, opened)
    }

    @Test
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

    /** An announced casilla has no page to open. */
    @Test
    fun theTagOfAnAnnouncedCasillaTakesNoTap() {
        compose.setContent {
            CoindexTheme {
                RecessedYearTag(year = "2027", onOpen = null, modifier = Modifier.testTag("tag"))
            }
        }

        val node = compose.onNodeWithTag("tag").fetchSemanticsNode()
        assertFalse(node.config.contains(SemanticsActions.OnClick))
    }
}
