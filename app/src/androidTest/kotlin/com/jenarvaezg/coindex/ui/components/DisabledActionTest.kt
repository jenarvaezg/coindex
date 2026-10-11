package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.ui.CANCEL_ACTION
import com.jenarvaezg.coindex.ui.ONBOARDING_SAVE_ACTION
import com.jenarvaezg.coindex.ui.boxDoorLabel
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import com.jenarvaezg.coindex.ui.theme.Paper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.max
import kotlin.math.min

/**
 * A disabled button is ruled in dashes, the mark of absence, and its label reads at 4.5:1 (#633).
 * Both are measured on paperDeep, the darkest paper a button sits on (the mode band).
 */
@RunWith(AndroidJUnit4::class)
class DisabledActionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun aDisabledPrimaryActionKeepsItsLabelLegible() {
        onDeepPaper { PrimaryAction(text = ONBOARDING_SAVE_ACTION, onClick = {}, enabled = false) }

        val ratio = labelContrast(ONBOARDING_SAVE_ACTION)

        assertTrue("PrimaryAction desactivado midió $ratio", ratio >= 4.5)
    }

    @Test
    fun aDisabledCardActionKeepsItsLabelLegible() {
        onDeepPaper { CardAction(text = EMPTY_DOOR, onClick = {}, enabled = false) }

        val ratio = labelContrast(EMPTY_DOOR)

        assertTrue("CardAction desactivado midió $ratio", ratio >= 4.5)
    }

    @Test
    fun aDisabledActionIsRuledInDashesWhereAnEnabledOneIsSolid() {
        onDeepPaper {
            Column {
                CardAction(text = CANCEL_ACTION, onClick = {})
                CardAction(text = EMPTY_DOOR, onClick = {}, enabled = false)
                PrimaryAction(text = ONBOARDING_SAVE_ACTION, onClick = {}, enabled = false)
            }
        }

        val enabled = strokesAlongTopEdge(CANCEL_ACTION)
        val door = strokesAlongTopEdge(EMPTY_DOOR)
        val primary = strokesAlongTopEdge(ONBOARDING_SAVE_ACTION)

        assertEquals("«Cancelar» activo", 1, enabled)
        assertTrue("CardAction desactivado: $door trazos", door >= 3)
        assertTrue("PrimaryAction desactivado: $primary trazos", primary >= 3)
    }

    private fun onDeepPaper(content: @Composable () -> Unit) {
        compose.setContent {
            CoindexTheme {
                Box(Modifier.background(Paper.paperDeep).padding(8.dp)) { content() }
            }
        }
    }

    private fun labelContrast(text: String): Double {
        val pixels = labelPixels(text)
        val darkest = pixels.minBy { it.luminance() }
        return contrast(darkest, mostFrequent(pixels))
    }

    /** The label's own node: the button's fill, or the paper through it, is what is behind it. */
    private fun labelPixels(text: String): List<Color> {
        val map = compose.onNodeWithText(text, useUnmergedTree = true).captureToImage().toPixelMap()
        return (0 until map.width).flatMap { x -> (0 until map.height).map { y -> map[x, y] } }
    }

    /**
     * Runs of rule along the button's topmost ruled row: one for a solid rule, one per dash for a
     * dashed one. The row is found rather than fixed, since Material pads its own target.
     */
    private fun strokesAlongTopEdge(text: String): Int {
        val map = compose.onNodeWithText(text).captureToImage().toPixelMap()
        val threshold = (Paper.paperDeep.luminance() + Paper.hairline.luminance()) / 2
        fun ruled(x: Int, y: Int) = map[x, y].luminance() < threshold
        val row = (0 until map.height).first { y ->
            (0 until map.width).count { x -> ruled(x, y) } * 3 >= map.width
        }
        return (0 until map.width).count { x -> ruled(x, row) && (x == 0 || !ruled(x - 1, row)) }
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
