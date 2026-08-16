package com.jenarvaezg.coindex.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.ui.screens.OffScreenSheet
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

private val OBVERSE = CoinPhoto(thumbnail = "https://example.invalid/a-180.jpg", picture = null)
private val REVERSE = CoinPhoto(thumbnail = "https://example.invalid/b-180.jpg", picture = null)

/** A tilt that only counts: whoever asks it for light is a coin photograph on screen. */
private class CountingTilt : CoinTilt {
    var coins = 0
        private set

    override val lateral = 0f

    override fun coinAppeared() {
        coins += 1
    }

    override fun coinLeft() {
        coins -= 1
    }
}

/**
 * Where the gloss goes, said as what asks the accelerometer for a reading.
 *
 * What is being defended is the rule of #303: **every coin photograph glosses and empty cardboard
 * never does**, and the export rule of ADR 0026 §4, which is one line in `OffScreenSheet` and this
 * test.
 *
 * The coins that must gloss are drawn from a file this test writes, and that is the point rather
 * than a convenience: since #510 a hole glosses the photograph it **painted**, not the one it was
 * given. An unreachable URL used to light the stand-in disc and follow the accelerometer while it
 * did, which is the brilliant disc of that ticket's own title.
 */
@RunWith(AndroidJUnit4::class)
class CoinGlossSurfacesTest {
    @get:Rule
    val compose = createComposeRule()

    private fun tiltOf(content: @Composable () -> Unit): CountingTilt {
        val tilt = CountingTilt()
        compose.setContent {
            CoindexTheme {
                CompositionLocalProvider(LocalCoinTilt provides tilt, content = content)
            }
        }
        compose.waitForIdle()
        return tilt
    }

    /** Waits for the picture to be on the hole, which is what the sensor now waits for too. */
    private fun awaitGloss(tilt: CountingTilt) =
        compose.waitUntil(PAINTS_MILLIS) { tilt.coins > 0 }

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun aCoinInItsHoleGlosses() {
        val tilt = tiltOf {
            AlbumHole(photo = onDisk(), otherSide = REVERSE, modifier = Modifier.size(104.dp))
        }
        awaitGloss(tilt)

        assertEquals(1, tilt.coins)
    }

    /** The disc of a photograph that never came is not metal, and #510 is that it looked like it. */
    @Test
    fun aPhotographThatNeverArrivedDoesNotGloss() {
        val tilt = tiltOf {
            AlbumHole(photo = OBVERSE, modifier = Modifier.size(104.dp))
        }
        compose.waitForIdle()

        assertEquals(0, tilt.coins)
    }

    @Test
    fun emptyCardboardNeverGlosses() {
        val tilt = tiltOf { AlbumHole(photo = null, modifier = Modifier.size(104.dp)) }

        assertEquals(0, tilt.coins)
    }

    /** The ghost of a member the collector does not have is a design, not metal. */
    @Test
    fun aMissingCasillaDoesNotGloss() {
        val tilt = tiltOf {
            AlbumHole(photo = OBVERSE, missing = true, modifier = Modifier.size(104.dp))
        }

        assertEquals(0, tilt.coins)
    }

    /**
     * A loose coin glosses too, and it is the same hole doing it.
     *
     * `PieceCard` painted both faces of a piece as flat squares until #423; the piece of a collection
     * is now the album's own hole without its cardboard, and one photograph asking for light is what
     * says so.
     */
    @Test
    fun aLoosePieceGlossesWithoutItsCardboard() {
        val tilt = tiltOf {
            AlbumHole(
                photo = onDisk(),
                otherSide = REVERSE,
                backed = false,
                modifier = Modifier.size(104.dp),
            )
        }
        awaitGloss(tilt)

        assertEquals(1, tilt.coins)
    }

    @Test
    fun aSheetComposedForExportCarriesNoGloss() {
        var gloss: CoinGloss? = CoinGloss.Default
        val tilt = CountingTilt()
        compose.setContent {
            CoindexTheme {
                CompositionLocalProvider(LocalCoinTilt provides tilt) {
                    OffScreenSheet(Density(1f)) {
                        gloss = LocalCoinGloss.current
                        AlbumHole(photo = OBVERSE, modifier = Modifier.size(104.dp))
                    }
                }
            }
        }
        compose.waitForIdle()

        assertNull(gloss)
        // And nothing on a sheet that is being photographed wakes the sensor either.
        assertEquals(0, tilt.coins)
    }

    /**
     * A photograph that actually paints, written once into the test's own cache directory.
     *
     * Not a fixture and not a network: Coil takes a `file://` like any other model, and what these
     * tests need is a hole that reaches [AsyncImagePainter.State.Success] — the state the gloss
     * hangs off since #510.
     */
    private fun onDisk(): CoinPhoto {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "gloss-coin.png")
        if (!file.exists()) {
            val bitmap = createBitmap(8, 8)
            bitmap.eraseColor(Color.GRAY)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        return CoinPhoto(thumbnail = file.toURI().toString(), picture = null)
    }

    private companion object {
        /** Room for a decode on an emulator, not a budget the app is held to. */
        const val PAINTS_MILLIS = 10_000L
    }
}
