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
object CatalogSeeds {
    private val json = Json {
        ignoreUnknownKeys = false
        explicitNulls = false
    }

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
