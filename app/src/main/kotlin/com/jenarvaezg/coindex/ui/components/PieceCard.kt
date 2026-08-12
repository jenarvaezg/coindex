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
import com.jenarvaezg.coindex.ui.numistaTypeUrl
import com.jenarvaezg.coindex.ui.pieceLine
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * One piece of the collection as the collector recorded it: the coin in its hole, its title, what
 * names it — the year on the row, or the emission where the year names nothing — and how many there
 * are.
 *
 * The photograph is **one face that turns over at a tap** and not the labelled pair it used to be
 * (#423). The pair was the last `CoinSides` left in the app: it printed «Anverso» and «Reverso» under
 * two squares of studio white, which is the prose the #300 pruned and the mount the #302 replaced
 * with cardboard everywhere else — and by then it was a leftover of the old exported PNG, which since
 * #431 is the printed page instead. Cardboard, because a piece on this screen is claimed by the
 * collection whose screen it is; which face rests up is [coinAlbumFaces], since neither a box nor a
 * collection without an issue list has a `printed_side` to declare one.
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
            val (face, otherSide) = coinAlbumFaces(images)
            // The same 104 dp hole as the casilla it may also sit in and as the ficha of Monedas
            // (#370): one hole, one size, wherever a coin of this album is looked at.
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
                    onClick = { onOpenSource(numistaTypeUrl(piece.item.typeId)) },
                )
                // Under the link out on purpose: seeing the page is how the collector finds out that
                // Numista already says something else, and this is what brings that here.
                FichaBrought(ficha, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}
