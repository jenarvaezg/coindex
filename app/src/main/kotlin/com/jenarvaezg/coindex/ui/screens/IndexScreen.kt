package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.SyncRecord
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionProposal
import com.jenarvaezg.coindex.domain.ProposalDisposition
import com.jenarvaezg.coindex.domain.collectionProposalFamilyLabel
import com.jenarvaezg.coindex.ui.BudgetStatus
import com.jenarvaezg.coindex.ui.ProposalStance
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.DispositionActions
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.LinkText
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.countLabel
import com.jenarvaezg.coindex.ui.lastSyncLabel
import com.jenarvaezg.coindex.ui.variantLabel
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.theme.PlateMetrics

/**
 * The collection index: one card per current proposal, in three blocks.
 *
 * Proposals are derived from the pieces the collector owns right now. Following one never
 * invents a gap. Every title opens its proposal, catalog or no catalog: the plate and the
 * source moved into that screen, because a title that opens something only when a curated
 * catalog happens to exist is a title that looks broken the rest of the time.
 */
@Composable
fun IndexScreen(
    state: CollectionState,
    budget: BudgetStatus,
    syncing: Boolean,
    lastSync: SyncRecord?,
    catalogs: List<CollectionCatalog>,
    onSync: () -> Unit,
    onOpenUnclassified: () -> Unit,
    onOpenProposal: (CollectionProposal) -> Unit,
    onOpenOwnGrouping: (groupingId: Long) -> Unit,
    onOpenPlate: (catalogId: String) -> Unit,
    onDisposition: (CollectionProposal, ProposalDisposition?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showIgnored by remember { mutableStateOf(false) }
    val proposals = state.proposals

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 20.dp,
            vertical = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(PlateMetrics.cardStack),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Eyebrow("Cuaderno de colección")
                Text("Láminas de plata", style = MaterialTheme.typography.displayLarge)
                Text(
                    "Propuestas a partir de las piezas que tienes ahora mismo.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Paper.muted,
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrimaryAction(
                    text = if (syncing) "Sincronizando…" else "Sincronizar",
                    onClick = onSync,
                    enabled = !syncing,
                )
                CardAction(
                    text = "Sin clasificar · ${state.unclassified.size}",
                    onClick = onOpenUnclassified,
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    lastSync?.let { lastSyncLabel(it, System.currentTimeMillis()) }
                        ?: "Todavía no has sincronizado con Numista.",
                    style = MaterialTheme.typography.labelLarge,
                    color = Paper.muted,
                )
                Text(
                    "Presupuesto de la API: ${budget.used} / ${budget.cap} llamadas este mes",
                    style = MaterialTheme.typography.labelLarge,
                    color = Paper.muted,
                )
            }
        }

        // An incomplete sync outlives its snackbar: what it left half-done is a property of the
        // collection on screen, not a notice about something that happened four seconds ago.
        lastSync?.partialFailure?.let { failure ->
            item {
                FieldCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
                    Eyebrow("Sincronización incompleta")
                    Text(
                        "La última sincronización no terminó, así que puede faltar alguna " +
                            "pieza o ficha. Vuelve a sincronizar cuando puedas.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        failure,
                        style = MaterialTheme.typography.labelLarge,
                        color = Paper.rust,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        if (proposals.isEmpty) {
            item {
                FieldCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Todavía no hay propuestas. Sincroniza para traer tu colección de " +
                            "Numista.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Paper.muted,
                    )
                }
            }
        }

        // The collector's own headings come first: they are the only ones nobody derived.
        if (state.ownGroupings.isNotEmpty()) {
            item {
                Column {
                    HorizontalDivider(color = Paper.line)
                    Text(
                        "Tus agrupaciones",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
            items(state.ownGroupings, key = { "own-${it.id}" }) { grouping ->
                FieldCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
                    Eyebrow("Agrupación tuya")
                    LinkText(
                        text = grouping.name,
                        style = MaterialTheme.typography.titleLarge,
                        onClick = { onOpenOwnGrouping(grouping.id) },
                    )
                    Text(
                        countLabel(grouping.distinctTypes, grouping.quantity),
                        style = MaterialTheme.typography.labelLarge,
                        color = Paper.muted,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        proposalBlock(
            title = "Seguidas",
            proposals = proposals.followed,
            stance = ProposalStance.Followed,
            state = state,
            catalogs = catalogs,
            onOpenProposal = onOpenProposal,
            onOpenPlate = onOpenPlate,
            onDisposition = onDisposition,
        )
        proposalBlock(
            title = "Disponibles",
            proposals = proposals.available,
            stance = ProposalStance.Available,
            state = state,
            catalogs = catalogs,
            onOpenProposal = onOpenProposal,
            onOpenPlate = onOpenPlate,
            onDisposition = onDisposition,
        )

        if (proposals.ignored.isNotEmpty()) {
            item {
                CardAction(
                    text = if (showIgnored) {
                        "Ocultar las ignoradas · ${proposals.ignored.size}"
                    } else {
                        "Propuestas ignoradas · ${proposals.ignored.size}"
                    },
                    onClick = { showIgnored = !showIgnored },
                )
            }
            if (showIgnored) {
                items(proposals.ignored, key = { it.key().toString() }) { proposal ->
                    ProposalCard(
                        proposal = proposal,
                        stance = ProposalStance.Ignored,
                        catalog = catalogs.firstOrNull { it.key() == proposal.key() },
                        evidenced = state.evidencedCatalogIds,
                        onOpenProposal = onOpenProposal,
                        onOpenPlate = onOpenPlate,
                        onDisposition = onDisposition,
                    )
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.proposalBlock(
    title: String,
    proposals: List<CollectionProposal>,
    stance: ProposalStance,
    state: CollectionState,
    catalogs: List<CollectionCatalog>,
    onOpenProposal: (CollectionProposal) -> Unit,
    onOpenPlate: (String) -> Unit,
    onDisposition: (CollectionProposal, ProposalDisposition?) -> Unit,
) {
    if (proposals.isEmpty()) return
    item {
        Column {
            HorizontalDivider(color = Paper.line)
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
    items(proposals, key = { "$title-${it.key()}" }) { proposal ->
        ProposalCard(
            proposal = proposal,
            stance = stance,
            catalog = catalogs.firstOrNull { it.key() == proposal.key() },
            evidenced = state.evidencedCatalogIds,
            onOpenProposal = onOpenProposal,
            onOpenPlate = onOpenPlate,
            onDisposition = onDisposition,
        )
    }
}

@Composable
private fun ProposalCard(
    proposal: CollectionProposal,
    stance: ProposalStance,
    catalog: CollectionCatalog?,
    evidenced: Set<String>,
    onOpenProposal: (CollectionProposal) -> Unit,
    onOpenPlate: (String) -> Unit,
    onDisposition: (CollectionProposal, ProposalDisposition?) -> Unit,
) {
    // The plate keeps its shortcut from the card, but as an action rather than as the title:
    // the same conditions resolvePlate applies, checked here so a dead button is never drawn.
    val plateCatalog = catalog?.takeIf {
        stance == ProposalStance.Followed && it.id in evidenced
    }
    FieldCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
        Eyebrow("Evidencia de colección")
        LinkText(
            text = collectionProposalFamilyLabel(proposal.family),
            style = MaterialTheme.typography.titleLarge,
            onClick = { onOpenProposal(proposal) },
        )
        Text(
            variantLabel(proposal.weightMillioz, proposal.finish),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            countLabel(proposal.distinctTypes, proposal.quantity),
            style = MaterialTheme.typography.labelLarge,
            color = Paper.muted,
            modifier = Modifier.padding(top = 4.dp),
        )
        DispositionActions(
            stance = stance,
            onDisposition = { disposition -> onDisposition(proposal, disposition) },
            onOpenPlate = plateCatalog?.let { curated -> { onOpenPlate(curated.id) } },
        )
    }
}
