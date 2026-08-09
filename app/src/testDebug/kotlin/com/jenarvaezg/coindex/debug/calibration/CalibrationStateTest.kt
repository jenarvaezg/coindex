package com.jenarvaezg.coindex.debug.calibration

import androidx.compose.ui.graphics.Color
import com.jenarvaezg.coindex.ui.components.AlbumToneConfig
import com.jenarvaezg.coindex.ui.components.CoinGloss
import com.jenarvaezg.coindex.ui.components.DieCutWall
import com.jenarvaezg.coindex.ui.components.GRAIN_OPACITY
import com.jenarvaezg.coindex.ui.components.GRAIN_TILE_DP
import com.jenarvaezg.coindex.ui.components.HOLE_CARD_PADDING_DP
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalibrationStateTest {
    @Test
    fun `approved values are the initial bench values`() {
        val state = CalibrationState()

        assertEquals(96f, GRAIN_TILE_DP)
        assertEquals(GRAIN_OPACITY, state.grainOpacity)
        assertEquals(105f, CoinGloss.Default.angleDegrees)
        assertEquals(CoinGloss.Default.intensity, state.glossIntensity)
        assertEquals(CoinGloss.Default.travel, state.glossTravel)
        assertEquals(420, state.flipDurationMillis)
        assertEquals(300, state.stampingDurationMillis)
        assertEquals(48.3f, CalibrationState.YEAR_TAG_WIDTH_DP)
        assertEquals(28f, CalibrationState.YEAR_TAG_HEIGHT_DP)
        assertEquals(0.14f, state.ghostOpacity)
        assertEquals(false, state.showGhost)
        assertEquals(CalibrationTab.EFFECTS, state.selectedTab)
        assertEquals(0x878577, state.hairlineColorRgb)
    }

    @Test
    fun `the tone tab opens exactly where production paints`() {
        // The bench used to open at the tones the app had before #349 calibrated them, so what it
        // showed was never what shipped. #357 was measured on the shipped drawing, not the bench's.
        assertEquals(AlbumToneConfig.Default, CalibrationState().albumToneConfig())
    }

    @Test
    fun `and so does the gloss, which is the same drawing the plate paints`() {
        assertEquals(CoinGloss.Default, CalibrationState().glossConfig())
    }

    @Test
    fun `the travel is a fraction of the diameter, with room above the approved value`() {
        assertEquals(0.7f, CalibrationControl.GLOSS_TRAVEL.range.endInclusive)
        assertTrue(CoinGloss.Default.travel < CalibrationControl.GLOSS_TRAVEL.range.endInclusive)
    }

    @Test
    fun `the wall of the die cannot be asked to cover the photograph`() {
        assertEquals(HOLE_CARD_PADDING_DP, CalibrationControl.DIE_WALL_WIDTH_DP.range.endInclusive)
    }

    @Test
    fun `controls update one value without changing the fixed geometry`() {
        val initial = CalibrationState()

        val adjusted = initial
            .withControl(CalibrationControl.GRAIN_OPACITY, 0.18f)
            .withControl(CalibrationControl.GLOSS_INTENSITY, 0.4f)
            .withControl(CalibrationControl.GLOSS_TRAVEL, 0.42f)
            .withControl(CalibrationControl.FLIP_DURATION_MILLIS, 510f)
            .withControl(CalibrationControl.STAMPING_DURATION_MILLIS, 260f)
            .withControl(CalibrationControl.RECESS_DEPTH_DP, 5f)
            .withControl(CalibrationControl.GHOST_OPACITY, 0.09f)

        assertEquals(0.18f, adjusted.grainOpacity)
        assertEquals(0.4f, adjusted.glossIntensity)
        assertEquals(0.42f, adjusted.glossTravel)
        assertEquals(510, adjusted.flipDurationMillis)
        assertEquals(260, adjusted.stampingDurationMillis)
        assertEquals(5f, adjusted.recessDepthDp)
        assertEquals(0.09f, adjusted.ghostOpacity)
        assertEquals(48.3f, CalibrationState.YEAR_TAG_WIDTH_DP)
        assertEquals(28f, CalibrationState.YEAR_TAG_HEIGHT_DP)
    }

    @Test
    fun `the same slot can expose the missing ghost state`() {
        assertEquals(true, CalibrationState().withGhostShown(true).showGhost)
    }

    @Test
    fun `controls clamp values to the range exposed by the HUD`() {
        val adjusted = CalibrationState()
            .withControl(CalibrationControl.GRAIN_OPACITY, 3f)
            .withControl(CalibrationControl.GLOSS_TRAVEL, -1f)
            .withControl(CalibrationControl.FLIP_DURATION_MILLIS, 1f)
            .withControl(CalibrationControl.GHOST_OPACITY, -2f)

        assertEquals(CalibrationControl.GRAIN_OPACITY.range.endInclusive, adjusted.grainOpacity)
        assertEquals(CalibrationControl.GLOSS_TRAVEL.range.start, adjusted.glossTravel)
        assertEquals(
            CalibrationControl.FLIP_DURATION_MILLIS.range.start.toInt(),
            adjusted.flipDurationMillis,
        )
        assertEquals(CalibrationControl.GHOST_OPACITY.range.start, adjusted.ghostOpacity)
    }

    @Test
    fun `tone controls update independently`() {
        val initial = CalibrationState()

        val cartouche = initial.withControl(CalibrationControl.CARTOUCHE_ALPHA, 0.55f)
        val card = initial.withControl(CalibrationControl.CARD_ALPHA, 0.31f)
        val hairline = initial.withControl(CalibrationControl.HAIRLINE_TONE, 159f)
        val rule = initial.withControl(CalibrationControl.CARTOUCHE_RULE_ALPHA, 0.18f)
        val width = initial.withControl(CalibrationControl.DIE_WALL_WIDTH_DP, 3.5f)
        val shadow = initial.withControl(CalibrationControl.DIE_WALL_SHADOW_ALPHA, 0.11f)
        val sheen = initial.withControl(CalibrationControl.DIE_WALL_SHEEN_ALPHA, 0.7f)

        assertEquals(0.55f, cartouche.cartoucheAlpha)
        assertEquals(initial.cardAlpha, cartouche.cardAlpha)
        assertEquals(initial.hairlineColorRgb, cartouche.hairlineColorRgb)
        assertEquals(initial.cartoucheRuleAlpha, cartouche.cartoucheRuleAlpha)

        assertEquals(0.31f, card.cardAlpha)
        assertEquals(initial.cartoucheAlpha, card.cartoucheAlpha)
        assertEquals(0x9F9B8B, hairline.hairlineColorRgb)
        assertEquals(initial.cartoucheRuleAlpha, hairline.cartoucheRuleAlpha)
        assertEquals(0.18f, rule.cartoucheRuleAlpha)
        assertEquals(initial.hairlineColorRgb, rule.hairlineColorRgb)

        assertEquals(3.5f, width.dieWall.widthDp)
        assertEquals(initial.dieWall.shadowAlpha, width.dieWall.shadowAlpha)
        assertEquals(0.11f, shadow.dieWall.shadowAlpha)
        assertEquals(initial.dieWall.sheenAlpha, shadow.dieWall.sheenAlpha)
        assertEquals(0.7f, sheen.dieWall.sheenAlpha)
        assertEquals(initial.dieWall.widthDp, sheen.dieWall.widthDp)
    }

    @Test
    fun `tone preview config mirrors all current control values`() {
        val state = CalibrationState(
            cartoucheAlpha = 0.81f,
            cardAlpha = 0.49f,
            hairlineTone = 135,
            cartoucheRuleAlpha = 0.31f,
            dieWall = DieCutWall(widthDp = 2.5f, shadowAlpha = 0.17f, sheenAlpha = 0.66f),
        )

        val tone = state.albumToneConfig()

        assertEquals(0.81f, tone.cartoucheAlpha)
        assertEquals(0.49f, tone.cardAlpha)
        assertEquals(Color(0xFF878577), tone.hairlineColor)
        assertEquals(0.31f, tone.cartoucheTopRuleAlpha)
        assertEquals(DieCutWall(widthDp = 2.5f, shadowAlpha = 0.17f, sheenAlpha = 0.66f), tone.dieWall)
    }

    @Test
    fun `tone controls clamp values and tabs remain independent`() {
        val adjusted = CalibrationState()
            .withTab(CalibrationTab.TONE)
            .withControl(CalibrationControl.CARTOUCHE_ALPHA, 2f)
            .withControl(CalibrationControl.CARD_ALPHA, -1f)
            .withControl(CalibrationControl.HAIRLINE_TONE, 300f)
            .withControl(CalibrationControl.CARTOUCHE_RULE_ALPHA, -1f)
            .withControl(CalibrationControl.DIE_WALL_WIDTH_DP, 40f)
            .withControl(CalibrationControl.DIE_WALL_SHADOW_ALPHA, 2f)
            .withControl(CalibrationControl.DIE_WALL_SHEEN_ALPHA, -3f)

        assertEquals(CalibrationTab.TONE, adjusted.selectedTab)
        assertEquals(1f, adjusted.cartoucheAlpha)
        assertEquals(0f, adjusted.cardAlpha)
        assertEquals(CalibrationControl.HAIRLINE_TONE.range.endInclusive.toInt(), adjusted.hairlineTone)
        assertEquals(0f, adjusted.cartoucheRuleAlpha)
        assertEquals(HOLE_CARD_PADDING_DP, adjusted.dieWall.widthDp)
        assertEquals(1f, adjusted.dieWall.shadowAlpha)
        assertEquals(0f, adjusted.dieWall.sheenAlpha)
    }
}
