package com.jenarvaezg.coindex.ui.screens

import android.content.Context
import android.graphics.Picture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.IntState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.ui.DownloadedExport
import com.jenarvaezg.coindex.ui.ExportDestination
import com.jenarvaezg.coindex.ui.SharedSheet
import com.jenarvaezg.coindex.ui.UiNotice
import com.jenarvaezg.coindex.ui.components.LocalCoinGloss
import com.jenarvaezg.coindex.ui.components.LocalStamping
import com.jenarvaezg.coindex.ui.components.coinSideImageCount
import com.jenarvaezg.coindex.ui.downloadLandedNotice
import com.jenarvaezg.coindex.ui.downloadPlateSheet
import com.jenarvaezg.coindex.ui.printedPhoto
import com.jenarvaezg.coindex.ui.recordInto
import com.jenarvaezg.coindex.ui.sharePlateSheet
import com.jenarvaezg.coindex.ui.sheetDownloadFailure
import com.jenarvaezg.coindex.ui.sheetExportFailure
import com.jenarvaezg.coindex.ui.sheetExportMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * How long an export waits for the pictures before capturing whatever is on the sheet.
 *
 * Twenty seconds was the whole budget when a refused picture failed instantly. Now a throttled one
 * is retried with a wait, and four at a time queue behind each other, so the old ceiling would have
 * cut the retries off exactly on the sheets that need them (issue #67).
 */
const val IMAGE_WAIT_MILLIS = 30_000L

/**
 * Composes a sheet off-screen, waits for every picture to settle, exports it as a PNG, and says
 * what it did.
 *
 * **The whole cycle and not the pieces of it.** A plate and a collection's pieces used to spell this
 * out line for line — a [Picture], a [SheetLayout], the count of pictures to expect, two counters,
 * an effect that shares and reports, and the off-screen composition that draws into the recording —
 * and the two copies had already drifted (#219). What genuinely differs between the two exports is
 * four values: how many members the geometry is for, where a member's Numista type is read from,
 * what the file is called, and what the sheet says it holds. They are the parameters; the rest is
 * here. [destination] is Descargas or the share sheet (#285).
 *
 * **What fits in one page is a PNG** (#401). The options panel measures the pages first; when
 * there is one, Descargar / Compartir leave through this cycle as a bitmap. When there are more,
 * the same doors take the PDF section of the notebook instead, and the switches that only paper
 * can honour apply for real.
 *
 * The sheet is measured with its own density and **never painted**: [recordInto] captures the
 * drawing commands instead of drawing them, so what gets shared is the complete sheet rather than
 * the part that happens to fit on a screen.
 *
 * Two counters and not one, because they answer different questions: settled is «has every picture
 * reported back», which is when it is safe to capture, and loaded is «did it arrive», which is what
 * the closing sentence is about. A picture that failed reports back exactly like one that arrived,
 * and conflating them is what announced a sheet of twelve empty cells as complete (issue #67).
 *
 * The whole of it is keyed on [key] — the catalog's id, the collection's title — so exporting a
 * different subject starts a fresh recording **and fresh counters** rather than continuing the
 * previous one's. A [String] and not an `Any`: a subject passed whole would compare by identity on
 * some types and restart the export on every recomposition.
 */
@Composable
fun <T> SheetExport(
    key: String,
    /** What the sheet draws, a slot or a row: sizes the grid, says which pictures to wait for. */
    items: List<T>,
    images: Map<Int, TypeImages>,
    /** Where an item's Numista type is read. A catalog's slot may have none; a piece always has. */
    typeId: (T) -> Int?,
    /** A plate asks for its declared face; a pieces sheet keeps asking for both. */
    printedSide: PrintedSide? = null,
    /** What this export is called, which is what the closing sentence and any failure are about. */
    sheet: SharedSheet,
    /** What the sheet says it holds, in its own words: «19 casillas», «4 de 12 · te faltan 8». */
    tally: String,
    fileName: String,
    destination: ExportDestination = ExportDestination.Download,
    onFinished: (UiNotice) -> Unit,
    /** Draws the sheet: at the geometry given, reporting each picture, into the recording. */
    content: @Composable (
        layout: SheetLayout,
        onImageSettled: (painted: Boolean) -> Unit,
        recording: Modifier,
    ) -> Unit,
) {
    val context = LocalContext.current
    val picture = remember(key) { Picture() }
    val layout = remember(items.size) { SheetLayout.forMemberCount(items.size) }
    // [typeId] is one of the keys and not just a captured lambda: both callers pass a
    // non-capturing one today, and a capturing one would leave a stale count behind — the very
    // kind of silent drift this composable exists to make impossible.
    val expectedImages = remember(items, images, typeId, printedSide) {
        sheetImageCount(items, images, printedSide, typeId)
    }
    val settled = remember(key) { mutableIntStateOf(0) }
    val loaded = remember(key) { mutableIntStateOf(0) }

    LaunchedEffect(key, destination) {
        val outcome = exportSettledSheet(
            context = context,
            picture = picture,
            fileName = fileName,
            expectedImages = expectedImages,
            settled = settled,
            destination = destination,
        )
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
                            expectedPhotos = expectedImages,
                            loadedPhotos = loaded.intValue,
                            uri = landed.uri,
                            mimeType = landed.mimeType,
                        )
                    }
                    ExportDestination.Share ->
                        UiNotice(
                            sheetExportMessage(sheet, tally, expectedImages, loaded.intValue),
                        )
                }
            },
        )
    }

    OffScreenSheet(layout) {
        content(
            layout,
            { painted ->
                settled.intValue += 1
                if (painted) loaded.intValue += 1
            },
            // The sheet paints its own paper; recording it from the outside would drop it.
            Modifier.recordInto(picture),
        )
    }
}

/**
 * Total pictures a sheet will request, so the export knows when it can capture.
 *
 * One function for both sheets: a catalog's slots and a collection's rows differ only in where the
 * Numista type is read from, and two copies of this arithmetic is how one of them would come to
 * wait for a photograph the other never requests.
 */
fun <T> sheetImageCount(
    items: List<T>,
    images: Map<Int, TypeImages>,
    printedSide: PrintedSide? = null,
    typeId: (T) -> Int?,
): Int = items.sumOf { item ->
    val typeImages = typeId(item)?.let { images[it] }
    if (printedSide == null) {
        coinSideImageCount(typeImages?.obverse, typeImages?.reverse)
    } else {
        if (typeImages?.printedPhoto(printedSide)?.hasPicture == true) 1 else 0
    }
}

/**
 * Waits for a sheet's pictures to settle, then writes it out to [destination].
 *
 * The step [SheetExport] is built around, kept apart from it because the waiting is the delicate
 * part and it is identical for the two sheets there are: what differs is what gets drawn, not when
 * it is safe to capture it. Descargas and the share sheet are two endings of the same wait (#285).
 *
 * Timing out and failing outright come to the same thing here: whatever had not painted by now is
 * a hole in the sheet that is about to leave, which is why the caller is handed [settled] to
 * count against and reports the shortfall in its own words.
 */
suspend fun exportSettledSheet(
    context: Context,
    picture: Picture,
    fileName: String,
    expectedImages: Int,
    settled: IntState,
    destination: ExportDestination = ExportDestination.Download,
): Result<DownloadedExport?> {
    awaitSettledImages(expectedImages, settled)
    return runCatching {
        when (destination) {
            ExportDestination.Download -> downloadPlateSheet(context, picture, fileName)
            ExportDestination.Share -> {
                sharePlateSheet(context, picture, fileName)
                null
            }
        }
    }
}

/**
 * Waits until every picture of what is being drawn has reported back, or until the budget runs out.
 *
 * Split out of [exportSettledSheet] because the printed notebook waits the same way and then does
 * something else with the result — a page of a PDF rather than a file to share — and the waiting is
 * the delicate part: what is not painted when this returns is a hole in what is about to be
 * captured, in all three exports alike.
 *
 * [timeoutMillis] is a **ceiling and not a cost**: what is already cached settles in a frame. The
 * notebook passes a shorter one because it has warmed every photograph first, so a page that has
 * not settled by then is not going to.
 */
suspend fun awaitSettledImages(
    expectedImages: Int,
    settled: IntState,
    timeoutMillis: Long = IMAGE_WAIT_MILLIS,
) {
    withTimeoutOrNull(timeoutMillis) {
        snapshotFlow { settled.intValue }.first { it >= expectedImages }
    }
    // The last picture reports before it is drawn, so let a frame land either way.
    withFrameNanos {}
    withFrameNanos {}
}

/**
 * Composes a sheet at its own density and off the screen, so it can be measured whole.
 *
 * Unbounded, because the point of the export is that a long sheet is taller than any screen, and
 * sized to nothing so it never lands on the page it is being exported from.
 */
@Composable
fun OffScreenSheet(layout: SheetLayout, content: @Composable () -> Unit) =
    OffScreenSheet(layout.density, content)

/** The same, for what is measured in millimetres of paper rather than in cells of a bitmap. */
@Composable
fun OffScreenSheet(density: Density, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(0.dp)
            .wrapContentSize(unbounded = true, align = Alignment.TopStart),
    ) {
        CompositionLocalProvider(
            LocalDensity provides density,
            // The export rule of ADR 0026 §4, in the one place all three exports pass through:
            // what is still travels to paper, what is alive does not. The gloss follows a sensor,
            // so a PNG must not carry it — not even the pose it rests in. It is accepted knowingly
            // that what the father shows other people carries no metal: the app is where he looks
            // at his collection, the PNG is where he shows it.
            LocalCoinGloss provides null,
            // The same rule, and the same line, cutting the other way (#339): the **stamp** is a
            // state and travels, the **stamping** is alive and does not. A sheet composed off
            // screen finds the ink already dry rather than watching it fall into a picture.
            LocalStamping provides null,
            content = content,
        )
    }
}
