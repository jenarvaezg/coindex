package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionProposalKey
import com.jenarvaezg.coindex.domain.ProposalDisposition
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.DispositionActions
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.ExternalLink
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.PieceCard
import com.jenarvaezg.coindex.ui.components.PieceSelectionToggle
import com.jenarvaezg.coindex.ui.components.SelectionControls
import com.jenarvaezg.coindex.ui.components.rememberPieceSelection
import com.jenarvaezg.coindex.ui.countLabel
import com.jenarvaezg.coindex.ui.plateUnavailableLabel
import com.jenarvaezg.coindex.ui.stanceFor
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.theme.PlateMetrics
import com.jenarvaezg.coindex.ui.variantLabel

/**
 * One proposal, opened: the pieces it is made of.
 *
 * Every card in the index leads here, whether or not a curated catalog exists for its variant.
 * Before this screen a title only opened something when a catalog happened to match it, so the
 * families Numista gives nobody else — every French coin in the collection — were titles that
 * did nothing at all.
 *
 * This is the inventory side of the pair: what you own, as you recorded it. The plate is the
 * catalog side, and it is the only one of the two that can show a gap.
 */
@Composable
fun ProposalScreen(
    state: CollectionState,
    key: CollectionProposalKey,
    catalog: CollectionCatalog?,
    title: String,
    plate: PlateResult?,
    onOpenPlate: (catalogId: String) -> Unit,
    onOpenSource: (url: String) -> Unit,
    onDisposition: (CollectionProposalKey, ProposalDisposition?) -> Unit,
    onCreateGrouping: (name: String, typeIds: List<Int>) -> Unit,
    onAddToGrouping: (groupingId: Long, typeIds: List<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selection = rememberPieceSelection()
    val proposal = state.proposalFor(key)
    val pieces = state.itemsByKey[key].orEmpty().sortedWith(
        compareBy({ it.recordedYear ?: Int.MAX_VALUE }, { it.title.orEmpty() }, { it.id }),
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(PlateMetrics.gutter),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Eyebrow("Propuesta de colección")
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    variantLabel(key.weightMillioz, key.finish, key.metal),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (proposal != null) {
                    Text(
                        countLabel(proposal.distinctTypes, proposal.quantity),
                        style = MaterialTheme.typography.labelLarge,
                        color = Paper.muted,
                    )
                    DispositionActions(
                        stance = state.stanceFor(key),
                        onDisposition = { disposition -> onDisposition(key, disposition) },
                    )
                }
            }
        }

        // The proposal is derived from what you own right now, so it can vanish under this
        // screen while it is open: a piece sold on Numista and synced away leaves the route
        // valid and its subject gone.
        if (proposal == null) {
            item {
                FieldCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Ya no tienes piezas de esta variante, así que esta propuesta no " +
                            "existe. Vuelve al índice.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Paper.muted,
                    )
                }
            }
        }

        catalog?.let { curated ->
            item { CatalogCard(curated, plate, onOpenPlate, onOpenSource) }
        }

        if (pieces.isNotEmpty()) {
            item {
                Column {
                    HorizontalDivider(color = Paper.line)
                    Text(
                        "Tus piezas",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    SelectionControls(
                        selection = selection,
                        existing = state.ownGroupings,
                        onCreate = onCreateGrouping,
                        onAddTo = onAddToGrouping,
                        modifier = Modifier.padding(top = 10.dp),
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
                // Where the year says 1966 six times over, the catalog is what names the piece.
                emissionLabel = catalog?.emissionLabelFor(piece),
            ) {
                if (selection.active) {
                    PieceSelectionToggle(
                        picked = selection.isPicked(piece.typeId),
                        onToggle = { selection.toggle(piece.typeId) },
                    )
                }
            }
        }
    }
}

/** What to call a piece: the catalog title if its type is cached, else what the row itself says. */
internal fun pieceTitle(state: CollectionState, item: CollectedItem): String =
    state.typeMeta[item.typeId]?.displayTitle
        ?: item.title
        ?: "Pieza ${item.id}"

/**
 * The curated catalog for this variant, if there is one.
 *
 * Both routes out are offered and told apart by the mark: the plate is a page of this notebook,
 * the source is numista.com. When the plate is not reachable it says why instead of hiding.
 */
@Composable
private fun CatalogCard(
    catalog: CollectionCatalog,
    plate: PlateResult?,
    onOpenPlate: (String) -> Unit,
    onOpenSource: (String) -> Unit,
) {
    FieldCard(modifier = Modifier.fillMaxWidth()) {
        Eyebrow("Catálogo curado")
        Text(
            catalog.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        when (plate) {
            is PlateResult.Available -> CardAction(
                text = "Ver lámina",
                onClick = { onOpenPlate(catalog.id) },
                modifier = Modifier.padding(top = 10.dp),
            )
            is PlateResult.Unavailable -> Text(
                plateUnavailableLabel(plate.reason),
                style = MaterialTheme.typography.bodyMedium,
                color = Paper.muted,
                modifier = Modifier.padding(top = 6.dp),
            )
            null -> Unit
        }
        ExternalLink(
            text = "Fuente en Numista",
            onClick = { onOpenSource(catalog.source) },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
