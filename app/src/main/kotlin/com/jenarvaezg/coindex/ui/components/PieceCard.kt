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
import com.jenarvaezg.coindex.ui.coinAlbumFaces
import com.jenarvaezg.coindex.ui.pieceLine
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * One piece as the collector recorded it, whose coin opens its sheet. The buttons, [extra]'s
 * included, sit below it, so a missed tap can't take it out of a box or spend a consulta
 * (ADR 0026 §3).
 */
@Composable
fun PieceCard(
    piece: DrawnPiece,
    name: CoinName,
    images: TypeImages?,
    /** Opens this coin's sheet (ADR 0026 §3). */
    onOpenCoin: (typeId: Int) -> Unit,
    /** The ficha's age and its refresh (#185). */
    ficha: FichaRefresh,
    modifier: Modifier = Modifier,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    FieldCard(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(PIECE_GAP),
            modifier = Modifier
                .fillMaxWidth()
                .opensTheCoinSheet { onOpenCoin(piece.item.typeId) },
        ) {
            // The same 104 dp hole as a casilla and the Monedas ficha (#370). A box or a
            // collection without an issue list has no `printed_side`.
            AlbumHole(
                photo = coinAlbumFaces(images).first,
                modifier = Modifier.size(PIECE_HOLE),
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
            }
        }
        // In the text's column, so the card keeps its two columns.
        Column(modifier = Modifier.padding(start = PIECE_HOLE + PIECE_GAP)) {
            extra()
            FichaBrought(ficha, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

private val PIECE_HOLE = 104.dp

private val PIECE_GAP = 14.dp
