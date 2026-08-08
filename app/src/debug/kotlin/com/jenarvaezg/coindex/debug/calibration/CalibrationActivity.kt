package com.jenarvaezg.coindex.debug.calibration

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jenarvaezg.coindex.ui.theme.CoindexTheme

class CalibrationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CoindexTheme {
                CalibrationBenchScreen()
            }
        }
    }
}
