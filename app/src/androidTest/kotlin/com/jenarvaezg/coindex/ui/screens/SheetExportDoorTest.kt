package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.ui.CANCEL_ACTION
import com.jenarvaezg.coindex.ui.DOWNLOAD_ACTION
import com.jenarvaezg.coindex.ui.NOTEBOOK_OPTIONS_EYEBROW
import com.jenarvaezg.coindex.ui.SHARE_ACTION
import com.jenarvaezg.coindex.ui.SharedSheet
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.sheetExportLabel
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * One door in, and the destination asked once, inside (#434). Descargar and Compartir belong to the
 * «Cómo se exporta» panel, so either one on screen before it opens means a second entrance is back.
 *
 * It mounts [SheetExportFlow], the door the lámina and the hoja both hang off their heading.
 */
@RunWith(AndroidJUnit4::class)
class SheetExportDoorTest {
    @get:Rule
    val compose = createComposeRule()

    private val door = sheetExportLabel(SharedSheet.PLATE, exporting = false)

    private fun mountTheDoor() {
        compose.setContent {
            CoindexTheme {
                SheetExportFlow(
                    sheet = SharedSheet.PLATE,
                    key = "prueba",
                    fileName = "prueba",
                    notebookOptions = NotebookOptions(),
                    onNotebookPrinted = {},
                    // No pages: what is under test is the flow, and the panel prints its cost over
                    // whatever it is handed.
                    notebookPages = { emptyList() },
                    onExporting = {},
                    onMessage = {},
                    tally = "12 casillas",
                ) { export ->
                    Column {
                        SheetExportDoorButton(export.door)
                        export.options?.invoke()
                        export.progress?.invoke()
                    }
                }
            }
        }
    }

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun theDestinationIsAskedInsideThePanelAndNowhereElse() {
        mountTheDoor()

        // One way in, with no destination on it.
        compose.onAllNodesWithText(door).assertCountEquals(1)
        compose.onAllNodesWithText(DOWNLOAD_ACTION).assertCountEquals(0)
        compose.onAllNodesWithText(SHARE_ACTION).assertCountEquals(0)

        compose.onNodeWithText(door).performClick()

        // Inside, each destination once.
        compose.onNodeWithText(NOTEBOOK_OPTIONS_EYEBROW).assertExists()
        compose.onAllNodesWithText(DOWNLOAD_ACTION).assertCountEquals(1)
        compose.onAllNodesWithText(SHARE_ACTION).assertCountEquals(1)
    }

    @Test
    // #512: a greyed-out door repeating the question already on screen read as broken.
    fun theDoorCedesItsPlaceToThePanelAndComesBackWithCancelar() {
        mountTheDoor()

        compose.onNodeWithText(door).performClick()

        compose.onAllNodesWithText(door).assertCountEquals(0)

        compose.onNodeWithText(CANCEL_ACTION).performClick()

        compose.onAllNodesWithText(door).assertCountEquals(1)
        compose.onAllNodesWithText(NOTEBOOK_OPTIONS_EYEBROW).assertCountEquals(0)
    }
}
