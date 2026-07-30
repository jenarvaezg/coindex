package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.domain.ProposalDisposition
import com.jenarvaezg.coindex.ui.ProposalStance

/**
 * What you can do with a proposal, wherever it is shown: the index card and the proposal's own
 * screen offer the same three decisions, so they share one row of buttons.
 *
 * A [FlowRow] rather than a [androidx.compose.foundation.layout.Row]: «Ver lámina · Dejar de
 * seguir · Ignorar» is three buttons wide and the narrow phone this is built for would clip the
 * last one off the card.
 */
@Composable
fun DispositionActions(
    stance: ProposalStance,
    onDisposition: (ProposalDisposition?) -> Unit,
    modifier: Modifier = Modifier,
    onOpenPlate: (() -> Unit)? = null,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(top = 12.dp),
    ) {
        onOpenPlate?.let { open -> CardAction(text = "Ver lámina", onClick = open) }
        when (stance) {
            ProposalStance.Followed -> {
                CardAction(text = "Dejar de seguir", onClick = { onDisposition(null) })
                CardAction(
                    text = "Ignorar",
                    onClick = { onDisposition(ProposalDisposition.Ignored) },
                )
            }
            ProposalStance.Available -> {
                CardAction(
                    text = "Seguir",
                    onClick = { onDisposition(ProposalDisposition.Followed) },
                )
                CardAction(
                    text = "Ignorar",
                    onClick = { onDisposition(ProposalDisposition.Ignored) },
                )
            }
            ProposalStance.Ignored -> CardAction(
                text = "Restaurar",
                onClick = { onDisposition(null) },
            )
        }
    }
}
