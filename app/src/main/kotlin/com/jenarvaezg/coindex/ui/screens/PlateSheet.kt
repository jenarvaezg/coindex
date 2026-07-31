package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.jenarvaezg.coindex.data.TypeImages
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogAlbumMember
import com.jenarvaezg.coindex.domain.CollectionCatalogMemberStatus
import com.jenarvaezg.coindex.ui.components.CoinSides
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.coinSideImageCount
import com.jenarvaezg.coindex.ui.PlateCommonFacts
import com.jenarvaezg.coindex.ui.plateCellFootnote
import com.jenarvaezg.coindex.ui.plateCommonFacts
import com.jenarvaezg.coindex.ui.plateEntries
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
    catalog: CollectionCatalog,
    members: List<CollectionCatalogAlbumMember>,
    ownedMembers: Int,
    images: Map<Int, TypeImages>,
    layout: SheetLayout,
    onImageSettled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        // The page is painted here, inside whatever the caller wraps the sheet in: applied
        // outside `recordInto` the paper is drawn but never recorded, and the export comes out
        // on a transparent background that every viewer fills with a colour of its own.
        modifier = modifier
            .width(layout.width)
            .background(Paper.paper)
            .padding(SHEET_PADDING),
        verticalArrangement = Arrangement.spacedBy(SHEET_GUTTER),
    ) {
        val common = plateCommonFacts(catalog.members)
        SheetHeading(
            catalog = catalog,
            entries = plateEntries(catalog, ownedMembers, common),
            layout = layout,
        )
        members.chunked(layout.columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SHEET_GUTTER),
            ) {
                row.forEach { albumMember ->
                    SheetCell(
                        albumMember = albumMember,
                        images = images[albumMember.member.numistaTypeId],
                        common = common,
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
            "Fuente: ${catalog.source}",
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
private fun SheetHeading(
    catalog: CollectionCatalog,
    entries: List<Pair<String, String>>,
    layout: SheetLayout,
) {
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
        Text(
            catalog.name,
            style = MaterialTheme.typography.headlineMedium.scaledBy(scale * 1.55f),
        )
        HorizontalDivider(thickness = 2.dp * scale, color = Paper.ink)
        // Flowed rather than divided into equal columns: what the plate has to say about itself
        // grew with the catalogs that share a type or a year, and six equal columns broke the
        // date across two lines.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SHEET_GUTTER * 2),
            verticalArrangement = Arrangement.spacedBy(SHEET_GUTTER * scale * 0.5f),
        ) {
            entries.forEach { (label, value) ->
                Column {
                    Text(
                        label.uppercase(),
                        style = MaterialTheme.typography.labelSmall.scaledBy(scale * 1.15f),
                        color = Paper.muted,
                    )
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
private fun TextStyle.scaledBy(scale: Float): TextStyle = copy(
    fontSize = fontSize * scale,
    lineHeight = if (lineHeight.isSpecified) lineHeight * scale else lineHeight,
    letterSpacing = if (letterSpacing.isSpecified) letterSpacing * scale else letterSpacing,
)

/** Total pictures the sheet will request, so the export knows when it can capture. */
fun sheetImageCount(
    members: List<CollectionCatalogAlbumMember>,
    images: Map<Int, TypeImages>,
): Int = members.sumOf { albumMember ->
    val typeImages = images[albumMember.member.numistaTypeId]
    coinSideImageCount(typeImages?.obverse, typeImages?.reverse)
}

@Composable
private fun SheetCell(
    albumMember: CollectionCatalogAlbumMember,
    images: TypeImages?,
    common: PlateCommonFacts,
    onImageSettled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val owned = albumMember.status as? CollectionCatalogMemberStatus.Owned
    val stateLabel = when {
        owned == null -> "Me falta"
        owned.quantity > 1 -> "Tengo · ×${owned.quantity}"
        else -> "Tengo"
    }
    FieldCard(modifier = modifier, emphasized = owned != null, dashed = owned == null) {
        CoinSides(
            label = albumMember.member.label,
            obverseUrl = images?.obverse,
            reverseUrl = images?.reverse,
            missing = owned == null,
            onImageSettled = onImageSettled,
            onPaper = true,
        )
        Text(
            stateLabel.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = if (owned != null) Paper.rust else Paper.muted,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(albumMember.member.label, style = MaterialTheme.typography.titleMedium)
        // Same rule as on screen: whatever every cell of the sheet shares is in the heading.
        plateCellFootnote(albumMember.member, common)?.let { footnote ->
            Text(
                footnote,
                style = MaterialTheme.typography.labelSmall,
                color = Paper.muted,
            )
        }
    }
}
