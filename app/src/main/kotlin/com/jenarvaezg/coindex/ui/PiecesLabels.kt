package com.jenarvaezg.coindex.ui

/**
 * The one sentence a collection of pieces counts itself with.
 *
 * It normally counts what is in the house — «4 monedas · 3 tipos» — because a sheet of pieces only
 * ever draws what is owned, which is exactly why a collection without an issue list can have one at
 * all (ADR 0021 §9). The exception is the collection whose catalog it owns no issued member of yet:
 * it arrives carrying the card's ratio (§7) and has to keep saying «0 de 12 · te faltan 12».
 *
 * It is read off the subject and never spelled out again, because the four surfaces that count this
 * collection — the screen, the shared PNG, the notebook page and the message about the file that has
 * just been shared — cannot count differently. Three of them spelled the same expression out and one
 * of them spelled it wrong (#226).
 */
val PiecesSubject.countSentence: String
    get() = coverage?.let(::coverageLabel) ?: countLabel(distinctTypes, quantity)

/**
 * The masthead of the sheet of pieces: what the collection is, and how much of it there is.
 *
 * A pure list rather than a composable's body so a JVM test can read what the shared file says.
 * The heading of the sheet of pieces used to be a private composable nobody could reach, and that is
 * how the PNG came to count differently from the screen that shared it without a single test going
 * red (#226). The drawing it belonged to is gone since #431 — the PNG is the printed page now — and
 * the reason for keeping the words out here outlived it: paper reads this too.
 *
 * What nothing can name goes unsaid instead of printed blank: the country is silent when the pieces
 * disagree about it, and a box states no variant at all — it spans whatever the collector put in it.
 */
fun piecesSheetFacts(subject: PiecesSubject): List<Pair<String, String>> = buildList {
    subject.issuer?.let { issuer -> add("País" to issuer) }
    subject.variant?.let { variant -> add("Variante" to variant) }
    add("Piezas" to subject.countSentence)
}

/**
 * The one wording of a collection that is no longer there (ADR 0026 §5).
 *
 * There were **four**, in three files — 13, 16, 8 and 32 words — for one fact. The 32-word one went
 * for engineer's prose: it explained that refreshing a ficha can move its coins to another card,
 * which is true, is the reason the route broke, and is of no use whatsoever to somebody holding a
 * phone. The short one is the one kept, because the way out is the whole message: the collection is
 * gone, and the índice is where the ones that are left are.
 *
 * It reads correctly on the three screens that reach for it — a route with nothing behind it, a
 * derived collection whose last piece went, and a plate whose variant no longer describes anything —
 * because they are one event seen from three places.
 */
const val COLLECTION_NO_LONGER_EXISTS: String = "Esta colección ya no existe. Vuelve al índice."

/**
 * What a box with nothing in it says, which is **not** the sentence above.
 *
 * A box survives empty, because it is the one thing the collector typed and having it vanish would
 * read as data loss (ADR 0021 §11). A derived collection cannot: with no pieces there is nothing
 * left to derive it from.
 */
const val EMPTY_BOX_EXPLANATION: String =
    "Ahora mismo no tienes ninguna de las piezas de esta colección. Sigue aquí por si vuelven."

/** A link that describes no variant of this collection, which is not the same as one that is gone. */
const val UNKNOWN_VARIANT_LINK: String =
    "Ese enlace no describe ninguna variante de tu colección. Vuelve al índice."

const val PIECES_HEADING: String = "Tus piezas"
const val REMOVE_TYPE_FROM_COLLECTION: String = "Quitar de la colección"
const val DELETE_COLLECTION_ACTION: String = "Deshacer la colección"

fun renameToggleLabel(renaming: Boolean): String =
    if (renaming) "Cerrar el nombre" else "Renombrar"

/**
 * The field a box is named in, and the one word above it.
 *
 * Said the same way in the baptism and in the rename, because it is the same field with the same
 * limit and the same counter (ADR 0021 §4): a rename cannot produce a name the card could not hold.
 *
 * The eyebrow says **colección** and not «tu caja» (#516). It is the species word of ADR 0021 §2 and
 * the one the snackbar under this dialog already answers with, and «caja» was the word of provenance
 * §2 removed — said, of all places, on the gesture that makes one.
 */
const val BOX_NAME_FIELD_LABEL: String = "Cómo se llama"
const val BOX_NAME_SAVE_ACTION: String = "Guardar el nombre"
const val BOX_EYEBROW: String = "Tu colección"
const val BOX_CREATE_ACTION: String = "Crear"

/** Stays, because from Coins it is the only way to grow a box (ADR 0021 §11). */
const val BOX_ADD_TO_EXISTING: String = "O añádelas a una que ya tienes:"

/**
 * The heading of the baptism, which is the count and **not the species word again**.
 *
 * [BOX_EYEBROW] is one line above it, so «Una colección de 2 monedas» underneath would be the same
 * word twice on a card three lines tall — the frequency rule of ADR 0026 §5. What the heading owes
 * the collector is the size of what they are about to name, in the verb the refusal under the field
 * already uses («elige al menos una moneda»).
 */
fun boxDialogHeading(count: Int): String = plural(count, "moneda elegida", "monedas elegidas")

fun namePickedBoxLabel(count: Int): String = "Nombrar la colección · $count"

/**
 * The two forms of the button that makes one (ADR 0021 §11).
 *
 * The count rides in the label so **the cost is written on it before it is pressed**: seeding
 * unconditionally offered the whole collection — 191 coins — and what the collector needs to see is
 * that the filter wants narrowing first.
 *
 * It says **colección** and not «Agrupar» (#516). ADR 0021 §11 wrote the button as «Agrupar estas
 * 6», and that verb was the last of the three words this one feature used for one thing: the door
 * grouped, the mode filled a caja, and the snackbar announced a colección. The verb is the one that
 * had no owner — nothing else in the app agrupa anything — and the noun it leaves behind is the one
 * §2 already made the only species there is.
 */
fun boxDoorLabel(seeded: Boolean, shown: Int): String =
    if (seeded) "Hacer una colección con estas $shown" else "Hacer una colección"

/**
 * Which side the work starts from, said once and only while the mode is open.
 *
 * Without a seed the gesture is the card itself (border when picked) — there is no «Elegir»
 * control to name (#402).
 */
fun selectionHintLabel(seeded: Boolean, shown: Int): String =
    if (seeded) {
        "Vienen elegidas las $shown que enseñaba el filtro. Quita las que no."
    } else {
        "Toca cada moneda que quieras meter en la colección."
    }
