package com.jenarvaezg.coindex.ui.screens

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
import com.jenarvaezg.coindex.ui.components.LocalCoinGloss
import com.jenarvaezg.coindex.ui.components.LocalPhotoNotices
import com.jenarvaezg.coindex.ui.components.LocalStamping
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
 * Waits until every picture of what is being drawn has reported back, or until the budget runs out.
 *
 * One wait for the two exports there are, because the waiting is the delicate part and it does not
 * depend on what is being drawn: what is not painted when this returns is a hole in what is about to
 * be captured, whether that is the PNG of one lámina or a page of the notebook's PDF.
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
 * Composes a page at the density of the paper and off the screen, so it can be measured whole.
 *
 * Unbounded, because the point of an export is that the sheet is taller than any screen, and sized
 * to nothing so it never lands on the page it is being exported from.
 */
@Composable
fun OffScreenSheet(density: Density, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(0.dp)
            .wrapContentSize(unbounded = true, align = Alignment.TopStart),
    ) {
        CompositionLocalProvider(
            LocalDensity provides density,
            // The export rule of ADR 0026 §4, in the one place both exports pass through: what is
            // still travels to paper, what is alive does not. The gloss follows a sensor, so a PNG
            // must not carry it — not even the pose it rests in. It is accepted knowingly that what
            // the father shows other people carries no metal: the app is where he looks at his
            // collection, the PNG is where he shows it.
            LocalCoinGloss provides null,
            // The same rule, and the same line, cutting the other way (#339): the **stamp** is a
            // state and travels, the **stamping** is alive and does not. A sheet composed off
            // screen finds the ink already dry rather than watching it fall into a picture.
            LocalStamping provides null,
            // The same rule for a notice and not for a movement (#510): the mark of a photograph
            // that has not arrived asks **this** phone to find a wifi, and the PNG is what the
            // father sends to somebody else. On paper the hole keeps the stand-in disc it has
            // always had, which is a picture of an album and not of a download.
            LocalPhotoNotices provides false,
            content = content,
        )
    }
}
