package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.ui.COIN_VIEW_ON_NUMISTA
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.CoinName
import com.jenarvaezg.coindex.ui.DrawnCell
import com.jenarvaezg.coindex.ui.OPEN_THE_COIN_SHEET
import com.jenarvaezg.coindex.ui.TURN_THE_COIN_OVER
import com.jenarvaezg.coindex.ui.components.FichaRefresh
import com.jenarvaezg.coindex.ui.plaqueOf
import com.jenarvaezg.coindex.ui.shelf.CoinClaim
import com.jenarvaezg.coindex.ui.shelf.CoinRow
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The cell of the plate on a 411 dp phone, the same three columns `PlateCellNameTest` measures. */
private val CELL_WIDTH = 113.dp

/** The casilla's hole, at the top of the casilla. */
private val HOLE = 104.dp

/** The 1 Bolívar, the reference plate for the casilla's layout. */
private const val A_TYPE = 10_338

private const val A_TITLE = "1 Bolívar - Simón Bolívar"

/** The name the casilla prints under its year. */
private const val A_NAME = "Bolívar"

/** Unreachable on purpose: two faces make a hole turnable, the pictures are beside the point. */
private val TWO_FACES = TypeImages(
    obverse = CoinPhoto(thumbnail = "https://example.invalid/a-180.jpg"),
    reverse = CoinPhoto(thumbnail = "https://example.invalid/b-180.jpg"),
)

private val A_COIN = CoinRow(
    typeId = A_TYPE,
    name = CoinName("1 Bolívar", "Simón Bolívar"),
    rawTitle = A_TITLE,
    issuer = "Venezuela",
    years = listOf(1_886),
    objectClass = ObjectClass.Coin,
    weightOz = null,
    // A hole: the collector holds no piece of the type.
    quantity = 0,
    claims = emptyList(),
    unclaimedPieces = 0,
)

/**
 * The whole casilla opens its coin's sheet inside the app, and the coin turns over in the sheet
 * (ADR 0026 §3). The casilla hands over a type, never a URL (#508).
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

        compose.onNodeWithText("1886", useUnmergedTree = true).performClick()

        assertEquals(A_TYPE, opened)
    }

    /** Where the father taps, and where the name once linked to Numista. */
    @Test
    fun theNameOfACasillaOpensItsCoin() {
        var opened: Int? = null
        compose.setContent {
            CoindexTheme {
                Casilla(numistaTypeId = A_TYPE, onOpenCoin = { opened = it })
            }
        }

        compose.onNodeWithText(A_NAME, useUnmergedTree = true).performClick()

        assertEquals(A_TYPE, opened)
    }

    /** The hole opens the sheet too; turning the coin over is the sheet's. */
    @Test
    fun theHoleOfACasillaOpensItsCoinAndDoesNotTurnIt() {
        var opened: Int? = null
        compose.setContent {
            CoindexTheme {
                Casilla(numistaTypeId = A_TYPE, images = TWO_FACES, onOpenCoin = { opened = it })
            }
        }

        compose.onNodeWithTag(CASILLA).performTouchInput {
            click(Offset(centerX, HOLE.toPx() / 2))
        }

        assertEquals(A_TYPE, opened)
        val labels = clickLabels()
        assertTrue(labels.toString(), TURN_THE_COIN_OVER !in labels)
    }

    /** One target, read out as one action. */
    @Test
    fun aCasillaIsOneTargetWithOneLabel() {
        compose.setContent {
            CoindexTheme {
                Casilla(numistaTypeId = A_TYPE, images = TWO_FACES, onOpenCoin = {})
            }
        }

        assertEquals(listOf(OPEN_THE_COIN_SHEET), clickLabels())
    }

    /** An announced member has no ficha to open. */
    @Test
    fun anAnnouncedCasillaOpensNothing() {
        var opened: Int? = null
        compose.setContent {
            CoindexTheme {
                Casilla(numistaTypeId = null, onOpenCoin = { opened = it })
            }
        }

        assertTrue(clickLabels().isEmpty())
        compose.onNodeWithText(A_NAME, useUnmergedTree = true).performClick()
        assertNull(opened)
    }

    /** The plate's own wiring, cut down to one casilla and the sheet over it. */
    @Test
    fun tappingTheNameOfACasillaOpensTheSheet() {
        compose.setContent {
            CoindexTheme {
                var open by remember { mutableStateOf<Int?>(null) }
                Box(modifier = Modifier.fillMaxSize()) {
                    Casilla(numistaTypeId = A_TYPE, onOpenCoin = { open = it })
                    Sheet(coin = A_COIN, typeId = open, onDismiss = { open = null })
                }
            }
        }
        compose.onNodeWithText(A_TITLE).assertDoesNotExist()

        compose.onNodeWithText(A_NAME, useUnmergedTree = true).performClick()

        compose.onNodeWithText(A_TITLE).assertIsDisplayed()
        compose.onNodeWithText(COIN_VIEW_ON_NUMISTA, substring = true).assertIsDisplayed()
    }

    /** «Ver en Numista ↗» is the sheet's only way to the browser, with the arrow the casilla lacks. */
    @Test
    fun theSheetOffersNumistaBehindItsOwnArrow() {
        var left: Int? = null
        compose.setContent {
            CoindexTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Sheet(coin = A_COIN, onOpenNumista = { left = it })
                }
            }
        }

        compose.onNodeWithText(A_TITLE).assertExists()
        compose.onNodeWithText(COIN_VIEW_ON_NUMISTA, substring = true).performClick()

        assertEquals(A_TYPE, left)
    }

    /** The coin turns over in the sheet's hole, the one hole that still turns. */
    @Test
    fun theSheetsHoleTurnsTheCoinOver() {
        compose.setContent {
            CoindexTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Sheet(
                        coin = A_COIN,
                        faces = TWO_FACES.reverse to TWO_FACES.obverse,
                    )
                }
            }
        }

        val labels = compose.onAllNodes(hasClickAction(), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .map { it.config[SemanticsActions.OnClick].label }
        assertTrue(labels.toString(), TURN_THE_COIN_OVER in labels)
    }

    /** Other collections that claim the coin keep their link; only the open lámina's is dropped. */
    @Test
    fun theSheetDrawsNoDoorOntoTheLaminaItWasOpenedFrom() {
        val plate = CardDestination.Plate("venezuela-bolivar")
        compose.setContent {
            CoindexTheme {
                Box(modifier = Modifier.fillMaxSize()) {
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
        }

        compose.onNodeWithText("1 Bolívar").assertDoesNotExist()
        compose.onNodeWithText("Las venezolanas").assertExists()
    }

    private fun clickLabels(): List<String?> = compose
        .onAllNodes(hasClickAction())
        .fetchSemanticsNodes()
        .map { it.config[SemanticsActions.OnClick].label }

    @Composable
    private fun Casilla(
        numistaTypeId: Int?,
        onOpenCoin: (Int) -> Unit,
        images: TypeImages? = null,
    ) {
        val year = if (numistaTypeId == null) "2027" else "1886"
        PlateCell(
            cell = DrawnCell(
                id = "casilla",
                label = A_NAME,
                numistaTypeId = numistaTypeId,
                footnote = null,
                year = year,
                owned = false,
                missing = numistaTypeId != null,
                plaque = plaqueOf(label = A_NAME, year = year, yearIsCommon = false),
            ),
            images = images,
            printedSide = PrintedSide.Reverse,
            travellingFrom = null,
            onOpenCoin = onOpenCoin,
            modifier = Modifier.width(CELL_WIDTH).testTag(CASILLA),
        )
    }

    @Composable
    private fun Sheet(
        coin: CoinRow,
        typeId: Int? = coin.typeId,
        here: CardDestination? = null,
        faces: Pair<CoinPhoto?, CoinPhoto?> = null to null,
        onOpenNumista: (Int) -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        CoinSheetOverlay(
            typeId = typeId,
            surface = CoinSheetSurface(
                coin = { coin },
                ficha = { FichaRefresh(fetchedAt = null, refreshing = false, onRefresh = {}) },
                value = { null },
                onOpenNumista = onOpenNumista,
                onOpenClaim = {},
            ),
            faces = { faces },
            onDismiss = onDismiss,
            here = here,
        )
    }
}

private const val CASILLA = "casilla"
