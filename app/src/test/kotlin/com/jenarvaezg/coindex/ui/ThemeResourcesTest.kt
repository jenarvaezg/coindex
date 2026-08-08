package com.jenarvaezg.coindex.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeResourcesTest {
    private val main = File("src/main")

    @Test
    fun `the app packages its own type families and never falls back to generic families`() {
        assertTrue(File(main, "res/font/bitter_variable.ttf").isFile)
        assertTrue(File(main, "res/font/barlow_condensed_regular.ttf").isFile)
        assertTrue(File(main, "res/font/barlow_condensed_semibold.ttf").isFile)

        val kotlin = File(main, "kotlin").walkTopDown()
            .filter { it.extension == "kt" }
            .joinToString("\n") { it.readText() }
        assertFalse(kotlin.contains("FontFamily.Serif"))
        assertFalse(kotlin.contains("FontFamily.SansSerif"))
        assertTrue(kotlin.contains("FontVariation.weight(400)"))
        assertTrue(kotlin.contains("fontFeatureSettings = \"'smcp', 'tnum'\""))
    }

    @Test
    fun `font fallback glyphs are replaced with drawings`() {
        val kotlin = File(main, "kotlin").walkTopDown()
            .filter { it.extension == "kt" }
            .joinToString("\n") { it.readText() }
        listOf(0x2190, 0x2713, 0x2197).map { it.toChar().toString() }.forEach { glyph ->
            assertFalse(kotlin.contains(glyph), glyph)
        }
    }

    @Test
    fun `system force dark is disabled for the paper theme`() {
        val theme = File(main, "res/values/themes.xml").readText()
        assertTrue(theme.contains("<item name=\"android:forceDarkAllowed\">false</item>"))
    }
}
