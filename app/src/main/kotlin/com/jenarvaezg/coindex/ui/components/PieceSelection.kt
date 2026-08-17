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
 * Which coins the collector is picking, while they pick them.
 *
 * By Numista type rather than by row, because that is what a box stores: two rows of the same type
 * are the same coin twice, and selecting one selects the coin.
 */
@Stable
class PieceSelection {
    var active by mutableStateOf(false)
        private set

    /**
     * Whether the baptism is open over the mode (#517).
     *
     * It used to be the `remember` of the control that drew both the button and the dialog. It moved
     * in here when the mode's controls moved to the band at the foot of the screen: the door in the
     * header and the band are two composables now, and the one thing they both have to agree about
     * is this. Anything else would be the dialog reopening itself the moment the door recomposed.
     */
    var naming by mutableStateOf(false)
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
        naming = false
        picked.clear()
    }

    fun name() {
        naming = true
    }

    /** The way back out of the baptism and **not** out of the mode: what was picked is still picked. */
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
 * The door into the box-making mode, on the header of Coins (ADR 0021 §11).
 *
 * **The button seeds only when the filter has already narrowed something.** With a filter or a search
 * on it says «Hacer una colección con estas N» and enters with those N marked; with nothing on it says
 * «Hacer una colección» and enters empty. Measured, not assumed: seeding unconditionally offered the
 * whole collection — 191 coins — and an arbitrary two-coin box, which is the shape of the real BCV
 * case, would have been made by unticking 189.
 *
 * The count rides in the button so **the cost is written on it before it is pressed**: with 59
 * premarked the work inverts, and what the collector needs to see is that the filter wants narrowing
 * first.
 *
 * **It is the door and nothing else** (#517). While the mode is open this prints nothing: the work of
 * the mode — what to touch, what it will be called, and the way out — is on [SelectionBand], at the
 * foot of the screen, where it is still there after the header has scrolled away.
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
 * The mode itself while it is open: the band at the foot of Coins, and the baptism it opens (#517).
 *
 * Which side the work starts from is said once and only here — with a seed there is something to
 * remove, and without one there is nothing yet to remove from — and it is said in **every frame** of
 * the screen now, which is what the mode was missing: the sentence used to sit in the header, two
 * rows of coins above wherever the collector was actually tapping.
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
            Eyebrow(BOX_EYEBROW)
            Text(
                boxDialogHeading(count),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
            OutlinedTextField(
                value = typed,
                // The limit is hard at the keystroke, so the 41st character never exists; the
                // complaint below is for what a paste or an IME can still get past it.
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
