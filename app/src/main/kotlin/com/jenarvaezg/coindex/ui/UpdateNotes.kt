package com.jenarvaezg.coindex.ui

/** Collapsed, the banner is a strip above the notebook and stays two lines tall. */
const val UPDATE_NOTES_COLLAPSED_LINES = 2

/** How much of an update's release notes the banner shows, and what the hint under them says. */
data class UpdateNotesDisclosure(val maxLines: Int, val hint: String?)

/**
 * Whether the release notes are cut short, and whether it is worth saying so.
 *
 * The notes are the only place the collector reads what a version brings, so they have to be
 * readable in full; but the banner rides above every screen and cannot claim more than a couple
 * of lines uninvited. Hence the disclosure: two lines by default, the whole note on a tap.
 *
 * [truncated] is what the text layout actually reported, not a guess from the string's length:
 * a note that already fits carries no hint, so «Ver más» never promises anything it cannot show.
 */
fun updateNotesDisclosure(expanded: Boolean, truncated: Boolean): UpdateNotesDisclosure = when {
    // Nothing hidden: no hint, and nothing for a tap to reveal.
    !truncated -> UpdateNotesDisclosure(UPDATE_NOTES_COLLAPSED_LINES, hint = null)
    expanded -> UpdateNotesDisclosure(maxLines = Int.MAX_VALUE, hint = "Ver menos")
    else -> UpdateNotesDisclosure(UPDATE_NOTES_COLLAPSED_LINES, hint = "Ver más")
}
