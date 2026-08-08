package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.fichaAgeLabel
import com.jenarvaezg.coindex.ui.fichaRefreshLabel
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * What one type's ficha needs to be asked for again (#185, ADR 0025).
 *
 * A value rather than four parameters, because it travels through two screens to reach the two
 * places a piece of a given type is visible: the piece inside a collection, and the row of Coins.
 *
 * @param fetchedAt when this phone got the ficha; null when it has none at all, which is a piece
 *   waiting for a sync to complete rather than a stale ficha.
 */
data class FichaRefresh(
    val fetchedAt: Long?,
    val refreshing: Boolean,
    val onRefresh: () -> Unit,
)

/**
 * When the ficha was brought and the gesture that brings it again, drawn together because neither
 * means much alone: a date with no way to act on it is trivia, and an action with no date is a
 * button whose cost the collector cannot judge.
 *
 * Named for the date and not for freshness: whether a ficha is *fresh* is exactly what this cannot
 * say (ADR 0025), since a ficha that arrived in the APK may hold older content than the day it
 * landed here.
 *
 * A piece with no ficha yet gets no line and no button: nothing has gone stale, the sync simply has
 * not finished bringing it, and one call spent here would be a call the next sync spends anyway.
 */
@Composable
fun FichaBrought(
    ficha: FichaRefresh,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val fetchedAt = ficha.fetchedAt ?: return
    Column(modifier = modifier) {
        Text(
            fichaAgeLabel(fetchedAt, nowMillis),
            style = MaterialTheme.typography.labelSmall,
            color = Paper.muted,
        )
        CardAction(
            text = fichaRefreshLabel(ficha.refreshing),
            onClick = ficha.onRefresh,
            enabled = !ficha.refreshing,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
