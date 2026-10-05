package com.jenarvaezg.coindex.ui.print

/**
 * Persists the notebook options in shared preferences, one key per switch, like `ShelfCodec`. A
 * missing key reads back as that switch's default rather than as off, so an absent «fotos» key
 * can't produce a notebook with no coins. Nothing is stored per card (ADR 0021 §7).
 */
object NotebookCodec {
    fun key(switch: NotebookSwitch): String = when (switch) {
        NotebookSwitch.Photographs -> "notebook_photographs"
        NotebookSwitch.BothFaces -> "notebook_both_faces"
        NotebookSwitch.ActualSize -> "notebook_actual_size"
        NotebookSwitch.SharePage -> "notebook_share_page"
        NotebookSwitch.NumistaQr -> "notebook_numista_qr"
        NotebookSwitch.Unclaimed -> "notebook_unclaimed"
        NotebookSwitch.Money -> "notebook_money"
    }

    /** Every switch, defaults included, rather than omitting keys left at their default. */
    fun encode(options: NotebookOptions): Map<String, Boolean> =
        NotebookSwitch.entries.associate { switch -> key(switch) to options[switch] }

    fun decode(read: (String) -> Boolean?): NotebookOptions =
        NotebookSwitch.entries.fold(NotebookOptions()) { options, switch ->
            read(key(switch))?.let { stored -> options.with(switch, stored) } ?: options
        }
}
