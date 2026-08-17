package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.TypeRefreshReport
import com.jenarvaezg.coindex.data.numista.NumistaException
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

private val MADRID = ZoneId.of("Europe/Madrid")

private fun at(year: Int, month: Int, day: Int, hour: Int = 12): Long =
    ZonedDateTime.of(year, month, day, hour, 0, 0, 0, MADRID).toInstant().toEpochMilli()

class FichaAgeLabelTest {
    private fun label(fetchedAt: Long, now: Long) = fichaAgeLabel(fetchedAt, now, MADRID)

    @Test
    fun `a ficha brought this morning is from today`() {
        assertEquals(
            "Ficha traída hoy",
            label(at(2026, 8, 5, hour = 9), at(2026, 8, 5, hour = 20)),
        )
    }

    @Test
    fun `last night is yesterday, not eleven hours`() {
        assertEquals(
            "Ficha traída ayer",
            label(at(2026, 8, 4, hour = 23), at(2026, 8, 5, hour = 10)),
        )
    }

    @Test
    fun `within the month it counts days`() {
        assertEquals("Ficha traída hace 3 días", label(at(2026, 8, 2), at(2026, 8, 5)))
        assertEquals("Ficha traída hace 29 días", label(at(2026, 7, 7), at(2026, 8, 5)))
    }

    @Test
    fun `the eight months of the issue read as eight months`() {
        assertEquals("Ficha traída hace 8 meses", label(at(2025, 12, 5), at(2026, 8, 5)))
    }

    @Test
    fun `a month and a half is one month, because whole months are what can be checked`() {
        assertEquals("Ficha traída hace 1 mes", label(at(2026, 6, 20), at(2026, 8, 5)))
    }

    @Test
    fun `beyond a year it counts years`() {
        assertEquals("Ficha traída hace 2 años", label(at(2024, 8, 5), at(2026, 8, 5)))
    }

    @Test
    fun `a clock that went backwards produces today, never a ficha from the future`() {
        assertEquals("Ficha traída hoy", label(at(2026, 8, 9), at(2026, 8, 5)))
    }
}

class FichaRefreshLabelTest {
    @Test
    fun `the action says what it costs before it is spent`() {
        assertEquals(
            "Actualizar la ficha · 1 consulta",
            fichaRefreshLabel(refreshing = false),
        )
    }

    @Test
    fun `while it is asking, it says who it is asking`() {
        assertEquals(
            "Preguntando a Numista…",
            fichaRefreshLabel(refreshing = true),
        )
    }
}

class FichaRefreshOutcomeTest {
    @Test
    fun `a corrected ficha is announced as changed`() {
        assertEquals(
            "Ficha de Numista 596807 actualizada: el dato había cambiado.",
            fichaRefreshMessage(TypeRefreshReport(596_807, changed = true)),
        )
    }

    @Test
    fun `no change still says the call was spent`() {
        assertEquals(
            "La ficha de Numista 596807 sigue igual. Has gastado 1 consulta.",
            fichaRefreshMessage(TypeRefreshReport(596_807, changed = false)),
        )
    }

    @Test
    fun `a 404 is a type Numista no longer publishes, not a wrong user id`() {
        assertEquals(
            "Numista ya no publica el tipo 596807. La ficha que tenías sigue en el móvil.",
            fichaRefreshErrorLabel(
                596_807,
                NumistaException.Api("/types/596807", 404, "not found"),
            ),
        )
    }

    @Test
    fun `every other failure keeps the sync's own wording`() {
        val error = NumistaException.BudgetExhausted(1_500, 1_500)
        assertEquals(syncErrorLabel(error), fichaRefreshErrorLabel(596_807, error))
    }
}
