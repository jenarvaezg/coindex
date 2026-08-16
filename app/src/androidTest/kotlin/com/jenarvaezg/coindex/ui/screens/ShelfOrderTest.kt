package com.jenarvaezg.coindex.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.ui.ShowcaseLabels
import com.jenarvaezg.coindex.ui.ShowcaseSort
import com.jenarvaezg.coindex.ui.ShowcaseTile
import com.jenarvaezg.coindex.ui.showcaseOrderNote
import com.jenarvaezg.coindex.ui.showcaseSlotsLabel
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The shelf both tests below draw: one plate with an amount and one without.
 *
 * Its note is asked of [showcaseOrderNote] rather than typed out — the wording is fixed once, in
 * `ShowcaseLabelsTest`, and what this file measures is that the screen prints what that function says
 * and prints it only when the order asks for it.
 */
private val MIXED_SHELF = listOf(shelfTile("panda", entryEur = 412.0), shelfTile("kooka"))

private val MIXED_NOTE = showcaseOrderNote(ShowcaseSort.ByEntryCost, MIXED_SHELF)!!

/**
 * The order of «Explorar»: which one is on, and what it could not place (#513).
 *
 * Both facts are drawn ones — a fill and a line of type — so they are measured where Compose renders
 * them rather than in the labels' own test. The two that mattered on the AVD were an active order
 * that read as a caption of the framed alternative, and a shelf that did not visibly move when it was
 * asked for an order it had no amounts for.
 */
@RunWith(AndroidJUnit4::class)
class ShelfOrderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun theOrderInForceIsTheSelectedOneAndTheOtherIsOnOffer() {
        compose.setContent {
            CoindexTheme {
                ExploreScreen(
                    tiles = MIXED_SHELF,
                    wishes = 0,
                    images = emptyMap(),
                    onOpenPlate = {},
                    onOpenWishes = {},
                )
            }
        }

        compose.onNodeWithText(ShowcaseSort.ByCasillas.label).assertIsSelected()
        compose.onNodeWithText(ShowcaseSort.ByEntryCost.label).assertIsNotSelected()

        compose.onNodeWithText(ShowcaseSort.ByEntryCost.label).performClick()

        compose.onNodeWithText(ShowcaseSort.ByEntryCost.label).assertIsSelected()
        compose.onNodeWithText(ShowcaseSort.ByCasillas.label).assertIsNotSelected()
    }

    /**
     * The default order owes nothing about prices, and the cost order says what it left at the end.
     *
     * The note is absent until it is asked for: «por casillas» sorts by a fact every plate has.
     */
    @Test
    fun theCostOrderSaysWhatItCouldNotPlace() {
        compose.setContent {
            CoindexTheme {
                ExploreScreen(
                    tiles = MIXED_SHELF,
                    wishes = 0,
                    images = emptyMap(),
                    onOpenPlate = {},
                    onOpenWishes = {},
                )
            }
        }

        compose.onNodeWithText(MIXED_NOTE).assertDoesNotExist()

        compose.onNodeWithText(ShowcaseSort.ByEntryCost.label).performClick()

        compose.onNodeWithText(MIXED_NOTE).assertIsDisplayed()
    }

    /**
     * A shelf with no price anywhere on it is told the order has nothing to sort by (#513).
     *
     * This is the state the shelf is **born** in (ADR 0030 §3): every plate is valued by hand, so a
     * collector who has valued none of them presses «Por coste de entrar» and the grid barely moves.
     * Silence there is what reads as a broken control.
     */
    @Test
    fun anUnvaluedShelfIsToldTheOrderHasNoPricesToSortBy() {
        compose.setContent {
            CoindexTheme {
                ExploreScreen(
                    tiles = listOf(shelfTile("panda"), shelfTile("kooka")),
                    wishes = 0,
                    images = emptyMap(),
                    onOpenPlate = {},
                    onOpenWishes = {},
                )
            }
        }

        compose.onNodeWithText(ShowcaseSort.ByEntryCost.label).performClick()

        compose.onNodeWithText(ShowcaseLabels.NOTHING_VALUED).assertIsDisplayed()
    }
}

/** A plate of the shelf window with the one fact the order reads: whether it carries an amount. */
private fun shelfTile(catalogId: String, entryEur: Double? = null) = ShowcaseTile(
    catalogId = catalogId,
    name = catalogId,
    typeId = null,
    printedSide = PrintedSide.Reverse,
    mine = false,
    footnote = showcaseSlotsLabel(3),
    entryEur = entryEur,
    slots = 3,
)
