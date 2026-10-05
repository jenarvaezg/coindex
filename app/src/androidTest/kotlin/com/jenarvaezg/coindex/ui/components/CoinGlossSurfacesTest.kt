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

/** Counts the coin photographs on screen that ask the tilt for light. */
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
 * Which surfaces gloss, observed as which ones register with the accelerometer: every coin
 * photograph does and empty cardboard never does (#303), and an exported sheet carries no gloss
 * (ADR 0026 §4).
 *
 * The glossing coins come from a file the test writes because since #510 a hole glosses only the
 * photograph it painted; the stand-in disc of an unreachable URL must stay matte.
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

    /** Waits until the picture is painted, which is when the hole registers with the sensor. */
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

    /** #510: the stand-in disc of an unreachable photograph used to gloss. */
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

    /** The ghost of a missing member is a printed design, not metal. */
    @Test
    fun aMissingCasillaDoesNotGloss() {
        val tilt = tiltOf {
            AlbumHole(
                photo = OBVERSE,
                absence = HoleAbsence.Missing,
                modifier = Modifier.size(104.dp),
            )
        }

        assertEquals(0, tilt.coins)
    }

    /** #520: `HoleAbsence.Wanted` draws the design whole, but the hole is still empty. */
    @Test
    fun aWantedCasillaDoesNotGlossEither() {
        val tilt = tiltOf {
            AlbumHole(
                photo = OBVERSE,
                absence = HoleAbsence.Wanted,
                modifier = Modifier.size(104.dp),
            )
        }

        assertEquals(0, tilt.coins)
    }

    /** Since #423 a loose piece is the album's own hole without its cardboard. */
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
        // Nor does a sheet rendered for export wake the sensor.
        assertEquals(0, tilt.coins)
    }

    /**
     * A photograph that actually paints, written once into the test's cache directory: Coil loads a
     * `file://` like any model, and the gloss hangs off [AsyncImagePainter.State.Success] (#510).
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
