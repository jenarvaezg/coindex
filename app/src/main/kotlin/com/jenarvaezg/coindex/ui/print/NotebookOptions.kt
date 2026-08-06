package com.jenarvaezg.coindex.ui.print

/**
 * How the notebook comes out: six independent switches, and no models with a name (#228).
 *
 * There is a notebook and a way of printing it, and until #228 the second was one shape — the album
 * page that serves the shelf at home and not the list you take to a fair. Named models
 * (`Álbum 1:1` / `Compacto` / `Lista`) were considered and dropped: the collector wants to combine,
 * and the live page count of the export sheet makes protecting them from a combination unnecessary.
 *
 * **The defaults are the notebook of today**, exactly, so nobody finds their notebook changed
 * without having asked. What each switch does to the millimetres landed with its own ticket, one at a
 * time, and all five have (#230-#234); what lands here is the door.
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
    /** The coins no collection claims, as one last lámina, and the notebook is the whole of it. */
    val unclaimed: Boolean = false,
) {
    operator fun get(switch: NotebookSwitch): Boolean = when (switch) {
        NotebookSwitch.Photographs -> photographs
        NotebookSwitch.BothFaces -> bothFaces
        NotebookSwitch.ActualSize -> actualSize
        NotebookSwitch.SharePage -> sharePage
        NotebookSwitch.NumistaQr -> numistaQr
        NotebookSwitch.Unclaimed -> unclaimed
    }

    fun with(switch: NotebookSwitch, on: Boolean): NotebookOptions = when (switch) {
        NotebookSwitch.Photographs -> copy(photographs = on)
        NotebookSwitch.BothFaces -> copy(bothFaces = on)
        NotebookSwitch.ActualSize -> copy(actualSize = on)
        NotebookSwitch.SharePage -> copy(sharePage = on)
        NotebookSwitch.NumistaQr -> copy(numistaQr = on)
        NotebookSwitch.Unclaimed -> copy(unclaimed = on)
    }

    /**
     * Whether [switch] is a question this configuration can even be asked.
     *
     * With the photographs off no coin reaches the page at all, so «ambas caras» and «tamaño real»
     * have nothing left to negotiate. The sheet greys them rather than leaving them ticked and
     * inert, which is the difference between a control that is unavailable and one that lies.
     *
     * **«Sin colección» is not answered here** (#275), because what makes it moot is not the
     * configuration but the collection: a phone where every coin is in some collection, or a filter
     * that leaves no loose coin standing, has no lámina to offer. That count is the export sheet's
     * to hold, so this stays a function of the switches alone and the sheet ands the two.
     */
    fun offers(switch: NotebookSwitch): Boolean = when (switch) {
        NotebookSwitch.BothFaces, NotebookSwitch.ActualSize -> photographs
        NotebookSwitch.Photographs, NotebookSwitch.SharePage, NotebookSwitch.NumistaQr -> true
        NotebookSwitch.Unclaimed -> true
    }
}

/**
 * The six switches, in the order the export sheet draws them.
 *
 * «Cabecera fina» is deliberately **not** one of them: it is derived from «compartir página», since
 * a band of forty millimetres per plate makes no sense once two of them share a folio — and that
 * derivation is where most of the saving comes from (104 pages become 90 sharing pages with today's
 * heading, 73 with the thin one).
 *
 * **All five do something now**, and the `pending` property is gone with the last of them (#233). While
 * a ticket was outstanding the switch was drawn, remembered and disabled with its issue named under it,
 * because the one thing a control must never be is tickable and inert; a property that can only ever be
 * null is the same lie in the other direction. What greys a switch from here on is the configuration
 * itself — `NotebookOptions.offers` — and that resolves the moment the collector changes their mind.
 */
enum class NotebookSwitch {
    /** No coin on the page at all: a line per member, and nothing left to warm (#231). */
    Photographs,

    /** The obverse beside the reverse, which doubles the cell and halves the page (#230). */
    BothFaces,

    /** The coin at a fraction of its diameter, with the ruler traded for a number (#233). */
    ActualSize,

    /** Two plates in one folio, under a band of fourteen millimetres instead of forty (#232). */
    SharePage,

    /** The caption grows and each coin gets its code (#234). */
    NumistaQr,

    /**
     * One last lámina with the coins no collection claims, and the notebook is the whole collection
     * (#275).
     *
     * The only one of the six that adds pages rather than rearranging them, and the only one that is
     * about **what** is printed rather than about how. It is last because the lámina is last.
     */
    Unclaimed,
}
