package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.domain.Finish
import com.jenarvaezg.coindex.domain.UnclassifiedReason

/** `1000` reads as "1 oz", `250` as "0,25 oz", `804` as "0,804 oz". */
fun weightLabel(weightMillioz: Int): String {
    val whole = weightMillioz / 1_000
    val fraction = (weightMillioz % 1_000).toString().padStart(3, '0').trimEnd('0')
    return if (fraction.isEmpty()) "$whole oz" else "$whole,$fraction oz"
}

fun finishLabel(finish: Finish?): String = when (finish) {
    null -> "Por confirmar"
    Finish.Bullion -> "Bullion"
    Finish.Proof -> "Proof"
    Finish.Coloured -> "Coloreado"
    Finish.ProofColoured -> "Proof coloreado"
    Finish.Gilded -> "Dorado"
    Finish.Antiqued -> "Envejecido"
}

fun countLabel(distinctTypes: Int, quantity: Int): String {
    val types = if (distinctTypes == 1) "1 tipo distinto" else "$distinctTypes tipos distintos"
    val pieces = if (quantity == 1) "1 pieza" else "$quantity piezas"
    return "$types · $pieces"
}

/** Nothing is discarded in silence: every ungrouped piece says why. */
fun unclassifiedReasonLabel(reason: UnclassifiedReason): String = when (reason) {
    UnclassifiedReason.MissingTypeMetadata ->
        "Ficha del tipo sin descargar: se completará en el próximo sincronizado."
    is UnclassifiedReason.TechnicalFamily ->
        "Numista la agrupa en el sistema monetario técnico «${reason.family}», que no es una " +
            "familia coleccionable."
    UnclassifiedReason.NoFamilyOrCatalog ->
        "Sin familia en Numista y sin catálogo curado que la referencie: candidata a catálogo."
    is UnclassifiedReason.UnknownWeight ->
        "«${reason.family}» sin peso en Numista: no se puede identificar la variante física."
}

fun numistaTypeUrl(typeId: Int): String = "https://en.numista.com/catalogue/pieces$typeId.html"
