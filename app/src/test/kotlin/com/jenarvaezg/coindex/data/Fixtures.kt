package com.jenarvaezg.coindex.data

import java.io.File

/**
 * The recorded Numista responses.
 *
 * Tests never touch the network: every byte comes from `fixtures/numista/`, which is only
 * refreshed by a deliberate, manual run of `scripts/record-fixture.py`.
 */
object Fixtures {
    private val root = File("../fixtures/numista")

    fun read(name: String): String {
        val file = File(root, name)
        require(file.exists()) { "falta el fixture ${file.absolutePath}" }
        return file.readText()
    }

    val oauthToken: String get() = read("oauth_token.json")
    val collectedItems: String get() = read("collected_items.json")
    fun type(typeId: Int): String = read("type_${typeId}_es.json")
}

/** The curated seeds as they ship: read straight from `data/`, not from a copy. */
private fun seedFiles(directory: String): List<Pair<String, String>> =
    File("../data/$directory").listFiles()
        .orEmpty()
        .filter { it.name.endsWith(".json") }
        .sortedBy { it.name }
        .map { it.name to it.readText() }

object CatalogFiles {
    fun all(): List<Pair<String, String>> = seedFiles("collection-catalogs")
}

/** The seeded type cache as it ships, read from `data/` like the curated seeds. */
object TypeCacheFile {
    fun read(): String = File("../data/numista-type-cache.json").readText()
}

object GroupingFiles {
    fun all(): List<Pair<String, String>> = seedFiles("groupings")
}
