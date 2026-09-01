package com.jenarvaezg.coindex.ui.print

import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.IndexCard

/**
 * What a notebook is **of**: the four doors into the printer, as a value (#539).
 *
 * There used to be four methods on the ViewModel producing pages — the index, one card, one plate,
 * the wish list — and three of them were the same call with `forSheetExport()` spelled again. The
 * cost of that was not the lines: `printPages`, `printGeometry` and the money switch were threaded
 * separately in two of them, so «the notebook is printed one way» was a coincidence maintained by
 * hand. With the subject as a value there is one producer, and what varies between the four doors is
 * declared here rather than reimplemented per door.
 *
 * **The paper does not gain a rule from this.** Which page a card gets is still `destinationOf` and
 * nothing else (`página(tarjeta) = su destino`, ADR 0021 §9); what a subject decides is only *which
 * cards go in*, which is the question the screen that pressed the button already answered.
 */
sealed interface NotebookSubject {
    /**
     * Whether this subject prints as one lámina rather than as the notebook.
     *
     * The **one** place `forSheetExport` is read (#401). Packing a folio and the loose-coins lámina
     * are switches that only mean something with neighbours, so every door but the index clears
     * them — and clearing them once here is what keeps a fifth door from arriving without the clause.
     */
    val asSheet: Boolean get() = true

    /**
     * The whole notebook: the cards the index is showing, and the coins no collection claims (#275).
     *
     * The one subject that is **not** a sheet, and the one that carries [unclaimed]: both lists are
     * the index screen's answers — filters and search included — so they come in rather than being
     * read off the state (#147).
     */
    data class Index(
        val cards: List<IndexCard>,
        val unclaimed: List<CollectedItem>,
    ) : NotebookSubject {
        override val asSheet: Boolean get() = false
    }

    /** One collection without a plate, or one of the collector's boxes, as its own sheet (#401). */
    data class Sheet(val card: IndexCard) : NotebookSubject

    /**
     * One curated plate, named by its catalog (#401).
     *
     * By id and not by card because the plate screen resolved an album and never held the [IndexCard]
     * the index drew — and the card is what has to reach the printer, so that a plate and its page
     * cannot disagree about which door the collection goes through (ADR 0021 §9).
     */
    data class Plate(val catalogId: String) : NotebookSubject

    /**
     * «La lista de lo que busco» (ADR 0029 §7): the marked casillas of every plate, in one lámina.
     *
     * The one subject whose coins are in no card of the index, which is why it could never be a
     * `cards` list — and the reason the four doors are a sealed type here instead of one signature
     * taking cards. Everything under it is shared: same geometry, same switches, same printer.
     */
    data object Wishes : NotebookSubject
}
