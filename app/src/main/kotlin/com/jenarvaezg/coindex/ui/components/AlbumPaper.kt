package com.jenarvaezg.coindex.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.ui.TURN_THE_COIN_OVER
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * How long the coin takes to come round, decided on an HTML prototype at phone size (#302) and
 * confirmed on the AVD before this was written: half a turn is about twenty-four distinct frames
 * there, which is a turn and not a cut.
 */
private const val COIN_TURN_MILLIS = 420

private const val HALF_TURN = 180f

/** Shallow enough that the near edge of the coin grows as it swings, as a real one does. */
private const val COIN_CAMERA_DISTANCE = 12f

/** One catalog face sunk into cardboard, with a fixed acetate reflection over it. */
@Composable
fun AlbumHole(
    photo: CoinPhoto?,
    modifier: Modifier = Modifier,
    missing: Boolean = false,
    /** False for a loose coin: the photograph remains, but there is no album board around it. */
    backed: Boolean = true,
    otherSide: CoinPhoto? = null,
    onImageSettled: ((painted: Boolean) -> Unit)? = null,
) {
    AlbumHole(
        photo = photo,
        modifier = modifier,
        missing = missing,
        backed = backed,
        otherSide = otherSide,
        tone = AlbumToneConfig.Default,
        onImageSettled = onImageSettled,
    )
}

/** Configurable seam used by the calibration bench while preserving the production API. */
@Composable
fun AlbumHole(
    photo: CoinPhoto?,
    modifier: Modifier = Modifier,
    missing: Boolean = false,
    /** False for a loose coin: the photograph remains, but there is no album board around it. */
    backed: Boolean = true,
    /**
     * The face this coin is not showing. When it is here the body of the hole takes a tap and the
     * coin turns over inside it (#337); when it is not, the hole is the still picture it always
     * was — and the sheet that composes itself off screen never gets one, so an exported PNG
     * cannot inherit a turned coin.
     */
    otherSide: CoinPhoto? = null,
    tone: AlbumToneConfig,
    onImageSettled: ((painted: Boolean) -> Unit)? = null,
) {
    // Which face is up is the hole's own business and it is deliberately not hoisted: the plate
    // goes back to `printed_side` on its own the moment the cell leaves the lazy grid, which is
    // what keeps the sheet of mixed states from becoming a state the album has to remember.
    var turned by remember(photo, otherSide) { mutableStateOf(false) }
    val turn by animateFloatAsState(
        targetValue = if (turned) HALF_TURN else 0f,
        animationSpec = tween(durationMillis = COIN_TURN_MILLIS),
        label = "coin turn",
    )
    // Past the quarter turn the far face comes round, and it is drawn from its own zero rather
    // than from the mirror of the near one: a photograph seen through its own back would be a
    // coin with its legend written backwards.
    val showsFront = turn <= HALF_TURN / 2
    val faceTurn = if (showsFront) turn else turn - HALF_TURN
    val face = if (showsFront) photo else otherSide

    val candidates = face?.candidates.orEmpty()
    var attempt by remember(candidates) { mutableIntStateOf(0) }
    var painted by remember(candidates) { mutableStateOf(false) }
    var settled by remember(candidates) { mutableStateOf(false) }
    val url = candidates.getOrNull(attempt)

    Box(
        modifier = modifier.then(
            if (otherSide != null) {
                Modifier.clickable(
                    role = Role.Button,
                    onClickLabel = TURN_THE_COIN_OVER,
                    indication = null,
                    interactionSource = null,
                ) { turned = !turned }
            } else {
                Modifier
            },
        ),
    ) {
        // The cardboard ring remains visible around the inset window: the wall of the cut is dark
        // at the top, where it shades itself, and pale at the bottom, where the die exposed a
        // fresh edge. It is one sweep and not two half arcs, and it stops at the photograph.
        if (backed) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Paper.card.copy(alpha = tone.cardAlpha))
                val wallWidth = tone.dieWall.widthDp.dp.toPx()
                drawCircle(
                    brush = Brush.sweepGradient(*tone.dieWall.stops(), center = center),
                    radius = size.minDimension / 2f - wallWidth / 2f,
                    style = Stroke(width = wallWidth),
                )
                // A different job from the wall: this is the rule that separates cardboard from
                // paper, and 1 dp of it at 3:1 is what #349 won. The wall does not have to be dark
                // — or opaque — for the cardboard to read.
                val hairlineWidth = 1.dp.toPx()
                drawCircle(
                    color = tone.hairlineColor,
                    radius = size.minDimension / 2f - hairlineWidth / 2f,
                    style = Stroke(width = hairlineWidth),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (backed) HOLE_CARD_PADDING_DP.dp else 1.dp)
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
                        // The coin turns; the casilla does not. Only the photograph goes into the
                        // layer, so the cardboard around it and the acetate's reflection over it
                        // stay exactly where they are while the metal comes round.
                        .graphicsLayer {
                            rotationY = faceTurn
                            cameraDistance = COIN_CAMERA_DISTANCE * density
                        }
                        .alpha(if (missing) 0.14f else 1f),
                )
            }

            // The die's shadow used to be painted here too, eight dp inside the edge and therefore
            // squarely on the coin's face. It is gone: the photograph brings its own light baked in
            // from the upper left (#303), and a second model of light drawn on top of the first
            // contradicted it — which is what read as a mark on the metal. The wall of the cut now
            // lives on the cardboard, where the shadow of a cut wall belongs.
            //
            // What stays is the acetate's fixed reflection. Whether it survives is not this
            // ticket's call: #337 freezes it and #338 would replace it with the gloss of variant H,
            // and both together rebuild #303's discarded variant D.
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.linearGradient(
                        0f to Color.Transparent,
                        0.42f to Color.Transparent,
                        0.5f to Color.White.copy(alpha = 0.24f),
                        0.58f to Color.Transparent,
                        1f to Color.Transparent,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, 0f),
                    ),
                )
                if (missing) {
                    drawCircle(
                        color = Paper.ink.copy(alpha = 0.48f),
                        radius = size.minDimension / 2f - 6.dp.toPx(),
                        style = Stroke(
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
