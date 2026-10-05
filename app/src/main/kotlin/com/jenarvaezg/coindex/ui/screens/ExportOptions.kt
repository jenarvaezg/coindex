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
import com.jenarvaezg.coindex.ui.CANCEL_ACTION
import com.jenarvaezg.coindex.ui.DOWNLOAD_ACTION
import com.jenarvaezg.coindex.ui.NOTEBOOK_COST_SCOPE
import com.jenarvaezg.coindex.ui.NOTEBOOK_EXPORTING_EYEBROW
import com.jenarvaezg.coindex.ui.NOTEBOOK_EXPORT_PATIENCE
import com.jenarvaezg.coindex.ui.NOTEBOOK_OPTIONS_EYEBROW
import com.jenarvaezg.coindex.ui.SHARE_ACTION
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.components.ShareGlyph
import com.jenarvaezg.coindex.ui.components.ToggleRow
import com.jenarvaezg.coindex.ui.notebookCostLabel
import com.jenarvaezg.coindex.ui.notebookStepLabel
import com.jenarvaezg.coindex.ui.notebookSwitchLabel
import com.jenarvaezg.coindex.ui.notebookSwitchNote
import com.jenarvaezg.coindex.ui.print.NotebookExportStep
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.print.NotebookSwitch
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * The notebook's print switches and what they cost in pages (#228, #275).
 *
 * The page count depends on what the index shows right now, filters and search included, which is
 * why this lives in the index and not in Ajustes. It is recounted on every tap without drawing.
 * A card in the progress card's slot, not a dialog: ADR 0021 §13 rules out confirming an export,
 * and this is for choosing.
 *
 * A switch is greyed only when the current choices make it moot («ambas caras» and «tamaño real»
 * with photographs off, «sin colección» with no loose coin under the narrowing).
 */
@Composable
fun ExportOptions(
    options: NotebookOptions,
    pages: Int,
    cards: Int,
    /**
     * Coins no collection claims among those the narrowing leaves (#275). Passed here rather than
     * held in [NotebookOptions], which knows nothing about the inventory. Zero on a single lámina,
     * which never offers «Sin colección» (#401).
     */
    loose: Int,
    onChange: (NotebookOptions) -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
    /**
     * Which switches this surface offers (#401). A single lámina or hoja drops packing and the
     * loose-coin plate, which only make sense over many cards.
     */
    switches: List<NotebookSwitch> = NotebookSwitch.entries,
    /** What the cost line counts over: the index, or one sheet (#401). */
    costScope: String = NOTEBOOK_COST_SCOPE,
    /**
     * The cost line. A single hoja passes [sheetExportCostLabel] so the noun matches (#401).
     */
    costLabel: String = notebookCostLabel(pages, cards),
    /** The note under a switch; a single sheet passes [sheetExportSwitchNote] (#401). */
    switchNote: (NotebookSwitch, Boolean) -> String? = ::notebookSwitchNote,
    modifier: Modifier = Modifier,
) {
    FieldCard(modifier = modifier.fillMaxWidth()) {
        Eyebrow(NOTEBOOK_OPTIONS_EYEBROW)
        Column(modifier = Modifier.padding(top = 6.dp)) {
            switches.forEach { switch ->
                val offered = options.offers(switch) &&
                    (switch != NotebookSwitch.Unclaimed || loose > 0)
                ToggleRow(
                    label = notebookSwitchLabel(switch),
                    note = switchNote(switch, offered),
                    checked = options[switch],
                    // Greyed when the other choices make it moot or there is no loose coin to
                    // add; the note says which.
                    enabled = offered,
                    onCheckedChange = { on -> onChange(options.with(switch, on)) },
                )
            }
        }
        HorizontalDivider(color = Paper.hairline, modifier = Modifier.padding(vertical = 10.dp))
        Text(
            costLabel,
            style = MaterialTheme.typography.titleMedium,
            color = Paper.rust,
        )
        Text(
            costScope,
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
            // Descargar is the one-tap path (#285); Compartir stays beside it.
            PrimaryAction(text = DOWNLOAD_ACTION, onClick = onDownload)
            CardAction(
                text = SHARE_ACTION,
                onClick = onShare,
                icon = { ShareGlyph(color = Paper.ink) },
            )
            CardAction(text = CANCEL_ACTION, onClick = onDismiss)
        }
    }
}

/** The export's current step, with a cancel action while one is possible. */
@Composable
fun ExportProgress(
    step: NotebookExportStep,
    pages: Int,
    onCancel: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    FieldCard(modifier = modifier.fillMaxWidth()) {
        Eyebrow(NOTEBOOK_EXPORTING_EYEBROW)
        Text(
            notebookStepLabel(step, pages),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            NOTEBOOK_EXPORT_PATIENCE,
            style = MaterialTheme.typography.labelLarge,
            color = Paper.muted,
            modifier = Modifier.padding(top = 4.dp),
        )
        onCancel?.let { cancel ->
            CardAction(
                text = CANCEL_ACTION,
                onClick = cancel,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}
