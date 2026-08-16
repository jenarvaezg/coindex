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
 * The year under a hole, as a tag sunk into the cardboard.
 *
 * It is the affordance turned into form the #302 map went looking for: no colour, no underline and
 * no arrow — the year can be pressed because it *looks* like a separate piece pressed into the
 * sheet. The arrow is what the map could not afford: neither Bitter nor Barlow carries that glyph
 * (#298), so twenty-two of them would be twenty-two letters of the system typeface on a sheet of
 * paper.
 *
 * 48,3 × 28 dp is the drawing the map measured, and it is the only one of its four candidates
 * whose ink reaches Android's 48 dp. None of the four reached 48 dp *tall*, so the tag buys the
 * rest of its target area with [minimumInteractiveComponentSize] instead of pretending its ink
 * is bigger than it is.
 *
 * [onOpen] is null for an announced casilla, which has no Numista page: the tag is then a label
 * and not a target, and it says so by not taking the click.
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
 * The same piece of cardboard, on a casilla whose year tells it from nothing (#511).
 *
 * Four plates of the seventy-five give every casilla the same year — Paquillos struck its five stars
 * in 1966 — so the tag was drawn five times identical while the difference sat underneath in plain
 * ink. What goes into the recess is what distinguishes the casilla, and the plate's specification
 * keeps saying the year once, where a fact of the whole plate belongs.
 *
 * **It spans the casilla and grows downwards**, because a name is not a year: 48,3 dp holds «1966»
 * and nothing else, and `height(28.dp)` would not have grown — «25 bolívares · jaguar · 28,28 g»
 * simply fell out of the box. Three lines is the same cut the name at the foot of a casilla takes
 * (#412), so nothing the screen prints whole becomes an ellipsis on paper.
 *
 * The letter is **the tag's own** and not the name's: this is the piece the collector presses, and
 * dressing it in Bitter would have made two different objects out of one. The plate that draws a
 * year and the plate that draws a star press the same thing.
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

/** The cut the name at the foot of a casilla already takes, and the notebook with it (#412). */
private const val NAME_TAG_MAX_LINES = 3

/**
 * The drawing of the tag, and the blank its target leaves around it.
 *
 * The plate does its proximity arithmetic with these (`PlateSpacing`, #411): what the eye measures
 * from is the ink, and between the ink and the next thing on the sheet there is always the
 * transparent half of the target the tag bought with [minimumInteractiveComponentSize].
 */
internal object YearTagMetrics {
    /** The tag the #302 bench drew and the AVD confirmed: 48,3 × 28 dp. */
    val width = 48.3.dp
    val height = 28.dp

    /** Android's minimum target, which is what a tag of 28 dp of ink is centred inside. */
    val target = 48.dp

    /** The transparent air above and below the ink, on a tag that takes the click. */
    val slack = (target - height) / 2
}
