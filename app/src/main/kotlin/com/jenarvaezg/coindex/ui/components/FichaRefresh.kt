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
 * What one type's ficha needs to be asked for again (#185, ADR 0025), bundled because it travels
 * through two screens.
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
 * When the ficha was fetched, with the button to fetch it again: the date lets the collector judge
 * whether the call is worth it. It says when, not how fresh: a ficha shipped in the APK may be
 * older than its arrival here (ADR 0025). Nothing is drawn without a ficha; the next sync will
 * bring it.
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
