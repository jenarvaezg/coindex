package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.components.ToggleRow
import com.jenarvaezg.coindex.ui.notebookCostLabel
import com.jenarvaezg.coindex.ui.notebookSwitchLabel
import com.jenarvaezg.coindex.ui.notebookSwitchNote
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.print.NotebookSwitch
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * How the notebook is about to be printed: six switches, and what they cost in paper (#228, #275).
 *
 * **The count is why this exists**, and why it could not go in Ajustes: pages are arithmetic over
 * what the index is showing at that moment — filters and search included — so a screen with no index
 * in front of it has no number to print. It is recounted on every tap and nothing is drawn to get it.
 *
 * It is a card in the index and not a modal dialog. ADR 0021 §13 refused a dialog in front of the
 * export on the grounds that the button already says how much it will export; what earns a surface
 * here is not confirming, it is **choosing** — and it lands in the same slot the progress card uses,
 * in the app's one visual language, rather than as the first bottom sheet in the app.
 *
 * All six do something now (#233, #275). While a ticket was outstanding its switch was drawn, remembered
 * and **disabled**, with the issue named under it, because the one thing a control must never be is
 * tickable and inert. What is left grey is what makes the question moot — «ambas caras» and «tamaño
 * real» with the photographs off, «sin colección» with no loose coin under the current narrowing — and
 * both resolve themselves: one when the coins come back, the other when the filter is cleared.
 */
@Composable
fun ExportOptions(
    options: NotebookOptions,
    pages: Int,
    cards: Int,
    /**
     * How many coins no collection claims, of the ones this narrowing leaves (#275).
     *
     * The sheet holds it and [NotebookOptions] does not, because it is a fact about the collection
     * on screen and not about the configuration: the switches know nothing about the inventory, and
     * teaching them would put the index inside them.
     */
    loose: Int,
    onChange: (NotebookOptions) -> Unit,
    onExport: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FieldCard(modifier = modifier.fillMaxWidth()) {
        Eyebrow("Cómo se exporta")
        Column(modifier = Modifier.padding(top = 6.dp)) {
            NotebookSwitch.entries.forEach { switch ->
                val offered = options.offers(switch) &&
                    (switch != NotebookSwitch.Unclaimed || loose > 0)
                ToggleRow(
                    label = notebookSwitchLabel(switch),
                    note = notebookSwitchNote(switch, offered),
                    checked = options[switch],
                    // Two reasons to be grey, and the note says which: the configuration has made
                    // the question moot, or there is no lámina left to add. «Pendiente · #233» went
                    // with the last of the five switches to land.
                    enabled = offered,
                    onCheckedChange = { on -> onChange(options.with(switch, on)) },
                )
            }
        }
        HorizontalDivider(color = Paper.hairline, modifier = Modifier.padding(vertical = 10.dp))
        Text(
            notebookCostLabel(pages, cards),
            style = MaterialTheme.typography.titleMedium,
            color = Paper.rust,
        )
        Text(
            "Es lo que hay en el índice ahora mismo, con los filtros puestos.",
            style = MaterialTheme.typography.labelLarge,
            color = Paper.muted,
            modifier = Modifier.padding(top = 2.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            PrimaryAction(text = "Exportar", onClick = onExport)
            CardAction(text = "Cancelar", onClick = onDismiss)
        }
    }
}
