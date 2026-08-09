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
 * The phone's own accelerometer, at `SENSOR_DELAY_UI`.
 *
 * A phone with no accelerometer — none of the two real ones, but an emulator can be told to have
 * none — simply never reports, and the sheet stays in its resting pose.
 */
class AccelerometerTiltSensor(context: Context) : TiltSensor, SensorEventListener {
    private val sensors = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var onGravity: ((Float, Float, Float) -> Unit)? = null

    override fun start(onGravity: (x: Float, y: Float, z: Float) -> Unit) {
        this.onGravity = onGravity
        accelerometer?.let { sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun stop() {
        sensors.unregisterListener(this)
        onGravity = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER || event.values.size < 3) return
        onGravity?.invoke(event.values[0], event.values[1], event.values[2])
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

/**
 * The tilt the whole app reads from, tied to the foreground.
 *
 * `LifecycleResumeEffect` is the second half of the ceiling — the first is a coin being on screen —
 * so the sensor is let go on `onPause` and picked up again on the way back.
 */
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
