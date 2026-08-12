package com.jenarvaezg.coindex.ui.screens

import com.jenarvaezg.coindex.ui.DrawnCell
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * How many lines of name a row of casillas reserves (#412).
 *
 * The reservation is the row's and never the cell's, which is the rule #337 bought: the tags of a
 * row line up because every casilla on it reserved the same box. What #412 adds is that the row
 * decides *how big* that box is instead of every row on every plate taking the same two lines.
 *
 * The measuring is Compose's — see `rememberPlateNameLines` — and this is the arithmetic on top of
 * it, so the rule can be read without a device: `linesOf` stands in for the text measurer.
 */
class PlateNameLinesTest {
    @Test
    fun `a row of date-run casillas reserves no name box at all`() {
        val cells = listOf(cell("1879"), cell("1880"), cell("1881"))

        assertEquals(listOf(0), plateRowNameLines(cells, columns = 3) { 1 })
    }

    @Test
    fun `a row whose names all fit keeps the two lines every plate used to reserve`() {
        val cells = listOf(cell("Onza Troy"), cell("1 Venezolano"), cell("Jaguar"))

        assertEquals(listOf(2), plateRowNameLines(cells, columns = 3) { 1 })
    }

    @Test
    fun `a row reserves the third line for the one name that needs it`() {
        val cells = listOf(
            cell("Onza Troy"),
            cell("V centenario de la primera vuelta al mundo"),
            cell("Jaguar"),
        )

        val lines = plateRowNameLines(cells, columns = 3) { name -> if (name.length > 34) 3 else 1 }

        assertEquals(listOf(3), lines)
    }

    @Test
    fun `a name past the third line is cut rather than growing its row further`() {
        val cells = listOf(
            cell("Iglesia de la Guarnición de Potsdam, con marca de ceca debajo (1934-1935)"),
        )

        assertEquals(listOf(3), plateRowNameLines(cells, columns = 1) { 7 })
    }

    @Test
    fun `each row answers for its own names`() {
        val cells = listOf(
            cell("Onza Troy"), cell("Jaguar"), cell("Cachicamo"),
            cell("Bicentenario del natalicio de José María Vargas"), cell("1986"), cell("1987"),
            cell("1988"), cell("1989"), cell("1990"),
        )

        val lines = plateRowNameLines(cells, columns = 3) { name -> if (name.length > 34) 3 else 1 }

        assertEquals(listOf(2, 3, 0), lines)
    }

    @Test
    fun `a last row shorter than the grid is still a row`() {
        val cells = listOf(cell("1879"), cell("1880"), cell("1881"), cell("Onza Troy"))

        assertEquals(listOf(0, 2), plateRowNameLines(cells, columns = 3) { 1 })
    }

    @Test
    fun `an untitled casilla is not what its row is measured by`() {
        // The name box a date-run casilla reserves inside a titled row is the row's whole box
        // (#473), and the year it prints is not a name that can ask for a third line.
        val cells = listOf(cell("1980"), cell("Muerte del Libertador"), cell("1982"))

        val lines = plateRowNameLines(cells, columns = 3) { name ->
            if (name.first().isDigit()) 9 else 2
        }

        assertEquals(listOf(2), lines)
    }
}

/** A casilla titled with [label], and titled with its own year where the two are the same. */
private fun cell(label: String) = DrawnCell(
    id = label,
    label = label,
    numistaTypeId = null,
    footnote = null,
    year = label.takeIf { it.toIntOrNull() != null },
    owned = true,
    missing = false,
)
