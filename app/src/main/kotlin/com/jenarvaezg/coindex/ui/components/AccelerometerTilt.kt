package com.jenarvaezg.coindex.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * Which way gravity is falling, at `SENSOR_DELAY_UI`. `TYPE_GRAVITY` rather than the raw
 * accelerometer (#372): the accelerometer adds the hand's tremor, which is large against a useful
 * signal of some 15°, while `TYPE_GRAVITY` is fused by Android (with the gyroscope if there is
 * one). The accelerometer is the fallback, since composite sensors aren't guaranteed; with neither,
 * the sheet stays at rest.
 */
class AccelerometerTiltSensor(context: Context) : TiltSensor, SensorEventListener {
    private val sensors = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gravity = sensors.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var onGravity: ((Float, Float, Float) -> Unit)? = null

    override fun start(onGravity: (x: Float, y: Float, z: Float) -> Unit) {
        this.onGravity = onGravity
        gravity?.let { sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun stop() {
        sensors.unregisterListener(this)
        onGravity = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        // Both sensors report in the same axes and units.
        if (event.values.size < 3) return
        onGravity?.invoke(event.values[0], event.values[1], event.values[2])
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

/** The app's tilt, released on `onPause` and resumed on the way back. */
@Composable
fun rememberCoinTilt(): CoinTilt {
    val context = LocalContext.current.applicationContext
    val tilt = remember(context) { SensedCoinTilt(AccelerometerTiltSensor(context)) }
    LifecycleResumeEffect(tilt) {
        tilt.enteredForeground()
        onPauseOrDispose { tilt.leftForeground() }
    }
    return tilt
}
