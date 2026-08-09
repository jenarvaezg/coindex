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
 * What the band blends against when the photograph is not there yet.
 *
 * The gloss lives inside the same `graphicsLayer` as the flip's `rotationY`, so that the light turns
 * with the face it belongs to. That raises a fair question: if the layer were composited off screen,
 * `Softlight` over a transparent backdrop degenerates into the source colour, and the band would
 * paint as an opaque black→white streak over the empty hole — while a picture is still loading, or
 * behind a catalog PNG with a transparent background, which this app knows exist.
 *
 * It does not: with the default compositing strategy the layer draws into the frame it is part of,
 * so the band blends against the paper underneath. The test pins that, because the day someone adds
 * an `alpha` or a `RenderEffect` above the gloss the layer *would* become an off-screen buffer and
 * the defect would appear where nobody is looking — on a slow photograph.
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

        // The paper is at 221. Blended against it, soft-light can only bend it: the shadow half of
        // the band lands around 206. Blended against nothing, the band keeps its own black at half
        // alpha and the same pixel lands around 110 — so 150 separates the two outcomes with room to
        // spare either way. (Measured on the AVD both ways, forcing `CompositingStrategy.Offscreen`
        // to see the defect appear.)
        assertTrue("el más oscuro fue $darkest", darkest > 150)
        assertTrue("el más claro fue $lightest", lightest < 250)
    }
}
