package com.jenarvaezg.coindex.data.ficha

import com.jenarvaezg.coindex.data.numista.NumistaTypeDto
import com.jenarvaezg.coindex.domain.recordedDiameter
import com.jenarvaezg.coindex.domain.recordedText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The five things a stored ficha's body says that its columns never captured.
 *
 * Numista's response carries far more than the cache ever promoted to a column, and the whole body
 * is kept precisely so a later version can read a field this one ignored without spending API
 * budget again. These five were read that way — on every pass over the cache, five parses per row
 * — and the memo that made it affordable had to be keyed on `fetchedAt`, which is what forced
 * `MappersTest` to invent a different one per assertion (#221).
 *
 * Now the body is read **once, when the ficha arrives**, and what it said is a column. The body is
 * still stored, so nothing is frozen: [FICHA_READING] is how a better reading reaches the rows that
 * were written by a worse one.
 */
data class FichaReading(
    val issuerName: String? = null,
    val composition: String? = null,
    val sizeMillimetres: Double? = null,
    val category: String? = null,
    val numistaUrl: String? = null,
)

/**
 * Which reading of the body wrote a row's columns.
 *
 * **Bump this whenever [readFichaBody] would answer differently**, and every ficha already on the
 * phone is read again from the body it already stores — no migration, no API call. It is the same
 * bargain the five read-on-every-pass fields had, moved from every read to one write: what a
 * column costs is that improving the rule needs a pass, and this integer is that pass's trigger.
 */
const val FICHA_READING: Int = 1

/**
 * Reads one stored Numista body.
 *
 * The rules about what counts as a value — a blank name, a diameter of zero — are the domain's
 * ([recordedText], [recordedDiameter]); what belongs here is only where in the body each fact is
 * written, and that a body nobody can parse says nothing at all rather than throwing.
 *
 * One parse for the five facts, and an empty reading for a body that is not JSON.
 */
fun readFichaBody(raw: String): FichaReading {
    val body = runCatching { lenient.parseToJsonElement(raw).jsonObject }.getOrNull()
        ?: return FichaReading()
    return FichaReading(
        // Only the code is a column, and a code is `australie` even in a Spanish catalogue: the
        // name is the only issuer string a card can print.
        issuerName = recordedText(body.text("issuer", "name")),
        // The prose and not the metal: `inferMetal` is a rule, and a rule stored as a column could
        // never be improved. What is stored is what Numista wrote.
        composition = recordedText(body.text("composition", "text")),
        sizeMillimetres = recordedDiameter(body.number("size")),
        // `coin` or `exonumia`, the prose again rather than `objectClassOf`'s verdict.
        category = recordedText(body.text("category")),
        // Read and never built. `https://es.numista.com/$typeId` would be **our** guess about
        // Numista's host and about which language the ficha was asked in — printed onto paper,
        // where nobody can correct it.
        numistaUrl = recordedText(body.text("url")),
    )
}

/**
 * The small picture of each face, read from a ficha the same way wherever it is read from.
 *
 * The cache is written from two places — the snapshot on the way in, and the backfill over the
 * fichas already stored — and a plate is only whole if both agree on what a thumbnail is.
 *
 * From the parsed response rather than from the body, because the two writers both hold one; it is
 * beside [readFichaBody] all the same, since «what a ficha says» is one subject.
 */
data class FichaThumbnails(val obverse: String?, val reverse: String?) {
    val isEmpty: Boolean = obverse == null && reverse == null
}

fun NumistaTypeDto.thumbnails(): FichaThumbnails =
    FichaThumbnails(obverse = obverse?.thumbnail, reverse = reverse?.thumbnail)

private val lenient = Json { ignoreUnknownKeys = true }

/**
 * Each field is read on its own terms: a body where one of the five is the wrong shape still gives
 * up the other four, which is how these read before they shared a parse.
 */
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
