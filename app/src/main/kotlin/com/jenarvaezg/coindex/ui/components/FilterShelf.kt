package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * The search box of a hierarchy: always visible, never persisted (ADR 0021 §1).
 *
 * Above the shelf and not inside it, because it is the one narrowing that answers while you type and
 * the only one that is gone next launch. Reopening the app with a stale word in this box and half the
 * collection hidden was measured as reading like a broken app, which is why the filters below it
 * survive a launch and this does not.
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Buscar", style = MaterialTheme.typography.bodyLarge) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * The shelf of filters, folded on entry.
 *
 * Folded because both hierarchies open on what the collector owns and not on a control panel: the
 * measured cost of an open shelf is the first card pushed below the fold. [summary] is the line that
 * keeps a filter set days ago from being invisible, and [tally] beside it is how much of the list is
 * showing right now.
 */
@Composable
fun FilterShelf(
    summary: String,
    tally: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // The whole line takes the tap and not just the caret: it is one line of a page,
                // and a 12dp triangle is not a target on a phone held in one hand.
                .clickable(role = Role.Button, onClick = onToggle)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${if (expanded) "▾" else "▸"} $summary",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(end = 12.dp),
            )
            Text(tally, style = MaterialTheme.typography.labelMedium, color = Paper.rust)
        }
        if (expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 8.dp),
                content = content,
            )
        }
    }
}

/** One row of chips under its own small-caps heading, wrapping as many lines as it needs. */
@Composable
fun Facet(title: String, content: @Composable FlowRowScope.() -> Unit) {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Eyebrow(title)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 4.dp),
            content = content,
        )
    }
}

/**
 * One chip of a facet, with the live count of what tapping it would leave.
 *
 * A [count] of null is the facet's «all» chip in the cases where a number beside it would be the
 * same number the tally already prints two lines up. Zero is drawn rather than hidden: a dead end
 * you can see is cheaper than a chip that vanishes and takes the collector's place in the shelf
 * with it.
 */
@Composable
fun FilterChip(
    label: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = count?.let { "$label · $it" } ?: label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) Paper.paper else Paper.ink,
        // The longest labels are gone — «Federación de Rusia (1991-presente)» is «Rusia» since
        // ADR 0023 — but a chip still truncates rather than wrap: a curated `short_name` may run to
        // 40 characters, and one chip taking a row to itself pushes the rest off the shelf.
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(if (selected) Paper.moss else Paper.card)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
