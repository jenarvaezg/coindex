package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.photos.PrefetchRefusal
import java.util.Locale

/**
 * What the phone holds of the catalog's photographs, for the one line that says it (#191).
 *
 * @param wanted the photographs the index would draw, minus the ones Numista says are gone.
 * @param missing how many of those are not on this phone yet.
 * @param bytes what the picture cache weighs.
 * @param held why they are not being brought right now, or null while they are.
 */
data class PhotoCacheStatus(
    val wanted: Int = 0,
    val missing: Int = 0,
    val bytes: Long = 0L,
    val held: PrefetchRefusal? = null,
)

/**
 * The prefetch's one sentence, in the settings screen.
 *
 * Everything else about it is silent by design — it is an optimization, and an optimization that
 * announces itself becomes a chore to supervise. This line exists because two situations that look
 * identical from the outside are not: «faltan 320 y están cayendo» and «faltan 320 porque estás con
 * datos móviles» need different things from the collector, and the second one needs him to know.
 */
fun photoCacheLabel(status: PhotoCacheStatus): String {
    if (status.wanted == 0) {
        return "Todavía no hay fichas en este teléfono, así que no hay fotos que traer."
    }
    val size = megabytesLabel(status.bytes)
    if (status.missing == 0) {
        return "Las ${status.wanted} fotos del catálogo están en este teléfono ($size). " +
            "Las láminas y el cuaderno se dibujan sin pedir nada."
    }
    val head = "Faltan ${status.missing} de ${status.wanted} fotos del catálogo " +
        "($size en este teléfono). "
    return head + when (status.held) {
        null -> "Se traen solas con la app abierta."
        PrefetchRefusal.MeteredNetwork ->
            "Se traerán cuando haya wifi: con datos móviles no se descargan."
        PrefetchRefusal.PowerSave -> "Esperan a que se apague el ahorro de energía."
        PrefetchRefusal.LowBattery -> "Esperan a que la batería se recupere."
        PrefetchRefusal.Syncing -> "Esperan a que termine el sincronizado."
    }
}

/**
 * Bytes as megabytes with a Spanish decimal comma, whatever language the phone is in.
 *
 * The locale is pinned rather than taken from the device because the sentence around it is written
 * in Spanish: «10.9 MB» in the middle of it reads as a typo, not as a setting.
 */
fun megabytesLabel(bytes: Long): String =
    String.format(Locale("es", "ES"), "%.1f MB", bytes / 1_000_000.0)
