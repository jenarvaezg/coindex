package com.jenarvaezg.coindex.ui.components

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Which way gravity is falling, sideways, in −1..1: the gloss's only input (#303). Zero is the
 * phone flat on the table; ±1 is the lateral tilt that saturates the travel ([TiltResponse]).
 */
@Stable
interface CoinTilt {
    /** Read only in the draw phase, so a sensor reading repaints the coins without recomposing. */
    val lateral: Float

    /** A coin photograph came on screen; the sensor runs only while there is one. */
    fun coinAppeared() = Unit

    /** A coin photograph left the screen. */
    fun coinLeft() = Unit

    companion object {
        /** The phone on the table: what a preview, a test or an unprovided tree looks at. */
        val Still: CoinTilt = object : CoinTilt {
            override val lateral = 0f
        }
    }
}

/** Where the light is falling for this tree. */
val LocalCoinTilt = staticCompositionLocalOf { CoinTilt.Still }

/** The accelerometer behind a seam, so tests can drive the registration policy above it. */
interface TiltSensor {
    fun start(onGravity: (x: Float, y: Float, z: Float) -> Unit)

    fun stop()
}

/**
 * Gravity, read only while the app is in the foreground and at least one coin photograph is
 * composed (#303). Stopping also resets the reading, so a resumed plate doesn't open with stale
 * light.
 */
class SensedCoinTilt(
    private val sensor: TiltSensor,
    private val response: TiltResponse = TiltResponse.Default,
) : CoinTilt {
    private val reading = mutableFloatStateOf(0f)
    private var coins = 0
    private var foreground = false

    var listening = false
        private set

    override val lateral: Float
        get() = reading.floatValue

    override fun coinAppeared() {
        coins += 1
        sync()
    }

    override fun coinLeft() {
        coins = (coins - 1).coerceAtLeast(0)
        sync()
    }

    fun enteredForeground() {
        foreground = true
        sync()
    }

    fun leftForeground() {
        foreground = false
        sync()
    }

    private fun sync() {
        val wanted = foreground && coins > 0
        if (wanted == listening) return
        listening = wanted
        if (wanted) {
            sensor.start { x, y, z ->
                reading.floatValue = smoothedTilt(
                    current = reading.floatValue,
                    reading = lateralTiltFraction(x, y, z, response),
                    response = response,
                )
            }
        } else {
            sensor.stop()
            reading.floatValue = 0f
        }
    }
}

/**
 * How the phone's lean becomes the gloss's signed travel, and how much hand tremor is filtered out.
 * Both values are tunable on the bench (ADR 0026 §15, #372).
 */
@Stable
data class TiltResponse(
    /**
     * The lean that spends the whole travel. A hand leans a phone 10 to 15°, so a larger value
     * leaves the gloss stuck in the middle of the coin.
     */
    val saturationDegrees: Float = 20f,
    /**
     * How much of each new reading is taken, in 0..1: a one-pole low-pass. At `SENSOR_DELAY_UI`
     * (about 16 samples a second) 0.18 follows a real turn within a few samples and averages out
     * tremor. 1 is unfiltered.
     */
    val smoothing: Float = 0.18f,
) {
    companion object {
        val Default = TiltResponse()
    }
}

/**
 * Maps gravity's lateral component to the gloss's signed travel, saturating at [response]. The
 * effect needs the lean, not the rotation speed; [AccelerometerTiltSensor] already asks for
 * `TYPE_GRAVITY`, which can use the gyroscope.
 */
fun lateralTiltFraction(
    x: Float,
    y: Float,
    z: Float,
    response: TiltResponse = TiltResponse.Default,
): Float {
    val degrees = Math.toDegrees(atan2(x.toDouble(), sqrt((y * y + z * z).toDouble())))
    return (degrees / response.saturationDegrees).toFloat().coerceIn(-1f, 1f)
}

/**
 * The lean as drawn: each sample moves it a fraction of the way to the reading. The tilt fraction
 * is filtered rather than raw gravity, so what is smoothed is the light, not the phone's pose.
 */
fun smoothedTilt(current: Float, reading: Float, response: TiltResponse): Float =
    current + (reading - current) * response.smoothing.coerceIn(0f, 1f)
