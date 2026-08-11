package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
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
import com.jenarvaezg.coindex.ui.ExportDestination
import com.jenarvaezg.coindex.ui.NOTHING_TO_PRINT_MESSAGE
import com.jenarvaezg.coindex.ui.PIECES_HEADING
import com.jenarvaezg.coindex.ui.PiecesSubject
import com.jenarvaezg.coindex.ui.REMOVE_TYPE_FROM_COLLECTION
import com.jenarvaezg.coindex.ui.SHARE_ACTION
import com.jenarvaezg.coindex.ui.SharedSheet
import com.jenarvaezg.coindex.ui.boxName
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.FichaRefresh
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.PieceCard
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.components.ShareGlyph
import com.jenarvaezg.coindex.ui.countSentence
import com.jenarvaezg.coindex.ui.notebookCancelledMessage
import com.jenarvaezg.coindex.ui.notebookWarmCancelledMessage
import com.jenarvaezg.coindex.ui.pieceName
import com.jenarvaezg.coindex.ui.piecesFileName
import com.jenarvaezg.coindex.ui.print.NotebookExportStep
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.print.PrintPage
import com.jenarvaezg.coindex.ui.print.sheetExportSwitches
import com.jenarvaezg.coindex.ui.renameToggleLabel
import com.jenarvaezg.coindex.ui.sheetDownloadLabel
import com.jenarvaezg.coindex.ui.sheetExportCostLabel
import com.jenarvaezg.coindex.ui.sheetExportCostScope
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.theme.PlateMetrics

/**
 * A collection whose plate does not open, opened: the pieces it is made of.
 *
 * **One screen for the two cases** (ADR 0021 §9). A collection the inventory derives and a box the
 * collector typed used to have a screen each, and the measurement behind the merge is that they
 * differ in what they *have* — a physical variant, an upkeep — never in what they are. Two screens
 * would have drifted apart the first time one of them grew a line, and the difference the collector
 * would have read is the word of provenance the card had just dropped.
 *
 * What it never shows is a gap. A collection with no issue list has nothing to be missing from
 * (ADR 0021 §3), and a box cannot contain one by construction, so what goes where a plate's empty
 * cells would be is nothing at all — not a hole, not a promise, not a «could have a catalog».
 */
@Composable
fun PiecesScreen(
    state: CollectionState,
    subject: PiecesSubject?,
    onOpenSource: (url: String) -> Unit,
    onMessage: (String) -> Unit,
    /**
     * How old each piece's ficha is and how to ask Numista again for it (#185). One type per tap:
     * there is no «refrescar la tarjeta entera» here, because a collection of twenty pieces would be
     * twenty calls spent on the nineteen nobody said were wrong (ADR 0025).
     */
    ficha: (typeId: Int) -> FichaRefresh,
    notebookOptions: NotebookOptions,
    onNotebookPrinted: (NotebookOptions) -> Unit,
    notebookPages: (NotebookOptions) -> List<PrintPage>,
    onExporting: (Boolean) -> Unit,
    /** Present exactly when the subject is a box: the same `if` all the way down. */
    upkeep: BoxUpkeep? = null,
    /**
     * Why there is nothing here, when there is nothing here. A box has been undone; a derived
     * collection may also have moved, because refreshing a ficha can change the family its key is
     * built from (#185), and the route still names the old one.
     */
    missingExplanation: String = COLLECTION_NO_LONGER_EXISTS,
    modifier: Modifier = Modifier,
) {
    if (subject == null) {
        MissingSubject(missingExplanation, modifier.fillMaxSize().padding(20.dp))
        return
    }

    var renaming by remember(subject.boxId) { mutableStateOf(false) }
    var configuring by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(notebookOptions) }
    var printing by remember { mutableStateOf<PiecesNotebookJob?>(null) }
    var step by remember { mutableStateOf<NotebookExportStep>(NotebookExportStep.Drawing(0, "")) }
    LaunchedEffect(printing != null) { onExporting(printing != null) }
    val preview = remember(configuring, draft, subject.title) {
        if (!configuring) null else notebookPages(draft)
    }
    val preparing = printing != null
    val actionsEnabled = !configuring && printing == null

    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(PlateMetrics.gutter),
        ) {
            item {
                PiecesHeading(
                    subject = subject,
                    upkeep = upkeep,
                    preparing = preparing,
                    actionsEnabled = actionsEnabled && subject.pieces.isNotEmpty(),
                    renaming = renaming,
                    onExport = {
                        if (!configuring) draft = notebookOptions
                        configuring = true
                    },
                    onToggleRename = { renaming = !renaming },
                )
            }

            preview?.let { pages ->
                item {
                    fun begin(destination: ExportDestination) {
                        if (pages.isEmpty()) {
                            onMessage(NOTHING_TO_PRINT_MESSAGE)
                        } else {
                            onNotebookPrinted(draft)
                            step = NotebookExportStep.Drawing(
                                0,
                                pages.first().blocks.first().section.title,
                            )
                            printing = PiecesNotebookJob(pages, destination)
                        }
                        configuring = false
                    }
                    ExportOptions(
                        options = draft,
                        pages = pages.size,
                        cards = 1,
                        loose = 0,
                        onChange = { draft = it },
                        onDownload = { begin(ExportDestination.Download) },
                        onShare = { begin(ExportDestination.Share) },
                        onDismiss = { configuring = false },
                        switches = sheetExportSwitches(),
                        costScope = sheetExportCostScope(SharedSheet.PIECES),
                        costLabel = sheetExportCostLabel(SharedSheet.PIECES, pages.size),
                    )
                }
            }

            printing?.let { job ->
                item {
                    ExportProgress(
                        step = step,
                        pages = job.pages.size,
                        onCancel = when (val current = step) {
                            is NotebookExportStep.Warming -> {
                                {
                                    printing = null
                                    onMessage(
                                        notebookWarmCancelledMessage(
                                            current.photographsDone,
                                            current.photographs,
                                        ),
                                    )
                                }
                            }
                            is NotebookExportStep.Drawing -> {
                                {
                                    printing = null
                                    onMessage(
                                        notebookCancelledMessage(
                                            current.pagesDone,
                                            job.pages.size,
                                        ),
                                    )
                                }
                            }
                            NotebookExportStep.Writing -> null
                        },
                    )
                }
            }

            // The upkeep of a box is an `if` and not a screen: two actions in the heading above
            // and one per row below.
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
                        // The box-making gesture is **not** here any more. It was hung off this
                        // screen and off «Sin clasificar», the two places §9 and §1 removed, and it
                        // is born in Coins now (ADR 0021 §11, #173): whoever wants to group is
                        // looking at the coins they want to group, not at a collection that already
                        // holds them.
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
                    onOpenSource = onOpenSource,
                    ficha = ficha(piece.item.typeId),
                ) {
                    // Dropping a type does not touch the piece (ADR 0013, §10): it stays in the
                    // inventory and on the card its variant derives, which is what makes a box a
                    // second membership rather than a move.
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

        printing?.let { job ->
            NotebookPdfExport(
                pages = job.pages,
                destination = job.destination,
                onStep = { step = it },
                onFinished = { message ->
                    printing = null
                    onMessage(message)
                },
                fileName = piecesFileName(subject.title),
                sheet = SharedSheet.PIECES,
            )
        }
    }
}

private data class PiecesNotebookJob(
    val pages: List<PrintPage>,
    val destination: ExportDestination,
)

/**
 * The heading: country, name, what it is made of, and what can be done to it.
 *
 * The eyebrow is **the country** — the same one the card carried — and not the species of
 * collection. That slot used to say «Propuesta de colección» or «Tu agrupación», which is the
 * distinction ADR 0021 §2 removed; the country was already the right answer and was being spent
 * on saying which of the two screens you had landed on.
 *
 * The count is the card's own sentence, ratio included where there is one: a collection whose
 * catalog it owns no issued member of yet says «0 de 12 · te faltan 12» on the card, and reading
 * «4 monedas · 3 tipos» one tap later would be the same collection contradicting itself. It is
 * [countSentence] and not the expression spelled out again — spelling it out is how the sheet this
 * screen exports came to contradict it (#226).
 */
@Composable
private fun PiecesHeading(
    subject: PiecesSubject,
    upkeep: BoxUpkeep?,
    preparing: Boolean,
    actionsEnabled: Boolean,
    renaming: Boolean,
    onExport: () -> Unit,
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
        // Descargar / Compartir open «Cómo se exporta» (#401); the panel owns the destination.
        PrimaryAction(
            text = sheetDownloadLabel(SharedSheet.PIECES, preparing),
            onClick = onExport,
            enabled = actionsEnabled,
            modifier = Modifier.padding(top = 12.dp),
        )
        CardAction(
            text = SHARE_ACTION,
            onClick = onExport,
            enabled = actionsEnabled,
            icon = { ShareGlyph(color = Paper.ink) },
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
 * Renaming a box: the same field, the same limit, the same counter as the baptism (ADR 0021 §4).
 *
 * The 40 characters are a property of the name and not of the act of typing it first, so a rename
 * cannot produce a name the card could not hold. Uniqueness is **not** rechecked, and that is the
 * ADR's own rule (§11): it is checked at creation and there it ends — two homonymous cards are a
 * signal to read, not a state to police.
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
 * Empty, and why — which is not the same sentence in the two cases.
 *
 * A box survives with nothing in it, because it is the one thing the collector typed and having it
 * vanish would read as data loss (ADR 0021 §11). A derived collection cannot: with no pieces there
 * is nothing left to derive it from.
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

/**
 * A route with nothing behind it: the collection was undone, sold away, or never described.
 *
 * Said plainly and never guessed at — the alternative is a screen about a collection that does not
 * exist, which reads as the app having lost it.
 */
@Composable
fun MissingSubject(explanation: String, modifier: Modifier = Modifier) {
    // No heading over the sentence: «Colección desconocida» was a fifth wording of the very fact
    // the sentence states, and the masthead already says which hierarchy this route belongs to.
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(explanation, style = MaterialTheme.typography.bodyLarge, color = Paper.muted)
    }
}
