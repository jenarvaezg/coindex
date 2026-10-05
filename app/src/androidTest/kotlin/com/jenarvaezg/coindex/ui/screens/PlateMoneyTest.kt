package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.ui.DrawnCell
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The cell of the plate on a 411 dp phone, the same three columns `PlateCellNameTest` measures. */
private val CELL_WIDTH = 113.dp

/** The hole the stamp is laid inside; the year's tag hangs off it (#473). */
private val HOLE = 104.dp

/** Any Numista type: it makes the tag clickable, as on real casillas. */
private const val A_TYPE = 10338

private const val VALUE_LINE = "Valor actual: 1.612 € · al mayor de tres precios"
private const val COST_LINE = "Coste de cerrar: 84 € · en sin circular"
private const val HOLE_COST = "84 €"

/**
 * The plate's two money figures and the price laid inside a hole, measured on the device (#493).
 * The structure came from an HTML prototype; only Bitter on Android can say whether the chip fits
 * inside its 104 dp hole without reaching the year's tag below.
 */
@RunWith(AndroidJUnit4::class)
class PlateMoneyTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun theStampOfAHoleStaysInsideItsOwnCardboard() {
        compose.setContent {
            CoindexTheme {
                Row {
                    Cell(year = "1886", missing = true, cost = HOLE_COST)
                }
            }
        }

        val cell = compose.onNodeWithTag("cell-1886", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val stamp = compose.onNodeWithText(HOLE_COST).fetchSemanticsNode().boundsInRoot
        val hole = with(compose.density) { HOLE.toPx() }

        // Inside the hole on all four sides: a chip wider than the cardboard would hang over the
        // casilla beside it, and one taller would reach the tag underneath.
        assertTrue("el sello se sale por arriba", stamp.top >= cell.top)
        assertTrue("el sello llega a la chapa del año", stamp.bottom <= cell.top + hole)
        assertTrue("el sello se sale por la izquierda", stamp.left >= cell.left)
        assertTrue("el sello se sale por la derecha", stamp.right <= cell.right)
        // Centred, so it reads as laid on the coin rather than pinned to an edge.
        assertEquals(cell.center.x, stamp.center.x, 1f)
        assertEquals(cell.top + hole / 2f, stamp.center.y, 1f)
    }

    @Test
    // A full casilla has a value, which the header shows, and no cost to draw over its coin.
    fun aFilledCasillaCarriesNoStamp() {
        compose.setContent {
            CoindexTheme {
                Row {
                    Cell(year = "1879", missing = false, cost = null)
                }
            }
        }

        compose.onNodeWithText(HOLE_COST).assertDoesNotExist()
    }

    @Test
    // The hierarchy is in the words, not the type: one statement about money, not two blocks.
    fun theTwoFiguresAreTwoLinesOfOneWeight() {
        compose.setContent { CoindexTheme { PlateMoneyLines(VALUE_LINE, COST_LINE) } }

        val value = compose.onNodeWithText(VALUE_LINE).fetchSemanticsNode().boundsInRoot
        val cost = compose.onNodeWithText(COST_LINE).fetchSemanticsNode().boundsInRoot

        assertEquals(value.height, cost.height, 0.5f)
        assertEquals(
            with(compose.density) { PLATE_MONEY_LINE_GAP.toPx() },
            cost.top - value.bottom,
            0.5f,
        )
    }

    @Test
    // The value line must read as well alone as with the cost line below it.
    fun aClosedPlateSaysOneLineAndNoZero() {
        compose.setContent { CoindexTheme { PlateMoneyLines(VALUE_LINE, cost = null) } }

        compose.onNodeWithText(VALUE_LINE).assertExists()
        compose.onNodeWithText("0 €", substring = true).assertDoesNotExist()
    }

    @Composable
    private fun Cell(year: String, missing: Boolean, cost: String?) {
        PlateCell(
            cell = DrawnCell(
                id = year,
                label = year,
                numistaTypeId = A_TYPE,
                footnote = null,
                year = year,
                owned = !missing,
                missing = missing,
                cost = cost,
            ),
            images = null,
            printedSide = PrintedSide.Obverse,
            travellingFrom = null,
            onOpenCoin = {},
            modifier = Modifier.width(CELL_WIDTH).testTag("cell-$year"),
        )
    }
}
