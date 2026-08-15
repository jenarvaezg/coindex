package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.ui.COIN_VIEW_ON_NUMISTA
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.CoinName
import com.jenarvaezg.coindex.ui.DrawnCell
import com.jenarvaezg.coindex.ui.components.FichaRefresh
import com.jenarvaezg.coindex.ui.shelf.CoinClaim
import com.jenarvaezg.coindex.ui.shelf.CoinRow
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The cell of the plate on a 411 dp phone, the same three columns `PlateCellNameTest` measures. */
private val CELL_WIDTH = 113.dp

/** The father's 1 Bolívar, which is the plate every map of the casilla was measured on. */
private const val A_TYPE = 10_338

private const val A_TITLE = "1 Bolívar - Simón Bolívar"

private val A_COIN = CoinRow(
    typeId = A_TYPE,
    name = CoinName("1 Bolívar", "Simón Bolívar"),
    rawTitle = A_TITLE,
    issuer = "Venezuela",
    years = listOf(1_886),
    objectClass = ObjectClass.Coin,
    weightOz = null,
    // A hole: the casilla is empty and the collector holds no piece of the type at all.
    quantity = 0,
    claims = emptyList(),
    unclaimedPieces = 0,
)

/**
 * The year of a casilla opens the coin's sheet **inside the app** (#508).
 *
 * The audit of 14 August 2026 left Chrome three times without meaning to: of the two targets of a
 * casilla (ADR 0026 §3) the body turned the coin over and the year left for Numista, and nothing on
 * the sunken tag said which was which — no arrow fits on it (#298, #302). What the tag hands over now
 * is a **type**, not a URL: there is no address in this composable for a browser to be given.
 */
@RunWith(AndroidJUnit4::class)
class CasillaOpensTheSheetTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun theYearOfACasillaHandsOverItsCoinAndNotAnAddress() {
        var opened: Int? = null
        compose.setContent {
            CoindexTheme {
                Casilla(numistaTypeId = A_TYPE, onOpenCoin = { opened = it })
            }
        }

        compose.onNodeWithText("1886").performClick()

        assertEquals(A_TYPE, opened)
    }

    /** An announced member is not in the catalogue: there is no ficha to open and no tap to take. */
    @Test
    fun theTagOfAnAnnouncedCasillaOpensNothing() {
        var opened: Int? = null
        compose.setContent {
            CoindexTheme {
                Casilla(numistaTypeId = null, onOpenCoin = { opened = it })
            }
        }

        val node = compose.onNodeWithText("2027").fetchSemanticsNode()
        assertFalse(node.config.contains(SemanticsActions.OnClick))
        assertNull(opened)
    }

    /**
     * The sheet a casilla opens leaves the app through one labelled door and no other.
     *
     * «Ver en Numista ↗» is the arrow the tag could never carry, and it is on the one element of this
     * sheet that goes anywhere near a browser.
     */
    @Test
    fun theSheetOffersNumistaBehindItsOwnArrow() {
        var left: Int? = null
        compose.setContent {
            CoindexTheme {
                Sheet(coin = A_COIN, onOpenNumista = { left = it })
            }
        }

        compose.onNodeWithText(A_TITLE).assertExists()
        compose.onNodeWithText(COIN_VIEW_ON_NUMISTA, substring = true).performClick()

        assertEquals(A_TYPE, left)
    }

    /**
     * The lámina the collector is standing on is not a door out of its own casilla's sheet.
     *
     * Every other collection that claims the coin still is: what is dropped is the one link that would
     * lead onto the sheet being read.
     */
    @Test
    fun theSheetDrawsNoDoorOntoTheLaminaItWasOpenedFrom() {
        val plate = CardDestination.Plate("venezuela-bolivar")
        compose.setContent {
            CoindexTheme {
                Sheet(
                    coin = A_COIN.copy(
                        claims = listOf(
                            CoinClaim("1 Bolívar", plate),
                            CoinClaim("Las venezolanas", CardDestination.Box(7)),
                        ),
                    ),
                    here = plate,
                )
            }
        }

        compose.onNodeWithText("1 Bolívar").assertDoesNotExist()
        compose.onNodeWithText("Las venezolanas").assertExists()
    }

    @Composable
    private fun Casilla(numistaTypeId: Int?, onOpenCoin: (Int) -> Unit) {
        PlateCell(
            cell = DrawnCell(
                id = "casilla",
                label = "Bolívar",
                numistaTypeId = numistaTypeId,
                footnote = null,
                year = if (numistaTypeId == null) "2027" else "1886",
                owned = false,
                missing = numistaTypeId != null,
            ),
            images = null,
            printedSide = PrintedSide.Reverse,
            travellingFrom = null,
            onOpenCoin = onOpenCoin,
            modifier = Modifier.width(CELL_WIDTH),
        )
    }

    @Composable
    private fun Sheet(
        coin: CoinRow,
        here: CardDestination? = null,
        onOpenNumista: (Int) -> Unit = {},
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CoinSheetOverlay(
                typeId = coin.typeId,
                surface = CoinSheetSurface(
                    coin = { coin },
                    ficha = { FichaRefresh(fetchedAt = null, refreshing = false, onRefresh = {}) },
                    value = { null },
                    onOpenNumista = onOpenNumista,
                    onOpenClaim = {},
                ),
                faces = { null to null },
                onDismiss = {},
                here = here,
            )
        }
    }
}
