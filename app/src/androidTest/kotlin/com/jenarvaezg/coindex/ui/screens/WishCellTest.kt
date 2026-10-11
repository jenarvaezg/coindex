package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.domain.WishKey
import com.jenarvaezg.coindex.ui.DrawnWish
import com.jenarvaezg.coindex.ui.OPEN_THE_COIN_SHEET
import com.jenarvaezg.coindex.ui.TURN_THE_COIN_OVER
import com.jenarvaezg.coindex.ui.WishLabels
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The cell of the list on a 411 dp phone, the plate's three columns. */
private val CELL_WIDTH = 113.dp

private const val A_TYPE = 10_338

private const val A_NAME = "Bolívar"

private const val A_PLATE = "1 Bolívar"

/** Unreachable on purpose: two faces would make a hole turnable. */
private val TWO_FACES = TypeImages(
    obverse = CoinPhoto(thumbnail = "https://example.invalid/a-180.jpg"),
    reverse = CoinPhoto(thumbnail = "https://example.invalid/b-180.jpg"),
)

/**
 * A marked casilla in «Lo que busco» opens its coin's sheet like a plate's (ADR 0026 §3), and
 * «Quitar» stays outside it, so a tap that misses the casilla doesn't unmark it.
 */
@RunWith(AndroidJUnit4::class)
class WishCellTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun theNameOfAMarkedCasillaOpensItsCoin() {
        var opened = 0
        compose.setContent { CoindexTheme { Cell(onOpenCoin = { opened += 1 }) } }

        compose.onNodeWithText(A_NAME, useUnmergedTree = true).performClick()

        assertEquals(1, opened)
    }

    /** The lámina's name is part of the casilla too. */
    @Test
    fun theLaminaUnderAMarkedCasillaOpensItsCoin() {
        var opened = 0
        compose.setContent { CoindexTheme { Cell(onOpenCoin = { opened += 1 }) } }

        compose.onNodeWithText(A_PLATE, useUnmergedTree = true).performClick()

        assertEquals(1, opened)
    }

    /** A casilla with no year has no tag, and opens all the same. */
    @Test
    fun aMarkedCasillaWithNoYearStillOpensItsCoin() {
        var opened = 0
        compose.setContent { CoindexTheme { Cell(year = null, onOpenCoin = { opened += 1 }) } }

        compose.onNodeWithText(A_NAME, useUnmergedTree = true).performClick()

        assertEquals(1, opened)
    }

    @Test
    fun quitarUnmarksWithoutOpeningAndSharesNoAreaWithTheCasilla() {
        var opened = 0
        var removed = 0
        compose.setContent {
            CoindexTheme { Cell(onOpenCoin = { opened += 1 }, onRemove = { removed += 1 }) }
        }

        compose.onNodeWithText(WishLabels.REMOVE_ACTION).performClick()

        assertEquals(1, removed)
        assertEquals(0, opened)
        val casilla = targets().single { it.config[SemanticsActions.OnClick].label == OPEN_THE_COIN_SHEET }
        val quitar = compose.onNodeWithText(WishLabels.REMOVE_ACTION).fetchSemanticsNode()
        assertFalse(
            "${casilla.touchBoundsInRoot} y ${quitar.touchBoundsInRoot} se pisan",
            casilla.touchBoundsInRoot.overlaps(quitar.touchBoundsInRoot),
        )
    }

    /** The coin turns over in its sheet, not in the list. */
    @Test
    fun theHoleOfAMarkedCasillaDoesNotTurnTheCoin() {
        compose.setContent { CoindexTheme { Cell(onOpenCoin = {}) } }

        val labels = targets().map { it.config[SemanticsActions.OnClick].label }
        assertTrue(labels.toString(), TURN_THE_COIN_OVER !in labels)
        assertTrue(labels.toString(), OPEN_THE_COIN_SHEET in labels)
    }

    private fun targets() = compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()

    @Composable
    private fun Cell(
        year: String? = "1886",
        onOpenCoin: () -> Unit,
        onRemove: () -> Unit = {},
    ) {
        WishCell(
            row = DrawnWish(
                id = "wish",
                key = WishKey(typeId = A_TYPE, year = 1_886, issueId = null),
                typeId = A_TYPE,
                label = A_NAME,
                year = year,
                plate = A_PLATE,
                printedSide = PrintedSide.Reverse,
                cost = null,
            ),
            images = TWO_FACES,
            onOpenCoin = onOpenCoin,
            onRemove = onRemove,
            modifier = Modifier.width(CELL_WIDTH),
        )
    }
}
