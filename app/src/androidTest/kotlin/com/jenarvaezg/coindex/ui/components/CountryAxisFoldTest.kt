package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.ui.shelf.CountryAxisBlock
import com.jenarvaezg.coindex.ui.shelf.CountryAxisCell
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Android's minimum, the same one the year tag of a casilla is measured against (#302, #473). */
private val MINIMUM_TARGET = 48.dp

/**
 * The width a country block gets on the Pixel 7 of the measurements: 411 dp of screen less the
 * two 12 dp page margins. The label column and its gap come out of it inside the row.
 */
private val PHONE_BLOCK_WIDTH = 387.dp

/**
 * The fold of the country axis on a phone (#417).
 *
 * The unit test fixes the arithmetic; this fixes the two things only a device can answer: that
 * 387 dp of block measures **seven** holes to a row — the number the whole decision was taken on —
 * and that the mark is a target of its own, big enough to press and quiet about the country behind
 * it. No photographs: the fold is about how many holes there are, not what is in them.
 */
@RunWith(AndroidJUnit4::class)
class CountryAxisFoldTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aPhoneBlockMeasuresSevenHolesToARowSoVenezuelaFoldsSixtySix() {
        content(venezuela())

        compose.onNodeWithText(FOLDED).assertExists()
    }

    @Test
    fun theMarkIsATargetOfItsOwnAndDoesNotOpenTheCountryBehindIt() {
        var opened = 0
        var toggled = 0
        content(venezuela(), onCountryClick = { opened += 1 }, onToggleFold = { toggled += 1 })

        val target = compose.onNodeWithText(FOLDED).fetchSemanticsNode().touchBoundsInRoot
        val minimum = with(compose.density) { MINIMUM_TARGET.toPx() }
        assertTrue("${target.width} × ${target.height}", target.height >= minimum)

        compose.onNodeWithText(FOLDED).performClick()
        assertEquals(1, toggled)
        // The block behind it opens Monedas; the mark opens holes. One tap does one of the two.
        assertEquals(0, opened)
    }

    @Test
    fun anOpenFoldPaintsEveryHoleAndNamesTheWayBack() {
        content(venezuela(), expanded = true)

        compose.onNodeWithText("Plegar las 66").assertExists()
    }

    @Test
    fun aCountryWhoseAbsencesFitOneRowShowsNoMark() {
        // Sudáfrica 2/9: seven absences, one row, nothing to hide.
        content(block(country = "Sudáfrica", owned = 2, issued = 9))

        assertTrue(
            compose.onAllNodesWithText("faltan", substring = true)
                .fetchSemanticsNodes()
                .isEmpty(),
        )
    }

    private fun content(
        block: CountryAxisBlock,
        expanded: Boolean = false,
        onCountryClick: (String) -> Unit = {},
        onToggleFold: (String) -> Unit = {},
    ) {
        compose.setContent {
            CoindexTheme {
                Box(modifier = Modifier.width(PHONE_BLOCK_WIDTH)) {
                    CountryAxisRow(
                        block = block,
                        images = emptyMap(),
                        onCountryClick = onCountryClick,
                        expanded = expanded,
                        onToggleFold = onToggleFold,
                    )
                }
            }
        }
    }

    private fun venezuela() = block(country = "Venezuela", owned = 42, issued = 115)

    private fun block(country: String, owned: Int, issued: Int) = CountryAxisBlock(
        country = country,
        owned = owned,
        issued = issued,
        cells = (0 until issued).map { index ->
            CountryAxisCell.Slot(
                catalogId = "c",
                memberId = "m$index",
                typeId = null,
                owned = index < owned,
                quantity = if (index < owned) 1 else 0,
            )
        },
    )

    private companion object {
        /** Venezuela 42/115 with seven holes to a row: 73 absences, one row shown, 66 folded. */
        const val FOLDED = "… y faltan 66"
    }
}
