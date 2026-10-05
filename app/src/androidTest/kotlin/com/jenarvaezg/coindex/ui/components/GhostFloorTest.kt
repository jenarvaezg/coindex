package com.jenarvaezg.coindex.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

private const val PAINTS_MILLIS = 5_000L

/**
 * Below [GHOST_MIN_DP] a missing casilla drops its 14 % penumbra and only the dotted rule marks it
 * empty (#556): at the 34 dp of the country axis the ghost read as a grey disc.
 *
 * A black coin over the pale paper is dark when painted whole and faint at 14 %, so the same
 * photograph must land on opposite sides of [PENUMBRA_FLOOR] at 104 dp and at 34 dp.
 */
@RunWith(AndroidJUnit4::class)
class GhostFloorTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun aCasillaAboveTheFloorSinksItsDesign() {
        // Measured once: the rule's activity takes one content per test, so the message can't
        // measure again.
        val darkest = darkestAt(104f)

        assertTrue("el fantasma de 104 dp midió $darkest", darkest > PENUMBRA_FLOOR)
    }

    @Test
    fun anAxisHoleUnderTheFloorDrawsTheCoinWhole() {
        val darkest = darkestAt(34f)

        assertTrue("la casilla de 34 dp midió $darkest", darkest < PENUMBRA_FLOOR)
    }

    /**
     * The darkest level in the middle third of the hole. The dotted rule (ink at 48 %) is drawn in
     * both absences, so a corner-to-corner minimum would measure the rule instead.
     */
    private fun darkestAt(sideDp: Float): Int {
        // Wait for the photograph: a capture while Coil is still loading reads the stand-in disc,
        // which is the same at both sizes (#510).
        var painted = false
        compose.setContent {
            CoindexTheme {
                AlbumHole(
                    photo = blackCoin(),
                    absence = HoleAbsence.Missing,
                    onImageSettled = { painted = it },
                    modifier = Modifier.size(sideDp.dp).testTag(HOLE),
                )
            }
        }
        compose.waitUntil(PAINTS_MILLIS) { painted }
        val pixels = compose.onNodeWithTag(HOLE).captureToImage().toPixelMap()
        val from = pixels.width / 3
        val to = pixels.width - from
        var darkest = 255
        for (x in from until to) {
            for (y in from until to) {
                darkest = minOf(darkest, (pixels[x, y].red * 255).toInt())
            }
        }
        return darkest
    }

    /**
     * A black photograph that actually paints (written like `CoinGlossSurfacesTest`'s, #510), so
     * the pixel reads the alpha it is drawn at.
     */
    private fun blackCoin(): CoinPhoto {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "ghost-floor-coin.png")
        if (!file.exists()) {
            val bitmap = createBitmap(8, 8)
            bitmap.eraseColor(Color.BLACK)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        return CoinPhoto(thumbnail = file.toURI().toString(), picture = null)
    }

    private companion object {
        const val HOLE = "hole"

        /**
         * The paper is at 243 of 255 (#509); a black coin painted whole reads near 0 and at 14 %
         * over the paper around 209. This sits between the two, below any paper of the album.
         */
        const val PENUMBRA_FLOOR = 120
    }
}
