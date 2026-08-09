package com.jenarvaezg.coindex.ui.components

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FilterShelfTest {
    @get:Rule
    val compose = createComposeRule()

    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    @Test
    fun theTrailingActionIsTouchSizedAndSharesTheShelfLabelsVerticalCentre() {
        compose.setContent {
            CoindexTheme {
                FilterShelf(
                    summary = "Todas",
                    tally = "47 colecciones",
                    expanded = false,
                    onToggle = {},
                    actionLabel = "Exportar láminas",
                    onAction = {},
                ) {}
            }
        }

        compose.onNodeWithText("Exportar láminas").assertHeightIsAtLeast(48.dp)

        val tallyCentre = compose.onNodeWithText("47 colecciones", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
            .center.y
        val separatorCentre = compose.onNodeWithText(" · ", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
            .center.y
        val actionCentre = compose.onNodeWithText("Exportar láminas", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
            .center.y

        assertEquals(tallyCentre, separatorCentre, 1f)
        assertEquals(tallyCentre, actionCentre, 1f)
    }
}
