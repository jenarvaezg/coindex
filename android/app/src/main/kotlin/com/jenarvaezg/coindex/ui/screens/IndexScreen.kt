package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.update.UpdateStatus
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionProposal
import com.jenarvaezg.coindex.domain.ProposalDisposition
import com.jenarvaezg.coindex.domain.collectionProposalFamilyLabel
import com.jenarvaezg.coindex.ui.BudgetStatus
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.countLabel
import com.jenarvaezg.coindex.ui.finishLabel
import com.jenarvaezg.coindex.ui.weightLabel
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.theme.PlateMetrics

private enum class CardState { Followed, Available, Ignored }

/** Announces a published APK. Coindex is sideloaded, so it updates itself. */
@Composable
private fun UpdateCard(
    update: UpdateStatus.Available,
    updating: Boolean,
    onInstall: () -> Unit,
) {
    FieldCard(modifier = Modifier.fillMaxWidth(), emphasized = true) {
        Eyebrow("Nueva versión")
        Text(
            "Coindex ${update.manifest.versionName}",
            style = MaterialTheme.typography.titleLarge,
        )
        update.manifest.notes?.takeIf(String::isNotBlank)?.let { notes ->
            Text(
                notes,
                style = MaterialTheme.typography.bodyMedium,
                color = Paper.muted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Button(
            onClick = onInstall,
            enabled = !updating,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(if (updating) "Descargando…" else "Instalar")
        }
    }
}

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
    update: UpdateStatus,
    updating: Boolean,
    catalogs: List<CollectionCatalog>,
    onInstallUpdate: () -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(PlateMetrics.gutter),
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
                Button(onClick = onSync, enabled = !syncing) {
                    Text(if (syncing) "Sincronizando…" else "Sincronizar")
                }
                TextButton(onClick = onOpenUnclassified) {
                    Text("Sin clasificar · ${state.unclassified.size}")
                }
            }
        }
        item {
            Text(
                "Presupuesto de la API: ${budget.used} / ${budget.cap} llamadas este mes",
                style = MaterialTheme.typography.labelLarge,
                color = Paper.muted,
            )
        }
        if (update is UpdateStatus.Available) {
            item {
                UpdateCard(update, updating, onInstallUpdate)
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
                TextButton(onClick = { showIgnored = !showIgnored }) {
                    Text(
                        "Propuestas ignoradas · ${proposals.ignored.size}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
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
            plateCatalog != null -> TextButton(
                onClick = { onOpenPlate(plateCatalog.id) },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = Paper.moss)
            }
            catalog != null -> TextButton(
                onClick = { onOpenSource(catalog.source) },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = Paper.moss)
            }
            else -> Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        }
        Text(
            "${weightLabel(proposal.weightMillioz)} · ${finishLabel(proposal.finish)}",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            countLabel(proposal.distinctTypes, proposal.quantity),
            style = MaterialTheme.typography.labelLarge,
            color = Paper.muted,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            when (cardState) {
                CardState.Followed -> {
                    TextButton(onClick = { onDisposition(proposal, null) }) {
                        Text("Dejar de seguir")
                    }
                    TextButton(onClick = { onDisposition(proposal, ProposalDisposition.Ignored) }) {
                        Text("Ignorar")
                    }
                }
                CardState.Available -> {
                    TextButton(onClick = { onDisposition(proposal, ProposalDisposition.Followed) }) {
                        Text("Seguir")
                    }
                    TextButton(onClick = { onDisposition(proposal, ProposalDisposition.Ignored) }) {
                        Text("Ignorar")
                    }
                }
                CardState.Ignored -> TextButton(onClick = { onDisposition(proposal, null) }) {
                    Text("Restaurar")
                }
            }
        }
    }
}
