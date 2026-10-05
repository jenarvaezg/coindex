package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.SyncRecord
import com.jenarvaezg.coindex.data.numista.NumistaException
import java.io.IOException
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private val MADRID = ZoneId.of("Europe/Madrid")

private fun at(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
): Long = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, MADRID).toInstant().toEpochMilli()

private fun record(
    atMillis: Long,
    items: Int = 22,
    types: Int = 3,
    calls: Int = 5,
    partialFailure: String? = null,
) = SyncRecord(atMillis, items, types, calls, partialFailure)

class LastSyncLabelTest {
    @Test
    fun `today is named as today, not dated`() {
        val label = lastSyncLabel(
            record(at(2026, 7, 30, 15, 30)),
            nowMillis = at(2026, 7, 30, 19, 0),
            zone = MADRID,
        )
        assertEquals("Última sincronización: hoy 15:30 · 22 piezas · 5 consultas", label)
    }

    @Test
    fun `yesterday is named rather than dated`() {
        val label = lastSyncLabel(
            record(at(2026, 7, 29, 9, 5)),
            nowMillis = at(2026, 7, 30, 0, 40),
            zone = MADRID,
        )
        assertEquals("Última sincronización: ayer 09:05 · 22 piezas · 5 consultas", label)
    }

    @Test
    fun `an older sync carries its date`() {
        val label = lastSyncLabel(
            record(at(2026, 3, 8, 23, 59)),
            nowMillis = at(2026, 7, 30, 12, 0),
            zone = MADRID,
        )
        assertEquals("Última sincronización: 8 mar 23:59 · 22 piezas · 5 consultas", label)
    }

    @Test
    fun `a single piece and a single call are not pluralized`() {
        val label = lastSyncLabel(
            record(at(2026, 7, 30, 8, 0), items = 1, calls = 1),
            nowMillis = at(2026, 7, 30, 8, 1),
            zone = MADRID,
        )
        assertEquals("Última sincronización: hoy 08:00 · 1 pieza · 1 consulta", label)
    }

    @Test
    fun `the day boundary is the calendar day, not twenty four hours`() {
        // Half past midnight, with the previous sync six hours earlier: still «ayer».
        val label = lastSyncLabel(
            record(at(2026, 7, 29, 18, 30)),
            nowMillis = at(2026, 7, 30, 0, 30),
            zone = MADRID,
        )
        assertTrue(label.contains("ayer 18:30"), label)
    }
}

class SyncReportLabelTest {
    @Test
    fun `the maintenance action reports a sync in flight`() {
        assertEquals("Sincronizar", syncActionLabel(syncing = false))
        assertEquals("Sincronizando…", syncActionLabel(syncing = true))
    }

    @Test
    fun `a complete run reports its three counters`() {
        assertEquals(
            "22 piezas · 3 fichas nuevas · 5 consultas",
            syncReportLabel(record(at(2026, 7, 30, 15, 30))),
        )
    }

    @Test
    fun `an incomplete run says so`() {
        val label = syncReportLabel(
            record(at(2026, 7, 30, 15, 30), partialFailure = "HTTP 500 en /types/404044"),
        )
        assertEquals("22 piezas · 3 fichas nuevas · 5 consultas · incompleto", label)
    }
}

class SyncErrorLabelTest {
    @Test
    fun `a rejected key points at the credentials instead of at the endpoint`() {
        val label = syncErrorLabel(NumistaException.Api("/oauth_token", 401, ""))
        // The screen's name is interpolated, not spelled again (#521).
        assertEquals("Numista rechazó tu API key. Revísala en Credenciales.", label)
        assertTrue(!label.contains("401") && !label.contains("oauth"), label)
    }

    @Test
    fun `a forbidden response is the same problem as an unauthorized one`() {
        assertEquals(
            syncErrorLabel(NumistaException.Api("/oauth_token", 401, "")),
            syncErrorLabel(NumistaException.Api("/users/1/collected_items", 403, "")),
        )
    }

    /** An exhausted month comes back on the 1st, a throttle in a while (#600). */
    @Test
    fun `the exhausted month and the throttle are not told to wait the same`() {
        val exhausted = syncErrorLabel(NumistaException.Api("/types/1", 429, "Quota exceeded"))
        val throttled = syncErrorLabel(NumistaException.Api("/types/1", 429, "too many requests"))

        assertTrue(exhausted.contains("día 1"), exhausted)
        assertFalse(exhausted.contains("un rato"), exhausted)
        assertTrue(throttled.contains("un rato"), throttled)
        assertFalse(throttled.contains("día 1"), throttled)
        // Neither of them sends the collector to Credenciales: the key is fine in both.
        assertFalse(exhausted.contains("Credenciales"), exhausted)
        assertFalse(throttled.contains("Credenciales"), throttled)
    }

    /**
     * `BudgetExhausted` is ADR 0003's local gate; the 429 is Numista refusing a key another phone
     * also spends (#562). Mixing them up would send the collector looking in the wrong place.
     */
    @Test
    fun `the month of the key and the month of the phone are different sentences`() {
        assertNotEquals(
            syncErrorLabel(NumistaException.BudgetExhausted(1500, 1500)),
            syncErrorLabel(NumistaException.Api("/types/1", 429, "Quota exceeded")),
        )
    }

    @Test
    fun `no network says the local collection is still there`() {
        val label = syncErrorLabel(
            NumistaException.Transport("/oauth_token", IOException("unreachable")),
        )
        assertTrue(label.contains("Sin conexión"), label)
        assertTrue(label.contains("colección local"), label)
    }

    @Test
    fun `an exhausted month only says to wait until day one`() {
        val label = syncErrorLabel(NumistaException.BudgetExhausted(1500, 1500))
        assertTrue(label.contains("espera al día 1", ignoreCase = true), label)
        assertFalse(label.contains("Credenciales"), label)
        assertFalse(label.contains("Presupuesto"), label)
        assertFalse(label.contains("1500"), label)
        // The app's one unit (#516), asserted here too because this file owns the string.
        assertTrue(label.contains("Consultas"), label)
        assertFalse(label.contains("llamada", ignoreCase = true), label)
    }

    @Test
    fun `a server error is not the collector's fault`() {
        assertTrue(syncErrorLabel(NumistaException.Api("/types/1", 503, "")).contains("caído"))
    }

    @Test
    fun `an unknown status is still reported rather than swallowed`() {
        assertTrue(syncErrorLabel(NumistaException.Api("/types/1", 418, "")).contains("418"))
    }

    @Test
    fun `anything unexpected keeps its own message`() {
        assertEquals("boom", syncErrorLabel(IllegalStateException("boom")))
    }
}
