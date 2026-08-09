package com.jenarvaezg.coindex.debug.calibration

import androidx.compose.ui.graphics.Color
import com.jenarvaezg.coindex.ui.components.AlbumToneConfig
import com.jenarvaezg.coindex.ui.components.DieCutWall
import com.jenarvaezg.coindex.ui.components.GRAIN_OPACITY
import com.jenarvaezg.coindex.ui.components.HOLE_CARD_PADDING_DP
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class CalibrationControl(
    val range: ClosedFloatingPointRange<Float>,
    /** Intermediate stops. The grain bakes its mosaic on every distinct value, so it steps. */
    val steps: Int = 0,
) {
    GRAIN_OPACITY(0f..1f, steps = 19),
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

    /**
     * Wider than the cardboard the die leaves free would put the wall on the photograph, so the
     * slider cannot ask for it (#357).
     */
    DIE_WALL_WIDTH_DP(1f..HOLE_CARD_PADDING_DP),
    DIE_WALL_SHADOW_ALPHA(0f..1f),
    DIE_WALL_SHEEN_ALPHA(0f..1f),
}

enum class CalibrationTab {
    EFFECTS,
    TONE,
}

data class CalibrationState(
    val grainOpacity: Float = GRAIN_OPACITY,
    val glossIntensity: Float = 0.5f,
    val glossTravelDp: Float = 55f,
    val flipDurationMillis: Int = 420,
    val stampingDurationMillis: Int = 300,
    val recessDepthDp: Float = 3f,
    val ghostOpacity: Float = 0.14f,
    val showGhost: Boolean = false,
    val selectedTab: CalibrationTab = CalibrationTab.EFFECTS,
    // The tone tab opens where production stands, the way the grain slider already does. It used
    // to open at #349's starting point, and a bench that shows a tone the app does not paint is a
    // bench that cannot tell you whether what you are looking at is the defect (#357).
    val cartoucheAlpha: Float = AlbumToneConfig.Default.cartoucheAlpha,
    val cardAlpha: Float = AlbumToneConfig.Default.cardAlpha,
    /** 0x87 is the tone whose ramp lands exactly on the calibrated `#878577`. */
    val hairlineTone: Int = 0x87,
    val cartoucheRuleAlpha: Float = AlbumToneConfig.Default.cartoucheTopRuleAlpha,
    val dieWall: DieCutWall = AlbumToneConfig.Default.dieWall,
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
            CalibrationControl.DIE_WALL_WIDTH_DP -> copy(dieWall = dieWall.copy(widthDp = value))
            CalibrationControl.DIE_WALL_SHADOW_ALPHA ->
                copy(dieWall = dieWall.copy(shadowAlpha = value))
            CalibrationControl.DIE_WALL_SHEEN_ALPHA ->
                copy(dieWall = dieWall.copy(sheenAlpha = value))
        }
    }

    fun withGhostShown(shown: Boolean): CalibrationState = copy(showGhost = shown)

    fun withTab(tab: CalibrationTab): CalibrationState = copy(selectedTab = tab)

    companion object {
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
    dieWall = dieWall,
)

/** Maps the accelerometer's lateral gravity to the gloss's signed ±45° travel. */
fun lateralTiltFraction(x: Float, y: Float, z: Float): Float {
    val degrees = Math.toDegrees(atan2(x.toDouble(), sqrt((y * y + z * z).toDouble())))
    return (degrees / 45.0).toFloat().coerceIn(-1f, 1f)
}
