package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.domain.CatalogSeeds
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.metalDeviations
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * El cruce de #40 sobre lo que de verdad se publica: los treinta y dos catálogos contra las 728 fichas
 * de la caché sembrada.
 *
 * Es la red que faltaba cuando un vigésimo de onza de **oro** vivió meses dentro del catálogo del
 * Kookaburra (#63). Aquel defecto era invisible por construcción: el hueco de una lámina sólo se
 * ve en el móvil que **no** tiene la moneda, y esa no la tenía ninguna de las dos colecciones.
 *
 * Rojo aquí no significa «arregla el catálogo». Significa «míralo»: si la casilla desviada entra a
 * propósito, se declara en prosa en su `variant_note` y el cruce la respeta.
 */
class CatalogMetalTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val catalogs: List<CollectionCatalog> = CatalogSeeds.parseAll(CatalogFiles.all())

    /** `composition.text` por tipo, leído del mismo sitio del que lo lee la app: la ficha entera. */
    private val compositions: Map<Int, String?> =
        json.parseToJsonElement(TypeCacheFile.read()).jsonObject.entries.associate { (id, ficha) ->
            id.toInt() to ficha.jsonObject["composition"]
                ?.jsonObject
                ?.get("text")
                ?.jsonPrimitive
                ?.contentOrNull
        }

    @Test
    fun `no shipped member contradicts the metal its catalog declares`() {
        assertEquals(emptyList(), metalDeviations(catalogs, compositions))
    }

    /**
     * Las dos colecciones son de plata de arriba abajo — 73 de las 75 propuestas medidas en #40 —
     * y eso es lo que hace que el defecto de la clave viviera escondido: hasta que no se cure el
     * primer catálogo de oro, nada choca. Fijarlo aquí es fijar la premisa, no el resultado.
     */
    @Test
    fun `every catalog that is not a set declares silver today`() {
        val declared = catalogs.filterNot { it.isSet }.map { it.id to it.metal }
        assertEquals(42, declared.size)
        assertEquals(emptyList(), declared.filterNot { (_, metal) -> metal == Metal.Silver })
        // El único conjunto no declara variante física de ninguna clase (ADR 0012).
        assertEquals(listOf(null), catalogs.filter { it.isSet }.map { it.metal })
    }
}
