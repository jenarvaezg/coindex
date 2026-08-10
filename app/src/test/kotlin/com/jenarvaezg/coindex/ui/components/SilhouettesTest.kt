package com.jenarvaezg.coindex.ui.components

import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.domain.Ladders
import com.jenarvaezg.coindex.domain.Referent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every rung of every ladder has a figure standing on it.
 *
 * The silhouettes are hand-made and are part of the identity — «no son un asset que se pueda descargar»
 * (`docs/ux/cifras-326.md`) — and a referent added without its drawing is invisible from the code:
 * `ReferentSilhouette` simply draws nothing, and the ladder gets a hole with a label under it.
 */
class SilhouettesTest {
    @Test
    fun `every referent has a drawing`() {
        assertEquals(emptyList(), referentsWithoutDrawing())
    }

    /**
     * The height is fixed and the width follows the drawing's own proportions.
     *
     * A bus and a person given the same box would put the bus's roof at the person's waist, which is what
     * standing them on one rule is for.
     */
    @Test
    fun `a figure keeps its own proportions at a fixed height`() {
        val person = silhouetteWidth(Referent.Person, SILHOUETTE_HEIGHT)
        val bus = silhouetteWidth(Referent.Bus, SILHOUETTE_HEIGHT)

        assertTrue(bus > person, "el autobús no es más ancho que la persona")
        assertTrue(person < SILHOUETTE_HEIGHT, "la persona es más alta que ancha")
    }

    /** Two rungs share one drawing, which is why the prototype counted fourteen figures for fifteen rungs. */
    @Test
    fun `the labrador and the shepherd are the same dog`() {
        assertEquals(
            silhouetteWidth(Referent.Labrador, 26.dp),
            silhouetteWidth(Referent.Shepherd, 26.dp),
        )
    }

    /** And every drawing stands on a rung: one nobody uses is a drawing nobody sees. */
    @Test
    fun `every drawing stands on some ladder`() {
        val standing = Ladders.all.flatMap { ladder -> ladder.rungs.map { it.referent } }.toSet()

        assertEquals(Referent.entries.toSet(), standing)
    }
}
