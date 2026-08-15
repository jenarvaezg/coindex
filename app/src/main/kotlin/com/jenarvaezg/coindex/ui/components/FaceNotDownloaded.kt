package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.FACE_NOT_DOWNLOADED
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * What a turned hole says when the face that came round is not on this phone yet (#509).
 *
 * The audit of 14 August 2026 turned coins over with the prefetch still pending and got a blank
 * disc — indistinguishable from a broken image, and the ticket's own complaint. The turn is kept
 * (the collector asked for the other face and the app answered) and the emptiness is named, which
 * is the only one of the three candidates that reports anything: not turning at all repeats the
 * dead target #508 just removed, and a jolt answers the finger without saying what happened.
 *
 * **No chip under it, unlike [HoleStamp].** That one is a note laid over a design it has to stay
 * legible against; this one falls on a disc where there is nothing at all, so paper under the words
 * would be drawing a second empty thing on top of the first.
 *
 * It does not turn with the coin: the sentence belongs to the hole, which is what stayed still.
 */
@Composable
fun FaceNotDownloaded(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(horizontal = NOTICE_PADDING), Alignment.Center) {
        Text(
            FACE_NOT_DOWNLOADED,
            style = MaterialTheme.typography.labelLarge,
            color = Paper.muted,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Room for the sentence to wrap inside the metal rather than against the wall of the die-cut.
 *
 * The hole is 104 dp on a plate and the ring already takes 5 of them; 10 dp more on each side is
 * what keeps three short lines off the edge without shrinking the type.
 */
private val NOTICE_PADDING = 10.dp
