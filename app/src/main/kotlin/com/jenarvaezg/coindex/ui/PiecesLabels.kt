package com.jenarvaezg.coindex.ui

/**
 * The sentence a collection of pieces counts itself with: what is owned, «4 monedas · 3 tipos»
 * (ADR 0021 §9), except for a catalog with no owned issued member yet, which keeps the card's
 * ratio, «0 de 12 · te faltan 12» (§7). Every surface that counts it reads this, so they can't
 * disagree (#226).
 */
val PiecesSubject.countSentence: String
    get() = coverage?.let(::coverageLabel) ?: countLabel(distinctTypes, quantity)

/**
 * The one wording of a collection that is no longer there (ADR 0026 §5), for a route with nothing
 * behind it, a derived collection whose last piece went, and a plate whose variant describes
 * nothing any more.
 */
const val COLLECTION_NO_LONGER_EXISTS: String = "Esta colección ya no existe. Vuelve al índice."

/**
 * What an empty box says. Unlike a derived collection, a box survives empty: the collector typed
 * it, and its vanishing would read as data loss (ADR 0021 §11).
 */
const val EMPTY_BOX_EXPLANATION: String =
    "Ahora mismo no tienes ninguna de las piezas de esta colección. Sigue aquí por si vuelven."

/** A link that describes no variant, as opposed to a collection that is gone. */
const val UNKNOWN_VARIANT_LINK: String =
    "Ese enlace no describe ninguna variante de tu colección. Vuelve al índice."

const val PIECES_HEADING: String = "Tus piezas"
const val REMOVE_TYPE_FROM_COLLECTION: String = "Quitar de la colección"
const val DELETE_COLLECTION_ACTION: String = "Deshacer la colección"

fun renameToggleLabel(renaming: Boolean): String =
    if (renaming) "Cerrar el nombre" else "Renombrar"

/**
 * The field a box is named in, the same when creating and renaming, with the same limit and counter
 * (ADR 0021 §4). The eyebrow says «colección», not «tu caja»: one species of collection
 * (ADR 0021 §2, #516).
 */
const val BOX_NAME_FIELD_LABEL: String = "Cómo se llama"
const val BOX_NAME_SAVE_ACTION: String = "Guardar el nombre"
const val BOX_EYEBROW: String = "Tu colección"
const val BOX_CREATE_ACTION: String = "Crear"

/** From Coins, the only way to grow a box (ADR 0021 §11). */
const val BOX_ADD_TO_EXISTING: String = "O añádelas a una que ya tienes:"

/**
 * The naming dialog's heading: the count, since [BOX_EYEBROW] already says «colección» right above
 * (ADR 0026 §5). «Elegida» matches the refusal under the field («elige al menos una moneda»).
 */
fun boxDialogHeading(count: Int): String = plural(count, "moneda elegida", "monedas elegidas")

fun namePickedBoxLabel(count: Int): String = "Nombrar la colección · $count"

/**
 * The button that makes a box (ADR 0021 §11). The count is on it before it is pressed, so a filter
 * that still shows the whole collection is visibly too wide. «Colección», not «Agrupar» (#516),
 * which ADR 0021 §11 originally wrote.
 */
fun boxDoorLabel(seeded: Boolean, shown: Int): String =
    if (seeded) "Hacer una colección con estas $shown" else "Hacer una colección"

/**
 * Shown while selecting, saying where the selection starts. Without a seed, tapping a card picks
 * it; there is no «Elegir» control to name (#402).
 */
fun selectionHintLabel(seeded: Boolean, shown: Int): String =
    if (seeded) {
        "Vienen elegidas las $shown que enseñaba el filtro. Quita las que no."
    } else {
        "Toca cada moneda que quieras meter en la colección."
    }
