package com.jenarvaezg.coindex.ui.print

import kotlin.test.Test
import kotlin.test.assertEquals
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
        )

        val written = NotebookCodec.encode(chosen)

        assertEquals(chosen, NotebookCodec.decode { key -> written[key] })
    }

    @Test
    fun `a phone that has never opened the sheet reads back the notebook of today`() {
        assertEquals(NotebookOptions(), NotebookCodec.decode { null })
    }

    /**
     * A key written on its own leaves the other four at their default.
     *
     * This is what a version that grows a sixth switch will look like on a phone that stored five:
     * the missing key is not an «off», it is the default of the switch it names — and the default of
     * «fotos» is on.
     */
    @Test
    fun `a switch stored on its own does not turn the other four off`() {
        val stored = NotebookCodec.encode(NotebookOptions(sharePage = true))
        val onlySharePage = NotebookCodec.key(NotebookSwitch.SharePage)

        val read = NotebookCodec.decode { key -> stored[key].takeIf { key == onlySharePage } }

        assertEquals(NotebookOptions(sharePage = true), read)
        assertTrue(read.photographs, "el cuaderno se ha quedado sin fotos por una clave ausente")
    }

    @Test
    fun `the five keys are distinct and say which notebook they are about`() {
        val keys = NotebookSwitch.entries.map(NotebookCodec::key)

        assertEquals(keys.size, keys.distinct().size, "dos interruptores comparten clave: $keys")
        assertTrue(keys.all { it.startsWith("notebook_") }, "claves sin prefijo: $keys")
        // Y la codificación escribe las cinco, para que «elegido a propósito» y «nunca elegido»
        // se lean igual — que es lo que son.
        assertEquals(keys.toSet(), NotebookCodec.encode(NotebookOptions()).keys)
    }
}
