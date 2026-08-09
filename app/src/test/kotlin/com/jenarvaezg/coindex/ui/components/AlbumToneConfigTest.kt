package com.jenarvaezg.coindex.ui.components

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class AlbumToneConfigTest {
    @Test
    fun `production album tones are the approved calibration`() {
        val tone = AlbumToneConfig()

        assertEquals(0.90f, tone.cartoucheAlpha)
        assertEquals(148f / 255f, tone.cardAlpha)
        assertEquals(Color(0xFF878577), tone.hairlineColor)
        assertEquals(0.34f, tone.cartoucheTopRuleAlpha)
    }

    @Test
    fun `the die-cut carries the geometry the bench calibrated`() {
        val tone = AlbumToneConfig()

        assertEquals(5f, tone.dieWallWidthDp)
        assertEquals(0.22f, tone.dieWallShadowAlpha)
        assertEquals(0.85f, tone.dieWallSheenAlpha)
    }
}
