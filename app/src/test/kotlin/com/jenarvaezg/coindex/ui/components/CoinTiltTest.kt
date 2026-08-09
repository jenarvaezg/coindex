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
        sensor.tilt(x = 9.81f)
        assertEquals(1f, tilt.lateral, 0.001f)

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
    fun `lateral acceleration becomes a signed position saturated at 45 degrees`() {
        assertEquals(0f, lateralTiltFraction(x = 0f, y = 0f, z = 9.81f), 0.001f)
        assertEquals(1f, lateralTiltFraction(x = 9.81f, y = 0f, z = 9.81f), 0.001f)
        assertEquals(-1f, lateralTiltFraction(x = -9.81f, y = 0f, z = 9.81f), 0.001f)
        assertEquals(1f, lateralTiltFraction(x = 100f, y = 0f, z = 1f), 0.001f)
    }
}
