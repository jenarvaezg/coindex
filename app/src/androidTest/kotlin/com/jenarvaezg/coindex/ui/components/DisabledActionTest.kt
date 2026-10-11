package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.ui.CANCEL_ACTION
import com.jenarvaezg.coindex.ui.ONBOARDING_SAVE_ACTION
import com.jenarvaezg.coindex.ui.boxDoorLabel
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.max
import kotlin.math.min

/**
 * A button that can't be pressed yet keeps a label that reads at 4.5:1 (#633). The fill and the
 * rule say it is off, as the export panel's tick box does (#512).
 */
@RunWith(AndroidJUnit4::class)
class DisabledActionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun aDisabledPrimaryActionKeepsItsLabelLegible() {
        compose.setContent {
            CoindexTheme {
                PrimaryAction(text = ONBOARDING_SAVE_ACTION, onClick = {}, enabled = false)
            }
        }

        val ratio = labelContrast(ONBOARDING_SAVE_ACTION)

        assertTrue("PrimaryAction desactivado midió $ratio", ratio >= 4.5)
    }

    @Test
    fun aDisabledCardActionKeepsItsLabelLegible() {
        compose.setContent {
            CoindexTheme { CardAction(text = EMPTY_DOOR, onClick = {}, enabled = false) }
        }

        val ratio = labelContrast(EMPTY_DOOR)

        assertTrue("CardAction desactivado midió $ratio", ratio >= 4.5)
    }

    @Test
    fun aDisabledCardActionIsToldByItsFill() {
        compose.setContent {
            CoindexTheme {
                Column {
                    CardAction(text = CANCEL_ACTION, onClick = {})
                    CardAction(text = EMPTY_DOOR, onClick = {}, enabled = false)
                }
            }
        }

        val ratio = contrast(fillBehind(CANCEL_ACTION), fillBehind(EMPTY_DOOR))

        // paper against paperDeep is 1.22; the same paper on both is 1.
        assertTrue("los dos rellenos midieron $ratio", ratio >= 1.1)
    }

    private fun labelContrast(text: String): Double {
        val pixels = pixelsOf(text)
        val darkest = pixels.minBy { it.luminance() }
        return contrast(darkest, mostFrequent(pixels))
    }

    private fun fillBehind(text: String): Color = mostFrequent(pixelsOf(text))

    /** The label's own node, whose background is the button's fill rather than the page. */
    private fun pixelsOf(text: String): List<Color> {
        val map = compose.onNodeWithText(text, useUnmergedTree = true).captureToImage().toPixelMap()
        return (0 until map.width).flatMap { x -> (0 until map.height).map { y -> map[x, y] } }
    }

    private fun mostFrequent(pixels: List<Color>): Color =
        pixels.groupingBy { it.toArgb() }.eachCount().maxBy { it.value }.key.let(::Color)

    private fun contrast(first: Color, second: Color): Double {
        val a = first.luminance().toDouble()
        val b = second.luminance().toDouble()
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    private companion object {
        /** The door of Coins when a search finds nothing. */
        val EMPTY_DOOR = boxDoorLabel(seeded = true, shown = 0)
    }
}
