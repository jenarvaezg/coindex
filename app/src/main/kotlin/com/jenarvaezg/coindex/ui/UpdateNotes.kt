package com.jenarvaezg.coindex.ui

/** Collapsed, the banner is a strip above the notebook and stays two lines tall. */
const val UPDATE_NOTES_COLLAPSED_LINES = 2

/** How much of an update's release notes the banner shows, and what the hint under them says. */
data class UpdateNotesDisclosure(val maxLines: Int, val hint: String?)

/**
 * Two lines of release notes by default and the whole note on a tap: the banner sits above every
 * screen, but the notes are where a version's changes are read. [truncated] comes from the text
 * layout, so a note that fits gets no «Ver más».
 */
fun updateNotesDisclosure(expanded: Boolean, truncated: Boolean): UpdateNotesDisclosure = when {
    // Nothing hidden: no hint, and nothing for a tap to reveal.
    !truncated -> UpdateNotesDisclosure(UPDATE_NOTES_COLLAPSED_LINES, hint = null)
    expanded -> UpdateNotesDisclosure(maxLines = Int.MAX_VALUE, hint = "Ver menos")
    else -> UpdateNotesDisclosure(UPDATE_NOTES_COLLAPSED_LINES, hint = "Ver más")
}
