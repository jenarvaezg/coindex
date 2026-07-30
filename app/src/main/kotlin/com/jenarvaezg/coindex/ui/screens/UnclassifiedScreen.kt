package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.ui.components.CoinSides
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.numistaTypeUrl
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.theme.PlateMetrics
import com.jenarvaezg.coindex.ui.unclassifiedReasonLabel

/**
 * Pieces that produced no proposal, each with the reason why.
 *
 * This screen is the mechanism by which the curated catalogs grow: an orphan with no family in
 * Numista is a candidate for the next catalog.
 */
@Composable
fun UnclassifiedScreen(
    state: CollectionState,
    onOpenSource: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(PlateMetrics.gutter),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Eyebrow("Sin clasificar")
                Text("Piezas huérfanas", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Nada se descarta en silencio: cada pieza dice por qué no ha entrado en " +
                        "ninguna propuesta.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Paper.muted,
                )
            }
        }
        if (state.unclassified.isEmpty()) {
            item {
                FieldCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "No hay piezas pendientes de clasificación.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Paper.muted,
                    )
                }
            }
        }
        items(state.unclassified, key = { it.item.id }) { orphan ->
            val images = state.images[orphan.item.typeId]
            val title = state.typeMeta[orphan.item.typeId]?.displayTitle
                ?: orphan.item.title
                ?: "Pieza ${orphan.item.id}"
            FieldCard(modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    CoinSides(
                        label = title,
                        obverseUrl = images?.obverse,
                        reverseUrl = images?.reverse,
                        missing = false,
                        modifier = Modifier.width(150.dp),
                    )
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Tipo Numista ${orphan.item.typeId} · cantidad ${orphan.item.quantity}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Paper.muted,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            unclassifiedReasonLabel(orphan.reason),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        TextButton(
                            onClick = { onOpenSource(numistaTypeUrl(orphan.item.typeId)) },
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text("Ver en Numista", color = Paper.moss)
                        }
                    }
                }
            }
        }
    }
}
