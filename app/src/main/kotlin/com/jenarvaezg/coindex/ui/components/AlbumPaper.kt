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

/** Half a turn of the coin (#302): about 24 frames, enough to read as a turn rather than a cut. */
private const val COIN_TURN_MILLIS = 420

/**
 * The turn's name in the Compose animation inspector, not copy. A constant so that
 * `CopyLivesInOnePlaceTest`, which treats `label =` as visible text, needs no exemption.
 */
private const val COIN_TURN_ANIMATION = "coin turn"

private const val HALF_TURN = 180f

/** Shallow enough that the near edge of the coin grows as it swings, as a real one does. */
private const val COIN_CAMERA_DISTANCE = 12f

/**
 * The smallest hole that draws [HoleAbsence.Missing] as the design sunk to 14 % (#556). Below it
 * the faint design reads as a grey disc, so the hole shows the whole coin under its dotted rule
 * instead. Calibrated at the bench of ADR 0026 §15; captures in `docs/ux/implementacion-556/`.
 */
const val GHOST_MIN_DP = 72f

/**
 * What a hole says about the coin that is not in it (ADR 0026 §15, #520). [Missing] («te falta») is
 * a casilla of a plate being filled, its design sunk to 14 % like an album's empty pocket; [Wanted]
 * («esto lo buscas») is a marked casilla, drawn whole so the coin can be recognised at a fair. Both
 * keep the dotted rule and never gloss. [Missing] falls back to the whole coin below [GHOST_MIN_DP]
 * (#556).
 */
enum class HoleAbsence {
    /** The coin is in the collection: full colour, and the metal's own light over it. */
    Filled,

    /** An empty casilla of a plate being filled: the catalog design at 14 % under a dotted rule. */
    Missing,

    /** An empty casilla the collector marked: the coin whole under a dotted rule. */
    Wanted,
}

/** One catalog face sunk into cardboard, with the metal's own light over it. */
@Composable
fun AlbumHole(
    photo: CoinPhoto?,
    modifier: Modifier = Modifier,
    absence: HoleAbsence = HoleAbsence.Filled,
    /** False for a loose coin: the photograph remains, but there is no album board around it. */
    backed: Boolean = true,
    otherSide: CoinPhoto? = null,
    onImageSettled: ((painted: Boolean) -> Unit)? = null,
) {
    AlbumHole(
        photo = photo,
        modifier = modifier,
        absence = absence,
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
    absence: HoleAbsence = HoleAbsence.Filled,
    /** False for a loose coin: the photograph remains, but there is no album board around it. */
    backed: Boolean = true,
    /**
     * The face this coin is not showing. When present, a tap turns the coin over inside the hole
     * (#337). The off-screen sheet never passes one, so an exported PNG can't show a turned coin.
     */
    otherSide: CoinPhoto? = null,
    tone: AlbumToneConfig,
    onImageSettled: ((painted: Boolean) -> Unit)? = null,
) {
    // Both absences get the dotted rule and no gloss; only [HoleAbsence.Missing] dims (`dimmed`).
    val empty = absence != HoleAbsence.Filled
    // Deliberately not hoisted: a cell that leaves the lazy grid returns to `printed_side`.
    var turned by remember(photo, otherSide) { mutableStateOf(false) }
    // Scaled by the system (#514, #337): at `animator_duration_scale 0` the coin just switches
    // face.
    val turn by animateFloatAsState(
        targetValue = if (turned) HALF_TURN else 0f,
        animationSpec = tween(durationMillis = COIN_TURN_MILLIS),
        label = COIN_TURN_ANIMATION,
    )
    // Past the quarter turn the far face is drawn from its own zero, not mirrored, so its legend
    // doesn't read backwards.
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
    // Scale the ring with the hole so small holes keep the 104 dp card's cardboard/coin ratio
    // (#357) instead of swallowing the metal.
    val ringDp = if (!backed || sidePx == 0) {
        HOLE_CARD_PADDING_DP
    } else {
        val holeDp = sidePx / density.density
        holeCardPaddingDp(holeDp) * (tone.dieWall.widthDp / HOLE_CARD_PADDING_DP)
    }
    // Only above [GHOST_MIN_DP] (#556). Before the first measure it starts dimmed, so a missing
    // coin never flashes at full colour.
    val dimmed = absence == HoleAbsence.Missing &&
        (sidePx == 0 || sidePx / density.density >= GHOST_MIN_DP)

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
        // The cardboard ring around the window: the cut wall is dark at the top and pale at the
        // bottom, one sweep that stops at the photograph.
        if (backed) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Paper.card.copy(alpha = tone.cardAlpha))
                val wallWidth = ringDp.dp.toPx()
                val wallRadius = size.minDimension / 2f - wallWidth / 2f
                // The wall's light flips with the face shown (#509), cross-faded on the turn's
                // progress.
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
                // The rule between cardboard and paper: 1 dp at 3:1 contrast (#349).
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
            val silence = holeSilence(candidates.size, settled = settled, painted = painted)
            if (!painted) {
                Silhouette(Modifier.fillMaxSize())
            }
            // A turned-to face with no photograph says so once the turn ends (#509), instead of
            // showing a mute disc.
            val turnedToNothing = !showsFront && turn >= HALF_TURN &&
                (silence == HoleSilence.NoPhotograph || silence == HoleSilence.NotOnThisPhone)
            if (turnedToNothing) {
                FaceNotDownloaded(Modifier.fillMaxSize())
            }
            // A face at rest whose photograph failed to download gets a mark (#510). The plain disc
            // still means «loading» or «Numista has no picture». The mark doesn't turn, so it waits
            // for the turn to end.
            val turnIsOver = turn <= 0f || turn >= HALF_TURN
            if (silence == HoleSilence.NotOnThisPhone && turnIsOver && !turnedToNothing) {
                PhotoNotDownloaded(Modifier.fillMaxSize())
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
                        // Only the photograph turns; the cardboard stays put, and the gloss turns
                        // with its face (#338).
                        .graphicsLayer {
                            rotationY = faceTurn
                            cameraDistance = COIN_CAMERA_DISTANCE * density.density
                        }
                        .alpha(if (dimmed) 0.14f else 1f)
                        // No gloss until a photograph is painted, or it lights the stand-in disc
                        // and keeps the accelerometer registered (#510).
                        .coinGloss(isCoin = !empty && painted),
                )
            }

            // Nothing but the gloss goes over the photograph: the die shadow (#357) and the
            // acetate reflection (#338) were removed on purpose.
            if (empty) {
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
