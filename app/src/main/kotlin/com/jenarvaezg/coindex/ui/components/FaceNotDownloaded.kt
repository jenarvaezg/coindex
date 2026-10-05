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
 * What a turned hole says when the face that came round isn't on this phone yet (#509), instead of
 * a blank disc that looks like a broken image. No paper chip as in [HoleStamp]: there is no design
 * under it to stay legible against. It doesn't turn with the coin.
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

/** Keeps the wrapped sentence off the die-cut wall in a 104 dp hole without shrinking the type. */
private val NOTICE_PADDING = 10.dp
