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
 * The floor under which «te falta» stops being said with a penumbra (#556).
 *
 * The ghost is one drawing at 14 % and the album draws its holes at two sizes: 104 dp on the plate, a
 * coin's own sheet and the shelf, and 34 dp on the country axis. At the small one the sunk design is not
 * a sentence — measured at 40 dp on the row of #520 it was two grey discs, which is what the owner
 * rejected with the prototype in front of him — so [GHOST_MIN_DP] withdraws it and the dotted rule says
 * the casilla is empty on its own.
 *
 * What is pinned here is the rule and not the number: a black coin over the album's pale paper is dark
 * where it paints whole and barely there at 14 %, so the same photograph at the two sizes has to come
 * back on opposite sides of the paper. The day the floor is dropped to nothing, the axis hole goes dark
 * and this test says so.
 */
@RunWith(AndroidJUnit4::class)
class GhostFloorTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun aCasillaAboveTheFloorSinksItsDesign() {
        // Measured once and held: the hole is drawn into the rule's own activity, which takes one
        // content per test, and an assertion message that measured again would be a second one.
        val darkest = darkestAt(104f)

        assertTrue("el fantasma de 104 dp midió $darkest", darkest > PENUMBRA_FLOOR)
    }

    @Test
    fun anAxisHoleUnderTheFloorDrawsTheCoinWhole() {
        val darkest = darkestAt(34f)

        assertTrue("la casilla de 34 dp midió $darkest", darkest < PENUMBRA_FLOOR)
    }

    /**
     * The darkest level the hole reaches, which is the photograph's own if it painted whole.
     *
     * Read off the middle third of the hole and not the whole of it: the dotted rule is ink at 48 % and
     * it is drawn in **both** absences, so a corner-to-corner minimum would be measuring the one mark
     * the floor does not move.
     */
    private fun darkestAt(sideDp: Float): Int {
        // The photograph has to be **on** the hole before anything is measured: a capture taken while
        // Coil is still working would read the stand-in disc, which is the same at both sizes and would
        // pass one of these two tests for the wrong reason (#510).
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
     * A photograph that actually paints, black so that the alpha it is drawn at is what the pixel says.
     *
     * Written to the test's own cache directory the way `CoinGlossSurfacesTest` writes its grey one:
     * since #510 what a hole draws is the photograph it **painted**, so a model that never arrives would
     * measure the stand-in disc at both sizes and pass for the wrong reason.
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
         * The album's paper is at 243 of 255 (#509) and its deep paper under a hole a little below. A
         * black coin painted whole lands on its own black; the same coin at 14 % over that paper lands
         * around 209. Half way between them separates the two outcomes with room either way, and it is
         * a level no paper of this album reaches on its own.
         */
        const val PENUMBRA_FLOOR = 120
    }
}
