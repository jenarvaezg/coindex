package com.jenarvaezg.coindex.ui.print

/**
 * How the notebook comes out: five independent switches, and no models with a name (#228).
 *
 * There is a notebook and a way of printing it, and until #228 the second was one shape — the album
 * page that serves the shelf at home and not the list you take to a fair. Named models
 * (`Álbum 1:1` / `Compacto` / `Lista`) were considered and dropped: the collector wants to combine,
 * and the live page count of the export sheet makes protecting them from a combination unnecessary.
 *
 * **The defaults are the notebook of today**, exactly, so nobody finds their notebook changed
 * without having asked. What each switch does to the millimetres lands with its own ticket
 * (see [NotebookSwitch.pending]); what lands here is the door.
 */
data class NotebookOptions(
    /** The coins themselves. Off, the notebook is a checklist and no cell can come out blank. */
    val photographs: Boolean = true,
    /** The obverse beside the reverse, which at 1:1 doubles a cell's width. */
    val bothFaces: Boolean = false,
    /** A coin printed at its real diameter, which is what the ruler at the foot is there for. */
    val actualSize: Boolean = true,
    /** Two plates in one folio, which is where most of the paper is saved. */
    val sharePage: Boolean = false,
    /** The Numista page of each issue, as a code to point a phone at. */
    val numistaQr: Boolean = false,
) {
    operator fun get(switch: NotebookSwitch): Boolean = when (switch) {
        NotebookSwitch.Photographs -> photographs
        NotebookSwitch.BothFaces -> bothFaces
        NotebookSwitch.ActualSize -> actualSize
        NotebookSwitch.SharePage -> sharePage
        NotebookSwitch.NumistaQr -> numistaQr
    }

    fun with(switch: NotebookSwitch, on: Boolean): NotebookOptions = when (switch) {
        NotebookSwitch.Photographs -> copy(photographs = on)
        NotebookSwitch.BothFaces -> copy(bothFaces = on)
        NotebookSwitch.ActualSize -> copy(actualSize = on)
        NotebookSwitch.SharePage -> copy(sharePage = on)
        NotebookSwitch.NumistaQr -> copy(numistaQr = on)
    }

    /**
     * Whether [switch] is a question this configuration can even be asked.
     *
     * With the photographs off no coin reaches the page at all, so «ambas caras» and «tamaño real»
     * have nothing left to negotiate. The sheet greys them rather than leaving them ticked and
     * inert, which is the difference between a control that is unavailable and one that lies.
     */
    fun offers(switch: NotebookSwitch): Boolean = when (switch) {
        NotebookSwitch.BothFaces, NotebookSwitch.ActualSize -> photographs
        NotebookSwitch.Photographs, NotebookSwitch.SharePage, NotebookSwitch.NumistaQr -> true
    }
}

/**
 * The five switches, in the order the export sheet draws them.
 *
 * «Cabecera fina» is deliberately **not** one of them: it is derived from «compartir página», since
 * a band of forty millimetres per plate makes no sense once two of them share a folio — and that
 * derivation is where most of the saving comes from (104 pages become 90 sharing pages with today's
 * heading, 73 with the thin one).
 */
enum class NotebookSwitch(
    /**
     * The issue that will make this switch do something, or null once it does.
     *
     * #228 opens the door and leaves today's notebook intact behind it, so a switch is drawn and
     * persisted before it moves a millimetre. Rather than offer a control that silently does
     * nothing, the sheet greys it and names the ticket. **The ticket that lands sets this to null**,
     * and when all five are null the property has no reason left to exist.
     */
    val pending: Int?,
) {
    Photographs(pending = 231),
    BothFaces(pending = 230),
    ActualSize(pending = 233),
    SharePage(pending = 232),
    NumistaQr(pending = 234),
}

/**
 * The millimetres a configuration declares — the one place a switch becomes geometry.
 *
 * Every one of the five is a change to the arithmetic and not to the brush, so this is what the page
 * count is computed from and what the page is drawn with. Today it returns the notebook of #169 for
 * all thirty-two combinations, because each switch's line lands with its own ticket: this function
 * gaining a line is what «un interruptor funciona» will mean.
 */
@Suppress("UNUSED_PARAMETER")
fun printGeometry(options: NotebookOptions): PrintGeometry = PrintGeometry()

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
    }

    /**
     * All five, always.
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
