package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.background
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
import com.jenarvaezg.coindex.ui.BOX_NAME_LIMIT
import com.jenarvaezg.coindex.ui.boxName
import com.jenarvaezg.coindex.ui.plural
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * Which coins the collector is picking, while they pick them.
 *
 * By Numista type rather than by row, because that is what a box stores: two rows of the same type
 * are the same coin twice, and selecting one selects the coin.
 */
@Stable
class PieceSelection {
    var active by mutableStateOf(false)
        private set

    private val picked = mutableStateListOf<Int>()

    val typeIds: List<Int> get() = picked.toList()

    val count: Int get() = picked.size

    /**
     * Opens the mode already holding [seed].
     *
     * The seed is the whole difference the prototype of #161 measured. Entering empty makes an
     * arbitrary two-coin box cheap and a «all the French ones» box a hundred taps; entering with what
     * the filter is showing inverts that. So the caller decides, and the two forms turned out to be
     * one gesture with the seed inside or outside.
     */
    fun start(seed: List<Int> = emptyList()) {
        picked.clear()
        picked.addAll(seed.distinct())
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

/** The per-coin control, shown inside a card only while a selection is open. */
@Composable
fun PieceSelectionToggle(picked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    CardAction(
        text = if (picked) "Elegida" else "Elegir",
        onClick = onToggle,
        modifier = modifier.padding(top = 8.dp),
        icon = if (picked) {
            { CheckGlyph() }
        } else {
            null
        },
    )
}

/**
 * The whole box-making gesture in one control, born in Coins (ADR 0021 §11).
 *
 * **The button seeds only when the filter has already narrowed something.** With a filter or a search
 * on it says «Agrupar estas N» and enters with those N marked; with nothing on it says «Agrupar
 * piezas» and enters empty. Measured, not assumed: seeding unconditionally offered «Agrupar estas
 * 191» — the whole collection — and an arbitrary two-coin box, which is the shape of the real BCV
 * case, would have been made by unticking 189.
 *
 * The count rides in the button so **the cost is written on it before it is pressed**: with 59
 * premarked the work inverts, and what the collector needs to see is that the filter wants narrowing
 * first.
 *
 * @param shown the type ids the list is showing right now — the seed
 * @param seeded whether anything is narrowing that list, which is what decides the two forms
 */
@Composable
fun SelectionControls(
    selection: PieceSelection,
    existing: List<OwnGroupingView>,
    taken: Collection<String>,
    shown: List<Int>,
    seeded: Boolean,
    onCreate: (name: String, typeIds: List<Int>) -> Unit,
    onAddTo: (boxId: Long, typeIds: List<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var naming by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (selection.active) {
                PrimaryAction(
                    text = "Nombrar la caja · ${selection.count}",
                    onClick = { naming = true },
                    enabled = selection.count > 0,
                )
                CardAction(text = "Cancelar", onClick = selection::cancel)
            } else if (seeded) {
                CardAction(
                    text = "Agrupar estas ${shown.size}",
                    onClick = { selection.start(shown) },
                    enabled = shown.isNotEmpty(),
                )
            } else {
                CardAction(text = "Agrupar piezas", onClick = { selection.start() })
            }
        }
        // Which side the work starts from, said once and only while the mode is open: with a seed
        // there is something to remove, and without one there is nothing yet to remove from.
        if (selection.active) {
            Text(
                if (seeded) {
                    "Vienen elegidas las ${shown.size} que enseñaba el filtro. Quita las que no."
                } else {
                    "Toca «Elegir» en cada moneda que quieras."
                },
                style = MaterialTheme.typography.labelLarge,
                color = Paper.muted,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    if (naming) {
        val close = {
            naming = false
            selection.cancel()
        }
        BoxDialog(
            count = selection.count,
            existing = existing,
            taken = taken,
            onDismiss = { naming = false },
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
 * The baptism: one field, and the way to extend a box instead of making one.
 *
 * It is a card on paper rather than a Material dialog, because it is part of the notebook — and it
 * carries its **own opaque background**, which is the defect the prototype of #161 destapó: `FieldCard`
 * paints the translucent `Paper.card` meant to sit on the page, and inside a `Dialog` there is no page
 * underneath. Nobody had seen it because nobody had ever reached this dialog (#17).
 *
 * «O añádelas a una que ya tienes» stays, because from Coins it is the only way to grow a box: the
 * collector wanting to add is looking at the coins they want to add (ADR 0021 §11).
 */
@Composable
fun BoxDialog(
    count: Int,
    existing: List<OwnGroupingView>,
    taken: Collection<String>,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    onAddTo: (Long) -> Unit,
) {
    var typed by remember { mutableStateOf("") }
    val name = boxName(typed, taken)
    Dialog(onDismissRequest = onDismiss) {
        FieldCard(modifier = Modifier.fillMaxWidth().background(Paper.paper)) {
            Eyebrow("Tu caja")
            Text(
                "Agrupar ${plural(count, "moneda", "monedas")}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
            OutlinedTextField(
                value = typed,
                // The limit is hard at the keystroke, so the 41st character never exists; the
                // complaint below is for what a paste or an IME can still get past it.
                onValueChange = { if (it.length <= BOX_NAME_LIMIT) typed = it },
                label = { Text("Cómo se llama") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                name.counter,
                style = MaterialTheme.typography.labelLarge,
                color = Paper.muted,
                modifier = Modifier.padding(top = 6.dp),
            )
            name.problem?.let { problem ->
                Text(
                    problem,
                    style = MaterialTheme.typography.labelLarge,
                    color = Paper.rust,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                PrimaryAction(
                    text = "Crear",
                    onClick = { onCreate(name.stored) },
                    enabled = name.canSave,
                )
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
