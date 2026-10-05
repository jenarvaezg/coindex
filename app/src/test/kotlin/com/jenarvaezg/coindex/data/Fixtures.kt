package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.domain.CuratedFiles
import com.jenarvaezg.coindex.domain.CuratedSpecies
import com.jenarvaezg.coindex.domain.Curation
import java.io.File

/**
 * Recorded Numista responses from `fixtures/numista/`, so tests never touch the network. They are
 * only refreshed by a manual run of `scripts/record-fixture.py`.
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
 * The curated seeds read straight from `data/`: the suite's side of the loading seam of #545. The
 * phone's side is `AssetCuratedFiles`, over the same directories packed as APK assets.
 */
object RepoCuratedFiles : CuratedFiles {
    override fun read(species: CuratedSpecies): List<Pair<String, String>> =
        File("../data/${species.directory}").listFiles()
            .orEmpty()
            .filter { it.name.endsWith(".json") }
            .map { it.name to it.readText() }
}

/**
 * The shipped curation, parsed once for the whole suite through the app's own loader, so every
 * test reads a curation that passed the app's invariants.
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
