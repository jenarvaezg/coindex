package com.jenarvaezg.coindex.ui.components

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Which way gravity is falling, sideways, in −1..1.
 *
 * It is the one input of the gloss (#303): the coin does not light up, the surface tilts. Zero is
 * the phone flat on the table — a defined pose and not «no effect» — and ±1 is the ±45° of lateral
 * tilt that saturate the travel.
 */
@Stable
interface CoinTilt {
    /**
     * Read in the draw phase and nowhere else, so a reading from the sensor repaints the coins
     * that are on screen instead of recomposing the sheet around them.
     */
    val lateral: Float

    /** A coin photograph came on screen; while any is there, the sensor is worth its battery. */
    fun coinAppeared() = Unit

    /** ...and left it. */
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

/**
 * The accelerometer, behind a seam.
 *
 * The policy above it — when it is worth registering — is the part worth defending in a test, and
 * `SensorManager` cannot be asked to fake a phone that has just been put face down on a desk.
 */
interface TiltSensor {
    fun start(onGravity: (x: Float, y: Float, z: Float) -> Unit)

    fun stop()
}

/**
 * Gravity, read while — and only while — somebody is looking at a coin.
 *
 * Two conditions and not one, both of them the ceiling #303 set and refused to fake: the app in the
 * foreground, and at least one coin photograph composed. A screen of prose registers nothing, and
 * neither does a plate the collector has left behind on his desk.
 *
 * Letting go also returns the sheet to rest, which matters for what comes back: a plate resumed
 * from a pause must not open with the light of wherever the phone was when it left.
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
 * How the phone's lean becomes the gloss's signed travel, and how much of the hand is filtered out.
 *
 * Both values are the bench's (ADR 0026 §15) and both were wrong in 1.0.0 — in the same direction,
 * which is why the effect read as *nervous*: a tiny signal under a large noise (#372).
 */
@Stable
data class TiltResponse(
    /**
     * The lean that spends the whole travel.
     *
     * It was 45°, which is the phone nearly on its edge. **A hand holding a phone leans it 10 to
     * 15°**, so in real use the gloss only ever spent a third of the travel it had and lived in the
     * middle of the coin. What the #338 bench approved is the travel in fractions of the diameter,
     * ±45 %, and that is untouched: this is what it costs to spend it.
     */
    val saturationDegrees: Float = 20f,
    /**
     * How much of a new reading is believed, per sample, in 0..1.
     *
     * A one-pole low-pass, which is the cheapest thing that turns a jittery series into a lean. At
     * `SENSOR_DELAY_UI` — about 16 samples a second — 0.18 settles a real turn in some four frames
     * and swallows the hand's tremor, which is uncorrelated between samples and therefore exactly
     * what an average kills. 1 is the unfiltered reading of 1.0.0.
     */
    val smoothing: Float = 0.18f,
) {
    companion object {
        val Default = TiltResponse()
    }
}

/**
 * Maps gravity's lateral component to the gloss's signed travel, saturating at [response].
 *
 * Only the component of gravity, which is why no gyroscope is asked for **here**: what the effect
 * wants to know is which way the sheet is leaning, not how fast it turned. Whether the reading
 * arrives already free of the hand is the sensor's business — see [AccelerometerTiltSensor], which
 * asks Android for `TYPE_GRAVITY` and lets the platform use the gyroscope if the phone has one.
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
 * The lean as it is drawn, once the tremor of the hand has been taken out of it.
 *
 * Every sample moves the drawn value a fraction of the way to the reading, so a turn arrives whole
 * within a few frames while a tremor — which changes sign between samples — averages to nothing.
 * It is the reading itself, not the raw gravity, that is filtered: filtering the axes and then
 * taking an angle would smooth the phone's pose rather than the coin's light.
 */
fun smoothedTilt(current: Float, reading: Float, response: TiltResponse): Float =
    current + (reading - current) * response.smoothing.coerceIn(0f, 1f)
