package com.jenarvaezg.coindex.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Lo que la lámina imprime cuando el texto viene del fichero curado.
 *
 * Cuarenta de los setenta y cinco nombres llevan cifra y unidad, y las casillas de tres de las cuatro
 * láminas de un solo año también («750 escudos · 12,5 g»), así que la viuda del #511 no era un caso
 * raro: era la mayoría del corpus esperando a una caja estrecha.
 */
class CuratedTextTest {
    /** El caso que abrió el ticket: la «g» de los fuertes abría línea sola. */
    @Test
    fun `una unidad no se queda sola al final de la linea`() {
        assertEquals(
            "Fuertes · Venezuela · plata 25\u00A0g · 1876-1936",
            "Fuertes · Venezuela · plata 25 g · 1876-1936".weldUnits(),
        )
    }

    /** Las cuatro unidades que el corpus usa, decimales incluidos. */
    @Test
    fun `la cifra y su unidad viajan juntas en todas las medidas`() {
        assertEquals("13,88\u00A0g", "13,88 g".weldUnits())
        assertEquals("1\u00A0oz", "1 oz".weldUnits())
        assertEquals("0,804\u00A0oz", "0,804 oz".weldUnits())
        assertEquals("34\u00A0mm", "34 mm".weldUnits())
        // Una fracción huérfana de su unidad es el mismo defecto escrito más pequeño.
        assertEquals("½\u00A0oz", "½ oz".weldUnits())
    }

    /**
     * Una denominación no es una unidad: «5 euros» parte como cualquier pareja de palabras, porque un
     * «euros» al principio de línea se lee como prosa y una «g» sola se lee como una errata.
     */
    @Test
    fun `una denominacion sigue partiendo`() {
        assertEquals("5 euros · 18\u00A0g", "5 euros · 18 g".weldUnits())
        assertEquals("100 pesetas de Franco", "100 pesetas de Franco".weldUnits())
    }

    /** Nada más cambia: es cómo se imprime el texto, no lo que dice. */
    @Test
    fun `el resto del nombre llega intacto`() {
        val name = "Silver Britannia 1 oz .999 · Reino Unido · bullion anual desde 2013"
        assertEquals(name, name.weldUnits().replace('\u00A0', ' '))
        assertTrue(name.weldUnits().contains("1\u00A0oz"))
        // Ni «Silver» ni «.999» son cifra y unidad, así que la única costura es la del oz.
        assertFalse(name.weldUnits().substringAfter("1\u00A0oz").contains('\u00A0'))
    }
}
