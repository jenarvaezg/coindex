package com.jenarvaezg.coindex.ui.print

/**
 * How the notebook comes out: independent switches rather than named presets (#228), so the
 * collector can combine them while the export sheet shows the page count live. The defaults are the
 * original 1:1 album of #169, so nobody's notebook changes without asking.
 */
data class NotebookOptions(
    /** Off, the notebook is a checklist and no cell can come out blank. */
    val photographs: Boolean = true,
    /** The obverse beside the reverse; at 1:1 it doubles a cell's width. */
    val bothFaces: Boolean = false,
    /** Coins at their real diameter, with the ruler at the foot to check it. */
    val actualSize: Boolean = true,
    /** More than one plate per folio, the switch that saves the most paper. */
    val sharePage: Boolean = false,
    /** The Numista page of each coin, as a code to point a phone at. */
    val numistaQr: Boolean = false,
    /**
     * The coins no collection claims, as one last lámina, so the notebook is the whole collection.
     */
    val unclaimed: Boolean = false,
    /**
     * What the collection is worth, on paper (ADR 0026 §10). Off by default. Off withdraws every
     * figure derived from money too: a share like «Venezuela · 30 % del valor» is money as much as
     * a total is.
     */
    val money: Boolean = false,
) {
    operator fun get(switch: NotebookSwitch): Boolean = when (switch) {
        NotebookSwitch.Photographs -> photographs
        NotebookSwitch.BothFaces -> bothFaces
        NotebookSwitch.ActualSize -> actualSize
        NotebookSwitch.SharePage -> sharePage
        NotebookSwitch.NumistaQr -> numistaQr
        NotebookSwitch.Unclaimed -> unclaimed
        NotebookSwitch.Money -> money
    }

    fun with(switch: NotebookSwitch, on: Boolean): NotebookOptions = when (switch) {
        NotebookSwitch.Photographs -> copy(photographs = on)
        NotebookSwitch.BothFaces -> copy(bothFaces = on)
        NotebookSwitch.ActualSize -> copy(actualSize = on)
        NotebookSwitch.SharePage -> copy(sharePage = on)
        NotebookSwitch.NumistaQr -> copy(numistaQr = on)
        NotebookSwitch.Unclaimed -> copy(unclaimed = on)
        NotebookSwitch.Money -> copy(money = on)
    }

    /**
     * Whether [switch] applies under this configuration; the sheet greys it otherwise rather than
     * leaving it ticked and inert. With the photographs off, «ambas caras» and «tamaño real» have
     * no coin to act on. Whether «Sin colección» has any coins to print depends on the collection,
     * not the switches, so the export sheet checks that itself (#275).
     */
    fun offers(switch: NotebookSwitch): Boolean = when (switch) {
        NotebookSwitch.BothFaces, NotebookSwitch.ActualSize -> photographs
        NotebookSwitch.Photographs, NotebookSwitch.SharePage, NotebookSwitch.NumistaQr -> true
        NotebookSwitch.Unclaimed -> true
        NotebookSwitch.Money -> true
    }
}

/**
 * The switches, in the order the export sheet draws them. The thin heading is not a switch: it
 * comes with «compartir página» (#232). What greys a switch is `NotebookOptions.offers`.
 */
enum class NotebookSwitch {
    /** Off, no coin on the page: a line per member, and no photographs to download (#231). */
    Photographs,

    /** The obverse beside the reverse, doubling the cell's width (#230). */
    BothFaces,

    /** Off, coins print at a fraction of their diameter, with the diameter as a number (#233). */
    ActualSize,

    /** Several plates per folio, under a 14 mm band instead of 40 (#232). */
    SharePage,

    /** Each coin gets a QR code to its Numista page (#234). */
    NumistaQr,

    /**
     * One last lámina with the coins no collection claims (#275). It changes what is printed rather
     * than how, so it goes after the layout switches, as its lámina goes last.
     */
    Unclaimed,

    /**
     * What the collection is worth, printed with it. ADR 0026 §10 calls it the sixth switch,
     * counting the five of #228 before #275 added [Unclaimed].
     */
    Money,
}

/**
 * The switches a single-lámina or hoja export offers (#401): all but «compartir página» and «Sin
 * colección», which only make sense across the index. The sheet reuses `ExportOptions` with these.
 */
fun sheetExportSwitches(): List<NotebookSwitch> = listOf(
    NotebookSwitch.Photographs,
    NotebookSwitch.BothFaces,
    NotebookSwitch.ActualSize,
    NotebookSwitch.NumistaQr,
    NotebookSwitch.Money,
)

/**
 * The options a single-sheet export prints under (#401): the stored notebook options with sharing
 * and the unclaimed lámina cleared, so a value left on from the index doesn't thin this heading.
 * The stored options aren't rewritten.
 */
fun NotebookOptions.forSheetExport(): NotebookOptions = copy(sharePage = false, unclaimed = false)
