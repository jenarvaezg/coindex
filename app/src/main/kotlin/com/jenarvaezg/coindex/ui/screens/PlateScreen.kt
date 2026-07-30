package com.jenarvaezg.coindex.ui.screens

import android.graphics.Picture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.data.PlateUnavailable
import com.jenarvaezg.coindex.data.TypeImages
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogAlbumMember
import com.jenarvaezg.coindex.domain.CollectionCatalogMemberStatus
import com.jenarvaezg.coindex.ui.components.CoinSides
import com.jenarvaezg.coindex.ui.components.ExternalLink
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.components.SpecificationCard
import com.jenarvaezg.coindex.ui.plateUnavailableLabel
import com.jenarvaezg.coindex.ui.plateFileName
import com.jenarvaezg.coindex.ui.recordInto
import com.jenarvaezg.coindex.ui.sharePlateSheet
import com.jenarvaezg.coindex.ui.numistaTypeUrl
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.theme.PlateMetrics
import com.jenarvaezg.coindex.ui.variantEntries
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The plate of a followed proposal against its curated catalog.
 *
 * Owned members are shown at full colour; missing ones keep their catalog design in grayscale
 * so the plate reads as a collection with gaps. Every member links to its Numista page.
 */
@Composable
fun PlateScreen(
    result: PlateResult,
    images: Map<Int, TypeImages>,
    onOpenSource: (String) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (result) {
        is PlateResult.Unavailable -> UnavailablePlate(result.reason, modifier)
        is PlateResult.Available -> AvailablePlate(
            catalog = result.catalog,
            members = result.album.members,
            ownedMembers = result.album.ownedMembers(),
            images = images,
            onOpenSource = onOpenSource,
            onMessage = onMessage,
            modifier = modifier,
        )
    }
}

@Composable
private fun AvailablePlate(
    catalog: CollectionCatalog,
    members: List<CollectionCatalogAlbumMember>,
    ownedMembers: Int,
    images: Map<Int, TypeImages>,
    onOpenSource: (String) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var exporting by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        PlateGrid(
            catalog = catalog,
            members = members,
            ownedMembers = ownedMembers,
            images = images,
            exporting = exporting,
            onOpenSource = onOpenSource,
            onExport = { exporting = true },
        )
        if (exporting) {
            PlateSheetExport(
                catalog = catalog,
                members = members,
                ownedMembers = ownedMembers,
                images = images,
                onFinished = { message ->
                    exporting = false
                    onMessage(message)
                },
            )
        }
    }
}

/**
 * Composes the full sheet off-screen, waits for every picture to settle, and shares it.
 *
 * The sheet is measured with its own density and never painted: [recordInto] captures the
 * drawing commands instead of drawing them, so the export is the complete plate rather than
 * the part that happens to be on screen.
 */
@Composable
private fun PlateSheetExport(
    catalog: CollectionCatalog,
    members: List<CollectionCatalogAlbumMember>,
    ownedMembers: Int,
    images: Map<Int, TypeImages>,
    onFinished: (String) -> Unit,
) {
    val context = LocalContext.current
    val picture = remember(catalog.id) { Picture() }
    val layout = remember(members.size) { SheetLayout.forMemberCount(members.size) }
    val expectedImages = remember(members, images) { sheetImageCount(members, images) }
    val settled = remember { mutableIntStateOf(0) }

    LaunchedEffect(catalog.id) {
        val everyPictureLoaded = withTimeoutOrNull(IMAGE_WAIT_MILLIS) {
            snapshotFlow { settled.intValue }.first { it >= expectedImages }
        } != null
        // The last picture reports before it is drawn, so let a frame land either way.
        withFrameNanos {}
        withFrameNanos {}
        val outcome = runCatching {
            sharePlateSheet(context, picture, plateFileName(catalog.id))
        }
        onFinished(
            when {
                outcome.isFailure ->
                    "No se pudo exportar la lámina: ${outcome.exceptionOrNull()?.message}"
                everyPictureLoaded -> "Lámina completa exportada · ${members.size} emisiones"
                else -> "Lámina exportada, pero alguna imagen no llegó a cargar"
            },
        )
    }

    // Off-screen: unbounded so the whole sheet is measured, and clipped away from the plate.
    Box(modifier = Modifier.size(0.dp).wrapContentSize(unbounded = true, align = Alignment.TopStart)) {
        CompositionLocalProvider(LocalDensity provides layout.density) {
            PlateSheet(
                catalog = catalog,
                members = members,
                ownedMembers = ownedMembers,
                images = images,
                layout = layout,
                onImageSettled = { settled.intValue += 1 },
                // The sheet paints its own paper; recording it from the outside would drop it.
                modifier = Modifier.recordInto(picture),
            )
        }
    }
}

private const val IMAGE_WAIT_MILLIS = 20_000L

@Composable
private fun PlateGrid(
    catalog: CollectionCatalog,
    members: List<CollectionCatalogAlbumMember>,
    ownedMembers: Int,
    images: Map<Int, TypeImages>,
    exporting: Boolean,
    onOpenSource: (String) -> Unit,
    onExport: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(PlateMetrics.minPlateCell),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(PlateMetrics.gutter),
        verticalArrangement = Arrangement.spacedBy(PlateMetrics.gutter),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Eyebrow("Catálogo curado")
                Text(catalog.name, style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Referencia curada de las emisiones catalogadas de esta variante; no " +
                        "afirma que sea una serie cerrada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Paper.muted,
                )
                SpecificationCard(
                    entries = listOf(
                        "Progreso" to "$ownedMembers / ${members.size} emisiones",
                    ) + variantEntries(catalog.weightMillioz, catalog.finish) +
                        listOf("Actualizado" to catalog.updatedAt),
                    modifier = Modifier.fillMaxWidth(),
                )
                // Exporting the plate is what this screen is for, so it is the only filled
                // button on it; as a bare text button it read as another section heading.
                PrimaryAction(
                    text = if (exporting) {
                        "Preparando la lámina…"
                    } else {
                        "Exportar lámina como imagen"
                    },
                    onClick = onExport,
                    enabled = !exporting,
                    share = !exporting,
                )
                ExternalLink(
                    text = "Fuente en Numista",
                    onClick = { onOpenSource(catalog.source) },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        items(members, key = { it.member.id }) { albumMember ->
            PlateCell(albumMember, images[albumMember.member.numistaTypeId], onOpenSource)
        }
    }
}

@Composable
private fun PlateCell(
    albumMember: CollectionCatalogAlbumMember,
    images: TypeImages?,
    onOpenSource: (String) -> Unit,
) {
    val owned = albumMember.status as? CollectionCatalogMemberStatus.Owned
    val stateLabel = when {
        owned == null -> "Me falta"
        owned.quantity > 1 -> "Tengo · ×${owned.quantity}"
        else -> "Tengo"
    }
    FieldCard(emphasized = owned != null, dashed = owned == null) {
        CoinSides(
            label = albumMember.member.label,
            obverseUrl = images?.obverse,
            reverseUrl = images?.reverse,
            missing = owned == null,
        )
        Text(
            stateLabel.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = if (owned != null) Paper.rust else Paper.muted,
            modifier = Modifier.padding(top = 10.dp),
        )
        ExternalLink(
            text = albumMember.member.label,
            style = MaterialTheme.typography.titleMedium,
            onClick = { onOpenSource(numistaTypeUrl(albumMember.member.numistaTypeId)) },
        )
        Text(
            "${albumMember.member.year} · Numista ${albumMember.member.numistaTypeId}",
            style = MaterialTheme.typography.labelLarge,
            color = Paper.muted,
        )
    }
}

@Composable
private fun UnavailablePlate(reason: PlateUnavailable, modifier: Modifier = Modifier) {
    val explanation = plateUnavailableLabel(reason)
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Eyebrow("Lámina no disponible")
        Text(explanation, style = MaterialTheme.typography.bodyLarge)
    }
}
