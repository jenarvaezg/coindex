package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.domain.CuratedFiles
import com.jenarvaezg.coindex.domain.CuratedSpecies
import com.jenarvaezg.coindex.domain.Curation
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

/**
 * The curated seeds as they ship: read straight from `data/`, not from a copy.
 *
 * The suite's side of the loading seam of #545 — the phone's is `AssetCuratedFiles`, over the very
 * same directories, because `data/` is an asset source directory of the APK.
 */
object RepoCuratedFiles : CuratedFiles {
    override fun read(species: CuratedSpecies): List<Pair<String, String>> =
        File("../data/${species.directory}").listFiles()
            .orEmpty()
            .filter { it.name.endsWith(".json") }
            .map { it.name to it.readText() }
}

/**
 * The shipped curation, through the app's own door and therefore under its own invariants.
 *
 * One curation for the whole suite, and one parse: reading every curated file once per test class
 * was the same bytes decoded twenty times over. Whatever a test asks of it — the catalogs, the
 * groupings, the programmes — it asks of a curation that could not have been built at all if a
 * catalog and a grouping read the same on two cards.
 */
val SHIPPED_CURATION: Curation by lazy { Curation.load(RepoCuratedFiles) }

/** The seeded type cache as it ships, read from `data/` like the curated seeds. */
object TypeCacheFile {
    fun read(): String = File("../data/numista-type-cache.json").readText()
}

/** The curated orphans register (#133), editorial and not loaded at app startup. */
object OrphanFile {
    fun read(): String = File("../data/orphans.json").readText()
}
