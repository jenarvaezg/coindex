package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * The year under a hole, as a tag sunk into the cardboard: it reads as pressable by its shape, with
 * no colour, underline or arrow (#302). [minimumInteractiveComponentSize] brings the 28 dp-tall tag
 * to a 48 dp target. [onOpen] is null for an announced casilla, which has no ficha to open, and the
 * tag then takes no click.
 */
@Composable
fun RecessedYearTag(
    year: String,
    onOpen: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val tag = Modifier
        .widthIn(min = YearTagMetrics.width)
        .height(YearTagMetrics.height)
        .recessedInBoard()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .then(
                if (onOpen != null) {
                    Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(role = Role.Button, onClick = onOpen)
                } else {
                    Modifier
                },
            )
            .then(tag),
    ) {
        Text(
            text = year,
            style = MaterialTheme.typography.labelLarge,
            color = Paper.ink,
        )
    }
}

/**
 * The same tag carrying a name, for plates where every casilla has the same year (#511), such as
 * the Paquillos; the plate's specification states the year once. It spans the casilla's width and
 * grows downwards to [NAME_TAG_MAX_LINES] lines. Same type as the year tag, so both read as the
 * same pressable piece.
 */
@Composable
fun RecessedNameTag(
    name: String,
    onOpen: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onOpen != null) {
                    Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(role = Role.Button, onClick = onOpen)
                } else {
                    Modifier
                },
            )
            .heightIn(min = YearTagMetrics.height)
            .recessedInBoard(),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            color = Paper.ink,
            textAlign = TextAlign.Center,
            maxLines = NAME_TAG_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
        )
    }
}

/** The same line limit as a casilla's name and the notebook (#412). */
private const val NAME_TAG_MAX_LINES = 3

/**
 * The tag's drawn size and the transparent slack its touch target adds around it, which
 * `PlateSpacing` accounts for when spacing from the ink (#411).
 */
internal object YearTagMetrics {
    /** The tag drawn at the #302 bench. */
    val width = 48.3.dp
    val height = 28.dp

    /** Android's minimum touch target, which the tag is centred inside. */
    val target = 48.dp

    /** The transparent air above and below the ink, on a tag that takes the click. */
    val slack = (target - height) / 2
}
