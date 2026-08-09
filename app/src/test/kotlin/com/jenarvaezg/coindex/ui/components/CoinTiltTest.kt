package com.jenarvaezg.coindex.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeTiltSensor : TiltSensor {
    var listening = false
        private set
    var starts = 0
        private set
    private var onGravity: ((Float, Float, Float) -> Unit)? = null

    override fun start(onGravity: (x: Float, y: Float, z: Float) -> Unit) {
        listening = true
        starts += 1
        this.onGravity = onGravity
    }

    override fun stop() {
        listening = false
        onGravity = null
    }

    fun tilt(x: Float, y: Float = 0f, z: Float = 9.81f) {
        onGravity?.invoke(x, y, z)
    }
}

/**
 * The ceiling #303 refused to fake and this block had to hold: **never awake outside the
 * foreground**, and never for a screen with no coin on it.
 */
class CoinTiltTest {
    @Test
    fun `the accelerometer sleeps until a coin is on screen`() {
        val sensor = FakeTiltSensor()
        val tilt = SensedCoinTilt(sensor)

        tilt.enteredForeground()

        assertFalse(sensor.listening)

        tilt.coinAppeared()

        assertTrue(sensor.listening)
    }

    @Test
    fun `a coin on a screen nobody is looking at does not wake it either`() {
        val sensor = FakeTiltSensor()
        val tilt = SensedCoinTilt(sensor)

        tilt.coinAppeared()

        assertFalse(sensor.listening)
    }

    @Test
    fun `it is released on pause with the coins still there, and the sheet goes back to rest`() {
        val sensor = FakeTiltSensor()
        val tilt = SensedCoinTilt(sensor)
        tilt.enteredForeground()
        tilt.coinAppeared()
        // Held over, not tapped: since #372 the lean is filtered, so it arrives over some frames.
        repeat(30) { sensor.tilt(x = 9.81f) }
        assertEquals(1f, tilt.lateral, 0.01f)

        tilt.leftForeground()

        assertFalse(sensor.listening)
        assertEquals(0f, tilt.lateral)
    }

    @Test
    fun `a plate of coins registers once, and the last one to leave turns it off`() {
        val sensor = FakeTiltSensor()
        val tilt = SensedCoinTilt(sensor)
        tilt.enteredForeground()

        repeat(22) { tilt.coinAppeared() }

        assertEquals(1, sensor.starts)

        repeat(21) { tilt.coinLeft() }

        assertTrue(sensor.listening)

        tilt.coinLeft()

        assertFalse(sensor.listening)
    }

    @Test
    fun `coming back to the foreground picks the sensor up again`() {
        val sensor = FakeTiltSensor()
        val tilt = SensedCoinTilt(sensor)
        tilt.enteredForeground()
        tilt.coinAppeared()
        tilt.leftForeground()

        tilt.enteredForeground()

        assertTrue(sensor.listening)
        assertEquals(2, sensor.starts)
    }

    @Test
    fun `lateral gravity becomes a signed position saturated at the declared lean`() {
        assertEquals(0f, lateralTiltFraction(x = 0f, y = 0f, z = 9.81f), 0.001f)
        assertEquals(1f, lateralTiltFraction(x = 100f, y = 0f, z = 1f), 0.001f)
        // 20° of lean, which is what a hand does, now spends the whole travel; 45° used to.
        val twenty = 9.81 * kotlin.math.tan(Math.toRadians(20.0))
        assertEquals(1f, lateralTiltFraction(twenty.toFloat(), 0f, 9.81f), 0.01f)
        assertEquals(-1f, lateralTiltFraction(-twenty.toFloat(), 0f, 9.81f), 0.01f)
    }

    /**
     * The whole of #372, said as a number: how much of a hand's tremor reaches the coin.
     *
     * A tremor is uncorrelated between samples — it changes sign — which is exactly what an average
     * kills and what the unfiltered reading of 1.0.0 painted frame for frame. The series below is a
     * phone held still at 8° with ±0.3 m/s² of hand on it, which is what `docs/ux/` measured the
     * gloss to be nervous at.
     */
    @Test
    fun `a tremor of the hand is swallowed, and a real lean still arrives`() {
        val held = 9.81 * kotlin.math.tan(Math.toRadians(8.0))
        val tremor = listOf(0.3, -0.28, 0.31, -0.3, 0.29, -0.31, 0.3, -0.29)

        val raw = tremor.map { lateralTiltFraction((held + it).toFloat(), 0f, 9.81f) }
        var smoothed = lateralTiltFraction(held.toFloat(), 0f, 9.81f)
        val filtered = raw.map { reading ->
            smoothed = smoothedTilt(smoothed, reading, TiltResponse.Default)
            smoothed
        }

        val rawSwing = raw.max() - raw.min()
        val filteredSwing = filtered.max() - filtered.min()
        // Better than four times quieter, which is what turns a jitter into a lean.
        assertTrue(
            filteredSwing < rawSwing / 4f,
            "el temblor pasa de $rawSwing a $filteredSwing, y debía caer a menos de un cuarto",
        )

        // And the lean itself still gets there: twelve samples is under a second at SENSOR_DELAY_UI.
        var arriving = 0f
        repeat(12) { arriving = smoothedTilt(arriving, 1f, TiltResponse.Default) }
        assertTrue(arriving > 0.9f, "una inclinación real llega al $arriving en doce muestras")
    }
}
