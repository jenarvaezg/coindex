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
 * A title that opens something, written as text rather than as a button: `TextButton` clips its
 * label to the button shape, cutting a serif title's edge letters and the lines of a wrapped one.
 * Underlined, because moss alone is too close to the surrounding prose to signal a link.
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

/** Level 1 of the action system: the action a screen exists for, in filled ink. */
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
 * Level 2: a card's own actions. Outlined and compact so the card keeps its weight; bordered
 * because a bare `TextButton` in ink reads as a caption. The border is the dashed cards' hairline
 * colour.
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
 * Level 3: a link that leaves the app, underlined with a drawn arrow marking it as a browser link.
 * The arrow follows a non-breaking space, so wrapping never strands it on its own line. Meant for
 * prose.
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

/**
 * [BackGlyph] reversed: the door of an annex (ADR 0026 §8). Drawn because neither Bitter nor Barlow
 * has an arrow glyph (#298), so a typed «→» would fall back to a system font.
 */
@Composable
fun ForwardGlyph(color: Color = Paper.ink, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 13.dp, height = 10.dp)) {
        val stroke = size.minDimension * 0.14f
        val right = Offset(size.width * 0.90f, size.height * 0.50f)
        drawLine(color, right, Offset(size.width * 0.58f, size.height * 0.12f), strokeWidth = stroke)
        drawLine(color, right, Offset(size.width * 0.58f, size.height * 0.88f), strokeWidth = stroke)
        drawLine(color, right, Offset(size.width * 0.08f, size.height * 0.50f), strokeWidth = stroke)
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

/** The share mark, drawn rather than imported: the app doesn't depend on Material's icon pack. */
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
 * Bordered paper card. [dashed] marks an absence (a «me falta» cell, a section with no cards),
 * never a card with pieces behind it. [emphasized] doubles the rule for cells the collector owns,
 * and [dashed] wins over it.
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
 * Multiplies a picture by the page it is printed on: catalog photographs are shot on white, and
 * scaling each channel by the mount's maps that white onto the card tone.
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
 * The filter a coin gets on a printed page (#169). Only print uses it; on screen a hole fades a
 * missing coin with alpha instead (#423).
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
