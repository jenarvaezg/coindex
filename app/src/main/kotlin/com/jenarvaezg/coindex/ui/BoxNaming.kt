package com.jenarvaezg.coindex.ui

/** The hard limit on the one name a collector types (ADR 0021 §4). */
const val BOX_NAME_LIMIT: Int = 40

/**
 * What the naming field of a box knows about what has been typed so far.
 *
 * The collector types **one** name, and it is the `short_name`: `name == short_name`, because a box
 * enumerates by hand and has no editorial scope to define (ADR 0021 §4). One field is also what makes
 * the prefix rule of #22 hold by construction rather than by validation — there is no long name for a
 * short one to fail to be a prefix of.
 */
data class BoxName(
    /** What would be stored, trimmed. Only ever passed on when [canSave]. */
    val stored: String,
    /** «13/40 · tiene que caber en una tarjeta»: the limit said as a fact, not as a warning. */
    val counter: String,
    /** What is wrong, in the collector's words, or null while there is nothing to say. */
    val problem: String?,
    val canSave: Boolean,
)

/**
 * Reads a half-typed name against the names already taken.
 *
 * **Uniqueness is checked here and only at creation** (ADR 0021 §11). Two homonymous cards in the
 * index are the signal that curation has covered what the collector noted down by hand, and the box is
 * undone with one tap — so a later file that collides is not policed, and this never runs again after
 * the box exists.
 *
 * The comparison ignores accents and case, because «Las Francesas» and «las francesas» are the same
 * shelf in anybody's head and two cards a letter apart would be the collector's own filing mistake
 * rather than a distinction. It is the same [fold] the search box uses.
 *
 * An empty field is **not** a complaint. «Crear» is off because there is nothing to create, and
 * scolding somebody for not having typed yet is the one message a dialog can be sure is premature.
 *
 * @param taken every curated `short_name` plus the names of the other boxes
 */
fun boxName(typed: String, taken: Collection<String>): BoxName {
    val trimmed = typed.trim()
    val clash = taken.firstOrNull { fold(it) == fold(trimmed) }
    val problem = when {
        trimmed.isEmpty() -> null
        trimmed.length > BOX_NAME_LIMIT ->
            "Son ${trimmed.length} caracteres y el límite son $BOX_NAME_LIMIT: tiene que caber " +
                "en una tarjeta."
        clash != null -> "Ya hay una colección que se llama «$clash». Ponle otro nombre."
        else -> null
    }
    return BoxName(
        stored = trimmed,
        counter = "${trimmed.length}/$BOX_NAME_LIMIT · tiene que caber en una tarjeta",
        problem = problem,
        canSave = trimmed.isNotEmpty() && problem == null,
    )
}

/**
 * What is to be stored about a box, or the sentence that refuses it.
 *
 * The last line of defence rather than the first: [boxName] has already read the name as it was
 * being typed, and this is what a gesture that got past it arrives at. A refusal here is a message
 * and never a silent no-op — a heading over nothing is not something to store, and a button that
 * did nothing at all would read as a bug.
 */
sealed interface BoxEntry {
    /**
     * The name as it would be stored. Not the coins: the caller has them in its hand — a selection
     * in Coins, or a box that already exists — and carrying them through the decision would only
     * hand them back.
     */
    data class Accepted(val name: String) : BoxEntry

    data class Refused(val message: String) : BoxEntry
}

/**
 * Creating a box: a name and at least one coin (ADR 0013, ADR 0021 §11).
 *
 * It says «colección» and not «agrupación», because there is one species of collection and no word
 * of provenance telling a box from the rest (ADR 0021 §2).
 */
fun boxToCreate(typed: String, typeIds: List<Int>): BoxEntry {
    val trimmed = typed.trim()
    if (trimmed.isEmpty() || typeIds.isEmpty()) {
        return BoxEntry.Refused("Ponle un nombre a la colección y elige al menos una moneda.")
    }
    return BoxEntry.Accepted(trimmed)
}

/**
 * Renaming one, where the coins are whatever they already were.
 *
 * Uniqueness is **not** checked: it is read once, at creation, and a name that collides later is
 * curation catching up with what the collector noted down by hand (ADR 0021 §11).
 */
fun boxToRename(typed: String): BoxEntry {
    val trimmed = typed.trim()
    if (trimmed.isEmpty()) {
        return BoxEntry.Refused("El nombre de la colección no puede estar vacío.")
    }
    return BoxEntry.Accepted(trimmed)
}

/** Said once the box exists, in the collector's own name for it. */
fun boxCreatedMessage(name: String): String = "Colección «$name» creada."
