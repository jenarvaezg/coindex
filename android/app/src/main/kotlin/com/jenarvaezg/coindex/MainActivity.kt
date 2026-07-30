package com.jenarvaezg.coindex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jenarvaezg.coindex.ui.CoindexApp
import com.jenarvaezg.coindex.ui.CoindexViewModel
import com.jenarvaezg.coindex.ui.theme.CoindexTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as CoindexApplication).container
        setContent {
            CoindexTheme {
                CoindexApp(
                    viewModel = viewModel(factory = CoindexViewModel.factory(container)),
                )
            }
        }
    }
}
