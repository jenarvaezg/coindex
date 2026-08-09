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
 * Which way gravity is falling, at `SENSOR_DELAY_UI`.
 *
 * **`TYPE_GRAVITY` and not `TYPE_ACCELEROMETER`** (#372). The raw accelerometer measures gravity
 * *plus whatever the hand is doing*, and the gloss was reading the hand: a tremor of 0.3 m/s² is
 * 1.75° of apparent lean, sixteen times a second, on an effect whose whole useful signal is some
 * 15°. `TYPE_GRAVITY` is the composite sensor Android already fuses — with the gyroscope where the
 * phone has one, with its own low-pass where it does not — and it is literally the question the
 * gloss asks: which way is the sheet leaning. It costs no permission and it is API 9.
 *
 * The accelerometer stays as the fallback, because a composite sensor is not guaranteed to exist.
 * A phone with neither — an emulator can be told to have none — simply never reports, and the sheet
 * stays in its resting pose.
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
        // Whichever of the two answered: both report gravity in the same three axes and the same
        // units, and which one the phone gave us is not something the gloss has any use for.
        if (event.values.size < 3) return
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
