package com.jenarvaezg.coindex.ui.components

import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.domain.Ladders
import com.jenarvaezg.coindex.domain.Referent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every rung of every ladder has a figure. The silhouettes are hand-drawn
 * (`docs/ux/cifras-326.md`), and for a referent without one `ReferentSilhouette` draws nothing.
 */
class SilhouettesTest {
    @Test
    fun `every referent has a drawing`() {
        assertEquals(emptyList(), referentsWithoutDrawing())
    }

    /** In one shared box, the bus's roof would sit at the person's waist. */
    @Test
    fun `a figure keeps its own proportions at a fixed height`() {
        val person = silhouetteWidth(Referent.Person, SILHOUETTE_HEIGHT)
        val bus = silhouetteWidth(Referent.Bus, SILHOUETTE_HEIGHT)

        assertTrue(bus > person, "el autobús no es más ancho que la persona")
        assertTrue(person < SILHOUETTE_HEIGHT, "la persona es más alta que ancha")
    }

    @Test
    fun `the labrador and the shepherd are the same dog`() {
        assertEquals(
            silhouetteWidth(Referent.Labrador, 26.dp),
            silhouetteWidth(Referent.Shepherd, 26.dp),
        )
    }

    @Test
    fun `every drawing stands on some ladder`() {
        val standing = Ladders.all.flatMap { ladder -> ladder.rungs.map { it.referent } }.toSet()

        assertEquals(Referent.entries.toSet(), standing)
    }
}
