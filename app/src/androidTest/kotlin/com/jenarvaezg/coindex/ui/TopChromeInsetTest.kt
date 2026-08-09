package com.jenarvaezg.coindex.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.data.update.UpdateManifest
import com.jenarvaezg.coindex.data.update.UpdateStatus
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TopChromeInsetTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun bannerAloneInTheTopBarStartsBelowTheStatusBar() {
        var statusBarInset = 0f
        compose.setContent {
            CoindexTheme {
                statusBarInset = WindowInsets.statusBars.getTop(LocalDensity.current).toFloat()
                // The two album roots draw no masthead (ADR 0026 §1), so the banner is first in
                // the column with nothing above it to pay the strip — which is how #356 happened.
                TopChrome {
                    UpdateBanner(update = AVAILABLE, updating = false, onInstall = {})
                }
            }
        }

        val install = compose.onNodeWithText("Instalar").fetchSemanticsNode().boundsInRoot

        assertTrue(
            "Sin franja de barra de estado el test no comprueba nada: revisa el dispositivo",
            statusBarInset > 0f,
        )
        assertTrue(
            "«Instalar» arranca en ${install.top} px, dentro de la franja de $statusBarInset px",
            install.top >= statusBarInset,
        )
    }

    private companion object {
        val AVAILABLE = UpdateStatus.Available(
            manifest = UpdateManifest(
                versionCode = 29,
                versionName = "0.18.3",
                apkAsset = "coindex.apk",
                notes = "El banner deja de esconderse bajo el reloj.",
            ),
            apkUrl = "https://example.invalid/coindex.apk",
            apkSize = 1_024L,
        )
    }
}
