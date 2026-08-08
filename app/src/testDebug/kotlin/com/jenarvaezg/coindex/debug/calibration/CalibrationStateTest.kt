package com.jenarvaezg.coindex.debug.calibration

import kotlin.test.Test
import kotlin.test.assertEquals

class CalibrationStateTest {
    @Test
    fun `approved values are the initial bench values`() {
        val state = CalibrationState()

        assertEquals(256, CalibrationState.GRAIN_MOSAIC_PX)
        assertEquals(105f, CalibrationState.GLOSS_ANGLE_DEGREES)
        assertEquals(0.5f, state.glossIntensity)
        assertEquals(55f, state.glossTravelDp)
        assertEquals(420, state.flipDurationMillis)
        assertEquals(300, state.stampingDurationMillis)
        assertEquals(48.3f, CalibrationState.YEAR_TAG_WIDTH_DP)
        assertEquals(28f, CalibrationState.YEAR_TAG_HEIGHT_DP)
        assertEquals(0.14f, state.ghostOpacity)
    }

    @Test
    fun `controls update one value without changing the fixed geometry`() {
        val initial = CalibrationState()

        val adjusted = initial
            .withControl(CalibrationControl.GRAIN_OPACITY, 0.18f)
            .withControl(CalibrationControl.GLOSS_INTENSITY, 0.4f)
            .withControl(CalibrationControl.GLOSS_TRAVEL_DP, 42f)
            .withControl(CalibrationControl.FLIP_DURATION_MILLIS, 510f)
            .withControl(CalibrationControl.STAMPING_DURATION_MILLIS, 260f)
            .withControl(CalibrationControl.RECESS_DEPTH_DP, 5f)
            .withControl(CalibrationControl.GHOST_OPACITY, 0.09f)

        assertEquals(0.18f, adjusted.grainOpacity)
        assertEquals(0.4f, adjusted.glossIntensity)
        assertEquals(42f, adjusted.glossTravelDp)
        assertEquals(510, adjusted.flipDurationMillis)
        assertEquals(260, adjusted.stampingDurationMillis)
        assertEquals(5f, adjusted.recessDepthDp)
        assertEquals(0.09f, adjusted.ghostOpacity)
        assertEquals(48.3f, CalibrationState.YEAR_TAG_WIDTH_DP)
        assertEquals(28f, CalibrationState.YEAR_TAG_HEIGHT_DP)
    }

    @Test
    fun `controls clamp values to the range exposed by the HUD`() {
        val adjusted = CalibrationState()
            .withControl(CalibrationControl.GRAIN_OPACITY, 3f)
            .withControl(CalibrationControl.GLOSS_TRAVEL_DP, -1f)
            .withControl(CalibrationControl.FLIP_DURATION_MILLIS, 1f)
            .withControl(CalibrationControl.GHOST_OPACITY, -2f)

        assertEquals(CalibrationControl.GRAIN_OPACITY.range.endInclusive, adjusted.grainOpacity)
        assertEquals(CalibrationControl.GLOSS_TRAVEL_DP.range.start, adjusted.glossTravelDp)
        assertEquals(
            CalibrationControl.FLIP_DURATION_MILLIS.range.start.toInt(),
            adjusted.flipDurationMillis,
        )
        assertEquals(CalibrationControl.GHOST_OPACITY.range.start, adjusted.ghostOpacity)
    }
}
