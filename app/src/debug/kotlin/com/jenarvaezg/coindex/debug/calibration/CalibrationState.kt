package com.jenarvaezg.coindex.debug.calibration

import kotlin.math.roundToInt

enum class CalibrationControl(val range: ClosedFloatingPointRange<Float>) {
    GRAIN_OPACITY(0f..0.24f),
    GLOSS_INTENSITY(0f..1f),
    GLOSS_TRAVEL_DP(0f..96f),
    FLIP_DURATION_MILLIS(200f..900f),
    STAMPING_DURATION_MILLIS(100f..700f),
    RECESS_DEPTH_DP(0f..8f),
    GHOST_OPACITY(0f..0.3f),
}

data class CalibrationState(
    val grainOpacity: Float = 0.08f,
    val glossIntensity: Float = 0.5f,
    val glossTravelDp: Float = 55f,
    val flipDurationMillis: Int = 420,
    val stampingDurationMillis: Int = 300,
    val recessDepthDp: Float = 3f,
    val ghostOpacity: Float = 0.14f,
) {
    fun withControl(control: CalibrationControl, requestedValue: Float): CalibrationState {
        val value = requestedValue.coerceIn(control.range)
        return when (control) {
            CalibrationControl.GRAIN_OPACITY -> copy(grainOpacity = value)
            CalibrationControl.GLOSS_INTENSITY -> copy(glossIntensity = value)
            CalibrationControl.GLOSS_TRAVEL_DP -> copy(glossTravelDp = value)
            CalibrationControl.FLIP_DURATION_MILLIS -> copy(flipDurationMillis = value.roundToInt())
            CalibrationControl.STAMPING_DURATION_MILLIS ->
                copy(stampingDurationMillis = value.roundToInt())
            CalibrationControl.RECESS_DEPTH_DP -> copy(recessDepthDp = value)
            CalibrationControl.GHOST_OPACITY -> copy(ghostOpacity = value)
        }
    }

    companion object {
        const val GRAIN_MOSAIC_PX = 256
        const val GLOSS_ANGLE_DEGREES = 105f
        const val YEAR_TAG_WIDTH_DP = 48.3f
        const val YEAR_TAG_HEIGHT_DP = 28f
    }
}
