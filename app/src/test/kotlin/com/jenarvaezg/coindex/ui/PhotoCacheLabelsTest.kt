package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.photos.PhotoCacheStatus
import com.jenarvaezg.coindex.data.photos.PrefetchRefusal
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The one line the prefetch is allowed to say out loud (#191).
 *
 * Everything else about it is silent on purpose — no snackbar, no banner, nothing to dismiss —
 * because an optimization that announces itself becomes a chore to supervise. This line lives in
 * the settings screen, where the collector goes when they want to know what the phone is holding,
 * and it is also the only way to tell «ya están todas» from «no las trae porque estás con datos».
 */
class PhotoCacheLabelsTest {
    @Test
    fun `with every photograph in hand it says so, and what that buys`() {
        val label = photoCacheLabel(
            PhotoCacheStatus(wanted = 1_632, missing = 0, bytes = 11_400_000),
        )

        assertEquals(
            "Las 1632 fotos del catálogo están en este teléfono (11,4 MB). Las láminas y el " +
                "cuaderno se dibujan sin pedir nada.",
            label,
        )
    }

    @Test
    fun `while there are photographs left it says how many, and that nobody has to do anything`() {
        val label = photoCacheLabel(
            PhotoCacheStatus(wanted = 1_632, missing = 320, bytes = 9_000_000),
        )

        assertEquals(
            "Faltan 320 de 1632 fotos del catálogo (9,0 MB en este teléfono). Se traen solas " +
                "con la app abierta.",
            label,
        )
    }

    @Test
    fun `a hold says what is holding it, because that is what the collector can act on`() {
        val held = { refusal: PrefetchRefusal ->
            photoCacheLabel(
                PhotoCacheStatus(wanted = 1_632, missing = 320, bytes = 9_000_000, held = refusal),
            ).substringAfter("teléfono). ")
        }

        assertEquals(
            "Se traerán cuando haya wifi: con datos móviles no se descargan.",
            held(PrefetchRefusal.MeteredNetwork),
        )
        assertEquals(
            "Esperan a que se apague el ahorro de energía.",
            held(PrefetchRefusal.PowerSave),
        )
        assertEquals("Esperan a que la batería se recupere.", held(PrefetchRefusal.LowBattery))
        assertEquals("Esperan a que termine el sincronizado.", held(PrefetchRefusal.Syncing))
    }

    @Test
    fun `before the first sync there are no fichas, so there is nothing to promise`() {
        assertEquals(
            "Todavía no hay fichas en este teléfono, así que no hay fotos que traer.",
            photoCacheLabel(PhotoCacheStatus()),
        )
    }

    @Test
    fun `the size is read in megabytes with a Spanish comma, whatever the phone's locale`() {
        // The device may well be in English; this line is written in Spanish and «10.9 MB» in the
        // middle of it reads as a typo.
        assertEquals("0,5 MB", megabytesLabel(500_000))
        assertEquals("11,4 MB", megabytesLabel(11_400_000))
        assertEquals("0,0 MB", megabytesLabel(0))
    }
}
