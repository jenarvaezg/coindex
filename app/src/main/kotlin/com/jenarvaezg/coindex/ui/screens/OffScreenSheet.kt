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
import com.jenarvaezg.coindex.ui.components.LocalStamping
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * How long an export waits for its pictures before capturing whatever is on the sheet. Long enough
 * for throttled pictures to be retried while queued four at a time (#67).
 */
const val IMAGE_WAIT_MILLIS = 30_000L

/**
 * Waits until every picture being drawn has reported back, or until [timeoutMillis]. Shared by the
 * lámina PNG and the notebook PDF: whatever hasn't painted by then is a hole in the capture.
 *
 * [timeoutMillis] is a ceiling, not a cost: cached pictures settle in a frame. The notebook passes
 * a shorter one because it warms every photograph first.
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
 * Composes a page off screen at the paper's density, so it can be measured whole. Unbounded because
 * an export is taller than any screen, and zero-sized so it never shows on the current screen.
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
            // ADR 0026 §4: static things travel to paper, live ones don't. The gloss follows a
            // sensor, so exports carry none, not even its resting pose.
            LocalCoinGloss provides null,
            // Same rule (#339): the stamp is printed, its animation is not.
            LocalStamping provides null,
            content = content,
        )
    }
}
