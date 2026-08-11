package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.ui.CoinName
import com.jenarvaezg.coindex.ui.DrawnPiece
import com.jenarvaezg.coindex.ui.PiecesSubject
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.print.PrintCell
import com.jenarvaezg.coindex.ui.print.PrintSection
import com.jenarvaezg.coindex.ui.print.printGeometry
import com.jenarvaezg.coindex.ui.print.printPages
import com.jenarvaezg.coindex.ui.screens.NotebookPageSheet
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import com.jenarvaezg.coindex.domain.CollectedItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlbumCartoucheAlignmentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun everyAlbumCartoucheHasTheSameHeightAndCentersItsThemeSlot() {
        compose.setContent {
            CoindexTheme {
                Column {
                    AlbumCartouche(
                        CoinName("5 Deutsche Mark", null),
                        Modifier.width(120.dp).testTag("without-theme"),
                    )
                    AlbumCartouche(
                        CoinName("1 Dollar", "Ox"),
                        Modifier.width(120.dp).testTag("one-line-theme"),
                    )
                    AlbumCartouche(
                        CoinName("5 Euro", "D.Fernando II and Gloria Frigate"),
                        Modifier.width(120.dp).testTag("two-line-theme"),
                    )
                }
            }
        }

        val withoutTheme = compose.onNodeWithTag("without-theme").fetchSemanticsNode().boundsInRoot
        val oneLineTheme = compose.onNodeWithTag("one-line-theme").fetchSemanticsNode().boundsInRoot
        val twoLineTheme = compose.onNodeWithTag("two-line-theme").fetchSemanticsNode().boundsInRoot

        assertEquals(withoutTheme.height, oneLineTheme.height, 0.5f)
        assertEquals(withoutTheme.height, twoLineTheme.height, 0.5f)

        val oneLineCentre = compose.onNodeWithText("Ox").fetchSemanticsNode().boundsInRoot.center.y -
            oneLineTheme.top
        val twoLineCentre = compose.onNodeWithText("D.Fernando II and Gloria Frigate")
            .fetchSemanticsNode()
            .boundsInRoot
            .center.y - twoLineTheme.top
        assertEquals(oneLineCentre, twoLineCentre, 0.5f)
    }

    @Test
    fun aPrintedRowAlignsYearsAcrossAllThreeCartoucheCases() {
        val section = PrintSection(
            eyebrow = "COINDEX · COLECCIÓN",
            title = "Tres cartelas",
            subtitle = null,
            facts = emptyList(),
            source = "Prueba",
            cells = listOf(
                printedCell(CoinName("5 Deutsche Mark", null), "1900"),
                printedCell(CoinName("1 Dollar", "Ox"), "1901"),
                printedCell(CoinName("5 Euro", "D.Fernando II and Gloria Frigate"), "1902"),
            ),
        )
        val page = printPages(listOf(section), printGeometry(NotebookOptions())).single()

        compose.setContent {
            // The PDF renderer scales every millimetre uniformly. Density 1 keeps the whole A4
            // semantics tree inside even the 320 px test AVD without changing relative layout.
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                NotebookPageSheet(page = page, onImageSettled = {})
            }
        }

        val yearTops = listOf("1900", "1901", "1902").map { year ->
            compose.onNodeWithText(year).fetchSemanticsNode().boundsInRoot.top
        }
        assertEquals(yearTops[0], yearTops[1], 0.5f)
        assertEquals(yearTops[0], yearTops[2], 0.5f)
    }


    private fun printedCell(name: CoinName, year: String) = PrintCell(
        name = name,
        state = null,
        footnote = year,
        diameterMm = 20f,
        faces = listOf(CoinPhoto()),
        filled = true,
    )
}
