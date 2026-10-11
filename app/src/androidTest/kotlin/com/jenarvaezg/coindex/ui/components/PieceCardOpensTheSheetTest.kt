package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.ui.COIN_VIEW_ON_NUMISTA
import com.jenarvaezg.coindex.ui.CoinName
import com.jenarvaezg.coindex.ui.DrawnPiece
import com.jenarvaezg.coindex.ui.OPEN_THE_COIN_SHEET
import com.jenarvaezg.coindex.ui.TURN_THE_COIN_OVER
import com.jenarvaezg.coindex.ui.screens.CoinSheetOverlay
import com.jenarvaezg.coindex.ui.screens.CoinSheetSurface
import com.jenarvaezg.coindex.ui.shelf.CoinRow
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val A_TYPE = 10_338

private const val A_DENOMINATION = "1 Bolívar"

private const val A_TITLE = "1 Bolívar - Simón Bolívar"

private const val REMOVE = "Quitar de esta colección"

private val OBVERSE = CoinPhoto(thumbnail = "https://example.invalid/a-180.jpg", picture = null)
private val REVERSE = CoinPhoto(thumbnail = "https://example.invalid/b-180.jpg", picture = null)

private val A_COIN = CoinRow(
    typeId = A_TYPE,
    name = CoinName(A_DENOMINATION, "Simón Bolívar"),
    rawTitle = A_TITLE,
    issuer = "Venezuela",
    years = listOf(1_977),
    objectClass = ObjectClass.Coin,
    weightOz = null,
    quantity = 2,
    claims = emptyList(),
    unclaimedPieces = 0,
)

/**
 * The card of a piece in a collection without a plate, or in a box, opens its coin's sheet whole,
 * like a casilla (ADR 0026 §3). The coin turns over in the sheet, and Numista is left from there.
 * The URLs are unreachable on purpose: the picture is beside the point.
 */
@RunWith(AndroidJUnit4::class)
class PieceCardOpensTheSheetTest {
    @get:Rule
    val compose = createComposeRule()

    private val piece = DrawnPiece(
        item = CollectedItem(id = 1, quantity = 2, typeId = A_TYPE, issueYear = 1977),
        emissionLabel = null,
    )

    private fun card(
        images: TypeImages? = TypeImages(obverse = OBVERSE, reverse = REVERSE),
        onOpenCoin: (Int) -> Unit = {},
        onRemove: () -> Unit = {},
    ) {
        compose.setContent {
            CoindexTheme {
                PieceCard(
                    piece = piece,
                    name = CoinName(denomination = A_DENOMINATION, theme = null),
                    images = images,
                    onOpenCoin = onOpenCoin,
                    ficha = FichaRefresh(fetchedAt = null, refreshing = false, onRefresh = {}),
                ) {
                    CardAction(text = REMOVE, onClick = onRemove)
                }
            }
        }
        compose.waitForIdle()
    }

    private fun clickLabels(): List<String?> = compose
        .onAllNodes(hasClickAction(), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .map { node -> node.config[SemanticsActions.OnClick].label }

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun theNameOfAPieceOpensItsCoin() {
        var opened: Int? = null
        card(onOpenCoin = { opened = it })

        compose.onNodeWithText(A_DENOMINATION, useUnmergedTree = true).performClick()

        assertEquals(A_TYPE, opened)
    }

    /** The coin turns over in its sheet; on the card, a tap on the hole opens it like the rest. */
    @Test
    fun aPieceNoLongerTurnsOverOnItsCard() {
        card()

        val labels = clickLabels()
        assertTrue(labels.toString(), TURN_THE_COIN_OVER !in labels)
        assertTrue(labels.toString(), OPEN_THE_COIN_SHEET in labels)
    }

    /** The sheet carries «Ver en Numista ↗», so the card doesn't print it once per piece. */
    @Test
    fun aPieceLeavesNumistaToItsSheet() {
        card()

        compose.onAllNodesWithText(COIN_VIEW_ON_NUMISTA, substring = true).assertCountEquals(0)
    }

    /** A box's own action on the card is its own target, and doesn't open the sheet. */
    @Test
    fun theCardsOwnActionDoesNotOpenTheSheet() {
        var opened: Int? = null
        var removed = 0
        card(onOpenCoin = { opened = it }, onRemove = { removed += 1 })

        compose.onNodeWithText(REMOVE).performClick()

        assertEquals(1, removed)
        assertEquals(null, opened)
    }

    /** A coin in a hole carries no face captions, on paper or on screen. */
    @Test
    fun aPieceNoLongerPrintsTheNamesOfItsFaces() {
        card()

        compose.onAllNodesWithText("Anverso").assertCountEquals(0)
        compose.onAllNodesWithText("Reverso").assertCountEquals(0)
    }

    /** The screen's wiring, cut down to one card and the sheet over it. */
    @Test
    fun tappingAPieceOpensTheSheet() {
        compose.setContent {
            CoindexTheme {
                var open by remember { mutableStateOf<Int?>(null) }
                Box(modifier = Modifier.fillMaxSize()) {
                    PieceCard(
                        piece = piece,
                        name = CoinName(denomination = A_DENOMINATION, theme = null),
                        images = null,
                        onOpenCoin = { open = it },
                        ficha = FichaRefresh(fetchedAt = null, refreshing = false, onRefresh = {}),
                    )
                    CoinSheetOverlay(
                        typeId = open,
                        surface = CoinSheetSurface(
                            coin = { A_COIN },
                            ficha = { FichaRefresh(fetchedAt = null, refreshing = false, onRefresh = {}) },
                            value = { null },
                            onOpenNumista = {},
                            onOpenClaim = {},
                        ),
                        faces = { null to null },
                        onDismiss = { open = null },
                    )
                }
            }
        }

        compose.onNodeWithText(A_DENOMINATION, useUnmergedTree = true).performClick()

        compose.onNodeWithText(A_TITLE).assertIsDisplayed()
    }
}
