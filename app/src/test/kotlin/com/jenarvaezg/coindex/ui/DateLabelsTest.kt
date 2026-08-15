package com.jenarvaezg.coindex.ui

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CatalogDateLabelTest {
    @Test
    fun `the curated date is a day of the album's own abbreviations, never the ISO string`() {
        assertEquals("1 ago 2026", catalogDateLabel("2026-08-01"))
        assertEquals("31 dic 2025", catalogDateLabel("2025-12-31"))
    }

    /**
     * The editorial date is **not** an age (#518): it is the version of the curated file, and nothing
     * about it expires. So it never borrows the wording the ficha and the price use for how old a
     * datum is — «hoy», «ayer», «hace 13 días» — even on the day the catalog was written.
     */
    @Test
    fun `the catalog's date never reads as an age`() {
        val today = catalogDateLabel("2026-08-15")
        assertFalse("hace" in today)
        assertFalse("hoy" in today)
        assertEquals("15 ago 2026", today)
    }

    /**
     * `CollectionCatalogValidation` refuses a blank `updated_at` but not a malformed one, so this is
     * the case that can actually reach a screen: it prints as it was written. Inventing «sin fecha»
     * over a value that is right there would hide from the curator what needs fixing.
     */
    @Test
    fun `a date this cannot read passes through untouched`() {
        assertEquals("verano de 2026", catalogDateLabel("verano de 2026"))
    }
}

class DayLabelTest {
    @Test
    fun `a day in full carries its year`() {
        assertEquals("2 jul 2026", dayMonthYearLabel(LocalDate.of(2026, 7, 2)))
    }

    /** The same abbreviations, for the lines close enough that the year says nothing. */
    @Test
    fun `a day without its year is the day and the month`() {
        assertEquals("13 ago", dayAndMonthLabel(LocalDate.of(2026, 8, 13)))
        assertEquals("1 ene", dayAndMonthLabel(LocalDate.of(2026, 1, 1)))
    }

    /**
     * September is «sep» and never CLDR's «sept», which is the whole reason the months are a written
     * list rather than a `DateTimeFormatter` pattern: two JDKs must not spell one month two ways.
     */
    @Test
    fun `every month keeps one spelling`() {
        assertEquals("30 sep 2026", dayMonthYearLabel(LocalDate.of(2026, 9, 30)))
    }
}
