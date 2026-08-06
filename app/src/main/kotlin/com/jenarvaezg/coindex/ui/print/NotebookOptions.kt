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
    /** The Numista page of each coin, as a code to point a phone at. */
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

    /** The first one that works: the caption grows and each coin gets its code (#234). */
    NumistaQr(pending = null),
}
