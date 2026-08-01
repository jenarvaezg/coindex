package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.data.TypeImages
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.ui.numistaTypeUrl
import com.jenarvaezg.coindex.ui.pieceLine
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * One piece of the collection as the collector recorded it: both sides, its title, the year on
 * the row and how many of it there are.
 *
 * This is a row of the inventory, not a catalog member: it never says «me falta», because a
 * piece that is here is a piece you own. [extra] is for whatever the screen showing it needs to
 * add — the reason it is unclassified, a selection control.
 */
@Composable
fun PieceCard(
    item: CollectedItem,
    title: String,
    images: TypeImages?,
    onOpenSource: (String) -> Unit,
    modifier: Modifier = Modifier,
    emissionLabel: String? = null,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    FieldCard(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CoinSides(
                label = title,
                obverse = images?.obverse,
                reverse = images?.reverse,
                missing = false,
                modifier = Modifier.width(150.dp),
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    pieceLine(item, emissionLabel),
                    style = MaterialTheme.typography.labelLarge,
                    color = Paper.muted,
                    modifier = Modifier.padding(top = 4.dp),
                )
                extra()
                ExternalLink(
                    text = "Ver en Numista",
                    onClick = { onOpenSource(numistaTypeUrl(item.typeId)) },
                )
            }
        }
    }
}
