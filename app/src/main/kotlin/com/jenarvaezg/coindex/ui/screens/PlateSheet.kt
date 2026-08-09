package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.ui.DrawnCell
import com.jenarvaezg.coindex.ui.PlateSubject
import com.jenarvaezg.coindex.ui.components.AlbumHole
import com.jenarvaezg.coindex.ui.components.StampedRatio
import com.jenarvaezg.coindex.ui.plateEntriesBesideRatio
import com.jenarvaezg.coindex.ui.printedPhoto
import com.jenarvaezg.coindex.ui.components.paperSurface
import com.jenarvaezg.coindex.ui.theme.Paper
import kotlin.math.ceil
import kotlin.math.sqrt

private val CELL_WIDTH = 200.dp
private val SHEET_GUTTER = 12.dp
private val SHEET_PADDING = 24.dp

/**
 * Geometry of an exported sheet.
 *
 * The grid is squared off rather than kept at the screen's column count: a 121-issue catalog
 * in two columns would be a bitmap far taller than any GPU or share target would accept.
 * The rendering density then shrinks as the catalog grows, which keeps small catalogs crisp
 * and large ones within a sane number of pixels.
 *
 * The cells keep the screen's dimensions, but the heading does not: type sized for a 411dp
 * phone is fine print on a sheet several times wider, so [headerScale] grows the masthead with
 * the grid it presides over.
 */
data class SheetLayout(
    val columns: Int,
    val width: Dp,
    val density: Density,
    val headerScale: Float,
) {
    companion object {
        fun forMemberCount(memberCount: Int): SheetLayout {
            val columns = ceil(sqrt(memberCount.coerceAtLeast(1) * 1.4)).toInt().coerceIn(2, 8)
            val width = CELL_WIDTH * columns +
                SHEET_GUTTER * (columns - 1) +
                SHEET_PADDING * 2
            val scale = when {
                memberCount <= 12 -> 2.5f
                memberCount <= 40 -> 1.8f
                else -> 1.2f
            }
            return SheetLayout(
                columns = columns,
                width = width,
                density = Density(scale, fontScale = 1f),
                // Four columns is the reference sheet; wider ones need a proportionally
                // bigger heading, narrower ones must not drop below the cell titles.
                headerScale = (columns / 4f).coerceIn(1f, 2f),
            )
        }
    }
}

/**
 * The whole plate as one printable sheet, laid out eagerly.
 *
 * Unlike the on-screen grid this composes every member, which is what makes a complete
 * export possible; it is only ever composed while an export is in flight.
 */
@Composable
fun PlateSheet(
    plate: PlateSubject,
    images: Map<Int, TypeImages>,
    layout: SheetLayout,
    onImageSettled: (painted: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        // The page is painted here, inside whatever the caller wraps the sheet in: applied
        // outside `recordInto` the paper is drawn but never recorded, and the export comes out
        // on a transparent background that every viewer fills with a colour of its own.
        modifier = modifier
            .width(layout.width)
            .paperSurface()
            .padding(SHEET_PADDING),
        verticalArrangement = Arrangement.spacedBy(SHEET_GUTTER),
    ) {
        SheetHeading(plate = plate, layout = layout)
        plate.cells.chunked(layout.columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SHEET_GUTTER),
            ) {
                row.forEach { cell ->
                    SheetCell(
                        cell = cell,
                        photo = cell.numistaTypeId
                            ?.let { images[it] }
                            ?.printedPhoto(plate.printedSide),
                        onImageSettled = onImageSettled,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Keep the last row's cells the same width as every other row's.
                repeat(layout.columns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        Text(
            "Fuente: ${plate.source}",
            style = MaterialTheme.typography.labelSmall.scaledBy(layout.headerScale),
            color = Paper.muted,
        )
    }
}

/**
 * The sheet's masthead: eyebrow, title, rule, and the catalog data as one legible strip.
 *
 * On screen the specification is a card of full-width rows, which on a sheet three times wider
 * pushes each label and its value to opposite edges — the data became unreadable exactly where
 * a printed plate needs it most. Here every pair stays together in its own column of the strip.
 */
@Composable
private fun SheetHeading(plate: PlateSubject, layout: SheetLayout) {
    val scale = layout.headerScale
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SHEET_GUTTER * scale * 0.5f),
    ) {
        Text(
            "COINDEX · CATÁLOGO CURADO",
            style = MaterialTheme.typography.labelMedium.scaledBy(scale * 1.3f),
            color = Paper.rust,
        )
        // The stamp travels and the stamping does not (ADR 0026 §4): what the father shows other
        // people is a complete sheet that says so, and the ink is dry before the picture is taken —
        // `OffScreenSheet` is where that is decided, not here.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SHEET_GUTTER),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                plate.title,
                style = MaterialTheme.typography.headlineMedium.scaledBy(scale * 1.55f),
                modifier = Modifier.weight(1f),
            )
            plate.ratio?.let { ratio ->
                // Composed at its own density rather than resized dp by dp: the stamp grows with
                // the masthead as **one drawing**, corners and rules included.
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = LocalDensity.current.density * scale * 1.55f,
                        fontScale = 1f,
                    ),
                ) {
                    StampedRatio(ratio = ratio, complete = plate.complete)
                }
            }
        }
        HorizontalDivider(thickness = 2.dp * scale, color = Paper.ink)
        // Flowed rather than divided into equal columns: what the plate has to say about itself
        // grew with the catalogs that share a type or a year, and six equal columns broke the
        // date across two lines.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SHEET_GUTTER * 2),
            verticalArrangement = Arrangement.spacedBy(SHEET_GUTTER * scale * 0.5f),
        ) {
            plateEntriesBesideRatio(plate.entries).forEach { (label, value) ->
                Column {
                    if (label.isNotEmpty()) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall.scaledBy(scale * 1.15f),
                            color = Paper.muted,
                        )
                    }
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium.scaledBy(scale * 1.35f),
                    )
                }
            }
        }
    }
}

/** The same style one step up the page, for type that has to hold a whole sheet together. */
internal fun TextStyle.scaledBy(scale: Float): TextStyle = copy(
    fontSize = fontSize * scale,
    lineHeight = if (lineHeight.isSpecified) lineHeight * scale else lineHeight,
    letterSpacing = if (letterSpacing.isSpecified) letterSpacing * scale else letterSpacing,
)

@Composable
private fun SheetCell(
    cell: DrawnCell,
    photo: CoinPhoto?,
    onImageSettled: (painted: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        AlbumHole(
            photo = photo,
            missing = cell.missing,
            onImageSettled = onImageSettled,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
        Text(
            cell.label,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        // Same rule as on screen, and it matters more here: what the sheet says under a coin is
        // read on paper, so it is the year or nothing.
        cell.footnote?.let { footnote ->
            Text(
                footnote,
                style = MaterialTheme.typography.labelSmall,
                color = Paper.muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
