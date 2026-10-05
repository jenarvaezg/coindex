package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.domain.WishKey
import com.jenarvaezg.coindex.ui.CANCEL_ACTION
import com.jenarvaezg.coindex.ui.DrawnCell
import com.jenarvaezg.coindex.ui.WishLabels
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.ModeBand
import com.jenarvaezg.coindex.ui.components.PieceSelection
import com.jenarvaezg.coindex.ui.components.SelectionBand
import com.jenarvaezg.coindex.ui.components.SelectionDoor
import com.jenarvaezg.coindex.ui.boxDoorLabel
import com.jenarvaezg.coindex.ui.namePickedBoxLabel
import com.jenarvaezg.coindex.ui.plaqueOf
import com.jenarvaezg.coindex.ui.selectionHintLabel
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The cell of the plate on a 411 dp phone, the same three columns `PlateCellNameTest` measures. */
private val CELL_WIDTH = 113.dp

/** The 1 Bolívar, the reference plate for the casilla's layout. */
private const val A_TYPE = 10_338

private const val A_YEAR = "1886"

private val A_KEY = WishKey(typeId = A_TYPE, year = 1_886, issueId = null)

/** The two coins Coins is showing while the box is being made. */
private val SHOWN = listOf(A_TYPE, 4_242)

/**
 * A mode changes the scene and not only the gesture (#517): the band names the mode and holds the
 * way out wherever the sheet is scrolled, what the mode cannot touch answers nothing, and closing
 * it restores the screen's gestures. The visual change (deeper paper, faint casilla) is checked on
 * the AVD.
 */
@RunWith(AndroidJUnit4::class)
class ModeChangesTheSceneTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun theBandSaysWhatTheModeIsForAndHoldsTheWayOut() {
        var out = 0
        compose.setContent {
            CoindexTheme {
                ModeBand(sentence = WishLabels.MARK_HINT) {
                    CardAction(text = WishLabels.MARK_DONE_ACTION, onClick = { out += 1 })
                }
            }
        }

        compose.onNodeWithText(WishLabels.MARK_HINT).assertIsDisplayed()
        compose.onNodeWithText(WishLabels.MARK_DONE_ACTION).performClick()

        assertEquals(1, out)
    }

    /** The only click target is the mark, labelled for screen readers. */
    @Test
    fun anEmptyCasillaTakesTheMarkWhileTheModeIsOpen() {
        var marked: WishKey? = null
        compose.setContent {
            CoindexTheme {
                Casilla(picking = true, onMark = { marked = it })
            }
        }

        // One target only: the year's tag stands down too.
        val targets = compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        assertEquals(1, targets.size)
        assertEquals(
            WishLabels.MARK_ACTION,
            targets.single().config[SemanticsActions.OnClick].label,
        )

        compose.onAllNodes(hasClickAction()).onFirst().performClick()
        assertEquals(A_KEY, marked)
    }

    /** Neither the body (turns the coin over) nor the year (opens its sheet) answers. */
    @Test
    fun aFullCasillaAnswersNothingWhileTheModeIsOpen() {
        compose.setContent {
            CoindexTheme {
                Casilla(picking = true, missing = false)
            }
        }

        assertTrue(compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().isEmpty())
    }

    /** With the mode closed the year opens its coin's sheet again. */
    @Test
    fun closingTheModeGivesTheCasillaItsOwnGesturesBack() {
        var opened: Int? = null
        compose.setContent {
            CoindexTheme {
                Casilla(picking = false, missing = false, onOpenCoin = { opened = it })
            }
        }

        compose.onNodeWithText(A_YEAR).performClick()

        assertEquals(A_TYPE, opened)
    }

    /** In Coins the header's door gives way to the band at the foot, which doesn't scroll away. */
    @Test
    fun theDoorOfCoinsBecomesTheBandAndComesBack() {
        val selection = PieceSelection()
        compose.setContent {
            CoindexTheme {
                Column {
                    SelectionDoor(selection = selection, shown = SHOWN, seeded = false)
                    SelectionBand(
                        selection = selection,
                        existing = emptyList(),
                        taken = emptySet(),
                        shown = SHOWN,
                        seeded = false,
                        onCreate = { _, _ -> },
                        onAddTo = { _, _ -> },
                    )
                }
            }
        }

        val door = boxDoorLabel(seeded = false, shown = SHOWN.size)
        compose.onNodeWithText(door).performClick()

        compose.onNodeWithText(door).assertDoesNotExist()
        compose.onNodeWithText(selectionHintLabel(seeded = false, shown = SHOWN.size))
            .assertIsDisplayed()
        compose.onNodeWithText(namePickedBoxLabel(0)).assertIsDisplayed()

        compose.onNodeWithText(CANCEL_ACTION).performClick()

        compose.onNodeWithText(door).assertIsDisplayed()
        assertTrue(selection.typeIds.isEmpty())
    }

    @Composable
    private fun Casilla(
        picking: Boolean,
        missing: Boolean = true,
        onMark: (WishKey) -> Unit = {},
        onOpenCoin: (Int) -> Unit = {},
    ) {
        PlateCell(
            cell = DrawnCell(
                id = "casilla",
                label = "Bolívar",
                numistaTypeId = A_TYPE,
                footnote = null,
                year = A_YEAR,
                owned = !missing,
                missing = missing,
                wishKey = A_KEY,
                // The tag comes from the plaque, not from `year` (#511); without one there is no
                // year for the mode to take away.
                plaque = plaqueOf(label = "Bolívar", year = A_YEAR, yearIsCommon = false),
            ),
            images = null,
            printedSide = PrintedSide.Reverse,
            travellingFrom = null,
            onOpenCoin = onOpenCoin,
            onMark = if (picking) onMark else null,
            picking = picking,
            modifier = Modifier.width(CELL_WIDTH),
        )
    }
}
