package com.jenarvaezg.coindex.ui.theme

import androidx.compose.ui.graphics.Color
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SinglePaletteTest {
    private val main = File("src/main")

    @Test
    fun `the app keeps one opaque warm-paper palette in every system theme`() {
        assertEquals(Color(0xFFEEE8D7), Paper.paper)
        assertEquals(1f, Paper.paper.alpha)

        val ui = File(main, "kotlin/com/jenarvaezg/coindex/ui").walkTopDown()
            .filter { it.extension == "kt" }
            .joinToString("\n") { it.readText() }
        assertFalse(ui.contains("darkColorScheme"))
        assertFalse(ui.contains("isSystemInDarkTheme"))

        val platformTheme = File(main, "res/values/themes.xml").readText()
        assertTrue(platformTheme.contains("<item name=\"android:forceDarkAllowed\">false</item>"))
    }

    @Test
    fun `muted text and hairlines clear their contrast thresholds on paper`() {
        assertTrue(contrastRatio(Paper.muted, Paper.paper) >= 4.5)
        assertTrue(contrastRatio(Paper.hairline, Paper.paper) >= 3.0)
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val light = max(relativeLuminance(first), relativeLuminance(second))
        val dark = min(relativeLuminance(first), relativeLuminance(second))
        return (light + 0.05) / (dark + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        fun linear(channel: Float): Double {
            val value = channel.toDouble()
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }

        return 0.2126 * linear(color.red) +
            0.7152 * linear(color.green) +
            0.0722 * linear(color.blue)
    }
}
