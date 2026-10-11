package com.jenarvaezg.coindex.ui.screens

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.ui.DrawnPiece
import com.jenarvaezg.coindex.ui.PiecesSubject
import com.jenarvaezg.coindex.ui.components.FichaRefresh
import com.jenarvaezg.coindex.ui.ficha.CoinFichaRequest
import com.jenarvaezg.coindex.ui.pieceLine
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val A_TYPE = 10_338

private const val THIS_BOX = "Mis bolívares"

private val A_PIECE = DrawnPiece(
    item = CollectedItem(id = 1, quantity = 2, typeId = A_TYPE, issueYear = 1977),
    emissionLabel = null,
)

/**
 * A piece of a box or of a collection without a plate opens its coin's screen (ADR 0026 §3, #643),
 * on the reverse, since such a collection declares no `printed_side`. The screen itself, with the
 * collection it came from named and not linked (#635), is `CoinFichaScreenTest`'s.
 */
@RunWith(AndroidJUnit4::class)
class PiecesOpenTheCoinTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun aPieceOpensItsCoinOnTheReverse() {
        var opened: CoinFichaRequest? = null
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
                    onOpenCoin = { opened = it },
                    onMessage = {},
                    ficha = { FichaRefresh(fetchedAt = null, refreshing = false, onRefresh = {}) },
                    notebookOptions = NotebookOptions(),
                    onNotebookPrinted = {},
                    notebookPages = { emptyList() },
                    onExporting = {},
                )
            }
        }
        compose.onNodeWithText(pieceLine(A_PIECE), useUnmergedTree = true).performClick()

        assertEquals(CoinFichaRequest(A_TYPE, PrintedSide.Reverse), opened)
    }
}
