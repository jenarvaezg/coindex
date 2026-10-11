package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
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
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.ui.DrawnCell
import com.jenarvaezg.coindex.ui.OPEN_THE_COIN_SHEET
import com.jenarvaezg.coindex.ui.TURN_THE_COIN_OVER
import com.jenarvaezg.coindex.ui.ficha.CasillaOrigin
import com.jenarvaezg.coindex.ui.plaqueOf
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

/** The name the casilla prints under its year. */
private const val A_NAME = "Bolívar"

private const val CASILLA = "casilla"

/** Unreachable on purpose: two faces make a hole turnable, the pictures are beside the point. */
private val TWO_FACES = TypeImages(
    obverse = CoinPhoto(thumbnail = "https://example.invalid/a-180.jpg"),
    reverse = CoinPhoto(thumbnail = "https://example.invalid/b-180.jpg"),
)

/**
 * The whole casilla opens its coin's screen inside the app (ADR 0026 §3, #643). The casilla hands
 * over a type and its catalogue facts, never a URL (#508).
 */
@RunWith(AndroidJUnit4::class)
class CasillaOpensTheCoinTest {
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

    /** The name is where a finger lands first (#632). */
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

    /** The hole opens the screen too; turning the coin over is the screen's. */
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

    /** The coin's screen learns the casilla it came from: its year, whether it is empty (#643). */
    @Test
    fun aCasillaHandsOverItsCatalogueFacts() {
        var opened: CasillaTap.Open? = null
        compose.setContent {
            CoindexTheme {
                PlateCell(
                    cell = cell(A_TYPE),
                    images = null,
                    printedSide = PrintedSide.Reverse,
                    travellingFrom = null,
                    onOpenCoin = { opened = it },
                    modifier = Modifier.width(CELL_WIDTH),
                )
            }
        }

        compose.onNodeWithText(A_NAME, useUnmergedTree = true).performClick()

        assertEquals(
            CasillaTap.Open(A_TYPE, CasillaOrigin(year = 1_886, missing = true, dated = true)),
            opened,
        )
    }

    private fun clickLabels(useUnmergedTree: Boolean = false): List<String?> = compose
        .onAllNodes(hasClickAction(), useUnmergedTree)
        .fetchSemanticsNodes()
        .map { it.config[SemanticsActions.OnClick].label }

    @Composable
    private fun Casilla(
        numistaTypeId: Int?,
        onOpenCoin: (Int) -> Unit,
        images: TypeImages? = null,
    ) {
        PlateCell(
            cell = cell(numistaTypeId),
            images = images,
            printedSide = PrintedSide.Reverse,
            travellingFrom = null,
            onOpenCoin = { onOpenCoin(it.typeId) },
            modifier = Modifier.width(CELL_WIDTH).testTag(CASILLA),
        )
    }

    private fun cell(numistaTypeId: Int?): DrawnCell {
        val year = if (numistaTypeId == null) "2027" else "1886"
        return DrawnCell(
            id = "casilla",
            label = A_NAME,
            numistaTypeId = numistaTypeId,
            footnote = null,
            year = year,
            owned = false,
            missing = numistaTypeId != null,
            plaque = plaqueOf(label = A_NAME, year = year, yearIsCommon = false),
        )
    }
}
