package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.PieceCard
import com.jenarvaezg.coindex.ui.components.PieceSelectionToggle
import com.jenarvaezg.coindex.ui.components.SelectionControls
import com.jenarvaezg.coindex.ui.components.rememberPieceSelection
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.theme.PlateMetrics
import com.jenarvaezg.coindex.ui.unclassifiedReasonLabel

/**
 * Pieces that produced no proposal, each with the reason why.
 *
 * This screen lists the automatic unclassified residue — rows `deriveCollection` could not
 * place. It is not the orphan list: an orphan is a curator verdict in `data/orphans.json`.
 * Residues with no Numista family remain candidates for the next catalog or grouping.
 */
@Composable
fun UnclassifiedScreen(
    state: CollectionState,
    onOpenSource: (String) -> Unit,
    onCreateGrouping: (name: String, typeIds: List<Int>) -> Unit,
    onAddToGrouping: (groupingId: Long, typeIds: List<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selection = rememberPieceSelection()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(PlateMetrics.gutter),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Eyebrow("Sin clasificar")
                Text("Sin clasificar", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Nada se descarta en silencio: cada pieza dice por qué no ha entrado en " +
                        "ninguna propuesta. Si sabes que varias van juntas, agrúpalas tú.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Paper.muted,
                )
                if (state.unclassified.isNotEmpty()) {
                    SelectionControls(
                        selection = selection,
                        existing = state.ownGroupings,
                        onCreate = onCreateGrouping,
                        onAddTo = onAddToGrouping,
                    )
                }
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
        items(state.unclassified, key = { it.item.id }) { entry ->
            PieceCard(
                item = entry.item.copy(quantity = entry.quantity),
                title = pieceTitle(state, entry.item),
                images = state.images[entry.item.typeId],
                onOpenSource = onOpenSource,
            ) {
                Text(
                    unclassifiedReasonLabel(entry.reason),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (selection.active) {
                    PieceSelectionToggle(
                        picked = selection.isPicked(entry.item.typeId),
                        onToggle = { selection.toggle(entry.item.typeId) },
                    )
                }
            }
        }
    }
}
