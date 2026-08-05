package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.TypeRefreshReport
import com.jenarvaezg.coindex.data.numista.NumistaException
import java.time.Instant
import java.time.Period
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * What refreshing one ficha costs, and it is the same number every time (ADR 0023).
 *
 * A constant rather than a measurement: `/types/{id}` needs no OAuth token, so the gesture is
 * exactly one reserved call. Counting the month's log before and after would have read a concurrent
 * sync's calls into this one tap and announced «has gastado 37 llamadas».
 */
const val FICHA_REFRESH_CALLS: Int = 1

/**
 * How old the ficha on this phone is, in the coarsest unit that is still true.
 *
 * It says **traída** and not «es de», because that is the fact the cache actually holds: the day
 * this phone got the ficha. For a ficha the collector synced the two are the same thing; for one
 * that arrived in the APK the *content* may be older than the day it landed here (ADR 0023), and
 * that is exactly why the gesture underneath is never hidden behind a «suficientemente fresca».
 *
 * Counted in calendar days rather than in elapsed milliseconds, so a ficha fetched last night reads
 * «ayer» this morning instead of «hace 11 horas» rounded to today.
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

/**
 * The gesture, saying what it costs before it is spent — the same bargain the sync button keeps.
 *
 * With no budget left it says so instead of failing on the tap: an exhausted month is the normal,
 * expected outcome of a free API key, not an error to discover by pressing.
 */
fun fichaRefreshLabel(refreshing: Boolean, budgetRemaining: Int): String = when {
    refreshing -> "Preguntando a Numista…"
    budgetRemaining < FICHA_REFRESH_CALLS -> "Sin presupuesto este mes"
    else -> "Actualizar la ficha · ${callsLabel(FICHA_REFRESH_CALLS)}"
}

/**
 * What the refresh found, for the snackbar.
 *
 * «Sin cambios» is a result and not a failure — it is the answer «lo que tienes es lo que hay en
 * Numista» — so it says the call was spent rather than pretending the tap was free.
 */
fun fichaRefreshMessage(report: TypeRefreshReport): String = if (report.changed) {
    "Ficha de Numista ${report.typeId} actualizada: el dato había cambiado."
} else {
    "La ficha de Numista ${report.typeId} sigue igual. " +
        "Has gastado ${callsLabel(FICHA_REFRESH_CALLS)}."
}

/**
 * A refresh that did not happen, in terms of what it means for this type.
 *
 * The one case the sync's own wording gets wrong is the 404: there it is a user id that does not
 * exist, and here it is a type Numista does not publish any more — a submission a referee deleted,
 * which is precisely the fate an unpublished type can have (#186). The ficha stays on the phone, and
 * the message says so: nothing about this gesture can lose data.
 */
fun fichaRefreshErrorLabel(typeId: Int, error: Throwable): String =
    if (error is NumistaException.Api && error.status == 404) {
        "Numista ya no publica el tipo $typeId. La ficha que tenías sigue en el móvil."
    } else {
        syncErrorLabel(error)
    }
