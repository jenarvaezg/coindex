package com.jenarvaezg.coindex.ui

import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Short month names spelt out: `DateTimeFormatter` writes September «sep» or «sept» depending on
 * the CLDR version.
 */
private val MONTHS = listOf(
    "ene", "feb", "mar", "abr", "may", "jun",
    "jul", "ago", "sep", "oct", "nov", "dic",
)

/** «13 ago»: a day close enough that its year says nothing. */
fun dayAndMonthLabel(date: LocalDate): String = "${date.dayOfMonth} ${MONTHS[date.monthValue - 1]}"

/** «2 jul 2026»: a day named in full, for a date that is not going to move. */
fun dayMonthYearLabel(date: LocalDate): String = "${dayAndMonthLabel(date)} ${date.year}"

/**
 * The curated catalog's date, which is a version, not an age (#518). Numista's age is relative
 * (`fichaAgeLabel`); this one is absolute, and the plate labels it «Catálogo» rather than
 * «Actualizado», which would read as «last checked».
 *
 * An unparseable string prints as written: `CollectionCatalogValidation` refuses a blank
 * `updated_at` but not a malformed one, and the curator needs to see it.
 */
fun catalogDateLabel(updatedAt: String): String = try {
    dayMonthYearLabel(LocalDate.parse(updatedAt))
} catch (_: DateTimeParseException) {
    updatedAt
}
