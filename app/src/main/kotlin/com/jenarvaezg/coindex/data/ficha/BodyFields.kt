package com.jenarvaezg.coindex.data.ficha

import com.jenarvaezg.coindex.domain.recordedText
import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

// Field readers for the stored bodies of fichas and collected rows. Each field is read on its own,
// so one malformed field doesn't lose the others.

internal val bodyJson = Json { ignoreUnknownKeys = true }

/** The body as an object, or null when it isn't JSON or isn't an object. */
internal fun bodyOf(raw: String): JsonObject? =
    runCatching { bodyJson.parseToJsonElement(raw).jsonObject }.getOrNull()

private inline fun <T> attempt(block: () -> T?): T? = runCatching(block).getOrNull()

internal fun JsonObject.objectAt(field: String): JsonObject? = attempt { this[field]?.jsonObject }

internal fun JsonObject.stringAt(field: String): String? =
    attempt { this[field]?.jsonPrimitive?.contentOrNull }

internal fun JsonObject.intAt(field: String): Int? =
    attempt { this[field]?.jsonPrimitive?.intOrNull }

internal fun JsonObject.longAt(field: String): Long? =
    attempt { this[field]?.jsonPrimitive?.longOrNull }

internal fun JsonObject.objectsAt(field: String): List<JsonObject> =
    attempt { this[field]?.jsonArray?.mapNotNull { attempt { it.jsonObject } } }.orEmpty()

/** A text field as the screen prints it: entities decoded, Windows line ends and «(en)» gone. */
internal fun JsonObject.plainAt(field: String): String? = recordedText(
    stringAt(field)?.let { untranslated(decodeEntities(it)).replace("\r\n", "\n").trim() },
)

internal fun JsonObject.plainAt(field: String, nested: String): String? =
    objectAt(field)?.plainAt(nested)

/** An ISO date, such as `acquisition_date`; null when absent or not a date. */
internal fun JsonObject.dateAt(field: String): LocalDate? =
    stringAt(field)?.let { attempt { LocalDate.parse(it) } }

/** Numista prefixes a text nobody translated with «(en)». */
internal fun untranslated(text: String): String = text.removePrefix(UNTRANSLATED).trimStart()

private const val UNTRANSLATED = "(en)"

/** The entities Numista leaves in plain fields, such as `Marked &quot;PLATA 1000&quot;`. */
internal fun decodeEntities(text: String): String =
    ENTITY.replace(text) { match ->
        val name = match.groupValues[1]
        when {
            name.startsWith("#x", ignoreCase = true) ->
                name.drop(2).toIntOrNull(16)?.let(::codePoint)
            name.startsWith("#") -> name.drop(1).toIntOrNull()?.let(::codePoint)
            else -> NAMED_ENTITIES[name]
        } ?: match.value
    }

private fun codePoint(code: Int): String? =
    runCatching { String(Character.toChars(code)) }.getOrNull()

private val ENTITY = Regex("&(#[0-9]+|#[xX][0-9a-fA-F]+|[a-zA-Z]+);")

private val NAMED_ENTITIES = mapOf(
    "quot" to "\"",
    "amp" to "&",
    "apos" to "'",
    "lt" to "<",
    "gt" to ">",
    "nbsp" to " ",
)
