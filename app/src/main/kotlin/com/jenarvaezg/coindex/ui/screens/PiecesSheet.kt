package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.ui.DrawnPiece
import com.jenarvaezg.coindex.ui.CoinName
import com.jenarvaezg.coindex.ui.PiecesSubject
import com.jenarvaezg.coindex.ui.components.CoinSides
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.pieceLine
import com.jenarvaezg.coindex.ui.piecesSheetFacts
import com.jenarvaezg.coindex.ui.theme.Paper

private val SHEET_GUTTER = 12.dp
private val SHEET_PADDING = 24.dp

/**
 * A collection without an issue list as one printable sheet.
 *
 * It is the plate's sheet with the one thing a plate has and this cannot: **an empty cell**. By
 * ADR 0020 a collection with no issue list has nothing to be missing from, and a box can never
 * contain a gap, so every cell here is a piece the collector owns — and that is what makes the
 * export honest rather than a plate with the holes quietly dropped.
 *
 * Laid out eagerly like [PlateSheet], and only ever composed while an export is in flight.
 */
@Composable
fun PiecesSheet(
    subject: PiecesSubject,
    names: (CollectedItem) -> CoinName,
    images: Map<Int, TypeImages>,
    layout: SheetLayout,
    onImageSettled: (painted: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        // The page is painted inside `recordInto`, or the export comes out on a transparent
        // background that every viewer fills with a colour of its own.
        modifier = modifier
            .width(layout.width)
            .background(Paper.paper)
            .padding(SHEET_PADDING),
        verticalArrangement = Arrangement.spacedBy(SHEET_GUTTER),
    ) {
        PiecesSheetHeading(subject, layout)
        subject.pieces.chunked(layout.columns).forEach { row ->
            // Tallest cell sets the row. A plate's cells hold catalog labels of a kind, but these
            // hold whatever Numista calls the coin — «2 Dollars - Charles III (Southern Cross)»
            // next to «5 Euros (Albacete)» — and cells that stop at different heights read as a
            // sheet printed crooked.
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(SHEET_GUTTER),
            ) {
                row.forEach { piece ->
                    PiecesSheetCell(
                        piece = piece,
                        name = names(piece.item),
                        images = images[piece.item.typeId],
                        onImageSettled = onImageSettled,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                // Keep the last row's cells the same width as every other row's.
                repeat(layout.columns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        Text(
            "Fuente: tu colección en Numista",
            style = MaterialTheme.typography.labelSmall.scaledBy(layout.headerScale),
            color = Paper.muted,
        )
    }
}

/**
 * The sheet's masthead.
 *
 * It says «COINDEX · COLECCIÓN» where a plate says «CATÁLOGO CURADO», because that is the one claim
 * this sheet must not make: nobody curated a sequence here, and the paper would outlive the app to
 * say otherwise.
 */
@Composable
private fun PiecesSheetHeading(subject: PiecesSubject, layout: SheetLayout) {
    val scale = layout.headerScale
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SHEET_GUTTER * scale * 0.5f),
    ) {
        Text(
            "COINDEX · COLECCIÓN",
            style = MaterialTheme.typography.labelMedium.scaledBy(scale * 1.3f),
            color = Paper.rust,
        )
        Text(subject.title, style = MaterialTheme.typography.headlineMedium.scaledBy(scale * 1.55f))
        HorizontalDivider(thickness = 2.dp * scale, color = Paper.ink)
        Row(horizontalArrangement = Arrangement.spacedBy(SHEET_GUTTER * 2)) {
            piecesSheetFacts(subject).forEach { (label, value) -> SheetFact(label, value, scale) }
        }
    }
}

@Composable
private fun SheetFact(label: String, value: String, scale: Float) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.scaledBy(scale * 1.15f),
            color = Paper.muted,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium.scaledBy(scale * 1.35f))
    }
}

@Composable
private fun PiecesSheetCell(
    piece: DrawnPiece,
    name: CoinName,
    images: TypeImages?,
    onImageSettled: (painted: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    FieldCard(modifier = modifier, emphasized = true) {
        CoinSides(
            label = name.text,
            obverse = images?.obverse,
            reverse = images?.reverse,
            // Never missing: everything on this sheet is a piece that is in the collection.
            missing = false,
            onImageSettled = onImageSettled,
            onPaper = true,
        )
        Text(
            name.denomination,
            style = MaterialTheme.typography.titleMedium,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 1.sp,
                maxFontSize = 17.sp,
                stepSize = 0.5.sp,
            ),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
            modifier = Modifier.padding(top = 8.dp),
        )
        name.theme?.let { theme ->
            Text(
                theme,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        Text(
            pieceLine(piece),
            style = MaterialTheme.typography.labelSmall,
            color = Paper.muted,
        )
    }
}
