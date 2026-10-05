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
 * One plate with an amount and one without. The note comes from [showcaseOrderNote], whose wording
 * `ShowcaseLabelsTest` pins; this file checks only when the screen prints it.
 */
private val MIXED_SHELF = listOf(shelfTile("panda", entryEur = 412.0), shelfTile("kooka"))

private val MIXED_NOTE = showcaseOrderNote(ShowcaseSort.ByEntryCost, MIXED_SHELF)!!

/**
 * The order of «Explorar»: which one is on, and what it could not place (#513). Both are drawn (a
 * fill and a line of type), so they are measured as Compose renders them.
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

    /** Only the cost order shows the note: «por casillas» sorts by what every plate has. */
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

    /** #513: the state a shelf starts in (ADR 0030 §3); without the note the order looks broken. */
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
