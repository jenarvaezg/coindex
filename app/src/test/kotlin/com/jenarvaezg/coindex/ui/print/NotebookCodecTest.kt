package com.jenarvaezg.coindex.ui.print

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Cómo sobrevive a un lanzamiento lo que el coleccionista eligió en la hoja de exportación.
 *
 * Un interruptor por clave, como los filtros de la estantería (`ShelfCodec`), y por la misma razón:
 * lo que esta versión no reconoce se lee como «el cuaderno de hoy» en vez de romper o, peor, dejar
 * encendido algo que nadie pidió.
 */
class NotebookCodecTest {
    @Test
    fun `what the collector chose survives a launch`() {
        val chosen = NotebookOptions(
            photographs = false,
            bothFaces = true,
            actualSize = false,
            sharePage = true,
            numistaQr = true,
            unclaimed = true,
        )

        val written = NotebookCodec.encode(chosen)

        assertEquals(chosen, NotebookCodec.decode { key -> written[key] })
    }

    @Test
    fun `a phone that has never opened the sheet reads back the notebook of today`() {
        assertEquals(NotebookOptions(), NotebookCodec.decode { null })
    }

    /**
     * A key written on its own leaves the other five at their default.
     *
     * The missing key is not an «off», it is the default of the switch it names — and the default of
     * «fotos» is on.
     */
    @Test
    fun `a switch stored on its own does not turn the other five off`() {
        val stored = NotebookCodec.encode(NotebookOptions(sharePage = true))
        val onlySharePage = NotebookCodec.key(NotebookSwitch.SharePage)

        val read = NotebookCodec.decode { key -> stored[key].takeIf { key == onlySharePage } }

        assertEquals(NotebookOptions(sharePage = true), read)
        assertTrue(read.photographs, "el cuaderno se ha quedado sin fotos por una clave ausente")
    }

    /**
     * El sexto interruptor sobre un móvil que guardó cinco (#275).
     *
     * Es el caso que este fichero llevaba escrito desde el #228, y ahora existe: una clave que la
     * versión anterior nunca escribió se lee como el valor por omisión del interruptor que nombra,
     * y el de «sin colección» es apagado. Un cuaderno que creciera tres páginas solo, en el primer
     * lanzamiento después de actualizar, sería exactamente lo que el #228 prometió que no pasaría.
     */
    @Test
    fun `un móvil que guardó cinco interruptores no estrena el sexto encendido`() {
        val cinco = NotebookCodec
            .encode(NotebookOptions(sharePage = true, unclaimed = true))
            .minus(NotebookCodec.key(NotebookSwitch.Unclaimed))

        val read = NotebookCodec.decode { key -> cinco[key] }

        assertEquals(NotebookOptions(sharePage = true), read)
        assertFalse(read.unclaimed, "el cuaderno ha estrenado la lámina de las sueltas sin permiso")
    }

    @Test
    fun `the six keys are distinct and say which notebook they are about`() {
        val keys = NotebookSwitch.entries.map(NotebookCodec::key)

        assertEquals(keys.size, keys.distinct().size, "dos interruptores comparten clave: $keys")
        assertTrue(keys.all { it.startsWith("notebook_") }, "claves sin prefijo: $keys")
        // Y la codificación escribe las seis, para que «elegido a propósito» y «nunca elegido»
        // se lean igual — que es lo que son.
        assertEquals(keys.toSet(), NotebookCodec.encode(NotebookOptions()).keys)
    }
}
