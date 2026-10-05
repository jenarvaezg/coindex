package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.jenarvaezg.coindex.ui.coinAlbumFaces
import com.jenarvaezg.coindex.ui.pieceLine
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * One piece of the collection as the collector recorded it: the coin in its hole, its title, the
 * year (or the emission where the year names nothing) and the quantity. The coin is one face that
 * turns over at a tap (#423); which face rests up comes from [coinAlbumFaces], since a box or a
 * collection without an issue list has no `printed_side`.
 *
 * An inventory row, never «me falta». [extra] holds what the hosting screen adds, such as a
 * selection control. [ficha] shows the age of the cached data and lets the collector refetch it
 * (#185).
 */
@Composable
fun PieceCard(
    piece: DrawnPiece,
    name: CoinName,
    images: TypeImages?,
    /** Opens this type's Numista page, by the app's single rule for it (#508). */
    onOpenNumista: (typeId: Int) -> Unit,
    ficha: FichaRefresh,
    modifier: Modifier = Modifier,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    FieldCard(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val (face, otherSide) = coinAlbumFaces(images)
            // The same 104 dp hole as a casilla and the Monedas ficha (#370).
            AlbumHole(
                photo = face,
                otherSide = otherSide,
                modifier = Modifier.size(104.dp),
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
                    onClick = { onOpenNumista(piece.item.typeId) },
                )
                // Under the link: the Numista page is where the collector notices it has changed.
                FichaBrought(ficha, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}
