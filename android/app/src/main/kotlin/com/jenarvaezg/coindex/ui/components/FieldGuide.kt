package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
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

/**
 * Obverse and reverse of one type.
 *
 * A missing piece still shows the catalog design, desaturated and faded, so the plate reads as
 * a gap in a collection rather than an empty box.
 */
@Composable
fun CoinSides(
    label: String,
    obverseUrl: String?,
    reverseUrl: String?,
    missing: Boolean,
    modifier: Modifier = Modifier,
    onImageSettled: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CoinSide("Anverso", label, obverseUrl, missing, Modifier.weight(1f), onImageSettled)
        CoinSide("Reverso", label, reverseUrl, missing, Modifier.weight(1f), onImageSettled)
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
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (url == null) {
            Silhouette(Modifier.fillMaxWidth().aspectRatio(1f))
        } else {
            AsyncImage(
                model = url,
                contentDescription = "$caption de $label",
                contentScale = ContentScale.Fit,
                // Exporting the whole sheet has to wait for every picture, so both outcomes
                // report back: a picture that failed will never arrive.
                onState = { state ->
                    if (state is AsyncImagePainter.State.Success ||
                        state is AsyncImagePainter.State.Error
                    ) {
                        onImageSettled?.invoke()
                    }
                },
                colorFilter = if (missing) ColorFilter.colorMatrix(GRAYSCALE) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .alpha(if (missing) 0.45f else 1f),
            )
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
