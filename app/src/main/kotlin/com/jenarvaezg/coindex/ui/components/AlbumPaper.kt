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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.ui.TURN_THE_COIN_OVER
import com.jenarvaezg.coindex.ui.theme.Paper
import kotlin.math.min

/**
 * How long the coin takes to come round, decided on an HTML prototype at phone size (#302) and
 * confirmed on the AVD before this was written: half a turn is about twenty-four distinct frames
 * there, which is a turn and not a cut.
 */
private const val COIN_TURN_MILLIS = 420

/**
 * What the turn is called in the Compose animation inspector, and **not copy**.
 *
 * No collector ever reads it: it is a name for a tool. It is a constant rather than a literal
 * because `label =` is a visible slot as far as `CopyLivesInOnePlaceTest` can tell, and the way to
 * keep that test free of exemptions is for the one string that is genuinely not copy to say so here
 * instead of asking for a place on a whitelist.
 */
private const val COIN_TURN_ANIMATION = "coin turn"

private const val HALF_TURN = 180f

/** Shallow enough that the near edge of the coin grows as it swings, as a real one does. */
private const val COIN_CAMERA_DISTANCE = 12f

/** One catalog face sunk into cardboard, with the metal's own light over it. */
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
        label = COIN_TURN_ANIMATION,
    )
    // Past the quarter turn the far face comes round, and it is drawn from its own zero rather
    // than from the mirror of the near one: a photograph seen through its own back would be a
    // coin with its legend written backwards.
    val showsFront = turn <= HALF_TURN / 2
    val faceTurn = if (showsFront) turn else turn - HALF_TURN
    val face = if (showsFront) photo else otherSide

    val density = LocalDensity.current
    val candidates = face?.candidates.orEmpty()
    var attempt by remember(candidates) { mutableIntStateOf(0) }
    var painted by remember(candidates) { mutableStateOf(false) }
    var settled by remember(candidates) { mutableStateOf(false) }
    var sidePx by remember { mutableIntStateOf(0) }
    val url = candidates.getOrNull(attempt)
    // 5 dp was measured on the 104 dp card (#357). Axis holes are 34 dp; scale the ring so the
    // cardboard/coin ratio matches entrada-default instead of swallowing the metal.
    val ringDp = if (!backed || sidePx == 0) {
        HOLE_CARD_PADDING_DP
    } else {
        val holeDp = sidePx / density.density
        holeCardPaddingDp(holeDp) * (tone.dieWall.widthDp / HOLE_CARD_PADDING_DP)
    }

    Box(
        modifier = modifier
            .onSizeChanged { sidePx = min(it.width, it.height) }
            .then(
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
                val wallWidth = ringDp.dp.toPx()
                val wallRadius = size.minDimension / 2f - wallWidth / 2f
                // The hole declares which face it is showing with its own cardboard (#509): as the
                // coin comes round, the light of the cut crosses to the other side and turns up.
                // The two profiles are cross-faded on the turn's own progress, so the wall arrives
                // exactly when the face does and nothing about the board moves.
                val overTurn = turn / HALF_TURN
                drawCircle(
                    brush = Brush.sweepGradient(*tone.dieWall.stops(), center = center),
                    radius = wallRadius,
                    style = Stroke(width = wallWidth),
                    alpha = 1f - overTurn,
                )
                drawCircle(
                    brush = Brush.sweepGradient(*tone.dieWall.turnedStops(), center = center),
                    radius = wallRadius,
                    style = Stroke(width = wallWidth),
                    alpha = overTurn,
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
                .padding(if (backed) ringDp.dp else 1.dp)
                .clip(CircleShape)
                .background(Paper.paperDeep),
        ) {
            if (!painted) {
                Silhouette(Modifier.fillMaxSize())
            }
            // A face that came round and never arrived says so, instead of leaving the mute disc
            // the audit of 14 August 2026 found (#509). It waits for the turn to finish — mid-turn
            // the photograph is simply still loading — and it is only ever the far face: the one at
            // rest has the die-cut and the ghost to say what it is.
            if (!showsFront && turn >= HALF_TURN && (candidates.isEmpty() || (settled && !painted))) {
                FaceNotDownloaded(Modifier.fillMaxSize())
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
                        // layer, so the cardboard around it stays exactly where it is while the
                        // metal comes round — and the gloss, which is inside the same layer, turns
                        // with the face it belongs to (#338).
                        .graphicsLayer {
                            rotationY = faceTurn
                            cameraDistance = COIN_CAMERA_DISTANCE * density.density
                        }
                        .alpha(if (missing) 0.14f else 1f)
                        .coinGloss(isCoin = !missing),
                )
            }

            // Nothing else is painted over the photograph any more. The die's shadow went in #357 —
            // eight dp inside the edge and therefore squarely on the coin's face — and the acetate's
            // fixed reflection went here, in #338: leaving it under the gloss would have rebuilt
            // #303's discarded variant D, two layers for the result of one. The wall of the cut
            // lives on the cardboard, where the shadow of a cut wall belongs, and what is over the
            // metal is the metal's own light.
            if (missing) {
                Canvas(Modifier.fillMaxSize()) {
                    val holeDp = size.minDimension / density.density
                    val dashInset = (6f * holeDp / DESIGN_HOLE_DP).coerceAtLeast(1.5f).dp.toPx()
                    drawCircle(
                        color = Paper.ink.copy(alpha = 0.48f),
                        radius = size.minDimension / 2f - dashInset,
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
