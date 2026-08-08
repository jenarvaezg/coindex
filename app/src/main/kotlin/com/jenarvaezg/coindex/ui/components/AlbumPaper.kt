package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.ui.theme.Paper

private const val GRAIN_MOSAIC_PX = 256
private const val GRAIN_FIBRES = 180

/** Fine offset-paper fibre, repeated as a deterministic 256 px soft-light mosaic. */
@Composable
fun PaperGrain(
    modifier: Modifier = Modifier,
    opacity: Float = 0.08f,
) {
    Canvas(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        },
    ) {
        var tileTop = 0f
        while (tileTop < size.height) {
            var tileLeft = 0f
            while (tileLeft < size.width) {
                repeat(GRAIN_FIBRES) { fibre ->
                    val x = ((fibre * 73 + 19) % GRAIN_MOSAIC_PX).toFloat()
                    val y = ((fibre * 151 + fibre * fibre + 7) % GRAIN_MOSAIC_PX).toFloat()
                    val length = 3f + ((fibre * 17) % 18)
                    val hash = (fibre * 47 + 31) and 0xFF
                    val tone = if (hash and 1 == 0) Color.White else Paper.ink
                    drawLine(
                        color = tone.copy(alpha = opacity * (0.25f + hash / 510f)),
                        start = androidx.compose.ui.geometry.Offset(tileLeft + x, tileTop + y),
                        end = androidx.compose.ui.geometry.Offset(
                            tileLeft + x + length,
                            tileTop + y + 0.7f,
                        ),
                        strokeWidth = 1f,
                        blendMode = BlendMode.Softlight,
                    )
                }
                tileLeft += GRAIN_MOSAIC_PX
            }
            tileTop += GRAIN_MOSAIC_PX
        }
    }
}

/** One catalog face sunk into cardboard, with a fixed acetate reflection over it. */
@Composable
fun AlbumHole(
    photo: CoinPhoto?,
    modifier: Modifier = Modifier,
    missing: Boolean = false,
    /** False for a loose coin: the photograph remains, but there is no album board around it. */
    backed: Boolean = true,
    onImageSettled: ((painted: Boolean) -> Unit)? = null,
) {
    val candidates = photo?.candidates.orEmpty()
    var attempt by remember(candidates) { mutableIntStateOf(0) }
    var painted by remember(candidates) { mutableStateOf(false) }
    var settled by remember(candidates) { mutableStateOf(false) }
    val url = candidates.getOrNull(attempt)

    Box(modifier = modifier) {
        // The cardboard ring remains visible around the inset window: the dark upper arc is the
        // cut's inner wall and the pale lower arc is the freshly exposed edge.
        if (backed) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Paper.card)
                drawCircle(
                    color = Paper.ink.copy(alpha = 0.22f),
                    radius = size.minDimension / 2f - 1.dp.toPx(),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5.dp.toPx()),
                )
                drawArc(
                    color = Color.White.copy(alpha = 0.62f),
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (backed) 5.dp else 1.dp)
                .clip(CircleShape)
                .background(Paper.paperDeep),
        ) {
            if (!painted) {
                Silhouette(Modifier.fillMaxSize())
            }
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onState = { state ->
                        when (state) {
                            is AsyncImagePainter.State.Success -> {
                                painted = true
                                if (!settled) {
                                    settled = true
                                    onImageSettled?.invoke(true)
                                }
                            }
                            is AsyncImagePainter.State.Error -> {
                                if (attempt < candidates.lastIndex) {
                                    attempt += 1
                                } else if (!settled) {
                                    settled = true
                                    onImageSettled?.invoke(false)
                                }
                            }
                            else -> Unit
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (missing) 0.14f else 1f),
                )
            }

            Canvas(Modifier.fillMaxSize()) {
                val inset = 3.dp.toPx()
                val arcSize = androidx.compose.ui.geometry.Size(
                    width = size.width - inset * 2,
                    height = size.height - inset * 2,
                )
                drawArc(
                    color = Paper.ink.copy(alpha = 0.28f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = arcSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5.dp.toPx()),
                )
                drawArc(
                    color = Color.White.copy(alpha = 0.52f),
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = arcSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                )
                drawCircle(
                    brush = Brush.linearGradient(
                        0f to Color.Transparent,
                        0.42f to Color.Transparent,
                        0.5f to Color.White.copy(alpha = 0.24f),
                        0.58f to Color.Transparent,
                        1f to Color.Transparent,
                        start = androidx.compose.ui.geometry.Offset(0f, size.height),
                        end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    ),
                )
                if (missing) {
                    drawCircle(
                        color = Paper.ink.copy(alpha = 0.48f),
                        radius = size.minDimension / 2f - 6.dp.toPx(),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(1.dp.toPx(), 4.dp.toPx()),
                            ),
                        ),
                    )
                }
            }
        }
    }
}
