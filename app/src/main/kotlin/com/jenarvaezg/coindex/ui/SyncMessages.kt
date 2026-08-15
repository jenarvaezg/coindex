package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.SyncRecord
import com.jenarvaezg.coindex.data.numista.NumistaException
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** What one run did, for the snackbar that announces it the moment it ends. */
fun syncReportLabel(record: SyncRecord): String = buildString {
    append(plural(record.collectionItems, "pieza", "piezas"))
    append(" · ")
    append(plural(record.typesFetched, "ficha nueva", "fichas nuevas"))
    append(" · ")
    append(callsLabel(record.callsSpent))
    record.partialFailure?.let { append(" · incompleto") }
}

fun syncActionLabel(syncing: Boolean): String =
    if (syncing) "Sincronizando…" else "Sincronizar"

/**
 * A run that stopped half-way, said on the índice rather than in a snackbar.
 *
 * What an incomplete sync left half-done is a property of the collection on screen and outlives the
 * four seconds a snackbar lasts, which is why it has a card of its own.
 */
const val PARTIAL_SYNC_EYEBROW: String = "Sincronización incompleta"
const val PARTIAL_SYNC_EXPLANATION: String =
    "La última sincronización no terminó, así que puede faltar alguna pieza o ficha. Vuelve a " +
        "sincronizar cuando puedas."

/**
 * The durable line under the sync button.
 *
 * A time of day alone is ambiguous the next morning, and a full date is noise for a sync that
 * happened an hour ago, so the day is named only once it stops being obvious.
 */
fun lastSyncLabel(
    record: SyncRecord,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val at = ZonedDateTime.ofInstant(Instant.ofEpochMilli(record.atMillis), zone)
    val today = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone).toLocalDate()
    val day = when (at.toLocalDate()) {
        today -> "hoy"
        today.minusDays(1) -> "ayer"
        else -> dayAndMonthLabel(at.toLocalDate())
    }
    val clock = "%02d:%02d".format(at.hour, at.minute)
    return buildString {
        append("Última sincronización: $day $clock · ")
        append(plural(record.collectionItems, "pieza", "piezas"))
        append(" · ")
        append(callsLabel(record.callsSpent))
    }
}

/**
 * A failed sync translated into what the collector can do about it.
 *
 * «Numista devolvió HTTP 401 en /oauth_token» is true and useless: the collector's problem is a
 * key that no longer works, and the way out is the settings screen. Anything genuinely
 * unexpected keeps its original text rather than being flattened into a friendly nothing.
 */
fun syncErrorLabel(error: Throwable): String = when (error) {
    is NumistaException.EmptyApiKey ->
        "Falta la API key de Numista. Añádela en Ajustes."
    is NumistaException.BudgetExhausted ->
        "Llamadas a la API agotadas este mes. Espera al día 1 para volver a intentarlo."
    is NumistaException.Transport ->
        "Sin conexión con Numista. Tu colección local sigue disponible."
    is NumistaException.Api -> when (error.status) {
        401, 403 -> "Numista rechazó tu API key. Revísala en Ajustes."
        404 -> "Numista no encuentra ese identificador de usuario. Revísalo en Ajustes."
        429 -> "Numista está limitando las peticiones. Vuelve a intentarlo dentro de un rato."
        in 500..599 -> "Numista está caído ahora mismo. Tu colección local sigue disponible."
        else -> "Numista devolvió un error (${error.status}). Vuelve a intentarlo más tarde."
    }
    is NumistaException.InvalidResponse ->
        "Numista respondió algo que Coindex no entiende. Vuelve a intentarlo más tarde."
    else -> error.message ?: error.toString()
}
