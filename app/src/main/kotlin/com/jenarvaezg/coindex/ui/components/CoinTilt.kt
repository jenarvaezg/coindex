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
class SensedCoinTilt(private val sensor: TiltSensor) : CoinTilt {
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
            sensor.start { x, y, z -> reading.floatValue = lateralTiltFraction(x, y, z) }
        } else {
            sensor.stop()
            reading.floatValue = 0f
        }
    }
}

/**
 * Maps the accelerometer's lateral gravity to the gloss's signed ±45° travel.
 *
 * Only the component of gravity, which is why `TYPE_ACCELEROMETER` is enough and no gyroscope is
 * asked for: what the effect wants to know is which way the sheet is leaning, not how fast it turned.
 */
fun lateralTiltFraction(x: Float, y: Float, z: Float): Float {
    val degrees = Math.toDegrees(atan2(x.toDouble(), sqrt((y * y + z * z).toDouble())))
    return (degrees / 45.0).toFloat().coerceIn(-1f, 1f)
}
