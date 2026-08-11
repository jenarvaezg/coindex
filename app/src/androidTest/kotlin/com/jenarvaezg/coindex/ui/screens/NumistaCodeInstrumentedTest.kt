package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.ui.components.paperSurface
import com.jenarvaezg.coindex.ui.theme.Paper
import kotlin.math.abs
import kotlin.math.floor
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The QR's four-module quiet zone stays plain paper even when the notebook grain is behind it. */
@RunWith(AndroidJUnit4::class)
class NumistaCodeInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun quietZoneIsOpaquePaperOverTheSheetGrain() {
        var codeVisible by mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides printDensity) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .paperSurface()
                        .testTag("sample"),
                ) {
                    if (codeVisible) {
                        NumistaCode(
                            url = "https://es.numista.com/1885",
                            sideMm = 12f,
                            modifier = Modifier.testTag("qr"),
                        )
                    }
                }
            }
        }
        compose.waitForIdle()

        val grain = compose.onNodeWithTag("sample").captureToImage().toPixelMap()
        compose.runOnIdle { codeVisible = true }
        val qr = compose.onNodeWithTag("sample").captureToImage().toPixelMap()

        // Version 2 is 25 modules; four quiet modules on each side make 33. Sample the outer three
        // modules on all four edges, leaving the fourth as clearance from antialiased dark modules.
        val quietEdge = floor(qr.width * 3f / 33f).toInt().coerceAtLeast(1)
        val quietPixels = buildList {
            for (x in 0 until qr.width) {
                for (y in 0 until qr.height) {
                    if (
                        x < quietEdge || x >= qr.width - quietEdge ||
                        y < quietEdge || y >= qr.height - quietEdge
                    ) {
                        add(x to y)
                    }
                }
            }
        }
        val sampledBackdropHasGrain = quietPixels.any { (x, y) ->
            colorDistance(grain[x, y], Paper.paper) > 1f / 255f
        }
        assertTrue("la zona muestreada no contenía grano antes del QR", sampledBackdropHasGrain)
        val quietZoneIsPaper = quietPixels.all { (x, y) ->
            colorDistance(qr[x, y], Paper.paper) <= 1f / 255f
        }
        assertTrue("la zona de silencio deja ver el grano", quietZoneIsPaper)
    }

    private fun colorDistance(left: Color, right: Color): Float = maxOf(
        abs(left.red - right.red),
        abs(left.green - right.green),
        abs(left.blue - right.blue),
        abs(left.alpha - right.alpha),
    )
}
