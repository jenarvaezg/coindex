package com.jenarvaezg.coindex.domain

import kotlinx.serialization.json.Json

/** A seeded catalog file that could not be trusted. Loading must fail loudly, never degrade. */
class CatalogSeedException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)

/**
 * Parses and validates the curated catalog seeds that ship with the app.
 *
 * Unknown JSON fields are rejected on purpose: a typo in a curated file is a bug to be seen
 * at startup, not a field silently ignored.
 */
private val strictJson = Json {
    ignoreUnknownKeys = false
    explicitNulls = false
}

object CatalogSeeds {
    private val json = strictJson

    fun parse(fileName: String, contents: String): CollectionCatalog {
        val catalog = try {
            json.decodeFromString<CollectionCatalog>(contents)
        } catch (error: IllegalArgumentException) {
            throw CatalogSeedException("cannot parse seed $fileName: ${error.message}", error)
        }
        catalog.validate()?.let { invalid ->
            throw CatalogSeedException("collection catalog seed $fileName is invalid: ${invalid.message}")
        }
        return catalog
    }

    /** Parses every seed and rejects duplicate catalog ids across files. */
    fun parseAll(files: List<Pair<String, String>>): List<CollectionCatalog> {
        val ids = mutableSetOf<String>()
        return files.map { (fileName, contents) ->
            val catalog = parse(fileName, contents)
            if (!ids.add(catalog.id)) {
                throw CatalogSeedException("collection catalog id `${catalog.id}` is duplicated")
            }
            catalog
        }
    }
}

/**
 * Parses and validates the curated grouping seeds (ADR 0013), under the same rules as the
 * catalogs: unknown fields are rejected and a bad file stops the app instead of degrading.
 */
object GroupingSeeds {
    fun parse(fileName: String, contents: String): CuratedGrouping {
        val grouping = try {
            strictJson.decodeFromString<CuratedGrouping>(contents)
        } catch (error: IllegalArgumentException) {
            throw CatalogSeedException("cannot parse seed $fileName: ${error.message}", error)
        }
        grouping.validate()?.let { invalid ->
            throw CatalogSeedException(
                "curated grouping seed $fileName is invalid: ${invalid.message}",
            )
        }
        return grouping
    }

    /**
     * Parses every seed and rejects duplicate grouping ids, and any type named by two
     * groupings: a piece has exactly one family, so two groupings claiming it is a curation
     * mistake rather than a precedence question to resolve at runtime.
     */
    fun parseAll(files: List<Pair<String, String>>): List<CuratedGrouping> {
        val ids = mutableSetOf<String>()
        val typeIds = mutableSetOf<Int>()
        return files.map { (fileName, contents) ->
            val grouping = parse(fileName, contents)
            if (!ids.add(grouping.id)) {
                throw CatalogSeedException("curated grouping id `${grouping.id}` is duplicated")
            }
            for (typeId in grouping.typeIds) {
                if (!typeIds.add(typeId)) {
                    throw CatalogSeedException(
                        "Numista type `$typeId` is claimed by more than one curated grouping",
                    )
                }
            }
            grouping
        }
    }
}
