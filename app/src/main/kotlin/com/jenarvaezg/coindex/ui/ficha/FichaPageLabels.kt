package com.jenarvaezg.coindex.ui.ficha

import com.jenarvaezg.coindex.data.ficha.CoinOrientation
import com.jenarvaezg.coindex.data.ficha.FichaReference
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.ui.dayMonthYearLabel
import com.jenarvaezg.coindex.ui.plural
import java.time.LocalDate
import java.util.Locale

/** The words of the coin's screen (#643, ADR 0026 §13). */
object FichaPageLabels {
    const val TURN_HINT = "tócala para darle la vuelta"
    const val EDGE_PHOTO = "Ver la foto del canto"
    const val EDGE_PHOTO_CAPTION = "El canto, en la foto de Numista · toca para cerrar"
    const val PIECE_PHOTO = "Tu foto de la pieza"
    const val PIECE_PHOTO_CAPTION = "Tu foto en Numista · toca para cerrar"
    const val MISSING = "Te falta"
    const val THIS_CASILLA = "· esta casilla"
    const val GREATEST = "el mayor"

    const val ONE_PIECE = "Tu pieza"
    const val COST = "Lo que costaría"
    const val COIN = "La moneda"
    const val MEDAL = "La medalla"
    const val FACES = "Las caras"
    const val COMMENTS = "Lo que cuenta Numista"
    const val OBVERSE = "Anverso"
    const val REVERSE = "Reverso"
    const val EDGE = "Canto"

    const val MARKET = "Catálogo de Numista"
    const val PAID = "Lo que pagaste"
    const val SILVER = "Lo que vale su plata"

    const val NUMISTA_HAS_NO_PRICE = "Numista no le pone precio"
    const val NUMISTA_NOT_ASKED = "Numista aún no la ha tasado"
    const val NO_ISSUE = "no anotaste su emisión"
    const val NOT_PAID = "no anotaste lo que pagaste"
    const val NO_SILVER = "no lleva plata"
    const val NO_FINE_SILVER = "la ficha no da su ley o su peso"
    const val NO_SPOT = "falta el precio de la plata"

    const val COST_SUMMARY = "Numista en unc · su plata"
    const val NO_YEAR = "sin año"
    const val FOR_SWAP = "para cambio"

    const val ROW_FACE_VALUE = "Valor facial"
    const val ROW_CURRENCY = "Moneda"
    const val ROW_RULER = "Gobernante"
    const val ROW_SERIES = "Serie"
    const val ROW_TOPIC = "Tema"
    const val ROW_YEARS = "Años"
    const val ROW_MINT = "Ceca"
    const val ROW_METAL = "Metal"
    const val ROW_WEIGHT = "Peso"
    const val ROW_FINE_SILVER = "Plata fina"
    const val ROW_DIAMETER = "Diámetro"
    const val ROW_THICKNESS = "Grosor"
    const val ROW_EDGE = "Canto"
    const val ROW_ORIENTATION = "Orientación"
    const val ROW_TECHNIQUE = "Técnica"
    const val ROW_LEGAL_TENDER = "Curso legal"
    const val ROW_HANDS = "Grabadores"
    const val ROW_REFERENCES = "Referencias"

    const val STILL_MONEY = "sigue siendo dinero"
    const val DEMONETIZED = "desmonetizada"

    /** Pluralised, unlike the Monedas' one-piece «Tu pieza». */
    fun pieces(count: Int): String = "Tus piezas · $count"

    fun related(count: Int): String = "Emparentadas · $count"

    /** «anverso · tócala para darle la vuelta», or the face alone when the coin can't turn. */
    fun faceUp(side: PrintedSide, turns: Boolean): String =
        listOfNotNull(sideName(side), TURN_HINT.takeIf { turns }).joinToString(" · ")

    fun sideName(side: PrintedSide): String = when (side) {
        PrintedSide.Obverse -> "anverso"
        PrintedSide.Reverse -> "reverso"
    }

    fun value(amount: String, origin: String): String = "Valor actual: $amount · $origin"

    const val GREATEST_OF_THREE = "al mayor de tres precios"

    fun holeCost(amount: String): String = "Te costaría: $amount · en sin circular"

    fun market(grade: String, neighbouring: Boolean): String =
        "$MARKET en $grade" + if (neighbouring) " (el grado vecino)" else ""

    fun fineSilverSource(grams: Double, spot: String): String = "${grams(grams)} finos · $spot"

    /** «pagaste 141,58 € el 22 jul 2026»: what was typed, to the cent, for the whole row. */
    fun paid(amount: Double, on: LocalDate?): String =
        "pagaste ${exactEuros(amount)}" + (on?.let { " el ${dayMonthYearLabel(it)}" } ?: "")

    fun arrived(on: LocalDate): String = "llegó el ${dayMonthYearLabel(on)}"

    fun mintage(count: Long): String = "tirada de ${thousands(count)}"

    fun quantity(count: Int): String = "×$count"

    fun issueComment(comment: String): String = "Numista: $comment"

    fun privateNote(note: String): String = "«$note»"

    /** «31 piezas en 21 fechas · 1879 – 1936». */
    fun piecesByDate(pieces: Int, dates: Int, span: String): String =
        "${plural(pieces, "pieza", "piezas")} en ${plural(dates, "fecha", "fechas")} · $span"

    fun millimetres(value: Double): String = "${trimmed(value, 2)} mm"

    fun grams(value: Double): String = "${trimmed(value, 2)} g"

    fun ounces(value: Double): String = "${trimmed(value, 2)} oz"

    fun orientation(orientation: CoinOrientation): String = when (orientation) {
        CoinOrientation.Medal -> "de medalla"
        CoinOrientation.Coin -> "de moneda"
        CoinOrientation.Variable -> "variable"
    }

    fun demonetizedOn(on: LocalDate): String = "$DEMONETIZED el ${dayMonthYearLabel(on)}"

    /** «KM# 1571 · Gomes# R 38», as Numista cites them. */
    fun references(references: List<FichaReference>): String =
        references.joinToString(" · ") { "${it.catalogue}# ${it.number}" }

    fun yearSpan(from: Int?, to: Int?): String? = when {
        from == null && to == null -> null
        from == null || to == null || from == to -> (from ?: to).toString()
        else -> "$from – $to"
    }
}

private val SPANISH = Locale.forLanguageTag("es-ES")

/** Up to [decimals] decimals, with the trailing zeros dropped: «39 mm», «38,61 mm», «2,2 mm». */
private fun trimmed(value: Double, decimals: Int): String =
    String.format(SPANISH, "%,.${decimals}f", value).trimEnd('0').trimEnd(',')

private fun thousands(value: Long): String = String.format(SPANISH, "%,d", value)

/** A paid price as it was typed: cents only when there are some. */
private fun exactEuros(amount: Double): String {
    val cents = Math.round(amount * 100)
    return if (cents % 100 == 0L) {
        "${thousands(cents / 100)} €"
    } else {
        "${String.format(SPANISH, "%,.2f", amount)} €"
    }
}
