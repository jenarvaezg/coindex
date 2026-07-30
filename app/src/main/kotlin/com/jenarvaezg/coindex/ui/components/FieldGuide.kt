package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.theme.PlateMetrics

/** Small caps rust label; the printed section marker of the guide. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
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
 */
@Composable
fun LinkText(
    text: String,
    style: TextStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Paper.moss,
) {
    Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp),
    )
}

/** Bordered paper card with the offset shadow of the web prototype. */
@Composable
fun FieldCard(
    modifier: Modifier = Modifier,
    dashed: Boolean = false,
    emphasized: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .background(Paper.card)
            .border(
                width = if (emphasized) 2.dp else 1.dp,
                color = if (dashed) Paper.hairline else Paper.line,
            )
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
                    text = label.uppercase(),
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
 */
@Composable
fun CoinSides(
    label: String,
    obverseUrl: String?,
    reverseUrl: String?,
    missing: Boolean,
    modifier: Modifier = Modifier,
    onImageSettled: (() -> Unit)? = null,
    onPaper: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CoinSide("Anverso", label, obverseUrl, missing, Modifier.weight(1f), onImageSettled, onPaper)
        CoinSide("Reverso", label, reverseUrl, missing, Modifier.weight(1f), onImageSettled, onPaper)
    }
}

/** Number of pictures [CoinSides] will actually request for this pair of URLs. */
fun coinSideImageCount(obverseUrl: String?, reverseUrl: String?): Int =
    listOfNotNull(obverseUrl, reverseUrl).size

@Composable
private fun CoinSide(
    caption: String,
    label: String,
    url: String?,
    missing: Boolean,
    modifier: Modifier = Modifier,
    onImageSettled: (() -> Unit)? = null,
    onPaper: Boolean = false,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        val frame = Modifier.fillMaxWidth().aspectRatio(1f)
        if (url == null) {
            Silhouette(frame)
        } else {
            // The silhouette waits behind the picture and stays for good if the picture never
            // arrives: offline or with a dead URL the cell must still read as a coin, not as a
            // blank. It is dropped on success so a transparent PNG does not sit on a circle.
            var painted by remember(url) { mutableStateOf(false) }
            Box(modifier = frame) {
                if (!painted) {
                    Silhouette(Modifier.matchParentSize())
                }
                AsyncImage(
                    model = url,
                    contentDescription = "$caption de $label",
                    contentScale = ContentScale.Fit,
                    // Exporting the whole sheet has to wait for every picture, so both outcomes
                    // report back: a picture that failed will never arrive.
                    onState = { state ->
                        when (state) {
                            is AsyncImagePainter.State.Success -> {
                                painted = true
                                onImageSettled?.invoke()
                            }
                            is AsyncImagePainter.State.Error -> onImageSettled?.invoke()
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
            text = caption.uppercase(),
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
