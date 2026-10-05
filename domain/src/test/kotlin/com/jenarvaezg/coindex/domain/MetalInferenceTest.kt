package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Cada caso es un `composition.text` real de las fichas sembradas, no un ejemplo inventado. Numista
 * no tiene campo de metal, sólo prosa (ADR 0005 para el acabado, ADR 0018 para el metal).
 */
class MetalInferenceTest {
    @Test
    fun `las ocho formas de escribir plata son plata`() {
        // La forma más común: la ley escrita de cuatro maneras y el desglose en paréntesis.
        listOf(
            "Plata",
            "Plata 999",
            "Plata 999,9",
            "Plata 925 (92.5% silver, 7.5% copper)",
            "Plata 800 (.800 silver .200 copper)",
            "Plata 835 (Copper .165)",
            "Plata 900 (.100 copper)",
            """Plata 999,9 (Marked &quot;PLATA 1000&quot;)""",
        ).forEach { text ->
            assertEquals(Metal.Silver, inferMetal(text), text)
        }
    }

    /** El vellón es una plata de ley baja y el coleccionista lo llama plata (#40). */
    @Test
    fun `el vellon es plata aunque el cobre pese mas`() {
        assertEquals(
            Metal.Silver,
            inferMetal("Vellón (plata 100) (Copper .700, Nickel .100, Zinc .100)"),
        )
        assertEquals(
            Metal.Silver,
            inferMetal("Vellón (plata 400) (Copper .500, Nickel .050, Zinc .050)"),
        )
    }

    /** El Koala de 2016: «Plata 999 (highlighted in 24-carat gold)» es plata con un dorado. */
    @Test
    fun `lo que hay entre parentesis no decide el metal`() {
        assertEquals(Metal.Silver, inferMetal("Plata 999 (highlighted in 24-carat gold)"))
    }

    @Test
    fun `las aleaciones con nombre propio no se leen como sus componentes`() {
        assertEquals(Metal.Cupronickel, inferMetal("Cuproníquel"))
        assertEquals(Metal.Bronze, inferMetal("Bronce"))
        assertEquals(Metal.Bronze, inferMetal("Bronce de aluminio"))
        assertEquals(Metal.Brass, inferMetal("Latón de níquel"))
    }

    /** [Metal.Other] significa sin metal dominante, no desconocido (#40). */
    @Test
    fun `la bimetalica y el nucleo recubierto no tienen metal dominante`() {
        assertEquals(
            Metal.Other,
            inferMetal(
                "Bimetálica: centro de níquel recubierto de latón de níquel y anillo de " +
                    "cuproníquel (Core: 75% Cu, 20% Zn, 5% Ni Ring: 75% Cu, 25% Ni)",
            ),
        )
        assertEquals(
            Metal.Other,
            inferMetal("Cobre recubierto de cuproníquel (91.67% Copper, 8.33% Nickel)"),
        )
    }

    @Test
    fun `lo que no se reconoce es nulo y no Other`() {
        assertNull(inferMetal(null))
        assertNull(inferMetal(""))
        assertNull(inferMetal("   "))
        assertNull(inferMetal("Aleación desconocida"))
        // Sólo hay paréntesis: la cabeza está vacía y no hay nada que leer.
        assertNull(inferMetal("(Copper .700)"))
    }

    /** Se pide `lang=es`, pero los paréntesis llegan en inglés a diario. */
    @Test
    fun `el ingles se lee igual que el castellano`() {
        assertEquals(Metal.Silver, inferMetal("Silver .900"))
        assertEquals(Metal.Gold, inferMetal("Gold 999.9"))
        assertEquals(Metal.Cupronickel, inferMetal("Copper-nickel"))
        assertEquals(Metal.Aluminium, inferMetal("Aluminum"))
        // «Nickel silver» no lleva plata: es una aleación de cobre y la palabra no puede llegar
        // a la regla de la plata.
        assertEquals(Metal.Copper, inferMetal("Nickel silver (Copper, Nickel, Zinc)"))
    }

    /** Los códigos de la clave y de la ruta, incluido el caso ausente. */
    @Test
    fun `los codigos de metal van y vuelven`() {
        Metal.entries.forEach { metal ->
            assertEquals(metal, metalFromCode(metalCode(metal))?.metal, metal.name)
        }
        assertEquals("unknown", metalCode(null))
        assertNull(metalFromCode("unknown")!!.metal)
        // Con un código que no existe, la clave no se reconstruye.
        assertNull(metalFromCode("plata"))
        assertNull(metalFromCode("Silver"))
    }
}
