package com.jenarvaezg.coindex.data.seed

import android.content.res.AssetManager
import com.jenarvaezg.coindex.domain.CatalogSeedException
import com.jenarvaezg.coindex.domain.CuratedFiles
import com.jenarvaezg.coindex.domain.CuratedSpecies

/**
 * The curated files as the APK ships them: one side of the loading seam of #545.
 *
 * Every `numista_type_id` in them was verified against numista.com before being committed, which
 * makes them the most expensive asset in the project to reproduce. A directory that is not there at
 * all is a broken build and says so here, before anything is parsed; that it brought no file is the
 * door's to refuse, because it is the same failure on the suite's side of the seam. Either way it
 * is loud: a catalog silently dropped would produce false «me falta» states, and a grouping or a
 * programme silently dropped would file a coin under the wrong heading or print «1 de 3» about
 * coins nobody counted.
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
