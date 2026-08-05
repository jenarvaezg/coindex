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
    /** What would be stored, trimmed. Only ever passed on when [canCreate]. */
    val stored: String,
    /** «13/40 · tiene que caber en una tarjeta»: the limit said as a fact, not as a warning. */
    val counter: String,
    /** What is wrong, in the collector's words, or null while there is nothing to say. */
    val problem: String?,
    val canCreate: Boolean,
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
        canCreate = trimmed.isNotEmpty() && problem == null,
    )
}
