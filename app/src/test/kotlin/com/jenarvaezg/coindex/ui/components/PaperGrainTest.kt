package com.jenarvaezg.coindex.ui.components

import com.jenarvaezg.coindex.ui.theme.Paper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private const val TILE = 252f

class PaperGrainTest {
    @Test
    fun `the same tile is drawn the same way twice`() {
        assertEquals(grainFibres(3, 5, TILE), grainFibres(3, 5, TILE))
    }

    @Test
    fun `neighbouring tiles do not repeat the mosaic`() {
        val tile = grainFibres(0, 0, TILE)

        // #351: identical tiles would make the tiling itself visible.
        assertNotEquals(tile, grainFibres(1, 0, TILE))
        assertNotEquals(tile, grainFibres(0, 1, TILE))
        assertNotEquals(grainFibres(1, 0, TILE), grainFibres(0, 1, TILE))
    }

    @Test
    fun `the baked mosaic is a grid of tiles that all differ`() {
        val tiles = (0 until GRAIN_TILES_PER_SIDE).flatMap { y ->
            (0 until GRAIN_TILES_PER_SIDE).map { x -> grainFibres(x, y, TILE) }
        }

        assertEquals(GRAIN_TILES_PER_SIDE * GRAIN_TILES_PER_SIDE, tiles.size)
        assertEquals(tiles.size, tiles.distinct().size)
    }

    @Test
    fun `every fibre starts inside its tile so tiles can be baked as one image`() {
        val side = 0 until GRAIN_TILES_PER_SIDE
        val fibres = side.flatMap { y -> side.flatMap { x -> grainFibres(x, y, TILE) } }

        assertEquals(GRAIN_FIBRES * GRAIN_TILES_PER_SIDE * GRAIN_TILES_PER_SIDE, fibres.size)
        assertTrue(fibres.all { it.x in 0f..TILE && it.y in 0f..TILE })
        assertTrue(fibres.all { it.length > 0f })
    }

    @Test
    fun `fibre length scales with the tile so the grain keeps its size in dp`() {
        // #351: measured in raw pixels, the calibrated grain changed size with screen density.
        val small = grainFibres(2, 2, 100f)
        val large = grainFibres(2, 2, 200f)

        assertEquals(small.size, large.size)
        small.zip(large).forEach { (a, b) ->
            assertEquals(a.x * 2f, b.x, 0.001f)
            assertEquals(a.y * 2f, b.y, 0.001f)
            assertEquals(a.length * 2f, b.length, 0.001f)
        }
    }

    @Test
    fun `fibres slant both ways, so the grain does not band in one direction`() {
        val slants = grainFibres(1, 1, TILE).map { it.slant }

        assertTrue(slants.any { it > 0.1f } && slants.any { it < -0.1f })
        assertTrue(slants.all { it in -GRAIN_SLANT_SPREAD / 2f..GRAIN_SLANT_SPREAD / 2f })
    }

    @Test
    fun `the sheet is opaque, which is what lets one destination cover another`() {
        // #381: both ends of a navigation paint this paper while the coin travels, so any
        // transparency shows one through the other. The grain is baked on top of this rect.
        assertEquals(1f, Paper.paper.alpha)
    }

    @Test
    fun `both tones of fibre appear, and none is drawn at full strength`() {
        val fibres = grainFibres(0, 0, TILE)

        assertTrue(fibres.any { it.light } && fibres.any { !it.light })
        assertTrue(fibres.all { it.strength in 0.25f..1f })
    }
}
