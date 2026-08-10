package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.domain.Referent

/**
 * One hand-drawn silhouette, on its own box.
 *
 * @param width the drawing's own box width, in the same arbitrary units as [height]: a bus is long and a
 *   person is tall, and squaring them all would leave the bus a stripe.
 * @param body the filled outline, as SVG path data.
 * @param cutouts what is paper rather than ink inside it — the finger holes of the bowling ball, the rim
 *   of the tyre, the windows of the bus. Drawn **over** the body in the sheet's own colour rather than
 *   punched out of it by winding order, which is a hole that vanishes the day the outline is edited.
 * @param strokes what is line rather than fill: a bicycle is two rings and a frame, and filling it would
 *   give a bicycle-shaped blob.
 */
internal data class Silhouette(
    val width: Float,
    val height: Float,
    val body: String? = null,
    val cutouts: String? = null,
    val strokes: String? = null,
)

/**
 * Side on, standing, and the same animal twice.
 *
 * 30 kg of labrador on the weight ladder and 60 cm of shepherd on the height ladder are one drawing,
 * which is why the prototype counted fourteen figures for fifteen rungs: what says «dog» at
 * twenty-six density-independent pixels is a body on four legs with a snout, and never a breed.
 */
private val DOG = Silhouette(
    width = 120f,
    height = 84f,
    body = "M16 34 C22 26 34 22 52 22 L84 22 C94 22 100 26 104 32 " +
        "L114 30 L112 40 L104 42 C104 52 100 58 94 60 " +
        "L94 80 L86 80 L86 62 L62 62 L62 80 L54 80 L54 60 " +
        "L36 60 L36 80 L28 80 L28 58 C20 54 14 46 16 34 Z " +
        "M16 30 C10 22 12 12 20 10 C22 18 22 26 20 32 Z",
)

/**
 * The fourteen drawings of the three ladders (`docs/ux/cifras-326.md`).
 *
 * **They are ours and they are maintained here.** «No son un asset que se pueda descargar: son parte de
 * la identidad» — the collector asked for little figures and not for italics, and the same comparison
 * written as prose is exactly the thing this map came to prune.
 *
 * Schematic on purpose, because they are read small and above a rule: four strokes that say «bus» beat a
 * faithful outline that turns to mud.
 */
private val SILHOUETTES: Map<Referent, Silhouette> = mapOf(
    Referent.Brick to Silhouette(
        width = 100f,
        height = 46f,
        body = "M2 4 L98 4 L98 42 L2 42 Z",
        cutouts = "M2 21 L98 21 L98 25 L2 25 Z",
    ),
    Referent.Cat to Silhouette(
        width = 78f,
        height = 100f,
        body = "M16 98 C16 66 26 54 34 50 C28 44 28 30 33 25 L40 34 " +
            "C46 31 54 31 60 34 L67 25 C72 30 72 44 66 50 " +
            "C74 54 74 98 60 98 Z " +
            "M58 98 C74 94 78 74 68 62 L60 70 C68 80 66 90 52 94 Z",
    ),
    Referent.BowlingBall to Silhouette(
        width = 88f,
        height = 88f,
        body = "M44 2 C67 2 86 21 86 44 C86 67 67 86 44 86 C21 86 2 67 2 44 C2 21 21 2 44 2 Z",
        cutouts = "M30 26 m-6 0 a6 6 0 1 0 12 0 a6 6 0 1 0 -12 0 " +
            "M53 25 m-6 0 a6 6 0 1 0 12 0 a6 6 0 1 0 -12 0 " +
            "M41 45 m-6 0 a6 6 0 1 0 12 0 a6 6 0 1 0 -12 0",
    ),
    Referent.Tyre to Silhouette(
        width = 92f,
        height = 92f,
        body = "M46 2 C70 2 90 22 90 46 C90 70 70 90 46 90 C22 90 2 70 2 46 C2 22 22 2 46 2 Z",
        cutouts = "M46 25 C57 25 67 35 67 46 C67 57 57 67 46 67 " +
            "C35 67 25 57 25 46 C25 35 35 25 46 25 Z",
    ),
    Referent.Labrador to DOG,
    Referent.Shepherd to DOG,
    Referent.Bicycle to Silhouette(
        width = 116f,
        height = 74f,
        strokes = "M28 50 m-20 0 a20 20 0 1 0 40 0 a20 20 0 1 0 -40 0 " +
            "M88 50 m-20 0 a20 20 0 1 0 40 0 a20 20 0 1 0 -40 0 " +
            "M28 50 L54 50 L66 20 L88 50 M54 50 L70 20 M46 18 L74 18",
    ),
    Referent.Car to Silhouette(
        width = 124f,
        height = 56f,
        body = "M6 42 L14 24 C18 17 26 13 36 13 L82 13 C92 13 100 17 106 25 " +
            "L114 38 C119 40 122 43 122 47 L122 50 L2 50 L2 47 C2 44 3 42 6 42 Z",
        cutouts = "M28 44 m-9 0 a9 9 0 1 0 18 0 a9 9 0 1 0 -18 0 " +
            "M96 44 m-9 0 a9 9 0 1 0 18 0 a9 9 0 1 0 -18 0 " +
            "M24 24 L56 24 L56 38 L20 38 Z M62 24 L84 24 L96 38 L62 38 Z",
    ),
    Referent.Bus to Silhouette(
        width = 148f,
        height = 58f,
        body = "M8 8 L138 8 C143 8 146 12 146 17 L146 48 L2 48 L2 17 C2 12 3 8 8 8 Z",
        cutouts = "M30 46 m-9 0 a9 9 0 1 0 18 0 a9 9 0 1 0 -18 0 " +
            "M116 46 m-9 0 a9 9 0 1 0 18 0 a9 9 0 1 0 -18 0 " +
            "M12 16 L44 16 L44 30 L12 30 Z M52 16 L84 16 L84 30 L52 30 Z " +
            "M92 16 L124 16 L124 30 L92 30 Z",
    ),
    Referent.Lorry to Silhouette(
        width = 158f,
        height = 62f,
        body = "M2 12 L96 12 L96 50 L2 50 Z " +
            "M102 22 L128 22 C136 22 142 27 146 35 L154 50 L102 50 Z",
        cutouts = "M28 48 m-10 0 a10 10 0 1 0 20 0 a10 10 0 1 0 -20 0 " +
            "M126 48 m-10 0 a10 10 0 1 0 20 0 a10 10 0 1 0 -20 0 " +
            "M112 28 L132 28 L138 38 L112 38 Z",
    ),
    Referent.Whale to Silhouette(
        width = 168f,
        height = 62f,
        body = "M8 40 C24 16 60 8 96 12 C126 15 146 24 152 30 " +
            "L166 12 L164 46 L150 36 C140 46 116 54 88 54 C50 54 18 50 8 40 Z " +
            "M62 11 C66 3 73 3 78 9 C73 7 67 8 62 11 Z",
    ),
    Referent.Stool to Silhouette(
        width = 74f,
        height = 94f,
        body = "M4 6 L70 6 L70 18 L4 18 Z " +
            "M14 18 L23 18 L31 92 L22 92 Z M51 18 L60 18 L52 92 L43 92 Z " +
            "M24 52 L50 52 L50 59 L24 59 Z",
    ),
    Referent.Countertop to Silhouette(
        width = 116f,
        height = 94f,
        body = "M2 6 L114 6 L114 20 L2 20 Z M8 20 L108 20 L108 92 L8 92 Z",
        cutouts = "M18 30 L54 30 L54 52 L18 52 Z M62 30 L98 30 L98 52 L62 52 Z " +
            "M18 60 L98 60 L98 84 L18 84 Z",
    ),
    Referent.Doorknob to Silhouette(
        width = 66f,
        height = 100f,
        body = "M6 2 L60 2 L60 100 L6 100 Z",
        cutouts = "M14 10 L52 10 L52 96 L14 96 Z " +
            "M22 52 m-7 0 a7 7 0 1 0 14 0 a7 7 0 1 0 -14 0",
    ),
    Referent.Person to Silhouette(
        width = 54f,
        height = 100f,
        body = "M27 4 C33 4 38 9 38 15 C38 21 33 26 27 26 C21 26 16 21 16 15 C16 9 21 4 27 4 Z " +
            "M18 30 L36 30 C42 30 46 34 46 40 L46 58 L40 58 L40 96 L32 96 L32 62 L22 62 " +
            "L22 96 L14 96 L14 58 L8 58 L8 40 C8 34 12 30 18 30 Z",
    ),
)

/** How tall a figure stands over its rung. */
internal val SILHOUETTE_HEIGHT: Dp = 26.dp

/** The line weight of the one drawing that is line and not fill, in the drawing's own units. */
private const val STROKE_UNITS = 6f

/**
 * A silhouette drawn at [height], keeping its own proportions.
 *
 * **The height is what is fixed, not the width**, because the three ladders stand their figures on a
 * rule: a bus and a person given the same box would put the bus's roof at the person's waist.
 */
@Composable
internal fun ReferentSilhouette(
    referent: Referent,
    height: Dp,
    color: Color,
    paper: Color,
    modifier: Modifier = Modifier,
) {
    val drawing = SILHOUETTES[referent] ?: return
    val body = remember(referent) { drawing.body?.toPath() }
    val cutouts = remember(referent) { drawing.cutouts?.toPath() }
    val strokes = remember(referent) { drawing.strokes?.toPath() }
    Canvas(
        modifier = modifier
            .height(height)
            .width(height * (drawing.width / drawing.height)),
    ) {
        val factor = size.height / drawing.height
        scale(scaleX = factor, scaleY = factor, pivot = Offset.Zero) {
            body?.let { drawPath(it, color, style = Fill) }
            strokes?.let { drawPath(it, color, style = Stroke(width = STROKE_UNITS)) }
            cutouts?.let { drawPath(it, paper, style = Fill) }
        }
    }
}

private fun String.toPath(): Path = PathParser().parsePathString(this).toPath()

/**
 * The referents with no drawing, which has to be nobody.
 *
 * A rung whose figure is missing is a ladder with a hole in it, and the hole is invisible from the code:
 * [ReferentSilhouette] simply draws nothing. Read by the suite, which is where a fifteenth referent added
 * without its figure gets caught.
 */
internal fun referentsWithoutDrawing(): List<Referent> =
    Referent.entries.filterNot { it in SILHOUETTES.keys }

/** How wide a figure is at [height], so a ladder can lay its rungs out before drawing them. */
internal fun silhouetteWidth(referent: Referent, height: Dp): Dp {
    val drawing = SILHOUETTES[referent] ?: return height
    return height * (drawing.width / drawing.height)
}
