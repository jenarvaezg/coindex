package com.jenarvaezg.coindex.data.ficha

import com.jenarvaezg.coindex.data.numista.NumistaTypeDto
import com.jenarvaezg.coindex.domain.recordedDiameter
import com.jenarvaezg.coindex.domain.recordedText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * What a stored ficha's body says beyond the cache's original columns, read once when the ficha
 * arrives and stored as columns (#221). The body is kept too, so [FICHA_READING] can bring a better
 * reading to rows written by an older one.
 */
data class FichaReading(
    val issuerName: String? = null,
    val composition: String? = null,
    val sizeMillimetres: Double? = null,
    val category: String? = null,
    val numistaUrl: String? = null,
    /** Read for «Las cifras» (ADR 0028 §7), from fichas the APK already ships. */
    val thicknessMillimetres: Double? = null,
    val demonetized: Boolean? = null,
    val hands: List<String> = emptyList(),
    val mints: List<String> = emptyList(),
    /**
     * The year a medal was issued (#460). Coin types carry `min_year` and `max_year`; medals carry
     * neither and write their date only in `issue_terms.issue_date`.
     */
    val issuedYear: Int? = null,
)

/**
 * Which reading of the body wrote a row's columns. Bump it whenever [readFichaBody] would answer
 * differently, and every cached ficha is read again from its stored body, with no migration and no
 * API call.
 */
const val FICHA_READING: Int = 3

/**
 * Reads one stored Numista body in one parse. What counts as a value (a blank name, a zero
 * diameter) is the domain's rule ([recordedText], [recordedDiameter]); this only knows where each
 * fact is written. A body that isn't JSON gives an empty reading rather than throwing.
 */
fun readFichaBody(raw: String): FichaReading {
    val body = runCatching { lenient.parseToJsonElement(raw).jsonObject }.getOrNull()
        ?: return FichaReading()
    return FichaReading(
        // The code is `australie` even in a Spanish catalogue; the name is what a card can print.
        issuerName = recordedText(body.text("issuer", "name")),
        // The prose, not the metal, so `inferMetal` stays a rule that can be improved.
        composition = recordedText(body.text("composition", "text")),
        sizeMillimetres = recordedDiameter(body.number("size")),
        // `coin` or `exonumia`, again the prose rather than `objectClassOf`'s verdict.
        category = recordedText(body.text("category")),
        // Read, never built: a guessed host or language would end up printed on paper.
        numistaUrl = recordedText(body.text("url")),
        // A zero thickness is no thickness, as with the diameter.
        thicknessMillimetres = recordedDiameter(body.number("thickness")),
        // Absent is not false: silence must not count as «still legal tender».
        demonetized = runCatching {
            body["demonetization"]?.jsonObject?.get("is_demonetized")?.jsonPrimitive?.booleanOrNull
        }.getOrNull(),
        // Engravers and designers together: Numista files the same person under either key
        // depending on who typed the ficha.
        hands = body.names("engravers") + body.names("designers"),
        mints = body.namedList("mints"),
        issuedYear = issuedYear(body.text("issue_terms", "issue_date")),
    )
}

/**
 * The thumbnail of each face. Both the snapshot and the backfill write it to the cache, so both
 * read it here, from the parsed response they each hold.
 */
data class FichaThumbnails(val obverse: String?, val reverse: String?) {
    val isEmpty: Boolean = obverse == null && reverse == null
}

fun NumistaTypeDto.thumbnails(): FichaThumbnails =
    FichaThumbnails(obverse = obverse?.thumbnail, reverse = reverse?.thumbnail)

/**
 * The year of a Numista issue date, or null. Medals write it as `1995-00-00`, with zeros for an
 * unknown month and day.
 */
private fun issuedYear(issueDate: String?): Int? = issueDate
    ?.substringBefore('-')
    ?.toIntOrNull()
    ?.takeIf { it > 0 }

private val lenient = Json { ignoreUnknownKeys = true }

/** Each field is read on its own, so one malformed field doesn't lose the others. */
private fun JsonObject.text(field: String): String? = runCatching {
    this[field]?.jsonPrimitive?.contentOrNull
}.getOrNull()

private fun JsonObject.number(field: String): Double? = runCatching {
    this[field]?.jsonPrimitive?.doubleOrNull
}.getOrNull()

/** A string one object down, or null if any step of the way is missing or not a string. */
private fun JsonObject.text(field: String, nested: String): String? = runCatching {
    this[field]?.jsonObject?.get(nested)?.jsonPrimitive?.contentOrNull
}.getOrNull()

/**
 * The names in a string array under each face, in order and without duplicates: `engravers` and
 * `designers` are per side, and a hand credited on both faces counts once.
 */
private fun JsonObject.names(field: String): List<String> = SIDES
    .flatMap { side ->
        runCatching {
            this[side]?.jsonObject?.get(field)?.jsonArray?.mapNotNull {
                it.jsonPrimitive.contentOrNull
            }
        }.getOrNull().orEmpty()
    }
    .mapNotNull(::recordedText)
    .distinct()

/** The `name` of every object in a top-level array, such as `mints`. */
private fun JsonObject.namedList(field: String): List<String> = runCatching {
    this[field]?.jsonArray?.mapNotNull { entry ->
        entry.jsonObject["name"]?.jsonPrimitive?.contentOrNull
    }
}.getOrNull().orEmpty().mapNotNull(::recordedText).distinct()

private val SIDES = listOf("obverse", "reverse")
