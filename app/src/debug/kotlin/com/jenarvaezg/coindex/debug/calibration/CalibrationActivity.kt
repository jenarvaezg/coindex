package com.jenarvaezg.coindex.debug.calibration

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import com.jenarvaezg.coindex.ui.theme.CoindexTheme

class CalibrationActivity : ComponentActivity(), SensorEventListener {
    private val glossPositionFraction = mutableFloatStateOf(0f)
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        setContent {
            val position by glossPositionFraction
            CoindexTheme {
                CalibrationBenchScreen(glossPositionFraction = position)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        sensorManager.unregisterListener(this)
        super.onPause()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER || event.values.size < 3) return
        glossPositionFraction.floatValue = lateralTiltFraction(
            x = event.values[0],
            y = event.values[1],
            z = event.values[2],
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
