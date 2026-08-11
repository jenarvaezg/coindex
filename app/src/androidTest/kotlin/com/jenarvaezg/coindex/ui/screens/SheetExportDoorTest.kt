package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jenarvaezg.coindex.ui.DOWNLOAD_ACTION
import com.jenarvaezg.coindex.ui.NOTEBOOK_OPTIONS_EYEBROW
import com.jenarvaezg.coindex.ui.SHARE_ACTION
import com.jenarvaezg.coindex.ui.SharedSheet
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.sheetExportLabel
import com.jenarvaezg.coindex.ui.theme.CoindexTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * One door in, and the destination asked once — inside (#434).
 *
 * The lámina and the hoja used to offer «Descargar lámina» **and** «Compartir», and since #401 both
 * opened the same «Cómo se exporta» card, which ends in Descargar / Compartir / Cancelar. The words
 * are the measure of the defect: Descargar and Compartir belong to the panel, so finding either of
 * them on the screen before the panel is open is the second entrance coming back.
 *
 * It mounts [SheetExportFlow] rather than a whole screen, because that is where the door now lives
 * and it is the same one both screens hang off their heading.
 */
@RunWith(AndroidJUnit4::class)
class SheetExportDoorTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun theDestinationIsAskedInsideThePanelAndNowhereElse() {
        val door = sheetExportLabel(SharedSheet.PLATE, exporting = false)

        compose.setContent {
            CoindexTheme {
                SheetExportFlow(
                    sheet = SharedSheet.PLATE,
                    key = "prueba",
                    fileName = "prueba",
                    notebookOptions = NotebookOptions(),
                    onNotebookPrinted = {},
                    // No page is drawn to run this: what is under test is the conversation, and
                    // the panel prints its cost over whatever it is handed.
                    notebookPages = { emptyList() },
                    onExporting = {},
                    onMessage = {},
                    bitmap = { _, _ -> },
                ) { export ->
                    Column {
                        PrimaryAction(
                            text = export.label,
                            onClick = export.onExport,
                            enabled = export.enabled,
                        )
                        export.options?.invoke()
                        export.progress?.invoke()
                    }
                }
            }
        }

        // One way in, and it promises a conversation rather than a destination.
        compose.onAllNodesWithText(door).assertCountEquals(1)
        compose.onAllNodesWithText(DOWNLOAD_ACTION).assertCountEquals(0)
        compose.onAllNodesWithText(SHARE_ACTION).assertCountEquals(0)

        compose.onNodeWithText(door).performClick()

        // And inside it, the destination — once each.
        compose.onNodeWithText(NOTEBOOK_OPTIONS_EYEBROW).assertExists()
        compose.onAllNodesWithText(DOWNLOAD_ACTION).assertCountEquals(1)
        compose.onAllNodesWithText(SHARE_ACTION).assertCountEquals(1)
    }
}
