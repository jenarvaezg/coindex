package com.jenarvaezg.coindex.data.seed

import android.content.res.AssetManager
import com.jenarvaezg.coindex.domain.CatalogSeedException
import com.jenarvaezg.coindex.domain.CatalogSeeds
import com.jenarvaezg.coindex.domain.CollectionCatalog

private const val CATALOG_DIR = "collection-catalogs"

/**
 * Curated collection catalogs shipped as assets.
 *
 * Every `numista_type_id` in these files was verified against numista.com before being
 * committed, which makes them the most expensive asset in the project to reproduce. They are
 * validated at startup and a failure is fatal on purpose: a silently degraded catalog would
 * produce false "me falta" states.
 */
object CatalogAssets {
    fun load(assets: AssetManager): List<CollectionCatalog> {
        val files = assets.list(CATALOG_DIR)?.filter { it.endsWith(".json") }?.sorted()
            ?: throw CatalogSeedException("no se encontró el directorio de assets `$CATALOG_DIR`")
        if (files.isEmpty()) {
            throw CatalogSeedException("no hay catálogos curados en `$CATALOG_DIR`")
        }
        val contents = files.map { fileName ->
            fileName to assets.open("$CATALOG_DIR/$fileName").use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            }
        }
        return CatalogSeeds.parseAll(contents)
    }
}
