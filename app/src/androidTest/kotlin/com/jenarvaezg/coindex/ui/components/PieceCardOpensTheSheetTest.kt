package com.jenarvaezg.coindex.ui.components

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.ui.COIN_VIEW_ON_NUMISTA
import com.jenarvaezg.coindex.ui.CoinName
import com.jenarvaezg.coindex.ui.DrawnPiece
import com.jenarvaezg.coindex.ui.OPEN_THE_COIN_SHEET
import com.jenarvaezg.coindex.ui.REMOVE_TYPE_FROM_COLLECTION
import com.jenarvaezg.coindex.ui.TURN_THE_COIN_OVER
import com.jenarvaezg.coindex.ui.fichaRefreshLabel
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val A_TYPE = 10_338

private const val A_DENOMINATION = "1 Bolívar"

private val OBVERSE = CoinPhoto(thumbnail = "https://example.invalid/a-180.jpg", picture = null)
private val REVERSE = CoinPhoto(thumbnail = "https://example.invalid/b-180.jpg", picture = null)

/** Any day: with a date the card draws «Actualizar la ficha». */
private const val BROUGHT_AT = 1_790_000_000_000L

/**
 * The card of a piece in a collection without a plate, or in a box, opens its coin's sheet like a
 * casilla (ADR 0026 §3); only its own buttons don't. The coin turns over in the sheet, and Numista
 * is left from there. The URLs are unreachable on purpose: the picture is beside the point.
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
        onRefresh: () -> Unit = {},
    ) {
        compose.setContent {
            CoindexTheme {
                PieceCard(
                    piece = piece,
                    name = CoinName(denomination = A_DENOMINATION, theme = null),
                    images = images,
                    onOpenCoin = onOpenCoin,
                    ficha = FichaRefresh(
                        fetchedAt = BROUGHT_AT,
                        refreshing = false,
                        onRefresh = onRefresh,
                    ),
                ) {
                    CardAction(text = REMOVE_TYPE_FROM_COLLECTION, onClick = onRemove)
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
    fun aPieceCardOffersNoTurn() {
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

    /**
     * The card's buttons sit outside the part that opens, as «Quitar» does in «Lo que busco»: a
     * missed tap must not take a coin out of a box or spend a consulta.
     */
    @Test
    fun theCardsOwnButtonsShareNoAreaWithWhatOpens() {
        var opened: Int? = null
        var removed = 0
        var refreshed = 0
        card(
            onOpenCoin = { opened = it },
            onRemove = { removed += 1 },
            onRefresh = { refreshed += 1 },
        )

        compose.onNodeWithText(REMOVE_TYPE_FROM_COLLECTION).performClick()
        compose.onNodeWithText(fichaRefreshLabel(refreshing = false)).performClick()

        assertEquals(1, removed)
        assertEquals(1, refreshed)
        assertNull(opened)
        val opens = compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()
            .single { it.config[SemanticsActions.OnClick].label == OPEN_THE_COIN_SHEET }
            .touchBoundsInRoot
        listOf(REMOVE_TYPE_FROM_COLLECTION, fichaRefreshLabel(refreshing = false)).forEach { button ->
            val bounds = compose.onNodeWithText(button).fetchSemanticsNode().touchBoundsInRoot
            assertFalse("$button $bounds pisa $opens", bounds.overlaps(opens))
        }
    }

    /** A coin in a hole carries no face captions, on paper or on screen. */
    @Test
    fun aPieceNoLongerPrintsTheNamesOfItsFaces() {
        card()

        compose.onAllNodesWithText("Anverso").assertCountEquals(0)
        compose.onAllNodesWithText("Reverso").assertCountEquals(0)
    }
}
