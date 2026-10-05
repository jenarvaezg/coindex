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
import com.jenarvaezg.coindex.ui.BOX_ADD_TO_EXISTING
import com.jenarvaezg.coindex.ui.BOX_CREATE_ACTION
import com.jenarvaezg.coindex.ui.BOX_EYEBROW
import com.jenarvaezg.coindex.ui.BOX_NAME_FIELD_LABEL
import com.jenarvaezg.coindex.ui.BOX_NAME_LIMIT
import com.jenarvaezg.coindex.ui.CANCEL_ACTION
import com.jenarvaezg.coindex.ui.boxDialogHeading
import com.jenarvaezg.coindex.ui.boxName
import com.jenarvaezg.coindex.ui.boxDoorLabel
import com.jenarvaezg.coindex.ui.namePickedBoxLabel
import com.jenarvaezg.coindex.ui.selectionHintLabel
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * Which coins the collector is picking, by Numista type rather than by row, since that is what a
 * box stores.
 */
@Stable
class PieceSelection {
    var active by mutableStateOf(false)
        private set

    /**
     * Whether the naming dialog is open over the mode (#517). Held here because the door in the
     * header and the band at the foot are separate composables that must agree on it.
     */
    var naming by mutableStateOf(false)
        private set

    private val picked = mutableStateListOf<Int>()

    val typeIds: List<Int> get() = picked.toList()

    val count: Int get() = picked.size

    /**
     * Opens the mode already holding [seed] (#161): empty suits a small arbitrary box, seeded with
     * the filtered list suits «all the French ones». The caller decides.
     */
    fun start(seed: List<Int> = emptyList()) {
        picked.clear()
        picked.addAll(seed.distinct())
        active = true
    }

    fun cancel() {
        active = false
        naming = false
        picked.clear()
    }

    fun name() {
        naming = true
    }

    /** Closes the naming dialog but not the mode: the picks are kept. */
    fun stopNaming() {
        naming = false
    }

    fun toggle(typeId: Int) {
        if (!picked.remove(typeId)) picked.add(typeId)
    }

    fun isPicked(typeId: Int): Boolean = typeId in picked
}

@Composable
fun rememberPieceSelection(): PieceSelection = remember { PieceSelection() }

/**
 * The door into the box-making mode, on the header of Coins (ADR 0021 §11). It seeds the mode only
 * when a filter or search is narrowing the list («Hacer una colección con estas N»), otherwise it
 * enters empty: seeding the whole collection would make a small box a matter of unticking almost
 * everything. The count is in the label so the collector sees it before pressing.
 *
 * Prints nothing while the mode is open; the mode's controls are on [SelectionBand] (#517).
 *
 * @param shown the type ids the list is showing right now — the seed
 * @param seeded whether anything is narrowing that list, which is what decides the two forms
 */
@Composable
fun SelectionDoor(
    selection: PieceSelection,
    shown: List<Int>,
    seeded: Boolean,
    modifier: Modifier = Modifier,
) {
    if (selection.active) return
    Column(modifier = modifier) {
        if (seeded) {
            CardAction(
                text = boxDoorLabel(seeded = true, shown = shown.size),
                onClick = { selection.start(shown) },
                enabled = shown.isNotEmpty(),
            )
        } else {
            CardAction(
                text = boxDoorLabel(seeded = false, shown = shown.size),
                onClick = { selection.start() },
            )
        }
    }
}

/**
 * The open mode: the band at the foot of Coins, with the hint that depends on whether it was
 * seeded, and the naming dialog it opens (#517). At the foot, the hint stays visible while
 * scrolling.
 */
@Composable
fun SelectionBand(
    selection: PieceSelection,
    existing: List<OwnGroupingView>,
    taken: Collection<String>,
    shown: List<Int>,
    seeded: Boolean,
    onCreate: (name: String, typeIds: List<Int>) -> Unit,
    onAddTo: (boxId: Long, typeIds: List<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!selection.active) return
    ModeBand(sentence = selectionHintLabel(seeded, shown.size), modifier = modifier) {
        PrimaryAction(
            text = namePickedBoxLabel(selection.count),
            onClick = selection::name,
            enabled = selection.count > 0,
        )
        CardAction(text = CANCEL_ACTION, onClick = selection::cancel)
    }

    if (selection.naming) {
        BoxDialog(
            count = selection.count,
            existing = existing,
            taken = taken,
            onDismiss = selection::stopNaming,
            onCreate = { name ->
                onCreate(name, selection.typeIds)
                selection.cancel()
            },
            onAddTo = { groupingId ->
                onAddTo(groupingId, selection.typeIds)
                selection.cancel()
            },
        )
    }
}

/**
 * The naming dialog: one field, plus the way to add to an existing box instead, which from Coins is
 * the only way to grow one (ADR 0021 §11). A paper card with its own opaque background:
 * `FieldCard`'s `Paper.card` is translucent, and a `Dialog` has no page underneath (#161).
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
            Eyebrow(BOX_EYEBROW)
            Text(
                boxDialogHeading(count),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
            OutlinedTextField(
                value = typed,
                // Hard limit at the keystroke; the message below catches what a paste or IME gets
                // past.
                onValueChange = { if (it.length <= BOX_NAME_LIMIT) typed = it },
                label = { Text(BOX_NAME_FIELD_LABEL) },
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
                    text = BOX_CREATE_ACTION,
                    onClick = { onCreate(name.stored) },
                    enabled = name.canSave,
                )
                CardAction(text = CANCEL_ACTION, onClick = onDismiss)
            }
            if (existing.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text(
                        BOX_ADD_TO_EXISTING,
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
