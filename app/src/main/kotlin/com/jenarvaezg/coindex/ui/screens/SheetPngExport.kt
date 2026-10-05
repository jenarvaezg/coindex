package com.jenarvaezg.coindex.ui.screens

import android.graphics.Picture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import com.jenarvaezg.coindex.ui.ExportDestination
import com.jenarvaezg.coindex.ui.SharedSheet
import com.jenarvaezg.coindex.ui.UiNotice
import com.jenarvaezg.coindex.ui.downloadLandedNotice
import com.jenarvaezg.coindex.ui.downloadPlateSheet
import com.jenarvaezg.coindex.ui.print.PrintGeometry
import com.jenarvaezg.coindex.ui.print.PrintPage
import com.jenarvaezg.coindex.ui.print.trimmedToContent
import com.jenarvaezg.coindex.ui.recordInto
import com.jenarvaezg.coindex.ui.sharePlateSheet
import com.jenarvaezg.coindex.ui.sheetDownloadFailure
import com.jenarvaezg.coindex.ui.sheetExportFailure
import com.jenarvaezg.coindex.ui.sheetExportMessage

/**
 * How many times the print density the PNG is rendered at. The page is vector commands, so this is
 * free: 2 keeps the width of the old plate bitmap (2.520 px) at the cost of about 4 MB of ARGB
 * while writing.
 */
private const val PNG_SCALE = 2f

/** Still one dp per millimetre, as in [printDensity], at [PNG_SCALE] times the pixels. */
private val pngDensity = Density(density = PrintGeometry.PX_PER_MM * PNG_SCALE, fontScale = 1f)

/**
 * A single lámina or hoja as a PNG: the printed notebook page, trimmed to its content (#431). Every
 * notebook switch applies, and the ruler at the foot lets «tamaño real» be checked in a viewer.
 *
 * Waits like the notebook and counts missing photographs ([PrintPage.photographs]) in the closing
 * message; a missing photograph never fails the export (#67).
 */
@Composable
fun SheetPngExport(
    page: PrintPage,
    destination: ExportDestination,
    fileName: String,
    sheet: SharedSheet,
    /** What the sheet holds, e.g. «19 casillas» or «4 de 12 · te faltan 8». */
    tally: String,
    onFinished: (UiNotice) -> Unit,
) {
    val context = LocalContext.current
    // A page that won't trim needs the whole folio.
    val printed = remember(page) { page.trimmedToContent() ?: page }
    val picture = remember(printed) { Picture() }
    val settled = remember(printed) { mutableIntStateOf(0) }
    val loaded = remember(printed) { mutableIntStateOf(0) }

    LaunchedEffect(printed, destination) {
        awaitSettledImages(printed.photographs, settled)
        val outcome = runCatching {
            when (destination) {
                ExportDestination.Download -> downloadPlateSheet(context, picture, fileName)
                ExportDestination.Share -> {
                    sharePlateSheet(context, picture, fileName)
                    null
                }
            }
        }
        onFinished(
            if (outcome.isFailure) {
                val cause = outcome.exceptionOrNull()?.message
                UiNotice(
                    when (destination) {
                        ExportDestination.Download -> sheetDownloadFailure(sheet, cause)
                        ExportDestination.Share -> sheetExportFailure(sheet, cause)
                    },
                )
            } else {
                when (destination) {
                    ExportDestination.Download -> {
                        val landed = requireNotNull(outcome.getOrThrow())
                        downloadLandedNotice(
                            expectedPhotos = printed.photographs,
                            loadedPhotos = loaded.intValue,
                            uri = landed.uri,
                            mimeType = landed.mimeType,
                        )
                    }
                    ExportDestination.Share ->
                        UiNotice(
                            sheetExportMessage(
                                sheet,
                                tally,
                                printed.photographs,
                                loaded.intValue,
                            ),
                        )
                }
            },
        )
    }

    OffScreenSheet(pngDensity) {
        NotebookPageSheet(
            page = printed,
            onImageSettled = { painted ->
                settled.intValue += 1
                if (painted) loaded.intValue += 1
            },
            // The page paints its own paper; recording it from the outside would drop it.
            modifier = Modifier.recordInto(picture),
        )
    }
}
