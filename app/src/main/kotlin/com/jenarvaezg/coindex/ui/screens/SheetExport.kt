package com.jenarvaezg.coindex.ui.screens

import android.content.Context
import android.graphics.Picture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.IntState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.sharePlateSheet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * How long an export waits for the pictures before capturing whatever is on the sheet.
 *
 * Twenty seconds was the whole budget when a refused picture failed instantly. Now a throttled one
 * is retried with a wait, and four at a time queue behind each other, so the old ceiling would have
 * cut the retries off exactly on the sheets that need them (issue #67).
 */
private const val IMAGE_WAIT_MILLIS = 30_000L

/**
 * Waits for a sheet's pictures to settle, then writes it out and hands it to the share sheet.
 *
 * Shared by the two sheets there are — a plate and a collection's pieces — because the waiting is
 * the delicate part and it is identical for both: what differs is what gets drawn, not when it is
 * safe to capture it.
 *
 * Timing out and failing outright come to the same thing here: whatever had not painted by now is
 * a hole in the sheet that is about to be shared, which is why the caller is handed [settled] to
 * count against and reports the shortfall in its own words.
 */
suspend fun shareSettledSheet(
    context: Context,
    picture: Picture,
    fileName: String,
    expectedImages: Int,
    settled: IntState,
): Result<Unit> {
    awaitSettledImages(expectedImages, settled)
    return runCatching { sharePlateSheet(context, picture, fileName) }
}

/**
 * Waits until every picture of what is being drawn has reported back, or until the budget runs out.
 *
 * Split out of [shareSettledSheet] because the printed notebook waits the same way and then does
 * something else with the result — a page of a PDF rather than a file to share — and the waiting is
 * the delicate part: what is not painted when this returns is a hole in what is about to be
 * captured, in all three exports alike.
 */
suspend fun awaitSettledImages(expectedImages: Int, settled: IntState) {
    withTimeoutOrNull(IMAGE_WAIT_MILLIS) {
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
        CompositionLocalProvider(LocalDensity provides density, content = content)
    }
}
