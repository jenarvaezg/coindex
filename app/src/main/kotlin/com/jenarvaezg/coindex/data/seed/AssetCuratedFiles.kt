package com.jenarvaezg.coindex.data.seed

import android.content.res.AssetManager
import com.jenarvaezg.coindex.domain.CatalogSeedException
import com.jenarvaezg.coindex.domain.CuratedFiles
import com.jenarvaezg.coindex.domain.CuratedSpecies

/**
 * The curated files as the APK ships them, for the loader of #545. A missing directory is a broken
 * build and throws here; an empty one is refused by the loader, as in the test suite. Either way it
 * fails loudly: a silently dropped catalog, grouping or programme would show wrong states and
 * counts.
 */
class AssetCuratedFiles(private val assets: AssetManager) : CuratedFiles {
    override fun read(species: CuratedSpecies): List<Pair<String, String>> {
        val directory = species.directory
        val files = assets.list(directory)?.filter { it.endsWith(".json") }
            ?: throw CatalogSeedException("no se encontró el directorio de assets `$directory`")
        return files.map { fileName ->
            fileName to assets.open("$directory/$fileName").use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            }
        }
    }
}
