package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jenarvaezg.coindex.domain.OwnGroupingView
import com.jenarvaezg.coindex.ui.plural
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * Which pieces the collector is picking, while they pick them.
 *
 * By Numista type rather than by row, because that is what a grouping stores: two rows of the
 * same type are the same coin twice, and selecting one selects the coin.
 */
@Stable
class PieceSelection {
    var active by mutableStateOf(false)
        private set

    private val picked = mutableStateListOf<Int>()

    val typeIds: List<Int> get() = picked.toList()

    val count: Int get() = picked.size

    fun start() {
        active = true
    }

    fun cancel() {
        active = false
        picked.clear()
    }

    fun toggle(typeId: Int) {
        if (!picked.remove(typeId)) picked.add(typeId)
    }

    fun isPicked(typeId: Int): Boolean = typeId in picked
}

@Composable
fun rememberPieceSelection(): PieceSelection = remember { PieceSelection() }

/** The per-piece control, shown inside a [PieceCard] only while a selection is open. */
@Composable
fun PieceSelectionToggle(picked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    CardAction(
        text = if (picked) "✓ Elegida" else "Elegir",
        onClick = onToggle,
        modifier = modifier.padding(top = 8.dp),
    )
}

/**
 * The whole grouping gesture in one control: start picking, then decide where they go.
 *
 * Any screen that lists pieces can offer it, and the two that do — the orphans and a
 * collection —
 * are the two places where the collector is already looking at coins that belong together in
 * their head and nowhere else.
 */
@Composable
fun SelectionControls(
    selection: PieceSelection,
    existing: List<OwnGroupingView>,
    onCreate: (name: String, typeIds: List<Int>) -> Unit,
    onAddTo: (groupingId: Long, typeIds: List<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var deciding by remember { mutableStateOf(false) }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        if (selection.active) {
            PrimaryAction(
                text = "Agrupar ${plural(selection.count, "pieza", "piezas")}",
                onClick = { deciding = true },
                enabled = selection.count > 0,
            )
            CardAction(text = "Cancelar", onClick = selection::cancel)
        } else {
            CardAction(text = "Agrupar piezas", onClick = selection::start)
        }
    }

    if (deciding) {
        val close = {
            deciding = false
            selection.cancel()
        }
        GroupingDialog(
            count = selection.count,
            existing = existing,
            onDismiss = { deciding = false },
            onCreate = { name ->
                onCreate(name, selection.typeIds)
                close()
            },
            onAddTo = { groupingId ->
                onAddTo(groupingId, selection.typeIds)
                close()
            },
        )
    }
}

/**
 * What to do with the pieces just picked: a new heading, or one that already exists.
 *
 * It is a card on paper rather than a Material dialog, because it is part of the notebook.
 */
@Composable
fun GroupingDialog(
    count: Int,
    existing: List<OwnGroupingView>,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    onAddTo: (Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        FieldCard(modifier = Modifier.fillMaxWidth()) {
            Eyebrow("Tu agrupación")
            Text(
                "Agrupar ${plural(count, "pieza", "piezas")}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre de la agrupación") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                PrimaryAction(text = "Crear", onClick = { onCreate(name) })
                CardAction(text = "Cancelar", onClick = onDismiss)
            }
            if (existing.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text(
                        "O añádelas a una que ya tienes:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Paper.muted,
                    )
                    existing.forEach { grouping ->
                        CardAction(
                            text = grouping.name,
                            onClick = { onAddTo(grouping.id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
