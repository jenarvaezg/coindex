package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.photos.PhotoCacheStatus
import com.jenarvaezg.coindex.data.photos.PrefetchRefusal
import java.util.Locale

/**
 * The prefetch's one sentence, on «Este teléfono»; otherwise it is silent. It tells photos still
 * arriving apart from photos held back (mobile data, power saving…), which the collector may need
 * to act on.
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

/** Bytes as megabytes with a decimal comma whatever the phone's language: the copy is Spanish. */
fun megabytesLabel(bytes: Long): String =
    String.format(Locale.forLanguageTag("es-ES"), "%.1f MB", bytes / 1_000_000.0)
