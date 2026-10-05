package com.jenarvaezg.coindex.ui.print

import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.IndexCard

/**
 * What a notebook is of: the four entry points into the printer as a value, so a single producer
 * prints them all (#539). A subject only decides which cards go in; which page a card gets is still
 * `destinationOf` (ADR 0021 §9).
 */
sealed interface NotebookSubject {
    /**
     * Whether this subject prints as one lámina rather than as the notebook, and so under
     * `forSheetExport` (#401). True for every subject but the index.
     */
    val asSheet: Boolean get() = true

    /**
     * The whole notebook: the cards the index is showing and the coins no collection claims (#275),
     * both as the index screen filtered them (#147).
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
     * One curated plate, by catalog id (#401): the plate screen holds no [IndexCard], so the
     * printer finds the card itself and routes it like the index does (ADR 0021 §9).
     */
    data class Plate(val catalogId: String) : NotebookSubject

    /**
     * «La lista de lo que busco» (ADR 0029 §7): the marked casillas of every plate, in one lámina.
     * Its coins are on no index card, which is why subjects are a sealed type rather than a list of
     * cards.
     */
    data object Wishes : NotebookSubject
}
