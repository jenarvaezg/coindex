package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.OwnGroupingView
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.PieceCard
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.countLabel
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.theme.PlateMetrics

/**
 * One of the collector's own groupings, opened.
 *
 * It is deliberately plainer than a proposal: there is no variant to state and no catalog to
 * compare against, because nobody claimed this is a series. It is a shelf the collector built,
 * so what it offers is the shelf's own upkeep — rename it, take a coin off it, take it apart.
 *
 * Taking a coin off does not touch the coin: a grouping is an extra view, and the piece was
 * never removed from the proposal it derives into.
 */
@Composable
fun OwnGroupingScreen(
    state: CollectionState,
    grouping: OwnGroupingView?,
    onOpenSource: (url: String) -> Unit,
    onRename: (name: String) -> Unit,
    onRemoveType: (typeId: Int) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (grouping == null) {
        Column(
            modifier = modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Eyebrow("Tu agrupación")
            Text(
                "Esta agrupación ya no existe.",
                style = MaterialTheme.typography.bodyLarge,
                color = Paper.muted,
            )
        }
        return
    }

    var renaming by remember(grouping.id) { mutableStateOf(false) }
    val pieces = grouping.items.sortedWith(
        compareBy({ it.recordedYear ?: Int.MAX_VALUE }, { it.title.orEmpty() }, { it.id }),
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(PlateMetrics.gutter),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Eyebrow("Tu agrupación")
                Text(grouping.name, style = MaterialTheme.typography.headlineMedium)
                Text(
                    countLabel(grouping.distinctTypes, grouping.quantity),
                    style = MaterialTheme.typography.labelLarge,
                    color = Paper.muted,
                )
                Text(
                    "La hiciste tú, así que no dice qué te falta: reúne las piezas que " +
                        "quisiste juntar y las deja también donde ya estaban.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Paper.muted,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    CardAction(
                        text = if (renaming) "Cerrar el nombre" else "Renombrar",
                        onClick = { renaming = !renaming },
                    )
                    CardAction(text = "Deshacer la agrupación", onClick = onDelete)
                }
            }
        }

        if (renaming) {
            item { RenameCard(grouping.name, onRename = { onRename(it); renaming = false }) }
        }

        if (pieces.isEmpty()) {
            item {
                FieldCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Ahora mismo no tienes ninguna de las piezas de esta agrupación. " +
                            "Sigue aquí por si vuelven.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Paper.muted,
                    )
                }
            }
        }

        items(pieces, key = { it.id }) { piece ->
            PieceCard(
                item = piece,
                title = pieceTitle(state, piece),
                images = state.images[piece.typeId],
                onOpenSource = onOpenSource,
            ) {
                CardAction(
                    text = "Quitar de la agrupación",
                    onClick = { onRemoveType(piece.typeId) },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun RenameCard(current: String, onRename: (String) -> Unit) {
    var name by remember(current) { mutableStateOf(current) }
    FieldCard(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre de la agrupación") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryAction(
            text = "Guardar el nombre",
            onClick = { onRename(name) },
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
