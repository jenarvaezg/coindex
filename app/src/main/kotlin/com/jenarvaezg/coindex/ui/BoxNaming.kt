package com.jenarvaezg.coindex.ui

/** The hard limit on the one name a collector types (ADR 0021 §4). */
const val BOX_NAME_LIMIT: Int = 40

/**
 * The state of a box's naming field. A box has one name, which is also its `short_name`
 * (ADR 0021 §4), so the prefix rule of #22 holds by construction.
 */
data class BoxName(
    /** Trimmed; used only when [canSave]. */
    val stored: String,
    val counter: String,
    /** Null while there is nothing to object to, including an empty field. */
    val problem: String?,
    val canSave: Boolean,
)

/**
 * Reads a half-typed name against the names already taken, ignoring accents and case ([fold]).
 *
 * Uniqueness is checked only here, at creation (ADR 0021 §11): a curated file that later takes the
 * same name signals that curation has caught up with the box, which is then undone with one tap.
 * An empty field gets no message; «Crear» is simply disabled.
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
 * What is to be stored about a box, or the message refusing it. [boxName] validates while typing;
 * this is the last check, and a refusal is always a message so the button never silently does
 * nothing.
 */
sealed interface BoxEntry {
    /** Only the name: the caller already holds the coins. */
    data class Accepted(val name: String) : BoxEntry

    data class Refused(val message: String) : BoxEntry
}

/**
 * Creating a box needs a name and at least one coin (ADR 0013, ADR 0021 §11). The copy says
 * «colección», never «agrupación»: boxes and curated collections share one word (ADR 0021 §2).
 */
fun boxToCreate(typed: String, typeIds: List<Int>): BoxEntry {
    val trimmed = typed.trim()
    if (trimmed.isEmpty() || typeIds.isEmpty()) {
        return BoxEntry.Refused("Ponle un nombre a la colección y elige al menos una moneda.")
    }
    return BoxEntry.Accepted(trimmed)
}

/** Renaming keeps the coins. Uniqueness is not checked again (ADR 0021 §11). */
fun boxToRename(typed: String): BoxEntry {
    val trimmed = typed.trim()
    if (trimmed.isEmpty()) {
        return BoxEntry.Refused("El nombre de la colección no puede estar vacío.")
    }
    return BoxEntry.Accepted(trimmed)
}

fun boxCreatedMessage(name: String): String = "Colección «$name» creada."
