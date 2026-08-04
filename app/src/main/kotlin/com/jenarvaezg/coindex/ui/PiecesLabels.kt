package com.jenarvaezg.coindex.ui

/**
 * What the collector is told once a sheet of pieces has been handed to the share sheet.
 *
 * The plate's own message counts «casillas», because a plate can draw a slot the mint has not
 * struck. This one counts **piezas**: a sheet of pieces only ever draws what is owned, which is
 * exactly why a collection without an issue list can have one at all (ADR 0021 §9).
 */
fun piecesExportMessage(
    distinctTypes: Int,
    quantity: Int,
    expectedPhotos: Int,
    loadedPhotos: Int,
): String {
    val absent = (expectedPhotos - loadedPhotos).coerceAtLeast(0)
    return when (absent) {
        // The same sentence the sheet itself prints under the title, so the message and the file
        // that has just been shared cannot count differently.
        0 -> "Hoja completa exportada · ${countLabel(distinctTypes, quantity)}"
        1 -> "Hoja exportada, pero una foto no llegó a cargar"
        else -> "Hoja exportada, pero $absent fotos no llegaron a cargar"
    }
}
