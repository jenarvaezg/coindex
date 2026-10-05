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
import androidx.compose.material3.HorizontalDivider
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
import com.jenarvaezg.coindex.ui.BOX_NAME_FIELD_LABEL
import com.jenarvaezg.coindex.ui.BOX_NAME_LIMIT
import com.jenarvaezg.coindex.ui.BOX_NAME_SAVE_ACTION
import com.jenarvaezg.coindex.ui.BoxUpkeep
import com.jenarvaezg.coindex.ui.COLLECTION_NO_LONGER_EXISTS
import com.jenarvaezg.coindex.ui.DELETE_COLLECTION_ACTION
import com.jenarvaezg.coindex.ui.EMPTY_BOX_EXPLANATION
import com.jenarvaezg.coindex.ui.PIECES_HEADING
import com.jenarvaezg.coindex.ui.PiecesSubject
import com.jenarvaezg.coindex.ui.REMOVE_TYPE_FROM_COLLECTION
import com.jenarvaezg.coindex.ui.SharedSheet
import com.jenarvaezg.coindex.ui.UiNotice
import com.jenarvaezg.coindex.ui.boxName
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.FichaRefresh
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.PieceCard
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.countSentence
import com.jenarvaezg.coindex.ui.pieceName
import com.jenarvaezg.coindex.ui.piecesFileName
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.print.PrintPage
import com.jenarvaezg.coindex.ui.renameToggleLabel
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.theme.PlateMetrics

/**
 * The pieces of a collection without a plate: a derived collection or a box, on one screen
 * (ADR 0021 §9) since they differ only in what they have (a physical variant, upkeep actions).
 *
 * It never shows a gap: a collection without an issue list has nothing to be missing from
 * (ADR 0021 §3), and neither does a box.
 */
@Composable
fun PiecesScreen(
    state: CollectionState,
    subject: PiecesSubject?,
    /** The card's «Ver en Numista», the only way out of the app from here (#508). */
    onOpenNumista: (typeId: Int) -> Unit,
    onMessage: (UiNotice) -> Unit,
    /**
     * Each piece's ficha age and how to ask Numista for it again (#185). One type per tap, never
     * the whole card, so calls aren't spent on fichas nobody flagged (ADR 0025).
     */
    ficha: (typeId: Int) -> FichaRefresh,
    notebookOptions: NotebookOptions,
    onNotebookPrinted: (NotebookOptions) -> Unit,
    notebookPages: (NotebookOptions) -> List<PrintPage>,
    onExporting: (Boolean) -> Unit,
    /** Non-null exactly when the subject is a box. */
    upkeep: BoxUpkeep? = null,
    /**
     * Shown when there is no subject: a box was deleted, or a ficha refresh moved a derived
     * collection to another family key (#185) while the route still names the old one.
     */
    missingExplanation: String = COLLECTION_NO_LONGER_EXISTS,
    modifier: Modifier = Modifier,
) {
    if (subject == null) {
        MissingSubject(missingExplanation, modifier.fillMaxSize().padding(20.dp))
        return
    }

    var renaming by remember(subject.boxId) { mutableStateOf(false) }

    // Export flow and drawing are shared (#430, #431); this supplies the name, file and count.
    SheetExportFlow(
        sheet = SharedSheet.PIECES,
        key = subject.title,
        fileName = piecesFileName(subject.title),
        notebookOptions = notebookOptions,
        onNotebookPrinted = onNotebookPrinted,
        notebookPages = notebookPages,
        onExporting = onExporting,
        onMessage = onMessage,
        tally = subject.countSentence,
        modifier = modifier,
    ) { export ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(PlateMetrics.gutter),
        ) {
            item {
                PiecesHeading(
                    subject = subject,
                    upkeep = upkeep,
                    // Absent while «Cómo se exporta» is open below (#512).
                    door = export.door,
                    renaming = renaming,
                    onToggleRename = { renaming = !renaming },
                )
            }

            export.options?.let { options -> item { options() } }

            export.progress?.let { progress -> item { progress() } }

            // Box upkeep: two actions in the heading and one per row.
            if (renaming && upkeep != null) {
                item {
                    RenameCard(subject.title, onRename = { upkeep.onRename(it); renaming = false })
                }
            }

            if (subject.pieces.isEmpty()) {
                item { EmptyCollection(subject.boxId != null) }
            } else {
                item {
                    Column {
                        HorizontalDivider(color = Paper.line)
                        // Boxes are created from Monedas, not here (ADR 0021 §11, #173).
                        Text(
                            PIECES_HEADING,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }
            }

            items(subject.pieces, key = { it.item.id }) { piece ->
                PieceCard(
                    piece = piece,
                    name = pieceName(state, piece.item),
                    images = state.images[piece.item.typeId],
                    onOpenNumista = onOpenNumista,
                    ficha = ficha(piece.item.typeId),
                ) {
                    // Removing a type from a box leaves the piece in the inventory and on its
                    // derived card: a box is an extra membership, not a move (ADR 0021 §10).
                    upkeep?.let { box ->
                        CardAction(
                            text = REMOVE_TYPE_FROM_COLLECTION,
                            onClick = { box.onRemoveType(piece.item.typeId) },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The heading: country, name, count, and actions.
 *
 * The eyebrow is the country, as on the card, not the kind of collection (ADR 0021 §2). The count
 * is [countSentence], the card's own sentence with its ratio, so the screen, the card and the
 * exported sheet can't disagree (#226).
 */
@Composable
private fun PiecesHeading(
    subject: PiecesSubject,
    upkeep: BoxUpkeep?,
    door: SheetExportDoor?,
    renaming: Boolean,
    onToggleRename: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        subject.issuer?.let { issuer -> Eyebrow(issuer) }
        Text(subject.title, style = MaterialTheme.typography.headlineMedium)
        subject.variant?.let { variant ->
            Text(variant, style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            subject.countSentence,
            style = MaterialTheme.typography.labelLarge,
            color = Paper.muted,
        )
        // One button into «Cómo se exporta», as in the index (#434); the panel asks the
        // destination.
        SheetExportDoorButton(
            door = door,
            modifier = Modifier.padding(top = 12.dp),
            // No pieces, nothing to export, whatever the export flow's state.
            enabled = subject.pieces.isNotEmpty(),
        )
        if (upkeep != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                CardAction(
                    text = renameToggleLabel(renaming),
                    onClick = onToggleRename,
                )
                CardAction(text = DELETE_COLLECTION_ACTION, onClick = upkeep.onDelete)
            }
        }
    }
}

/**
 * Renaming a box, with the same field, 40-character limit and counter as creating one
 * (ADR 0021 §4). Uniqueness is checked only at creation (§11), not on rename.
 */
@Composable
private fun RenameCard(current: String, onRename: (String) -> Unit) {
    var typed by remember(current) { mutableStateOf(current) }
    val name = boxName(typed, taken = emptyList())
    FieldCard(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = typed,
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
        PrimaryAction(
            text = BOX_NAME_SAVE_ACTION,
            onClick = { onRename(name.stored) },
            enabled = name.canSave,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

/**
 * The empty state. A box survives empty, since deleting it would read as data loss (ADR 0021 §11);
 * a derived collection with no pieces no longer exists.
 */
@Composable
private fun EmptyCollection(isBox: Boolean) {
    FieldCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
        Text(
            if (isBox) EMPTY_BOX_EXPLANATION else COLLECTION_NO_LONGER_EXISTS,
            style = MaterialTheme.typography.bodyLarge,
            color = Paper.muted,
        )
    }
}

/** A route whose collection no longer exists or never did, said plainly. */
@Composable
fun MissingSubject(explanation: String, modifier: Modifier = Modifier) {
    // No heading: it would restate the sentence, and the top bar names the section.
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(explanation, style = MaterialTheme.typography.bodyLarge, color = Paper.muted)
    }
}
