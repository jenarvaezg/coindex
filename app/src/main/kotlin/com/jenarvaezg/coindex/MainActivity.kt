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
                // Whether anything is allowed to move at all (#514), and where the light falls on
                // the metal (#338). Installed here and not inside the theme because this is the one
                // composable with a system behind it: the tilt sensor is registered while the
                // activity is resumed and there is a coin on screen, and both readings are the
                // device's rather than the collection's.
                //
                // **The gloss is the movement the scale cannot reach**, and the only one the app
                // has to answer for by itself: it follows the sensor and has no duration for a
                // factor to divide. So where quiet was asked for it is not slowed, it is not
                // registered — `CoinTilt.Still` is the phone on the table, a defined pose and not
                // an effect switched off halfway.
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
