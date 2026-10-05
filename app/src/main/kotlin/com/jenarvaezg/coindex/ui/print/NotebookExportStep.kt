package com.jenarvaezg.coindex.ui.print

/**
 * What the notebook export is doing right now. The steps differ in whether the collector may
 * cancel: cancelling while [Writing] would close the document under the thread writing it.
 */
sealed interface NotebookExportStep {
    /**
     * Fetching every photograph before any page is drawn. The longest step, so it reports progress
     * in photographs.
     */
    data class Warming(val photographsDone: Int, val photographs: Int) : NotebookExportStep

    /** Drawing page [pagesDone] + 1 of the notebook, of the collection called [title]. */
    data class Drawing(val pagesDone: Int, val title: String) : NotebookExportStep

    /** Writing the finished notebook out and handing it over. Not cancellable. */
    data object Writing : NotebookExportStep
}
