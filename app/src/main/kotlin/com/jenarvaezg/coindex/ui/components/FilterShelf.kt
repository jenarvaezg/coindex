package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.shelf.SEARCH_CLEAR_LABEL
import com.jenarvaezg.coindex.ui.shelf.shelfDisclosure
import com.jenarvaezg.coindex.ui.theme.Paper

/** How tall the search box is, shared with the aspa that clears it and with the test of the pair. */
val SEARCH_FIELD_HEIGHT = 40.dp

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
    /**
     * What the empty box says, and **there is no default** (#515).
     *
     * Three screens draw this same box over three populations — the cards of the index, the types of
     * the inventory, the curated plates the collector owns nothing of — and the placeholder is the
     * only thing that can tell them apart. A default was what let two of the three say «Buscar» and
     * leave the collector to find out by typing; required, it makes declaring the scope the price of
     * drawing the box.
     */
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Paper.ink),
        cursorBrush = SolidColor(Paper.rust),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = modifier
            .fillMaxWidth()
            .height(SEARCH_FIELD_HEIGHT)
            .background(Paper.card),
        decorationBox = { field ->
            val clearable = value.isNotEmpty()
            Row(
                // The end padding is the aspa's when there is one: the button carries its own air
                // inside its square, and 10 dp more would push its ink off the field's margin.
                modifier = Modifier.padding(start = 10.dp, end = if (clearable) 0.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchGlyph()
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (!clearable) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Paper.muted,
                        )
                    }
                    field()
                }
                if (clearable) {
                    ClearGlyph(onClick = { onValueChange("") })
                }
            }
        },
    )
}

/**
 * The aspa that empties the box, offered only while there is something to empty (#414).
 *
 * Its ink is [SEARCH_FIELD_HEIGHT] square because the field is that tall, and the drawn cross is
 * 16 dp of that — so the tap does not have to find the stroke. Android's 48 dp it buys the way the
 * year tag does ([RecessedYearTag]), with [minimumInteractiveComponentSize]: the target grows past
 * the field without the field growing with it, which is the whole reason that modifier exists.
 */
@Composable
private fun ClearGlyph(onClick: () -> Unit) {
    Canvas(
        Modifier
            .minimumInteractiveComponentSize()
            .size(SEARCH_FIELD_HEIGHT)
            .semantics { contentDescription = SEARCH_CLEAR_LABEL }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
    ) {
        val stroke = 1.5.dp.toPx()
        drawLine(Paper.muted, Offset(0f, 0f), Offset(size.width, size.height), stroke)
        drawLine(Paper.muted, Offset(size.width, 0f), Offset(0f, size.height), stroke)
    }
}

@Composable
private fun SearchGlyph() {
    Canvas(Modifier.size(18.dp)) {
        drawCircle(
            color = Paper.muted,
            radius = size.minDimension * 0.32f,
            center = Offset(size.width * 0.42f, size.height * 0.42f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()),
        )
        drawLine(
            color = Paper.muted,
            start = Offset(size.width * 0.66f, size.height * 0.66f),
            end = Offset(size.width * 0.92f, size.height * 0.92f),
            strokeWidth = 1.5.dp.toPx(),
        )
    }
}

/**
 * Air, not punctuation, between the tally and the trailing action (#416).
 *
 * A « · » here read as the leftover seam of something that had been taken out: the button carries
 * its own border, so what it needed from the count beside it was distance and not a mark saying
 * «and then». Shared with the test that measures it, so the promise and the assertion are one
 * number.
 */
val SHELF_ACTION_GAP = 12.dp

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
    actionLabel: String? = null,
    actionEnabled: Boolean = true,
    onAction: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // For the action, whose 48 dp is a touch target: the button grows to it and centres its own
    // label. A **Text** given the same modifier does not centre anything — it lays out one 13 dp
    // line at the top of a 48 dp box, which is where the tally of this shelf was printed, 17 dp
    // above the line it belongs to. The Row is 48 dp tall and centres its children already, so the
    // labels need nothing here.
    val touchSizedAction = Modifier
        .wrapContentHeight(Alignment.CenterVertically)
        .heightIn(min = 48.dp)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The summary and tally remain one large toggle target. A trailing action, when present,
            // is its sibling rather than a clickable nested inside another clickable.
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(role = Role.Button, onClick = onToggle),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Weighted, and weighted so that the tally is measured **first**: a Row hands the
                // unweighted children the room they ask for and leaves this one the remainder. With
                // both children unweighted the summary took the whole width and «170 de 678
                // casillas» wrapped into eight lines inside a 48 dp row. The summary is the one that
                // can afford to give: it truncates by design, and half a count is worse than a
                // shortened line about the filters.
                Text(
                    "${shelfDisclosure(expanded)}$summary",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                )
                // No `maxLines` of its own: the weight above is what keeps this on one line, and it
                // keeps it there at twice the type size too — measured, because the Row is 48 dp
                // tall and a tally that wraps is read through half of its own second line.
                Text(
                    tally,
                    style = MaterialTheme.typography.labelMedium,
                    color = Paper.rust,
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.width(SHELF_ACTION_GAP))
                CardAction(
                    text = actionLabel,
                    onClick = onAction,
                    enabled = actionEnabled,
                    modifier = touchSizedAction,
                )
            }
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
 * same number the tally already prints two lines up. Callers omit chips whose count is zero
 * ([FacetCounts.populated] / [FacetCounts.populatedIn]): offering a dead end costs a tap into an
 * empty list, and the other filters already condition the counts.
 *
 * **This is the album's one drawing of «elegido»**, which is why the pair of orders of «Explorar»
 * wears it too (#513) although that shelf has no facets at all (ADR 0030 §8 clause 4): a second
 * shape for the same state is a second thing to learn. Filled in moss with the paper's own ink on
 * it when it is the one in force, plain card when it is the one on offer. There a null [count] is
 * not the «all» chip of anything — an order narrows nothing, so there is no remainder to print.
 *
 * `selectable` and not `clickable`: the fill is what the eye reads, and the state it stands for is
 * what a screen reader has to hear — one of a set, and this one is on. It is also what lets a test
 * assert the state rather than the paint.
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
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
