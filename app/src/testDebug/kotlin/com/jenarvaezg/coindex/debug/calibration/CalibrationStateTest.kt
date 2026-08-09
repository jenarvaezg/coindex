package com.jenarvaezg.coindex.debug.calibration

import androidx.compose.ui.graphics.Color
import com.jenarvaezg.coindex.ui.components.GRAIN_OPACITY
import kotlin.test.Test
import kotlin.test.assertEquals

class CalibrationStateTest {
    @Test
    fun `approved values are the initial bench values`() {
        val state = CalibrationState()

        assertEquals(96f, CalibrationState.GRAIN_TILE_DP)
        assertEquals(GRAIN_OPACITY, state.grainOpacity)
        assertEquals(105f, CalibrationState.GLOSS_ANGLE_DEGREES)
        assertEquals(0.5f, state.glossIntensity)
        assertEquals(55f, state.glossTravelDp)
        assertEquals(420, state.flipDurationMillis)
        assertEquals(300, state.stampingDurationMillis)
        assertEquals(48.3f, CalibrationState.YEAR_TAG_WIDTH_DP)
        assertEquals(28f, CalibrationState.YEAR_TAG_HEIGHT_DP)
        assertEquals(0.14f, state.ghostOpacity)
        assertEquals(false, state.showGhost)
        assertEquals(CalibrationTab.EFFECTS, state.selectedTab)
        assertEquals(0.72f, state.cartoucheAlpha)
        assertEquals(87f / 255f, state.cardAlpha)
        assertEquals(0x9F9B8B, state.hairlineColorRgb)
        assertEquals(0.24f, state.cartoucheRuleAlpha)
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
    fun `the same slot can expose the missing ghost state`() {
        assertEquals(true, CalibrationState().withGhostShown(true).showGhost)
    }

    @Test
    fun `lateral acceleration becomes a signed gloss position saturated at 45 degrees`() {
        assertEquals(0f, lateralTiltFraction(x = 0f, y = 0f, z = 9.81f), 0.001f)
        assertEquals(1f, lateralTiltFraction(x = 9.81f, y = 0f, z = 9.81f), 0.001f)
        assertEquals(-1f, lateralTiltFraction(x = -9.81f, y = 0f, z = 9.81f), 0.001f)
        assertEquals(1f, lateralTiltFraction(x = 100f, y = 0f, z = 1f), 0.001f)
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

    @Test
    fun `tone controls update independently`() {
        val initial = CalibrationState()

        val cartouche = initial.withControl(CalibrationControl.CARTOUCHE_ALPHA, 0.9f)
        val card = initial.withControl(CalibrationControl.CARD_ALPHA, 0.58f)
        val hairline = initial.withControl(CalibrationControl.HAIRLINE_TONE, 135f)
        val rule = initial.withControl(CalibrationControl.CARTOUCHE_RULE_ALPHA, 0.34f)

        assertEquals(0.9f, cartouche.cartoucheAlpha)
        assertEquals(initial.cardAlpha, cartouche.cardAlpha)
        assertEquals(initial.hairlineColorRgb, cartouche.hairlineColorRgb)
        assertEquals(initial.cartoucheRuleAlpha, cartouche.cartoucheRuleAlpha)

        assertEquals(0.58f, card.cardAlpha)
        assertEquals(initial.cartoucheAlpha, card.cartoucheAlpha)
        assertEquals(0x878577, hairline.hairlineColorRgb)
        assertEquals(initial.cartoucheRuleAlpha, hairline.cartoucheRuleAlpha)
        assertEquals(0.34f, rule.cartoucheRuleAlpha)
        assertEquals(initial.hairlineColorRgb, rule.hairlineColorRgb)
    }

    @Test
    fun `tone preview config mirrors all current control values`() {
        val state = CalibrationState(
            cartoucheAlpha = 0.81f,
            cardAlpha = 0.49f,
            hairlineTone = 135,
            cartoucheRuleAlpha = 0.31f,
        )

        val tone = state.albumToneConfig()

        assertEquals(0.81f, tone.cartoucheAlpha)
        assertEquals(0.49f, tone.cardAlpha)
        assertEquals(Color(0xFF878577), tone.hairlineColor)
        assertEquals(0.31f, tone.cartoucheTopRuleAlpha)
    }

    @Test
    fun `tone controls clamp values and tabs remain independent`() {
        val adjusted = CalibrationState()
            .withTab(CalibrationTab.TONE)
            .withControl(CalibrationControl.CARTOUCHE_ALPHA, 2f)
            .withControl(CalibrationControl.CARD_ALPHA, -1f)
            .withControl(CalibrationControl.HAIRLINE_TONE, 300f)
            .withControl(CalibrationControl.CARTOUCHE_RULE_ALPHA, -1f)

        assertEquals(CalibrationTab.TONE, adjusted.selectedTab)
        assertEquals(1f, adjusted.cartoucheAlpha)
        assertEquals(0f, adjusted.cardAlpha)
        assertEquals(CalibrationControl.HAIRLINE_TONE.range.endInclusive.toInt(), adjusted.hairlineTone)
        assertEquals(0f, adjusted.cartoucheRuleAlpha)
    }
}
