package com.jenarvaezg.coindex.ui.shelf

/**
 * How the notebook sheet is ordered (ADR 0026 §9). The country and year axes are the same sheet in
 * another order, not separate screens; by plate is the default.
 */
enum class NotebookAxis(val label: String) {
    ByPlate("Por lámina"),
    ByCountry("Por país"),
    ByYear("Por año"),
}
