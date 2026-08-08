package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.jenarvaezg.coindex.data.photos.CoinPhoto
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
 * reading. [share] adds the share mark for the one action that hands the plate to another app.
 */
@Composable
fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    share: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RectangleShape,
        modifier = modifier,
    ) {
        if (share) {
            ShareGlyph(color = Paper.paper, modifier = Modifier.padding(end = 8.dp))
        }
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
fun CheckGlyph(color: Color = Paper.ink, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(12.dp)) {
        val stroke = size.minDimension * 0.14f
        val joint = Offset(size.width * 0.42f, size.height * 0.76f)
        drawLine(
            color,
            Offset(size.width * 0.10f, size.height * 0.48f),
            joint,
            strokeWidth = stroke,
        )
        drawLine(
            color,
            joint,
            Offset(size.width * 0.90f, size.height * 0.16f),
            strokeWidth = stroke,
        )
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
 * Exposed for the notebook of #169, which draws its coins itself — at their real diameter and
 * reverse only — instead of through [CoinSides], and must not reinvent the mapping of the studio
 * white onto the page tone.
 */
fun paperCoinFilter(missing: Boolean): ColorFilter? = coinColorFilter(missing, onPaper = true)

private fun coinColorFilter(missing: Boolean, onPaper: Boolean): ColorFilter? = when {
    missing && onPaper -> ColorFilter.colorMatrix(GRAYSCALE_ON_PAPER)
    missing -> ColorFilter.colorMatrix(GRAYSCALE)
    onPaper -> ColorFilter.colorMatrix(PAPER_TINT)
    else -> null
}

/**
 * Obverse and reverse of one type.
 *
 * A missing piece still shows the catalog design, desaturated and faded, so the plate reads as
 * a gap in a collection rather than an empty box. [onPaper] is for the exported sheet, which is
 * a printed page rather than a screen and cannot afford the studio white around each photo.
 *
 * [onImageSettled] reports each side once it can no longer change, saying whether a photograph
 * actually arrived: the export waits on the count and has to know how many cells it is about to
 * freeze empty.
 */
@Composable
fun CoinSides(
    label: String,
    obverse: CoinPhoto?,
    reverse: CoinPhoto?,
    missing: Boolean,
    modifier: Modifier = Modifier,
    onImageSettled: ((painted: Boolean) -> Unit)? = null,
    onPaper: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CoinSide("Anverso", label, obverse, missing, Modifier.weight(1f), onImageSettled, onPaper)
        CoinSide("Reverso", label, reverse, missing, Modifier.weight(1f), onImageSettled, onPaper)
    }
}

/** Number of pictures [CoinSides] will actually report for this pair of faces. */
fun coinSideImageCount(obverse: CoinPhoto?, reverse: CoinPhoto?): Int =
    listOfNotNull(obverse, reverse).count { it.hasPicture }

@Composable
private fun CoinSide(
    caption: String,
    label: String,
    photo: CoinPhoto?,
    missing: Boolean,
    modifier: Modifier = Modifier,
    onImageSettled: ((painted: Boolean) -> Unit)? = null,
    onPaper: Boolean = false,
) {
    // The thumbnail first and the original behind it, so a refused picture costs a second
    // request rather than the cell (issue #67).
    val candidates = photo?.candidates.orEmpty()
    var attempt by remember(candidates) { mutableIntStateOf(0) }
    val url = candidates.getOrNull(attempt)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // The mount, drawn rather than inherited from the photograph: catalog pictures are shot
        // on white but cropped tight and unevenly, so a row of them left ragged white rectangles
        // of different sizes on the cream card. A square of the same white behind every picture,
        // ruled with the card's own hairline, makes the white deliberate. On the exported sheet
        // the mount is the paper's own tone, the one PAPER_TINT maps the studio white onto.
        val frame = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(if (onPaper) MOUNT else Color.White)
            .border(1.dp, Paper.hairline)
            .padding(3.dp)
        if (url == null) {
            Silhouette(frame)
        } else {
            // The silhouette waits behind the picture and stays for good if no candidate ever
            // arrives: offline or with a dead URL the cell must still read as a coin, not as a
            // blank. It is dropped on success so a transparent PNG does not sit on a circle.
            var painted by remember(candidates) { mutableStateOf(false) }
            Box(modifier = frame) {
                if (!painted) {
                    Silhouette(Modifier.matchParentSize())
                }
                AsyncImage(
                    model = url,
                    contentDescription = "$caption de $label",
                    contentScale = ContentScale.Fit,
                    // Exporting the whole sheet has to wait for every picture, so both outcomes
                    // report back — but only once the last candidate has had its turn.
                    onState = { state ->
                        when (state) {
                            is AsyncImagePainter.State.Success -> {
                                painted = true
                                onImageSettled?.invoke(true)
                            }
                            is AsyncImagePainter.State.Error ->
                                if (attempt < candidates.lastIndex) {
                                    attempt += 1
                                } else {
                                    onImageSettled?.invoke(false)
                                }
                            else -> Unit
                        }
                    },
                    colorFilter = coinColorFilter(missing, onPaper),
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(if (missing) 0.45f else 1f),
                )
            }
        }
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = Paper.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

/** Stand-in for a type whose catalog pictures are not cached. */
@Composable
fun Silhouette(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color(0x14646559), CircleShape)
            .border(1.dp, Paper.hairline, CircleShape),
    ) {}
}
