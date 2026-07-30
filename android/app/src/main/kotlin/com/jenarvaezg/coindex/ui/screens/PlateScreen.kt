package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.data.PlateUnavailable
import com.jenarvaezg.coindex.data.TypeImages
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogAlbumMember
import com.jenarvaezg.coindex.domain.CollectionCatalogMemberStatus
import com.jenarvaezg.coindex.ui.components.CoinSides
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.SpecificationCard
import com.jenarvaezg.coindex.ui.finishLabel
import com.jenarvaezg.coindex.ui.numistaTypeUrl
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.theme.PlateMetrics
import com.jenarvaezg.coindex.ui.weightLabel

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
    onExport: () -> Unit,
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
            onExport = onExport,
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
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(PlateMetrics.minPlateCell),
        modifier = modifier.fillMaxSize(),
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
                        "Peso" to weightLabel(catalog.weightMillioz),
                        "Acabado" to finishLabel(catalog.finish),
                        "Actualizado" to catalog.updatedAt,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { onOpenSource(catalog.source) }) {
                    Text("Fuente en Numista", color = Paper.moss)
                }
                TextButton(onClick = onExport) {
                    Text("Exportar lámina como imagen", color = Paper.moss)
                }
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
        TextButton(
            onClick = { onOpenSource(numistaTypeUrl(albumMember.member.numistaTypeId)) },
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(
                albumMember.member.label,
                style = MaterialTheme.typography.titleMedium,
                color = Paper.moss,
            )
        }
        Text(
            "${albumMember.member.year} · Numista ${albumMember.member.numistaTypeId}",
            style = MaterialTheme.typography.labelLarge,
            color = Paper.muted,
        )
    }
}

@Composable
private fun UnavailablePlate(reason: PlateUnavailable, modifier: Modifier = Modifier) {
    val explanation = when (reason) {
        PlateUnavailable.UnknownCatalog -> "No existe ese catálogo curado."
        PlateUnavailable.NotAProposal ->
            "Ya no tienes piezas de esta variante, así que la propuesta no existe."
        PlateUnavailable.NotFollowed -> "Sigue la propuesta para abrir su lámina."
        PlateUnavailable.NoEvidence ->
            "Aún no tienes ninguna emisión oficial de este catálogo."
    }
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Eyebrow("Lámina no disponible")
        Text(explanation, style = MaterialTheme.typography.bodyLarge)
    }
}
