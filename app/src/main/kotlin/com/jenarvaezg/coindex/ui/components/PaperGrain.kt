package com.jenarvaezg.coindex.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.theme.Paper
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The fibre of offset paper, calibrated in #351 after the measure showed the approved value was
 * invisible: 4 % of the pixels of an empty region, 12 levels of amplitude, 0.78 of standard
 * deviation. Raising it costs three things the first version did not pay, and this file pays them:
 *
 * - the mosaic no longer repeats exactly, or what a visible grain would show is the tiling;
 * - the tile and the fibre are measured in dp, or the calibrated value dies with the density;
 * - the paper is one surface, applied wherever the sheet is painted rather than on two screens.
 */

/** Side of one mosaic tile. 97.5 dp was the accidental size of the 256 px tile at 420 dpi. */
internal const val GRAIN_TILE_DP = 96f

/** Fibres per tile. The tile is measured in dp, so this is a density per area and not per pixel. */
internal const val GRAIN_FIBRES = 2600

/** Tiles a side of the baked mosaic. Sixteen tiles that differ: the period is 384 dp, a screen. */
internal const val GRAIN_TILES_PER_SIDE = 4

private const val GRAIN_FIBRE_WIDTH_DP = 0.5f

/** Radians of slant a fibre may take either way: enough to break the banding of a single angle. */
internal const val GRAIN_SLANT_SPREAD = 0.9f

/** Soft-light strength of the fibre, calibrated at 1:1 on the AVD (#351). */
internal const val GRAIN_OPACITY = 0.75f

/** One fibre, in tile-local pixels: it starts inside its tile and may run past its edge. */
internal data class GrainFibre(
    val x: Float,
    val y: Float,
    val length: Float,
    /** Radians off the horizontal. Paper has a grain direction; fibres are not all parallel. */
    val slant: Float,
    val strength: Float,
    val light: Boolean,
)

/**
 * The fibres of one tile of the mosaic. Deterministic — the same tile is always drawn the same
 * way — but seeded with the tile's own coordinates, so no two neighbouring tiles are the same.
 */
internal fun grainFibres(
    tileX: Int,
    tileY: Int,
    tileSize: Float,
    count: Int = GRAIN_FIBRES,
): List<GrainFibre> {
    val rolls = GrainRolls(tileSeed(tileX, tileY))
    return List(count) {
        GrainFibre(
            x = rolls.unit() * tileSize,
            y = rolls.unit() * tileSize,
            length = tileSize * (0.012f + rolls.unit() * 0.07f),
            slant = (rolls.unit() - 0.5f) * GRAIN_SLANT_SPREAD,
            strength = 0.25f + rolls.unit() * 0.5f,
            light = rolls.next() and 1 == 0,
        )
    }
}

/** Paints the sheet: the paper's tone with its fibre, as one surface rather than an overlay. */
@Composable
fun Modifier.paperSurface(opacity: Float = GRAIN_OPACITY): Modifier {
    val density = LocalDensity.current
    val atlas = remember(density.density, opacity) { grainAtlas(density, opacity) }
    // The mosaic is anchored to the window and not to each surface, so the index, the coins and
    // the plate are one sheet: without this every screen would start its own tiling and the seam
    // between two of them would be a visible step in the grain.
    var origin by remember { mutableStateOf(Offset.Zero) }
    return this
        .onGloballyPositioned { origin = it.positionInWindow() }
        .drawBehind { drawPaperGrain(atlas, origin) }
}

internal class GrainAtlas(val brush: ShaderBrush, val periodPx: Int)

private fun DrawScope.drawPaperGrain(atlas: GrainAtlas, origin: Offset) {
    val period = atlas.periodPx.toFloat()
    // One rectangle with a repeating shader, and the paper's tone baked into it rather than filled
    // underneath: an earlier attempt blitted some fifty transformed tiles per frame per surface and
    // drew worse than the effect it replaced.
    clipRect {
        translate(-origin.x.mod(period), -origin.y.mod(period)) {
            drawRect(
                brush = atlas.brush,
                size = Size(size.width + period, size.height + period),
            )
        }
    }
}

private data class GrainKey(val density: Float, val opacity: Float)

/**
 * Read and written only from composition, which is the main thread. Room for more than one entry
 * because an export composes at its own density: with a single slot, every exported sheet evicted
 * the screen's mosaic and both were baked again on the way back.
 */
private val cachedAtlases = LinkedHashMap<GrainKey, GrainAtlas>()

private const val GRAIN_ATLASES_KEPT = 3

/**
 * The atlas is baked once per density and opacity, and not per frame: the first version drew some
 * nine thousand soft-light lines inside an offscreen layer on every frame, which is what made
 * raising the opacity expensive rather than free (#351).
 */
private fun grainAtlas(density: Density, opacity: Float): GrainAtlas {
    val key = GrainKey(density.density, opacity)
    cachedAtlases[key]?.let { return it }
    val atlas = bakeGrainAtlas(density, opacity)
    cachedAtlases[key] = atlas
    while (cachedAtlases.size > GRAIN_ATLASES_KEPT) {
        cachedAtlases.remove(cachedAtlases.keys.first())
    }
    return atlas
}

private fun bakeGrainAtlas(density: Density, opacity: Float): GrainAtlas {
    val tile = with(density) { GRAIN_TILE_DP.dp.toPx() }.roundToInt().coerceAtLeast(16)
    val period = tile * GRAIN_TILES_PER_SIDE
    val image = ImageBitmap(period, period)
    val size = Size(period.toFloat(), period.toFloat())
    CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), size) {
        // The paper is baked in with the fibre: soft light needs something underneath it, and a
        // mosaic that already carries its own tone is one opaque image instead of a blended layer.
        drawRect(Paper.paper)
        val fibreWidth = GRAIN_FIBRE_WIDTH_DP.dp.toPx()
        repeat(GRAIN_TILES_PER_SIDE) { tileY ->
            repeat(GRAIN_TILES_PER_SIDE) { tileX ->
                val left = (tileX * tile).toFloat()
                val top = (tileY * tile).toFloat()
                clipRect(left, top, left + tile, top + tile) {
                    grainFibres(tileX, tileY, tile.toFloat()).forEach { fibre ->
                        val colour = (if (fibre.light) Color.White else Paper.ink)
                            .copy(alpha = opacity * fibre.strength)
                        val runX = fibre.length * cos(fibre.slant)
                        val runY = fibre.length * sin(fibre.slant)
                        // A fibre that runs past its tile is drawn again a tile back, so it comes
                        // in on the other side and tiles meet without a bald seam between them.
                        val wrapX = if (fibre.x + runX > tile) -tile.toFloat() else 0f
                        val wrapY = when {
                            fibre.y + runY + fibreWidth > tile -> -tile.toFloat()
                            fibre.y + runY < 0f -> tile.toFloat()
                            else -> 0f
                        }
                        wraps(wrapX, wrapY).forEach { (offsetX, offsetY) ->
                            val x = left + fibre.x + offsetX
                            val y = top + fibre.y + offsetY
                            drawLine(
                                color = colour,
                                start = Offset(x, y),
                                end = Offset(x + runX, y + runY),
                                strokeWidth = fibreWidth,
                                blendMode = BlendMode.Softlight,
                            )
                        }
                    }
                }
            }
        }
    }
    return GrainAtlas(
        brush = ShaderBrush(ImageShader(image, TileMode.Repeated, TileMode.Repeated)),
        periodPx = period,
    )
}

private fun wraps(x: Float, y: Float): List<Pair<Float, Float>> = when {
    x == 0f && y == 0f -> listOf(0f to 0f)
    x == 0f -> listOf(0f to 0f, 0f to y)
    y == 0f -> listOf(0f to 0f, x to 0f)
    else -> listOf(0f to 0f, x to 0f, 0f to y, x to y)
}

private fun tileSeed(tileX: Int, tileY: Int): Int {
    var seed = tileX * 0x27D4EB2D + tileY * 0x165667B1 + 0x9E3779B1.toInt()
    seed = seed xor (seed ushr 15)
    seed *= 0x2545F491
    seed = seed xor (seed ushr 13)
    return seed
}

/** Deterministic rolls for one tile: an xorshift, so the fibres never need an array of hashes. */
private class GrainRolls(seed: Int) {
    private var state = if (seed == 0) 0x9E3779B9.toInt() else seed

    fun next(): Int {
        state = state xor (state shl 13)
        state = state xor (state ushr 17)
        state = state xor (state shl 5)
        return state
    }

    fun unit(): Float = (next() ushr 8).toFloat() / (1 shl 24).toFloat()
}
