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

    /** The label itself and not the button that merges it, which is what these measurements are of. */
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

    /** Where the ink of a one-line label actually sits, in root pixels — `Paper.ink`, drawn. */
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

        // The ink and not the box: a `heightIn(min = 48.dp)` on the tally kept its **node** centred
        // on the action's while printing the line 17 dp above it, and a test that compared the two
        // boxes called that centred for as long as it was there.
        val tallyInk = inkCentreOf("47 colecciones")
        val summaryInk = inkCentreOf("▸ Todas")
        val actionCentre = label("Exportar láminas").fetchSemanticsNode().boundsInRoot.center.y

        // Two pixels of tolerance and not one: the labels are two type sizes, so their lines round
        // to the half-pixel in opposite directions. The failure this guards against was 45 px.
        assertEquals(actionCentre, tallyInk, 2f)
        assertEquals(summaryInk, tallyInk, 2f)
    }

    /**
     * The tally does not end in a punctuation mark that separates nothing (#416).
     *
     * A « · » between the count and a bordered button read as the leftover seam of an element that
     * had been removed — the shelf line ended in a dangling mid-dot in every state. The gap is what
     * separates them now, and the gap is measured here so it cannot silently close either.
     */
    @Test
    fun theTallyIsSeparatedFromTheActionByAirAndNotByAMidDot() {
        shelfWithAction()

        label(" · ").assertDoesNotExist()

        val tally = label("47 colecciones").getBoundsInRoot()
        // The button's own box and not its label: the air between them is what has to be there, and
        // the 14 dp of contentPadding inside the border is not air.
        val action = compose.onNodeWithText("Exportar láminas").getBoundsInRoot()
        val gap = action.left - tally.right
        assertEquals(SHELF_ACTION_GAP.value, gap.value, 0.5f)
    }

    /**
     * The tally survives the longest line the shelf can put beside it (#416).
     *
     * Naming the unit took the country axis from «170/678» to twenty characters, and the summary is
     * the one that has to give: it truncates by design, whereas half a count is worse than no count.
     * The worst real pair is a folded shelf on the country axis with two filters and a chosen sort.
     */
    @Test
    fun theCountRatherThanTheSummaryKeepsTheRoomItNeeds() {
        shelfWithAction(
            summary = "▸ 2 filtros · orden alfabético · Eje País",
            tally = "170 de 678 casillas",
        )

        val laid = textLayoutOf("170 de 678 casillas")

        // One line, and a box wide enough for the whole of it. Not `hasVisualOverflow`: it compares
        // the box against the width the paragraph was **offered** (777 px here), so it reads true
        // for every text that asks for less room than it was given.
        assertEquals(1, laid.lineCount)
        assertTrue(
            "El recuento no cabe: caja=${laid.size.width}, texto=${laid.multiParagraph.maxIntrinsicWidth}",
            laid.size.width >= laid.multiParagraph.maxIntrinsicWidth,
        )
    }

    /**
     * The line of #414 fits whole: «1 filtro · Año 1960 · Eje Año» beside its count.
     *
     * Naming the chosen chip is worth nothing if the name is the part that gets ellipsised, and the
     * summary is by design the one that gives way to the tally. This is the case the issue asked
     * for — one filter, on the year axis of Monedas — measured on the shelf and not in a string.
     */
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
     * And it survives a collector who reads the phone at twice the type size (#416).
     *
     * Android's largest real setting, and the count still lands on one line: doubled it takes 160
     * of the 218 dp left on this side of a 411 dp shelf, and the summary is truncated to the 58 dp
     * that remain — the order of sacrifice the weight was put there for. Measured, so that nobody
     * adds a `softWrap = false` here against a case this shelf does not have.
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
