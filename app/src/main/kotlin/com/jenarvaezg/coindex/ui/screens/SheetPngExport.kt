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
 * How much finer than the paper the shared PNG is drawn.
 *
 * The page is measured in millimetres and drawn from vector commands, so the number of pixels is a
 * free choice made at the last moment: two is what keeps the file at the 2.520 px of width the plate
 * had when it was a bitmap of its own, rather than the 1.260 the printer's density would give. It
 * costs nothing in fidelity — nothing is upscaled — and about four megabytes of ARGB while the
 * recording is being written.
 */
private const val PNG_SCALE = 2f

/** One dp is half a millimetre: the printed page, at twice the paper's resolution. */
private val pngDensity = Density(density = PrintGeometry.PX_PER_MM * PNG_SCALE, fontScale = 1f)

/**
 * A single lámina or hoja as a PNG: the printed page, on the folio it needs (#431).
 *
 * **The same drawing as the PDF, and no longer one of its own.** A plate used to have two: the page
 * of the notebook, which honours every switch, and `PlateSheet`, which received none — so the panel
 * had to annotate all five with «Sólo en el cuaderno» whenever the measure came out a PNG. What was
 * measured (#431) is that the two differ in the folio and not in the drawing, so what is left is the
 * page with its blank third cut off ([trimmedToContent]). Fotos, ambas caras, tamaño real, QR and el
 * valor now reach the file, and the ruler at the foot comes with them — the PNG never had one, and
 * it is what makes «tamaño real» falsifiable in a viewer that offers «ajustar a la página».
 *
 * The wait is the notebook's and so is the arithmetic of holes: what a page asks for is
 * [PrintPage.photographs], and a photograph that never arrived is a gap in something about to be
 * sent to somebody — said out loud, and never a reason to fail the export (#67).
 */
@Composable
fun SheetPngExport(
    page: PrintPage,
    destination: ExportDestination,
    fileName: String,
    sheet: SharedSheet,
    /** What the sheet says it holds, in its own words: «19 casillas», «4 de 12 · te faltan 8». */
    tally: String,
    onFinished: (UiNotice) -> Unit,
) {
    val context = LocalContext.current
    // Falls back to the folio it was counted on: a page that will not trim is one that needs the
    // whole sheet of paper, and half a lámina is not an improvement over an empty third.
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
