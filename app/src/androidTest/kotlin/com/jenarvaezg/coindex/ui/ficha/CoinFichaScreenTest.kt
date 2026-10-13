package com.jenarvaezg.coindex.ui.ficha

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.printToLog
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.ui.COIN_CLAIM_HERE_SUFFIX
import com.jenarvaezg.coindex.ui.COIN_IN_SEVERAL_COLLECTIONS
import com.jenarvaezg.coindex.ui.COIN_VIEW_ON_NUMISTA
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.TURN_THE_COIN_OVER
import com.jenarvaezg.coindex.ui.components.FichaRefresh
import com.jenarvaezg.coindex.ui.shelf.CoinClaim
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val A_TYPE = 10_340
private const val A_TITLE = "5 Bolívares"
private const val THIS_PLATE = "Fuertes"

/** Unreachable on purpose: two faces make the coin turnable, the pictures are beside the point. */
private val TWO_FACES = mapOf(
    A_TYPE to TypeImages(
        obverse = CoinPhoto(thumbnail = "https://example.invalid/a-180.jpg"),
        reverse = CoinPhoto(thumbnail = "https://example.invalid/b-180.jpg"),
    ),
)

private val PLATE = CardDestination.Plate("venezuela-fuertes")

private fun piece(id: Long, year: String) = FichaPiece(
    itemId = id,
    dateKey = year,
    heading = year,
    grade = "xf",
    quantity = null,
    here = year == "1929",
    facts = "pagado en $year",
    rungs = emptyList(),
    issueComment = null,
    note = null,
    photo = null,
)

private val A_COIN = CoinFichaSubject(
    typeId = A_TYPE,
    title = A_TITLE,
    identity = "Venezuela · 1879 – 1936 · N# 10340",
    owned = true,
    backed = true,
    value = "Valor actual: 60 € · precio de catálogo en xf",
    missing = false,
    cost = null,
    sections = listOf(
        PiecesSection(
            title = "Tus piezas · 4",
            summary = "4 piezas en 4 fechas · 1879 – 1936",
            open = true,
            pieces = listOf(piece(1, "1879"), piece(2, "1886"), piece(3, "1929"), piece(4, "1936")),
            dates = listOf("1879", "1886", "1929", "1936").map { year ->
                DateTile(year, year, "xf", null, noted = false, casilla = year == "1929")
            },
            selected = "1929",
        ),
        ClaimsSection(
            title = COIN_IN_SEVERAL_COLLECTIONS,
            summary = "$THIS_PLATE · Las venezolanas",
            claims = listOf(
                FichaClaim(CoinClaim(THIS_PLATE, PLATE), here = true),
                FichaClaim(CoinClaim("Las venezolanas", CardDestination.Box(7)), here = false),
            ),
            strip = null,
        ),
        SpecSection(
            title = "La moneda",
            summary = "Plata 900 · 25 g",
            rows = listOf("Metal" to "Plata 900", "Peso" to "25 g"),
        ),
    ),
)

/** The coin's screen (#643, ADR 0026 §13), drawn from a subject made by hand. */
@RunWith(AndroidJUnit4::class)
class CoinFichaScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun theWayBackNamesWhereItCameFromAndGoesThere() {
        var back = 0
        show(onBack = { back++ })

        compose.onNodeWithText(THIS_PLATE, useUnmergedTree = true).performClick()

        assertEquals(1, back)
    }

    @Test
    fun theCoinTurnsAndItsCaptionNamesTheFaceUp() {
        show()

        compose.onNodeWithText(FichaPageLabels.faceUp(PrintedSide.Reverse, turns = true))
            .assertExists()
        compose.onNode(
            hasClickAction() and hasClickLabel(TURN_THE_COIN_OVER),
            useUnmergedTree = true,
        ).performClick()

        compose.onNodeWithText(FichaPageLabels.faceUp(PrintedSide.Obverse, turns = true))
            .assertExists()
    }

    /** From a date run's casilla the pieces enter open on that date; the rest enter closed. */
    @Test
    fun thePiecesEnterOpenOnTheCasillasDateAndTheRestClosed() {
        show()

        compose.onNodeWithText("pagado en 1929").assertExists()
        compose.onNodeWithText("pagado en 1879").assertDoesNotExist()
        compose.onNodeWithText("Peso").assertDoesNotExist()

        compose.onNodeWithText("1879", useUnmergedTree = true).performClick()
        compose.onNodeWithText("pagado en 1879").assertExists()
        compose.onNodeWithText("pagado en 1929").assertDoesNotExist()

        compose.onNodeWithText("La moneda").performClick()
        compose.onNodeWithText("Peso").assertExists()
    }

    /** The lámina it came from is named and marked, without a link; the others keep theirs. */
    @Test
    fun theClaimsMarkTheOneItCameFromWithoutALink() {
        show()

        compose.onNodeWithText(COIN_IN_SEVERAL_COLLECTIONS).performClick()

        compose.onNodeWithText(COIN_CLAIM_HERE_SUFFIX, useUnmergedTree = true).assertExists()
        // «Volver» names it too; neither the way back's word nor the claim is a link.
        compose.onAllNodes(hasText(THIS_PLATE) and hasClickAction(), useUnmergedTree = true)
            .assertCountEquals(0)
        compose.onNodeWithText("Las venezolanas", useUnmergedTree = true).assert(hasClickAction())
    }

    @Test
    fun numistaIsLeftForBehindItsOwnArrow() {
        var left = 0
        show(onOpenNumista = { left++ })

        compose.onNode(hasScrollAction())
            .performScrollToNode(hasText(COIN_VIEW_ON_NUMISTA, substring = true))
        compose.onNodeWithText(COIN_VIEW_ON_NUMISTA, substring = true).performClick()

        assertEquals(1, left)
    }

    /** The compact bar holds the title once the head has scrolled off. */
    @Test
    fun theCompactBarAppearsOnceTheTitleScrollsOff() {
        // A coin with enough pieces open for the head to leave the screen.
        show(
            subject = A_COIN.copy(
                sections = listOf(
                    PiecesSection(
                        title = "Tus piezas · 12",
                        summary = "",
                        open = true,
                        pieces = (1..12).map { piece(it.toLong(), (1900 + it).toString()) },
                        dates = emptyList(),
                        selected = null,
                    ),
                ),
            ),
        )
        assertEquals(1, compose.onAllNodesWithText(A_TITLE).fetchSemanticsNodes().size)

        compose.onNode(hasScrollAction())
            .performScrollToNode(hasText(COIN_VIEW_ON_NUMISTA, substring = true))

        compose.waitForIdle()
        compose.onRoot(useUnmergedTree = true).printToLog("FICHA643")
        assertEquals(2, compose.onAllNodesWithText(A_TITLE).fetchSemanticsNodes().size)
    }

    private fun show(
        subject: CoinFichaSubject = A_COIN,
        onBack: () -> Unit = {},
        onOpenNumista: () -> Unit = {},
    ) {
        compose.setContent {
            CoindexTheme {
                CoinFichaScreen(
                    subject = subject,
                    images = TWO_FACES,
                    side = PrintedSide.Reverse,
                    back = THIS_PLATE,
                    ficha = FichaRefresh(fetchedAt = null, refreshing = false, onRefresh = {}),
                    onBack = onBack,
                    onOpenNumista = onOpenNumista,
                    onOpenType = {},
                    onOpenClaim = {},
                )
            }
        }
    }
}

/** A node whose click action is read out as [label]. */
private fun hasClickLabel(label: String) = SemanticsMatcher("click label $label") { node ->
    node.config.getOrNull(SemanticsActions.OnClick)?.label == label
}
