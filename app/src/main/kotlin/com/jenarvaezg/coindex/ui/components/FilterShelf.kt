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

/** Height of the search box, shared with its clear button and the test that checks them. */
val SEARCH_FIELD_HEIGHT = 40.dp

/**
 * The search box of a hierarchy: always visible, never persisted (ADR 0021 §1), so the app never
 * reopens with a stale word hiding half the collection. The filters below it do persist.
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    /**
     * What the empty box says. No default (#515): three screens search different things, and the
     * placeholder is what tells the collector which.
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
                // No end padding with the clear button, which has its own inside its square.
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
 * The cross that empties the box, shown only while it has text (#414). Drawn [SEARCH_FIELD_HEIGHT]
 * square; [minimumInteractiveComponentSize] grows the touch target to 48 dp without growing the
 * field, as for [RecessedYearTag].
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
 * Space, not a « · », between the tally and the bordered trailing action (#416). Shared with its
 * test.
 */
val SHELF_ACTION_GAP = 12.dp

/**
 * The shelf of filters, folded on entry so the first card stays above the fold. [summary] keeps
 * active filters visible while folded; [tally] says how much of the list is showing.
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
    // Only for the button, which centres its label in the 48 dp target. A Text given this would sit
    // at the top of the box; the 48 dp Row already centres the labels.
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
            // The summary and tally remain one large toggle target. A trailing action, when
            // present, is its sibling rather than a clickable nested inside another clickable.
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(role = Role.Button, onClick = onToggle),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Weighted so the tally is measured first and the summary truncates instead;
                // unweighted, the summary took the whole width and the tally wrapped.
                Text(
                    "${shelfDisclosure(expanded)}$summary",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                )
                // No `maxLines`: the weight above keeps it on one line, even at twice the font
                // size.
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
 * One chip of a facet, with the live count of what tapping it would leave. A null [count] is an
 * «all» chip whose number would repeat the tally. Callers omit chips counting zero
 * ([FacetCounts.populated] / [FacetCounts.populatedIn]).
 *
 * The album's only drawing of a selected option, so «Explorar» uses it for its two orders too
 * (#513, ADR 0030 §8 clause 4), with a null [count]. `selectable` rather than `clickable`, so
 * screen readers and tests get the selected state.
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
        // Truncates rather than wraps: a curated `short_name` (ADR 0023) may run to 40 characters.
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(if (selected) Paper.moss else Paper.card)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
