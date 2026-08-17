package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * A mode, said by the sheet itself: the band at the foot, and the paper it stands on (#517).
 *
 * «Marcar lo que busco» and «Hacer una colección» change what touching a casilla or a card *means*, and
 * until this existed the scene barely said so — one small line of instruction in a header that
 * scrolls away, and a grid that looked exactly like the grid outside the mode. Two taps down the
 * plate there was no frame of that screen that could tell you which of two apps you were in.
 *
 * Three things say it now, and they say it in the album's own materials rather than in a colour
 * borrowed from somewhere else:
 *
 * - **The band** — this composable: a strip of deeper card at the foot of the sheet, ruled off with
 *   the same hairline every other cut in this notebook is ruled with, holding what the mode is for
 *   and the way out of it. It is a **row of the layout and not a floating bar**: it takes its
 *   height off the grid, so nothing is ever hidden underneath it and there is no inset arithmetic
 *   to get wrong.
 * - **The paper** — [sheetUnderMode]: while a mode is open the whole page goes a shade deeper, the
 *   way a sheet does when it is taken out of the album and put on the workbench.
 * - **The step back** — [outsideTheMode]: what the mode cannot touch stops competing for the eye.
 *
 * The band prints no copy of its own. It is handed the sentence the mode already had — the one that
 * names the spend where the gesture is (ADR 0029 §5, #282) — and its actions are the mode's own,
 * moved down here from the header they used to scroll away with.
 */
@Composable
fun ModeBand(
    sentence: String,
    modifier: Modifier = Modifier,
    actions: @Composable FlowRowScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Paper.paperDeep)
            .drawBehind {
                val rule = 1.dp.toPx()
                drawRect(
                    color = Paper.hairline,
                    size = size.copy(height = rule),
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            sentence,
            style = MaterialTheme.typography.bodyMedium,
            color = Paper.ink,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = actions,
        )
    }
}

/**
 * The page a mode is worked on: the same paper, a shade deeper (#517).
 *
 * Drawn **behind** what the surface holds rather than over it, so no photograph is dimmed and no
 * word loses contrast: what changes is the cardboard the casillas are cut out of. It is the
 * cheapest possible signal — one rectangle, no layer, no animation — and it is the one that is true
 * of every frame, including the ones scrolled far away from any button.
 *
 * The tone is the album's own [Paper.paperDeep], and it goes **down** and not up on purpose: the
 * app's paper is already at 238 of 255 and has nowhere left to lighten (#509), so a wash that
 * relied on brightening would be measured in the browser and vanish on the phone.
 */
fun Modifier.sheetUnderMode(open: Boolean): Modifier =
    if (!open) this else drawBehind { drawRect(Paper.paperDeep.copy(alpha = MODE_WASH_OPACITY)) }

/**
 * What the open mode does not reach, stepping back out of the way (#517).
 *
 * A full casilla cannot be marked and does not pretend to be: while the mode is open it keeps its
 * coin and loses its weight, which is what makes the empty ones — the ones the sentence in the band
 * is talking about — the only thing on the sheet that still looks pressable.
 *
 * It is only ever paired with taking the tap away. Something drawn faint that still answers a press
 * is worse than either half on its own.
 */
fun Modifier.outsideTheMode(outside: Boolean): Modifier =
    if (!outside) this else alpha(MODE_ASIDE_OPACITY)

/**
 * How deep the page goes while a mode is open.
 *
 * At 0.55 the deep card lands about ten levels of 255 under the sheet — visible as a change of air
 * across the whole grid, and nowhere near enough to read as a dialog's scrim or to put the ghost of
 * an empty casilla in shadow.
 */
private const val MODE_WASH_OPACITY = 0.55f

/** Faint enough to stop asking for the eye, solid enough that the coin is still recognisable. */
private const val MODE_ASIDE_OPACITY = 0.45f
