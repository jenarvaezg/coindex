package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.domain.DerivedCollectionDisposition
import com.jenarvaezg.coindex.ui.DerivedCollectionStance

/**
 * What you can do with a collection, wherever it is shown: the index card and its own
 * screen offer the same three decisions, so they share one row of buttons.
 *
 * A [FlowRow] rather than a [androidx.compose.foundation.layout.Row]: «Ver lámina · Dejar de
 * seguir · Ignorar» is three buttons wide and the narrow phone this is built for would clip the
 * last one off the card.
 */
@Composable
fun DispositionActions(
    stance: DerivedCollectionStance,
    onDisposition: (DerivedCollectionDisposition?) -> Unit,
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
            DerivedCollectionStance.Followed -> {
                CardAction(text = "Dejar de seguir", onClick = { onDisposition(null) })
                CardAction(
                    text = "Ignorar",
                    onClick = { onDisposition(DerivedCollectionDisposition.Ignored) },
                )
            }
            DerivedCollectionStance.Available -> {
                CardAction(
                    text = "Seguir",
                    onClick = { onDisposition(DerivedCollectionDisposition.Followed) },
                )
                CardAction(
                    text = "Ignorar",
                    onClick = { onDisposition(DerivedCollectionDisposition.Ignored) },
                )
            }
            DerivedCollectionStance.Ignored -> CardAction(
                text = "Restaurar",
                onClick = { onDisposition(null) },
            )
        }
    }
}
