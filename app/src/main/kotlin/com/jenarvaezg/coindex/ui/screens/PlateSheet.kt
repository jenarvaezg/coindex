package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.data.TypeImages
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogAlbumMember
import com.jenarvaezg.coindex.domain.CollectionCatalogMemberStatus
import com.jenarvaezg.coindex.ui.components.CoinSides
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.SpecificationCard
import com.jenarvaezg.coindex.ui.components.coinSideImageCount
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.variantEntries
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
 */
data class SheetLayout(val columns: Int, val width: Dp, val density: Density) {
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
            return SheetLayout(columns, width, Density(scale, fontScale = 1f))
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
        modifier = modifier.width(layout.width).padding(SHEET_PADDING),
        verticalArrangement = Arrangement.spacedBy(SHEET_GUTTER),
    ) {
        Eyebrow("Coindex · catálogo curado")
        Text(catalog.name, style = MaterialTheme.typography.headlineMedium)
        SpecificationCard(
            entries = listOf("Progreso" to "$ownedMembers / ${members.size} emisiones") +
                variantEntries(catalog.weightMillioz, catalog.finish) +
                listOf("Actualizado" to catalog.updatedAt),
            modifier = Modifier.fillMaxWidth(),
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
            style = MaterialTheme.typography.labelSmall,
            color = Paper.muted,
        )
    }
}

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
        )
        Text(
            stateLabel.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = if (owned != null) Paper.rust else Paper.muted,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(albumMember.member.label, style = MaterialTheme.typography.titleMedium)
        Text(
            "${albumMember.member.year} · N#${albumMember.member.numistaTypeId}",
            style = MaterialTheme.typography.labelSmall,
            color = Paper.muted,
        )
    }
}
