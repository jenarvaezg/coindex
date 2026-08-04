package com.jenarvaezg.coindex.data.seed

import android.content.res.AssetManager
import com.jenarvaezg.coindex.domain.CatalogSeedException
import com.jenarvaezg.coindex.domain.CatalogSeeds
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CommemorativeProgramme
import com.jenarvaezg.coindex.domain.CuratedGrouping
import com.jenarvaezg.coindex.domain.GroupingSeeds
import com.jenarvaezg.coindex.domain.ProgrammeSeeds

private const val CATALOG_DIR = "collection-catalogs"
private const val GROUPING_DIR = "groupings"
private const val PROGRAMME_DIR = "programmes"

/** Reads every `.json` in one asset directory, in a stable order, or fails saying which. */
private fun readSeedDir(assets: AssetManager, directory: String): List<Pair<String, String>> {
    val files = assets.list(directory)?.filter { it.endsWith(".json") }?.sorted()
        ?: throw CatalogSeedException("no se encontró el directorio de assets `$directory`")
    if (files.isEmpty()) {
        throw CatalogSeedException("no hay ficheros curados en `$directory`")
    }
    return files.map { fileName ->
        fileName to assets.open("$directory/$fileName").use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        }
    }
}

/**
 * Curated collection catalogs shipped as assets.
 *
 * Every `numista_type_id` in these files was verified against numista.com before being
 * committed, which makes them the most expensive asset in the project to reproduce. They are
 * validated at startup and a failure is fatal on purpose: a silently degraded catalog would
 * produce false "me falta" states.
 */
object CatalogAssets {
    fun load(assets: AssetManager): List<CollectionCatalog> =
        CatalogSeeds.parseAll(readSeedDir(assets, CATALOG_DIR))
}

/**
 * Curated groupings shipped as assets (ADR 0013).
 *
 * They carry no coverage claim, so a bad one cannot invent a missing piece — but it can put a
 * coin under the wrong heading, and the type ids were verified against numista.com just the
 * same. Validated at startup, and fatal for the same reason as a catalog.
 */
object GroupingAssets {
    fun load(assets: AssetManager): List<CuratedGrouping> =
        GroupingSeeds.parseAll(readSeedDir(assets, GROUPING_DIR))
}

/**
 * Curated commemorative programmes shipped as assets (ADR 0022).
 *
 * They reach no card and no denominator, so a bad one cannot invent a missing piece either — but
 * it prints «1 de 3» beside a plate row, which is a claim about coins that exist, and the type
 * ids were verified against numista.com the same way. Validated at startup, and fatal for the
 * same reason as a catalog.
 */
object ProgrammeAssets {
    fun load(assets: AssetManager): List<CommemorativeProgramme> =
        ProgrammeSeeds.parseAll(readSeedDir(assets, PROGRAMME_DIR))
}
