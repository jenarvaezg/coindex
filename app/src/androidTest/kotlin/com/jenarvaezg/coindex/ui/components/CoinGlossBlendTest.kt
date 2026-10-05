package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The gloss shares the flip's `graphicsLayer` so the light turns with its face. Composited off
 * screen, `Softlight` over a transparent backdrop would paint an opaque streak over an empty hole
 * (a loading photograph, or a catalog PNG with a transparent background). With the default
 * compositing strategy it blends against the paper; an `alpha` or `RenderEffect` above the gloss
 * would break that.
 */
@RunWith(AndroidJUnit4::class)
class CoinGlossBlendTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun theBandBlendsAgainstThePaperAndNotAgainstNothing() {
        val paper = Color(0xFFDDD3BB)
        compose.setContent {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .background(paper)
                    .testTag("hole"),
            ) {
                // An empty layer: the case of a hole whose photograph has not arrived.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 0f }
                        .coinGloss(CoinGloss.Default, CoinTilt.Still),
                )
            }
        }

        val pixels = compose.onNodeWithTag("hole").captureToImage().toPixelMap()
        var darkest = 255
        var lightest = 0
        for (x in 0 until pixels.width) {
            for (y in 0 until pixels.height) {
                val level = (pixels[x, y].red * 255).toInt()
                darkest = minOf(darkest, level)
                lightest = maxOf(lightest, level)
            }
        }

        // Against the paper (221) the band's shadow lands around 206; against nothing it keeps its
        // own black at half alpha, around 110. Measured on the AVD both ways, forcing
        // `CompositingStrategy.Offscreen` for the second.
        assertTrue("el más oscuro fue $darkest", darkest > 150)
        assertTrue("el más claro fue $lightest", lightest < 250)
    }
}
