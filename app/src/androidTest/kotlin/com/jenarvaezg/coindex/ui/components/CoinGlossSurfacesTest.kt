package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.ui.screens.OffScreenSheet
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
 * The photographs never arrive — the URLs are unreachable on purpose — because none of this is about
 * the picture. What is being defended is the rule of #303: **every coin photograph glosses and empty
 * cardboard never does**, and the export rule of ADR 0026 §4, which is one line in `OffScreenSheet`
 * and this test.
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

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun aCoinInItsHoleGlosses() {
        val tilt = tiltOf {
            AlbumHole(photo = OBVERSE, otherSide = REVERSE, modifier = Modifier.size(104.dp))
        }

        assertEquals(1, tilt.coins)
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
                photo = OBVERSE,
                otherSide = REVERSE,
                backed = false,
                modifier = Modifier.size(104.dp),
            )
        }

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
}
