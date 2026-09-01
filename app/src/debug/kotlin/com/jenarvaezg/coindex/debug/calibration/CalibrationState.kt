package com.jenarvaezg.coindex.debug.calibration

import androidx.compose.ui.graphics.Color
import com.jenarvaezg.coindex.ui.components.AlbumToneConfig
import com.jenarvaezg.coindex.ui.components.CoinGloss
import com.jenarvaezg.coindex.ui.components.DieCutWall
import com.jenarvaezg.coindex.ui.components.GHOST_MIN_DP
import com.jenarvaezg.coindex.ui.components.GRAIN_OPACITY
import com.jenarvaezg.coindex.ui.components.HOLE_CARD_PADDING_DP
import kotlin.math.roundToInt

enum class CalibrationControl(
    val range: ClosedFloatingPointRange<Float>,
    /** Intermediate stops. The grain bakes its mosaic on every distinct value, so it steps. */
    val steps: Int = 0,
) {
    GRAIN_OPACITY(0f..1f, steps = 19),
    GLOSS_INTENSITY(0f..1f),

    /**
     * A fraction of the diameter and not dp: the prototype's ±55 dp were ±45 % of a 121 dp hole, and
     * the same dp on production's 104 dp casilla would be ±53 % — a band that spends more time off
     * the coin than on it. The ceiling leaves room above the approved ±45 % — a slider that opens at
     * 90 % of its own range can only be pushed one way.
     */
    GLOSS_TRAVEL(0f..0.7f),
    FLIP_DURATION_MILLIS(200f..900f),
    STAMPING_DURATION_MILLIS(100f..700f),
    RECESS_DEPTH_DP(0f..8f),
    GHOST_OPACITY(0f..0.3f),

    /**
     * The diameter the ghost is being read at, which is what #556 came to the bench to calibrate.
     *
     * The album draws its holes at two sizes and the penumbra was written for one of them: 104 dp on
     * the plate, the sheet and the shelf, and 34 dp on the country axis. The range covers both ends and
     * the 40 dp of #520's row in between — the size at which the sunk design measured as two grey discs
     * — so the floor can be found by walking it rather than argued.
     */
    GHOST_DIAMETER_DP(24f..104f),
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
    // The gloss opens where production stands, for the same reason the tone tab does (#357): a
    // bench showing a value the app does not paint cannot tell you whether what you see is the
    // defect.
    val glossIntensity: Float = CoinGloss.Default.intensity,
    val glossTravel: Float = CoinGloss.Default.travel,
    val flipDurationMillis: Int = 420,
    val stampingDurationMillis: Int = 300,
    val recessDepthDp: Float = 3f,
    val ghostOpacity: Float = 0.14f,
    // The ghost slot opens at the diameter production says is the floor, the way the gloss and the tone
    // open where production stands: a bench showing a size the app does not draw cannot tell you
    // whether what you are looking at is the defect (#357).
    val ghostDiameterDp: Float = GHOST_MIN_DP,
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
            CalibrationControl.GLOSS_TRAVEL -> copy(glossTravel = value)
            CalibrationControl.FLIP_DURATION_MILLIS -> copy(flipDurationMillis = value.roundToInt())
            CalibrationControl.STAMPING_DURATION_MILLIS ->
                copy(stampingDurationMillis = value.roundToInt())
            CalibrationControl.RECESS_DEPTH_DP -> copy(recessDepthDp = value)
            CalibrationControl.GHOST_OPACITY -> copy(ghostOpacity = value)
            CalibrationControl.GHOST_DIAMETER_DP -> copy(ghostDiameterDp = value)
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
        const val YEAR_TAG_WIDTH_DP = 48.3f
        const val YEAR_TAG_HEIGHT_DP = 28f
    }
}

/** What the bench is asking the production effect to draw right now. */
internal fun CalibrationState.glossConfig(): CoinGloss =
    CoinGloss(intensity = glossIntensity, travel = glossTravel)

internal fun CalibrationState.albumToneConfig(): AlbumToneConfig = AlbumToneConfig(
    cartoucheAlpha = cartoucheAlpha,
    cardAlpha = cardAlpha,
    hairlineColor = Color(0xFF000000 or hairlineColorRgb.toLong()),
    cartoucheTopRuleAlpha = cartoucheRuleAlpha,
    dieWall = dieWall,
)
