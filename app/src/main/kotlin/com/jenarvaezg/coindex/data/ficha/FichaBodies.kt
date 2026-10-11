package com.jenarvaezg.coindex.data.ficha

/**
 * What the coin's screen reads from the stored bodies of one type and of its rows (#643), by row
 * id. Read when the screen opens, at no consulta.
 */
data class FichaBodies(
    val details: FichaDetails = FichaDetails(),
    val pieces: Map<Long, PieceReading> = emptyMap(),
)
