package com.jenarvaezg.coindex.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.ui.COIN_CLAIM_HERE
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.CoinName
import com.jenarvaezg.coindex.ui.DrawnPiece
import com.jenarvaezg.coindex.ui.PiecesSubject
import com.jenarvaezg.coindex.ui.components.FichaRefresh
import com.jenarvaezg.coindex.ui.pieceLine
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.shelf.CoinClaim
import com.jenarvaezg.coindex.ui.shelf.CoinRow
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val A_TYPE = 10_338

private const val A_TITLE = "1 Bolívar - Simón Bolívar"

private const val THIS_BOX = "Mis bolívares"

private val HERE = CardDestination.Box(7)

private val A_PIECE = DrawnPiece(
    item = CollectedItem(id = 1, quantity = 2, typeId = A_TYPE, issueYear = 1977),
    emissionLabel = null,
)

private val A_COIN = CoinRow(
    typeId = A_TYPE,
    name = CoinName("1 Bolívar", "Simón Bolívar"),
    rawTitle = A_TITLE,
    issuer = "Venezuela",
    years = listOf(1_977),
    objectClass = ObjectClass.Coin,
    weightOz = null,
    quantity = 2,
    claims = listOf(
        CoinClaim(THIS_BOX, HERE),
        CoinClaim("Las venezolanas", CardDestination.Box(8)),
    ),
    unclaimedPieces = 0,
)

/**
 * A piece of a box or of a collection without a plate opens its coin's sheet over the list
 * (ADR 0026 §3), which links to the collection's other claims and names the one on screen without
 * a link (#635).
 */
@RunWith(AndroidJUnit4::class)
class PiecesOpenTheSheetTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun aPieceOpensItsCoinsSheetNamingThisCollectionWithoutALink() {
        compose.setContent {
            CoindexTheme {
                PiecesScreen(
                    state = CollectionState(),
                    subject = PiecesSubject(
                        title = THIS_BOX,
                        issuer = null,
                        variant = null,
                        coverage = null,
                        distinctTypes = 1,
                        quantity = 2,
                        pieces = listOf(A_PIECE),
                        boxId = 7,
                    ),
                    sheet = CoinSheetSurface(
                        coin = { A_COIN },
                        ficha = { FichaRefresh(fetchedAt = null, refreshing = false, onRefresh = {}) },
                        value = { null },
                        onOpenNumista = {},
                        onOpenClaim = {},
                    ),
                    here = HERE,
                    onMessage = {},
                    ficha = { FichaRefresh(fetchedAt = null, refreshing = false, onRefresh = {}) },
                    notebookOptions = NotebookOptions(),
                    onNotebookPrinted = {},
                    notebookPages = { emptyList() },
                    onExporting = {},
                )
            }
        }
        compose.onNodeWithText(A_TITLE).assertDoesNotExist()

        compose.onNodeWithText(pieceLine(A_PIECE), useUnmergedTree = true).performClick()

        compose.onNodeWithText(A_TITLE).assertIsDisplayed()
        compose.onNodeWithText("Las venezolanas").assertIsDisplayed()
        compose.onNodeWithText(COIN_CLAIM_HERE, useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText(THIS_BOX) and hasClickAction(), useUnmergedTree = true)
            .assertDoesNotExist()
    }
}
