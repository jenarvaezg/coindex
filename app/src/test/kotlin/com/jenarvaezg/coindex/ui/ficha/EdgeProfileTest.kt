package com.jenarvaezg.coindex.ui.ficha

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The edge in profile is drawn from Numista's description of it (#643). */
class EdgeProfileTest {
    /** The four forms, over descriptions the father's fichas actually carry. */
    @Test
    fun `the description picks the drawing`() {
        assertEquals(EdgePattern.Reeded, profile("Reed").pattern)
        assertEquals(EdgePattern.Reeded, profile("Fresado").pattern)
        assertEquals(EdgePattern.Reeded, profile("Ranuras finas.").pattern)
        assertEquals(EdgePattern.Reeded, profile("Dentado").pattern)
        assertEquals(EdgePattern.Reeded, profile("Carrizo con 201 cañas").pattern)
        assertEquals(EdgePattern.Plain, profile("Llano").pattern)
        assertEquals(EdgePattern.Plain, profile("Liso con inscripción").pattern)
        assertEquals(EdgePattern.Ornamented, profile("Con motivos en relieve (rectángulo y redondo)").pattern)
        assertEquals(EdgePattern.Ornamented, profile("Diseños en relieve").pattern)
    }

    /** Raised letters are letters, not ornament; and only the first sentence describes the edge. */
    @Test
    fun `letters in relief and a later sentence don't make an ornament`() {
        assertEquals(EdgePattern.Plain, profile("Letras en relieve.").pattern)
        assertEquals(
            EdgePattern.Plain,
            profile(
                "Liso con inscripción. Fin y principio de la leyenda separados por un diseño floral.",
            ).pattern,
        )
        assertEquals(
            EdgePattern.Reeded,
            profile("Reed.\r\n\r\nBorde de seguridad. La ubicación del detalle varía.").pattern,
        )
    }

    @Test
    fun `the national police two euros is reeded with its legend printed inside`() {
        val edge = edgeProfile(
            description = "Letras sobre lengüeta. La secuencia \"2 ★★★\" repetida seis veces.",
            lettering = "2 ★ ★ ↊ ★ ★ 2 ★ ★ ↊ ★ ★ 2 ★ ★ ↊ ★ ★",
            thicknessMillimetres = 2.2,
            diameterMillimetres = 25.75,
        )!!

        assertEquals(EdgePattern.Reeded, edge.pattern)
        assertEquals("2 ★ ★ ↊ ★ ★ 2 ★ ★ ↊ ★ ★ 2 ★ ★ ↊ ★ ★", edge.lettering)
    }

    /** With the thickness the strip is drawn to the photograph's scale; without it, nominal. */
    @Test
    fun `the thickness sets the strip's proportion, and its absence says so`() {
        val unicorn = edgeProfile("Reed", null, thicknessMillimetres = 6.0, diameterMillimetres = 38.61)!!
        assertEquals(6.0 / 38.61, unicorn.heightOverWidth)

        val fuerte = edgeProfile("Reed", null, thicknessMillimetres = null, diameterMillimetres = 37.2)!!
        assertNull(fuerte.heightOverWidth)
        assertNull(edgeProfile("Reed", null, 2.5, diameterMillimetres = null)!!.heightOverWidth)
    }

    @Test
    fun `no description and no legend draw no edge`() {
        assertNull(edgeProfile(null, null, 2.0, 30.0))
        assertNull(edgeProfile("  ", null, 2.0, 30.0))
        assertEquals(EdgePattern.Plain, edgeProfile(null, "DEUS ET PATRIA", null, 30.0)?.pattern)
    }

    /** Neither Bitter nor Barlow has `↊` or `↋`, so the digit is drawn turned instead. */
    @Test
    fun `a turned digit is drawn as its digit turned over`() {
        assertEquals(
            listOf(
                LetteringRun("2 ★ ★ "),
                LetteringRun("2", turned = true),
                LetteringRun(" ★ ★ "),
                LetteringRun("3", turned = true),
            ),
            letteringRuns("2 ★ ★ ↊ ★ ★ ↋"),
        )
        assertEquals(listOf(LetteringRun("2 EURO")), letteringRuns("2 EURO"))
        assertEquals(emptyList(), letteringRuns(""))
    }

    private fun profile(description: String): EdgeProfile =
        edgeProfile(description, lettering = null, thicknessMillimetres = null, diameterMillimetres = 30.0)!!
}
