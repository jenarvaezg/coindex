package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.ui.DrawnPiece
import com.jenarvaezg.coindex.ui.CoinName
import com.jenarvaezg.coindex.ui.COIN_VIEW_ON_NUMISTA
import com.jenarvaezg.coindex.ui.numistaTypeUrl
import com.jenarvaezg.coindex.ui.pieceLine
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * One piece of the collection as the collector recorded it: both sides, its title, what names it —
 * the year on the row, or the emission where the year names nothing — and how many of it there are.
 *
 * This is a row of the inventory, not a catalog member: it never says «me falta», because a
 * piece that is here is a piece you own. [extra] is for whatever the screen showing it needs to
 * add — the reason it is unclassified, a selection control.
 *
 * [ficha] is how old what the card is saying actually is, and the gesture that asks Numista again
 * (#185): the title, the year and both photographs come from a ficha that was cached once and, until
 * this existed, never questioned again.
 */
@Composable
fun PieceCard(
    piece: DrawnPiece,
    name: CoinName,
    images: TypeImages?,
    onOpenSource: (String) -> Unit,
    ficha: FichaRefresh,
    modifier: Modifier = Modifier,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    FieldCard(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CoinSides(
                label = name.text,
                obverse = images?.obverse,
                reverse = images?.reverse,
                missing = false,
                modifier = Modifier.width(150.dp),
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    name.denomination,
                    style = MaterialTheme.typography.titleMedium,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 1.sp,
                        maxFontSize = 17.sp,
                        stepSize = 0.5.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                )
                name.theme?.let { theme ->
                    Text(
                        theme,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    pieceLine(piece),
                    style = MaterialTheme.typography.labelLarge,
                    color = Paper.muted,
                    modifier = Modifier.padding(top = 4.dp),
                )
                extra()
                ExternalLink(
                    text = COIN_VIEW_ON_NUMISTA,
                    onClick = { onOpenSource(numistaTypeUrl(piece.item.typeId)) },
                )
                // Under the link out on purpose: seeing the page is how the collector finds out that
                // Numista already says something else, and this is what brings that here.
                FichaBrought(ficha, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}
