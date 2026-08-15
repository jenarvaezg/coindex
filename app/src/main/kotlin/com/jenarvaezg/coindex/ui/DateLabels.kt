package com.jenarvaezg.coindex.ui

import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Short month names written out, so a date reads the same on every JDK and locale.
 *
 * The one list in the app: `d MMM yyyy` through a `DateTimeFormatter` spells September «sep» on one
 * CLDR and «sept» on another, and two spellings of the same month is exactly the drift a shared list
 * cannot have.
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
 * The curated catalog's own date, which is a **version and not an age** (#518).
 *
 * Two clocks used to look alike on these screens: «Ficha traída hace 13 días» — how old the datum
 * this phone holds of Numista is, which expires and can be brought again — and «Actualizado
 * 2026-08-01», which is the edition of a curated file that nothing on the phone can refresh. Same
 * muted small line, same air, and no way for the reader to tell which of the two was a reproach.
 *
 * So each keeps one form. Numista's age is relative and says how long ago (`fichaAgeLabel`); the
 * editorial date is absolute, in the album's own abbreviations, and the plate labels it «Catálogo»
 * rather than «Actualizado» — a generic participle that reads as «comprobado por última vez», which
 * is precisely what this date cannot mean.
 *
 * A string this cannot parse prints as it was written. `CollectionCatalogValidation` refuses a blank
 * `updated_at` but not a malformed one, and «sin fecha» over a value that is right there would hide
 * from the curator exactly what needs fixing.
 */
fun catalogDateLabel(updatedAt: String): String = try {
    dayMonthYearLabel(LocalDate.parse(updatedAt))
} catch (_: DateTimeParseException) {
    updatedAt
}
