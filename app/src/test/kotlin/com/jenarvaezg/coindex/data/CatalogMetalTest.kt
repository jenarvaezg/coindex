package com.jenarvaezg.coindex.data

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
 * El cruce de #40, los catálogos publicados contra la caché sembrada. El hueco de una lámina sólo
 * se ve en el móvil que no tiene la moneda, y así vivió meses un vigésimo de onza de oro dentro del
 * Kookaburra (#63). Si falla, se mira: una casilla desviada a propósito se declara en su
 * `variant_note`, que el cruce respeta.
 */
class CatalogMetalTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val catalogs: List<CollectionCatalog> = SHIPPED_CURATION.catalogs

    /** `composition.text` por tipo, leído de la ficha entera como lo lee la app. */
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
     * Fija la premisa de que el metal de la clave separa tarjetas (#157, #216). Una bimetálica no
     * tiene metal dominante y declara `other`, lo mismo que `inferMetal` deduce de la composición
     * de sus piezas; si no coincidieran, la pieza y su lámina caerían en tarjetas distintas.
     */
    @Test
    fun `every catalog that is not a set declares its metal, two cupronickel and one bimetallic`() {
        val declared = catalogs.filterNot { it.isSet }.map { it.id to it.metal }
        assertEquals(
            listOf(
                "espana-2-euros-conmemorativos" to Metal.Other,
                "portugal-2-50-escudos-cuproniquel" to Metal.Cupronickel,
                "portugal-5-escudos-cuproniquel" to Metal.Cupronickel,
            ),
            declared.filterNot { (_, metal) -> metal == Metal.Silver }.sortedBy { it.first },
        )
        // Los conjuntos no declaran variante física (ADR 0012): sus pesos no caben en una clave.
        assertEquals(listOf(null, null, null), catalogs.filter { it.isSet }.map { it.metal })
    }
}
