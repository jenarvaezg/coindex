package com.jenarvaezg.coindex.ui.print

/**
 * How the chosen configuration survives a launch, one key per switch.
 *
 * On shared preferences and one key per switch, exactly as the shelves are stored (`ShelfCodec`),
 * and for the same reason: a key this version does not recognise, or one an older version never
 * wrote, reads back as the **default of the switch it names** instead of as an «off». The default of
 * «fotos» is on, so an absent key must not be allowed to hand the collector a notebook with no coins
 * in it.
 *
 * Nothing here is per card, so ADR 0021 §7 is untouched: how the notebook is printed is what the
 * collector is looking through, not something stored about a collection.
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

    /**
     * All six, always.
     *
     * The default is written as the default and not as an absence, so a switch chosen on purpose and
     * one never touched read back the same — which they are.
     */
    fun encode(options: NotebookOptions): Map<String, Boolean> =
        NotebookSwitch.entries.associate { switch -> key(switch) to options[switch] }

    fun decode(read: (String) -> Boolean?): NotebookOptions =
        NotebookSwitch.entries.fold(NotebookOptions()) { options, switch ->
            read(key(switch))?.let { stored -> options.with(switch, stored) } ?: options
        }
}
