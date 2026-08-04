package com.jenarvaezg.coindex.ui.print

/**
 * What the notebook export is doing right now.
 *
 * Two steps and not a page counter, because they differ in what the collector may do: while pages
 * are being drawn there is nothing to lose by stopping, and once the file is being written there is
 * nothing left to stop — the PDF is complete and on its way to the share sheet. Cancelling in that
 * window would close the document out from under the thread writing it.
 */
sealed interface NotebookExportStep {
    /** Drawing page [pagesDone] + 1 of the notebook, of the collection called [title]. */
    data class Drawing(val pagesDone: Int, val title: String) : NotebookExportStep

    /** Writing the finished notebook out and handing it over. Not cancellable. */
    data object Writing : NotebookExportStep
}
