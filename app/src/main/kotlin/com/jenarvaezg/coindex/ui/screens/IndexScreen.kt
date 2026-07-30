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
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.ExternalLink
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.LinkText
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.countLabel
import com.jenarvaezg.coindex.ui.lastSyncLabel
import com.jenarvaezg.coindex.ui.variantLabel
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.theme.PlateMetrics

private enum class CardState { Followed, Available, Ignored }

/**
 * The collection index: one card per current proposal, in three blocks.
 *
 * Proposals are derived from the pieces the collector owns right now. Following one never
 * invents a gap; when a curated catalog exists for that exact variant and at least one of its
 * official types is owned, the title opens the local plate instead of Numista.
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
    onOpenPlate: (catalogId: String) -> Unit,
    onOpenSource: (url: String) -> Unit,
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

        proposalBlock(
            title = "Seguidas",
            proposals = proposals.followed,
            cardState = CardState.Followed,
            state = state,
            catalogs = catalogs,
            onOpenPlate = onOpenPlate,
            onOpenSource = onOpenSource,
            onDisposition = onDisposition,
        )
        proposalBlock(
            title = "Disponibles",
            proposals = proposals.available,
            cardState = CardState.Available,
            state = state,
            catalogs = catalogs,
            onOpenPlate = onOpenPlate,
            onOpenSource = onOpenSource,
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
                        cardState = CardState.Ignored,
                        catalog = catalogs.firstOrNull { it.key() == proposal.key() },
                        evidenced = state.evidencedCatalogIds,
                        onOpenPlate = onOpenPlate,
                        onOpenSource = onOpenSource,
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
    cardState: CardState,
    state: CollectionState,
    catalogs: List<CollectionCatalog>,
    onOpenPlate: (String) -> Unit,
    onOpenSource: (String) -> Unit,
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
            cardState = cardState,
            catalog = catalogs.firstOrNull { it.key() == proposal.key() },
            evidenced = state.evidencedCatalogIds,
            onOpenPlate = onOpenPlate,
            onOpenSource = onOpenSource,
            onDisposition = onDisposition,
        )
    }
}

@Composable
private fun ProposalCard(
    proposal: CollectionProposal,
    cardState: CardState,
    catalog: CollectionCatalog?,
    evidenced: Set<String>,
    onOpenPlate: (String) -> Unit,
    onOpenSource: (String) -> Unit,
    onDisposition: (CollectionProposal, ProposalDisposition?) -> Unit,
) {
    val plateCatalog = catalog?.takeIf {
        cardState == CardState.Followed && it.id in evidenced
    }
    FieldCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
        Eyebrow("Evidencia de colección")
        val title = collectionProposalFamilyLabel(proposal.family)
        when {
            plateCatalog != null -> LinkText(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                onClick = { onOpenPlate(plateCatalog.id) },
            )
            // Two titles that both open something, told apart by the mark: the plate is a
            // screen of this app, the catalog source is a page on numista.com.
            catalog != null -> ExternalLink(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                onClick = { onOpenSource(catalog.source) },
            )
            else -> Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        }
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            val ignore = { onDisposition(proposal, ProposalDisposition.Ignored) }
            val clear = { onDisposition(proposal, null) }
            when (cardState) {
                CardState.Followed -> {
                    CardAction(text = "Dejar de seguir", onClick = clear)
                    CardAction(text = "Ignorar", onClick = ignore)
                }
                CardState.Available -> {
                    CardAction(
                        text = "Seguir",
                        onClick = { onDisposition(proposal, ProposalDisposition.Followed) },
                    )
                    CardAction(text = "Ignorar", onClick = ignore)
                }
                CardState.Ignored -> CardAction(text = "Restaurar", onClick = clear)
            }
        }
    }
}
