package com.jenarvaezg.coindex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jenarvaezg.coindex.ui.CoindexApp
import com.jenarvaezg.coindex.ui.CoindexViewModel
import com.jenarvaezg.coindex.ui.components.CoinTilt
import com.jenarvaezg.coindex.ui.components.LocalCoinTilt
import com.jenarvaezg.coindex.ui.components.LocalMotion
import com.jenarvaezg.coindex.ui.components.rememberCoinTilt
import com.jenarvaezg.coindex.ui.components.rememberSystemMotion
import com.jenarvaezg.coindex.ui.theme.CoindexTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as CoindexApplication).container
        setContent {
            CoindexTheme {
                // Whether anything may move (#514), and where the light falls on the metal (#338).
                // Provided here rather than in the theme because both come from the device: the
                // tilt sensor is registered while the activity is resumed with a coin on screen.
                //
                // The gloss follows the sensor and has no duration for the system's animation
                // scale to divide, so with motion off the sensor isn't registered at all and the
                // coin rests in `CoinTilt.Still`.
                val moving = rememberSystemMotion()
                CompositionLocalProvider(
                    LocalCoinTilt provides if (moving) rememberCoinTilt() else CoinTilt.Still,
                    LocalMotion provides moving,
                ) {
                    CoindexApp(
                        viewModel = viewModel(factory = CoindexViewModel.factory(container)),
                    )
                }
            }
        }
    }
}
