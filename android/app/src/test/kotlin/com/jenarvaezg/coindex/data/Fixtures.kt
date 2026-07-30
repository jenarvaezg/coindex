package com.jenarvaezg.coindex.data

import java.io.File

/**
 * The recorded Numista responses shared with the frozen Rust implementation.
 *
 * Tests never touch the network: every byte comes from `fixtures/numista/`, which is only
 * refreshed by a deliberate, manual recording run.
 */
object Fixtures {
    private val root = File("../../fixtures/numista")

    fun read(name: String): String {
        val file = File(root, name)
        require(file.exists()) { "falta el fixture ${file.absolutePath}" }
        return file.readText()
    }

    val oauthToken: String get() = read("oauth_token.json")
    val collectedItems: String get() = read("collected_items.json")
    fun type(typeId: Int): String = read("type_${typeId}_es.json")
}

/** The curated catalogs as they ship: read straight from `data/`, not from a copy. */
object CatalogFiles {
    private val root = File("../../data/collection-catalogs")

    fun all(): List<Pair<String, String>> = root.listFiles()
        .orEmpty()
        .filter { it.name.endsWith(".json") }
        .sortedBy { it.name }
        .map { it.name to it.readText() }
}
