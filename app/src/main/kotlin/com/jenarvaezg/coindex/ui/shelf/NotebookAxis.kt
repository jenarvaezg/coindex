package com.jenarvaezg.coindex.ui.shelf

/**
 * How the notebook sheet is ordered (ADR 0026 §9).
 *
 * Not a destination: the stain and the year axis are the same álbum sheet with another order
 * chosen in the folded shelf. The default is today's Collections — by plate — so the app still
 * opens the same and nobody has to learn anything.
 */
enum class NotebookAxis(val label: String) {
    ByPlate("Por lámina"),
    ByCountry("Por país"),
    ByYear("Por año"),
}
