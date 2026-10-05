package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.TypeRefreshReport
import com.jenarvaezg.coindex.data.numista.NumistaException
import java.time.Instant
import java.time.Period
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Calls spent refreshing one ficha (ADR 0025): `/types/{id}` needs no OAuth token, so it is always
 * one. A constant because diffing the month's log would also count a concurrent sync's calls.
 */
const val FICHA_REFRESH_CALLS: Int = 1

/**
 * How long ago this phone got the ficha, in the coarsest unit that is still true.
 *
 * «Traída», not «es de»: the cache only knows when the ficha arrived, and one shipped in the APK
 * may be older than that (ADR 0025), so the refresh is never hidden. Counted in calendar days, so a
 * ficha fetched last night reads «ayer».
 */
fun fichaAgeLabel(
    fetchedAt: Long,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val brought = Instant.ofEpochMilli(fetchedAt).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    // A clock that has gone backwards is not a ficha from the future: it is today's.
    val days = ChronoUnit.DAYS.between(brought, today).coerceAtLeast(0)
    val elapsed = Period.between(brought, today)
    return "Ficha traída " + when {
        days == 0L -> "hoy"
        days == 1L -> "ayer"
        days < 30L -> "hace ${plural(days.toInt(), "día", "días")}"
        elapsed.years >= 1 -> "hace ${plural(elapsed.years, "año", "años")}"
        // A month and a half is «hace un mes»: the whole months are what the collector can check.
        else -> "hace ${plural(elapsed.months.coerceAtLeast(1), "mes", "meses")}"
    }
}

/** The refresh button, with its cost. */
fun fichaRefreshLabel(refreshing: Boolean): String = when {
    refreshing -> "Preguntando a Numista…"
    else -> "Actualizar la ficha · ${queriesLabel(FICHA_REFRESH_CALLS)}"
}

/** What the refresh found, for the snackbar. An unchanged ficha still says the call was spent. */
fun fichaRefreshMessage(report: TypeRefreshReport): String = if (report.changed) {
    "Ficha de Numista ${report.typeId} actualizada: el dato había cambiado."
} else {
    "La ficha de Numista ${report.typeId} sigue igual. " +
        "Has gastado ${queriesLabel(FICHA_REFRESH_CALLS)}."
}

/**
 * A failed refresh. Only the 404 differs from the sync's wording: here it means Numista no longer
 * publishes the type, such as a submission a referee deleted (#186). The ficha stays on the phone.
 */
fun fichaRefreshErrorLabel(typeId: Int, error: Throwable): String =
    if (error is NumistaException.Api && error.status == 404) {
        "Numista ya no publica el tipo $typeId. La ficha que tenías sigue en el móvil."
    } else {
        syncErrorLabel(error)
    }
