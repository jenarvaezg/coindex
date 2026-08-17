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

/** The father's 1 Bolívar, which is the plate every map of the casilla was measured on. */
private const val A_TYPE = 10_338

private const val A_YEAR = "1886"

private val A_KEY = WishKey(typeId = A_TYPE, year = 1_886, issueId = null)

/** The two coins Coins is showing while the box is being made. */
private val SHOWN = listOf(A_TYPE, 4_242)

/**
 * A mode changes the scene and not only the gesture (#517).
 *
 * «Marcar lo que busco» and «Hacer una colección» used to be a line of small print in a header that
 * scrolls away: two taps down the plate no frame of that screen could tell you which of two apps you
 * were in, and a full casilla went on turning its coin over as if nothing had been opened.
 *
 * What is checked here is what a test can hold of that change: the band says the mode and holds the
 * way out wherever the sheet is scrolled to, what the mode cannot touch answers nothing at all, and
 * closing gives the screen its meanings back. The change of air itself — the paper a shade deeper,
 * the ghost of the mark, the faint casilla — is drawing, and it is measured on the AVD.
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

    /** While the mode is open the hole marks, and it says so where a screen reader can hear it. */
    @Test
    fun anEmptyCasillaTakesTheMarkWhileTheModeIsOpen() {
        var marked: WishKey? = null
        compose.setContent {
            CoindexTheme {
                Casilla(picking = true, onMark = { marked = it })
            }
        }

        // One target on the casilla and no other: the year's tag has stood down with everything else.
        val targets = compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        assertEquals(1, targets.size)
        assertEquals(
            WishLabels.MARK_ACTION,
            targets.single().config[SemanticsActions.OnClick].label,
        )

        compose.onAllNodes(hasClickAction()).onFirst().performClick()
        assertEquals(A_KEY, marked)
    }

    /**
     * A full casilla has nothing to mark, so while the mode is open it answers nothing.
     *
     * Neither the body — which turns the coin over the rest of the time — nor the year, which opens
     * the coin's sheet. The sheet means one thing at a time, and this is the other half of the step
     * back its ink makes.
     */
    @Test
    fun aFullCasillaAnswersNothingWhileTheModeIsOpen() {
        compose.setContent {
            CoindexTheme {
                Casilla(picking = true, missing = false)
            }
        }

        assertTrue(compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().isEmpty())
    }

    /** With the mode closed the casilla is what it always was: the year opens its coin's sheet. */
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

    /**
     * In Coins the mode is one thing at a time too: the door, or the band that replaces it.
     *
     * The door lives in the header and the band at the foot, which is the whole of the fix — the
     * sentence and the way out used to scroll away with the filters.
     */
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
                // What the tag says is the subject's rule and no longer the `year` field (#511):
                // a casilla built without a plaque has no year for the mode to take away.
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
