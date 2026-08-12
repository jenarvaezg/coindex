package com.jenarvaezg.coindex.ui.components

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.ui.CoinName
import com.jenarvaezg.coindex.ui.DrawnPiece
import com.jenarvaezg.coindex.ui.TURN_THE_COIN_OVER
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val OBVERSE = CoinPhoto(thumbnail = "https://example.invalid/a-180.jpg", picture = null)
private val REVERSE = CoinPhoto(thumbnail = "https://example.invalid/b-180.jpg", picture = null)

/**
 * A piece of a collection is the same coin as one in a casilla, so it turns over the same way (#423).
 *
 * The photographs never arrive — the URLs are unreachable on purpose — because this is not about the
 * picture: what is pinned is that the card offers **the turn** where it used to print «Anverso» and
 * «Reverso» under a pair of flat squares.
 */
@RunWith(AndroidJUnit4::class)
class PieceCardTurnTest {
    @get:Rule
    val compose = createComposeRule()

    private val piece = DrawnPiece(
        item = CollectedItem(id = 1, quantity = 2, typeId = 10_338, issueYear = 1977),
        emissionLabel = null,
    )

    private fun card(images: TypeImages?) {
        compose.setContent {
            CoindexTheme {
                PieceCard(
                    piece = piece,
                    name = CoinName(denomination = "1 Bolívar", theme = null),
                    images = images,
                    onOpenSource = {},
                    ficha = FichaRefresh(fetchedAt = null, refreshing = false, onRefresh = {}),
                )
            }
        }
        compose.waitForIdle()
    }

    private fun clickLabels(): List<String?> = compose
        .onAllNodes(hasClickAction(), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .map { node ->
            node.config.takeIf { it.contains(SemanticsActions.OnClick) }
                ?.get(SemanticsActions.OnClick)
                ?.label
        }

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun aPieceWithTwoPhotographedFacesTurnsOver() {
        card(TypeImages(obverse = OBVERSE, reverse = REVERSE))

        assertTrue(clickLabels().toString(), TURN_THE_COIN_OVER in clickLabels())
    }

    /** The captions went with the pair: a coin in a hole is not labelled, on paper or on screen. */
    @Test
    fun aPieceNoLongerPrintsTheNamesOfItsFaces() {
        card(TypeImages(obverse = OBVERSE, reverse = REVERSE))

        compose.onAllNodesWithText("Anverso").assertCountEquals(0)
        compose.onAllNodesWithText("Reverso").assertCountEquals(0)
    }

    /** One photographed face is a coin with no back, and a hole that swings onto nothing is worse. */
    @Test
    fun aPieceWithOnlyOneFaceOffersNoTurn() {
        card(TypeImages(reverse = REVERSE))

        assertTrue(clickLabels().toString(), TURN_THE_COIN_OVER !in clickLabels())
    }
}
