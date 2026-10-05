package com.jenarvaezg.coindex.ui.print

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Las opciones del cuaderno entre lanzamientos. Como en `ShelfCodec`, una clave por interruptor, y
 * la que falta se lee como el valor por omisión de ese interruptor.
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

    /** A missing key means the switch's default, not «off», and «fotos» defaults to on. */
    @Test
    fun `a switch stored on its own does not turn the other five off`() {
        val stored = NotebookCodec.encode(NotebookOptions(sharePage = true))
        val onlySharePage = NotebookCodec.key(NotebookSwitch.SharePage)

        val read = NotebookCodec.decode { key -> stored[key].takeIf { key == onlySharePage } }

        assertEquals(NotebookOptions(sharePage = true), read)
        assertTrue(read.photographs, "el cuaderno se ha quedado sin fotos por una clave ausente")
    }

    /** «Sin colección» (#275) es apagado por omisión: actualizar no alarga el cuaderno (#228). */
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
        // Se escriben las seis, para que «elegido a propósito» y «nunca elegido» se lean igual.
        assertEquals(keys.toSet(), NotebookCodec.encode(NotebookOptions()).keys)
    }
}
