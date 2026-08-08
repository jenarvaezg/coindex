package com.jenarvaezg.coindex.debug.calibration

import androidx.compose.ui.graphics.Color
import com.jenarvaezg.coindex.ui.components.AlbumToneConfig
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class CalibrationControl(val range: ClosedFloatingPointRange<Float>) {
    GRAIN_OPACITY(0f..0.24f),
    GLOSS_INTENSITY(0f..1f),
    GLOSS_TRAVEL_DP(0f..96f),
    FLIP_DURATION_MILLIS(200f..900f),
    STAMPING_DURATION_MILLIS(100f..700f),
    RECESS_DEPTH_DP(0f..8f),
    GHOST_OPACITY(0f..0.3f),
    CARTOUCHE_ALPHA(0f..1f),
    CARD_ALPHA(0f..1f),
    HAIRLINE_TONE(32f..223f),
    CARTOUCHE_RULE_ALPHA(0f..1f),
}

enum class CalibrationTab {
    EFFECTS,
    TONE,
}

data class CalibrationState(
    val grainOpacity: Float = 0.08f,
    val glossIntensity: Float = 0.5f,
    val glossTravelDp: Float = 55f,
    val flipDurationMillis: Int = 420,
    val stampingDurationMillis: Int = 300,
    val recessDepthDp: Float = 3f,
    val ghostOpacity: Float = 0.14f,
    val showGhost: Boolean = false,
    val selectedTab: CalibrationTab = CalibrationTab.EFFECTS,
    val cartoucheAlpha: Float = 0.72f,
    val cardAlpha: Float = 87f / 255f,
    val hairlineTone: Int = 0x9F,
    val cartoucheRuleAlpha: Float = 0.24f,
) {
    val hairlineColorRgb: Int
        get() {
            val green = (155f + (hairlineTone - 159) * 22f / 24f).roundToInt()
            val blue = (139f + (hairlineTone - 159) * 20f / 24f).roundToInt()
            return (hairlineTone shl 16) or
                (green.coerceIn(0, 255) shl 8) or
                blue.coerceIn(0, 255)
        }

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
            CalibrationControl.CARTOUCHE_ALPHA -> copy(cartoucheAlpha = value)
            CalibrationControl.CARD_ALPHA -> copy(cardAlpha = value)
            CalibrationControl.HAIRLINE_TONE -> copy(hairlineTone = value.roundToInt())
            CalibrationControl.CARTOUCHE_RULE_ALPHA -> copy(cartoucheRuleAlpha = value)
        }
    }

    fun withGhostShown(shown: Boolean): CalibrationState = copy(showGhost = shown)

    fun withTab(tab: CalibrationTab): CalibrationState = copy(selectedTab = tab)

    companion object {
        const val GRAIN_MOSAIC_PX = 256
        const val GLOSS_ANGLE_DEGREES = 105f
        const val YEAR_TAG_WIDTH_DP = 48.3f
        const val YEAR_TAG_HEIGHT_DP = 28f
    }
}

internal fun CalibrationState.albumToneConfig(): AlbumToneConfig = AlbumToneConfig(
    cartoucheAlpha = cartoucheAlpha,
    cardAlpha = cardAlpha,
    hairlineColor = Color(0xFF000000 or hairlineColorRgb.toLong()),
    cartoucheTopRuleAlpha = cartoucheRuleAlpha,
)

/** Maps the accelerometer's lateral gravity to the gloss's signed ±45° travel. */
fun lateralTiltFraction(x: Float, y: Float, z: Float): Float {
    val degrees = Math.toDegrees(atan2(x.toDouble(), sqrt((y * y + z * z).toDouble())))
    return (degrees / 45.0).toFloat().coerceIn(-1f, 1f)
}
