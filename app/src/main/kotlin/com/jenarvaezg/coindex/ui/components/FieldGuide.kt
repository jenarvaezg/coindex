package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.theme.PlateMetrics

/** Small caps rust label; the printed section marker of the guide. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = Paper.rust,
        modifier = modifier,
    )
}

/**
 * A title that opens something, written as text rather than as a button.
 *
 * `TextButton` is the obvious control, but it centres its label inside a fixed minimum height
 * and clips it to the button shape: with `contentPadding = 0` a serif title loses its first and
 * last letters, and a title that wraps loses whole lines. Here the text keeps its own metrics
 * and takes the click itself, with the tap area grown by the padding.
 *
 * Underlined, because with `primary = ink` the moss is too close to the prose around it to
 * carry the affordance on its own.
 */
@Composable
fun LinkText(
    text: String,
    style: TextStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Paper.moss,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        style = style.copy(textDecoration = TextDecoration.Underline),
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp),
    )
}

/**
 * Level 1 of the action system: the action a screen exists for.
 *
 * Filled ink, as «Sincronizar» always was, and rare enough that the eye finds it without
 * reading.
 */
@Composable
fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RectangleShape,
        modifier = modifier,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Level 2: a card's own actions.
 *
 * Outlined and compact so the card keeps its weight, but bordered: with `primary = ink` a bare
 * `TextButton` is the same colour as the prose around it and reads as a caption. The hairline
 * is the same one the dashed cards use, so the button belongs to the page.
 */
@Composable
fun CardAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RectangleShape,
        border = BorderStroke(1.dp, Paper.hairline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Paper.ink),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        modifier = modifier,
    ) {
        icon?.let {
            it()
            Spacer(modifier = Modifier.size(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Level 3: a link that leaves the app.
 *
 * An underlined link with a drawn arrow appended: the underline says it opens something, the
 * arrow says the something is a browser rather than another page of this notebook. The arrow is
 * inline content after a non-breaking space, so wrapping never strands it on a line of its own.
 *
 * It is for prose, and the casilla of a plate is the one place that asked it to be something else:
 * the name under a hole had to shrink and truncate inside a 113 dp cell, and the arrow was 19 dp of
 * that cell. The plate's names are plain ink now and the year's recessed tag opens Numista instead
 * (#337), so the parameters that grid asked for are gone again.
 */
@Composable
fun ExternalLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val markedText = buildAnnotatedString {
        append(text)
        append('\u00A0')
        appendInlineContent(EXTERNAL_LINK_GLYPH_ID)
    }
    Text(
        text = markedText,
        inlineContent = mapOf(
            EXTERNAL_LINK_GLYPH_ID to InlineTextContent(
                placeholder = Placeholder(
                    width = 0.85.em,
                    height = 0.85.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
            ) {
                ExternalLinkGlyph(color = Paper.moss, modifier = Modifier.fillMaxSize())
            },
        ),
        style = style.copy(textDecoration = TextDecoration.Underline),
        color = Paper.moss,
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp),
    )
}

private const val EXTERNAL_LINK_GLYPH_ID = "external-link-glyph"

@Composable
fun BackGlyph(color: Color = Paper.ink, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 13.dp, height = 10.dp)) {
        val stroke = size.minDimension * 0.14f
        val left = Offset(size.width * 0.10f, size.height * 0.50f)
        val elbow = Offset(size.width * 0.42f, size.height * 0.12f)
        drawLine(color, left, elbow, strokeWidth = stroke)
        drawLine(color, left, Offset(size.width * 0.42f, size.height * 0.88f), strokeWidth = stroke)
        drawLine(color, left, Offset(size.width * 0.92f, size.height * 0.50f), strokeWidth = stroke)
    }
}

@Composable
private fun ExternalLinkGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.12f
        val corner = Offset(size.width * 0.84f, size.height * 0.16f)
        drawLine(
            color,
            Offset(size.width * 0.22f, size.height * 0.78f),
            corner,
            strokeWidth = stroke,
        )
        drawLine(
            color,
            Offset(size.width * 0.48f, size.height * 0.16f),
            corner,
            strokeWidth = stroke,
        )
        drawLine(
            color,
            corner,
            Offset(size.width * 0.84f, size.height * 0.52f),
            strokeWidth = stroke,
        )
    }
}

/**
 * The share mark, drawn rather than imported.
 *
 * Material's icon pack is not a dependency of this app and would be the only piece of Material
 * iconography in a notebook drawn with rules and circles; three dots and two strokes are the
 * same idea in the guide's own hand.
 */
@Composable
fun ShareGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(13.dp)) {
        val radius = size.minDimension * 0.14f
        val stroke = size.minDimension * 0.08f
        val hinge = Offset(radius, size.height / 2)
        val top = Offset(size.width - radius, radius)
        val bottom = Offset(size.width - radius, size.height - radius)
        drawLine(color, hinge, top, strokeWidth = stroke)
        drawLine(color, hinge, bottom, strokeWidth = stroke)
        listOf(hinge, top, bottom).forEach { drawCircle(color, radius, it) }
    }
}

/**
 * A hairline rule drawn as dashes, in the guide's own hand.
 *
 * [Modifier.border] has no dashed form, so the rectangle is stroked by hand with a
 * [PathEffect.dashPathEffect]; inset by half the stroke so the dashes land inside the card
 * instead of straddling its edge.
 */
private fun Modifier.dashedBorder(color: Color, width: Dp): Modifier = drawBehind {
    val stroke = width.toPx()
    val inset = stroke / 2
    drawRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(size.width - stroke, size.height - stroke),
        style = Stroke(
            width = stroke,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(DASH_LENGTH.toPx(), DASH_GAP.toPx()),
            ),
        ),
    )
}

private val DASH_LENGTH = 5.dp
private val DASH_GAP = 4.dp

/**
 * Bordered paper card.
 *
 * [dashed] is drawn dashed, and means what a dashed box means on paper: nothing is mounted here
 * yet. It is for absences — a «me falta» cell, a section with no cards in it — never for a card
 * that has pieces behind it. [emphasized] is the opposite end: a double-weight rule for the
 * cells the collector actually owns, and it gives way to [dashed], because an absence is never
 * something to emphasize.
 *
 * (The parameter used to only change the border colour, while its comment promised both a
 * dashed rule and an offset shadow the card never drew.)
 */
@Composable
fun FieldCard(
    modifier: Modifier = Modifier,
    dashed: Boolean = false,
    emphasized: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val outline = if (dashed) {
        Modifier.dashedBorder(Paper.hairline, 1.dp)
    } else {
        Modifier.border(width = if (emphasized) 2.dp else 1.dp, color = Paper.line)
    }
    Column(
        modifier = modifier
            .background(Paper.card)
            .then(outline)
            .padding(PlateMetrics.cardPadding),
        content = content,
    )
}

/** Key/value specification list, the field notebook's data block. */
@Composable
fun SpecificationCard(entries: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    FieldCard(modifier = modifier) {
        entries.forEachIndexed { index, (label, value) ->
            if (index > 0) {
                HorizontalDivider(
                    color = Paper.paperDeep,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Paper.muted,
                )
                Text(text = value, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private val GRAYSCALE = ColorMatrix().apply { setToSaturation(0f) }

/** What a coin photo is actually mounted on: the translucent card over the paper. */
private val MOUNT = Paper.card.compositeOver(Paper.paper)

/**
 * Multiplies a picture by the page it is printed on.
 *
 * Catalog photographs are shot on white, and a white rectangle on cream paper is the one thing
 * that gives away that the plate is a screenshot and not a page. Scaling each channel by the
 * mount's own maps that white exactly onto the card and leaves the coin where it was.
 */
private val PAPER_TINT = ColorMatrix().apply {
    this[0, 0] = MOUNT.red
    this[1, 1] = MOUNT.green
    this[2, 2] = MOUNT.blue
}

private val GRAYSCALE_ON_PAPER = ColorMatrix(GRAYSCALE.values.copyOf()).apply {
    timesAssign(PAPER_TINT)
}

/**
 * The filter a coin gets on a printed page, where every coin is on paper by definition.
 *
 * The notebook of #169 draws its coins itself — at their real diameter and one face only — and this
 * is here so it does not reinvent the mapping of the studio white onto the page tone. It is the last
 * caller: on **screen** a coin is now always a photograph inside a die-cut hole, which fades a
 * missing design with its own alpha and needs no matrix (#423).
 */
fun paperCoinFilter(missing: Boolean): ColorFilter =
    ColorFilter.colorMatrix(if (missing) GRAYSCALE_ON_PAPER else PAPER_TINT)

/** Stand-in for a type whose catalog pictures are not cached. */
@Composable
fun Silhouette(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color(0x14646559), CircleShape)
            .border(1.dp, Paper.hairline, CircleShape),
    ) {}
}
