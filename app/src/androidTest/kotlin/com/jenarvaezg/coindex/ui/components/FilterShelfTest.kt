package com.jenarvaezg.coindex.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FilterShelfTest {
    @get:Rule
    val compose = createComposeRule()

    private fun shelfWithAction(
        summary: String = "Todas",
        tally: String = "47 colecciones",
        fontScale: Float = 1f,
    ) {
        compose.setContent {
            val scaled = Density(LocalDensity.current.density, fontScale)
            CompositionLocalProvider(LocalDensity provides scaled) {
                CoindexTheme {
                    FilterShelf(
                        summary = summary,
                        tally = tally,
                        expanded = false,
                        onToggle = {},
                        actionLabel = "Exportar láminas",
                        onAction = {},
                    ) {}
                }
            }
        }
    }

    /** The label itself, not the button that merges it. */
    private fun label(text: String) = compose.onNodeWithText(text, useUnmergedTree = true)

    /** The line of text the node draws, which is not always where the node is. */
    private fun textLayoutOf(text: String): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        label(text)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(layouts)
        return layouts.first()
    }

    /** Vertical centre of a one-line label's ink, in root pixels. */
    private fun inkCentreOf(text: String): Float {
        val laid = textLayoutOf(text)
        val top = label(text).fetchSemanticsNode().boundsInRoot.top
        return top + (laid.getLineTop(0) + laid.getLineBottom(0)) / 2f
    }

    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    @Test
    fun theTrailingActionIsTouchSizedAndSharesTheShelfLabelsVerticalCentre() {
        shelfWithAction()

        compose.onNodeWithText("Exportar láminas").assertHeightIsAtLeast(48.dp)

        // Ink, not boxes: a `heightIn(min = 48.dp)` on the tally once kept its node centred on the
        // action's while drawing the line 17 dp above it.
        val tallyInk = inkCentreOf("47 colecciones")
        val summaryInk = inkCentreOf("▸ Todas")
        val actionCentre = label("Exportar láminas").fetchSemanticsNode().boundsInRoot.center.y

        // Two pixels: the labels are two type sizes and round to the half-pixel in opposite
        // directions. The bug this guards against was 45 px off.
        assertEquals(actionCentre, tallyInk, 2f)
        assertEquals(summaryInk, tallyInk, 2f)
    }

    /** #416: a mid-dot before a bordered button read as a dangling seam. */
    @Test
    fun theTallyIsSeparatedFromTheActionByAirAndNotByAMidDot() {
        shelfWithAction()

        label(" · ").assertDoesNotExist()

        val tally = label("47 colecciones").getBoundsInRoot()
        // The button's box, not its label: the 14 dp of contentPadding inside the border isn't gap.
        val action = compose.onNodeWithText("Exportar láminas").getBoundsInRoot()
        val gap = action.left - tally.right
        assertEquals(SHELF_ACTION_GAP.value, gap.value, 0.5f)
    }

    /**
     * #416: the summary truncates by design so the count never does. The worst real pair is a
     * folded shelf on the country axis with two filters and a chosen sort.
     */
    @Test
    fun theCountRatherThanTheSummaryKeepsTheRoomItNeeds() {
        shelfWithAction(
            summary = "▸ 2 filtros · orden alfabético · Eje País",
            tally = "170 de 678 casillas",
        )

        val laid = textLayoutOf("170 de 678 casillas")

        // Against the intrinsic width rather than `hasVisualOverflow`, which compares with the
        // width the paragraph was offered (777 px here).
        assertEquals(1, laid.lineCount)
        assertTrue(
            "El recuento no cabe: caja=${laid.size.width}, texto=${laid.multiParagraph.maxIntrinsicWidth}",
            laid.size.width >= laid.multiParagraph.maxIntrinsicWidth,
        )
    }

    /** The case #414 asked for: one filter on the year axis of Monedas, chip name not cut. */
    @Test
    fun theNamedFilterOfOneChosenChipFitsBesideTheCount() {
        shelfWithAction(summary = "1 filtro · Año 1960 · Eje Año", tally = "6 de 191 tipos")

        val laid = textLayoutOf("▸ 1 filtro · Año 1960 · Eje Año")

        assertEquals(1, laid.lineCount)
        assertTrue(
            "El resumen no cabe: caja=${laid.size.width}, texto=${laid.multiParagraph.maxIntrinsicWidth}",
            laid.size.width >= laid.multiParagraph.maxIntrinsicWidth,
        )
    }

    /**
     * #416: at Android's largest font scale the count takes 160 of the 218 dp on its side of a
     * 411 dp shelf, so it needs no `softWrap = false`.
     */
    @Test
    fun theCountStaysOnOneLineAtTwiceTheTypeSize() {
        shelfWithAction(
            summary = "▸ 2 filtros · orden alfabético · Eje País",
            tally = "170 de 678 casillas",
            fontScale = 2f,
        )

        assertEquals(1, textLayoutOf("170 de 678 casillas").lineCount)
    }
}
