package com.jenarvaezg.coindex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jenarvaezg.coindex.ui.CoindexApp
import com.jenarvaezg.coindex.ui.CoindexViewModel
import com.jenarvaezg.coindex.ui.components.LocalCoinTilt
import com.jenarvaezg.coindex.ui.components.rememberCoinTilt
import com.jenarvaezg.coindex.ui.theme.CoindexTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as CoindexApplication).container
        setContent {
            CoindexTheme {
                // Where the light falls on the metal (#338). It is installed here and not inside
                // the theme because it is the activity that has a foreground to be in: the sensor
                // is registered while this window is resumed and there is a coin on screen, and
                // released on the way out.
                CompositionLocalProvider(LocalCoinTilt provides rememberCoinTilt()) {
                    CoindexApp(
                        viewModel = viewModel(factory = CoindexViewModel.factory(container)),
                    )
                }
            }
        }
    }
}
