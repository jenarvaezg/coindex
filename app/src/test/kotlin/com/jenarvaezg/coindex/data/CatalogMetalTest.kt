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
     * Hasta el 4 de agosto de 2026 las dos colecciones eran de plata de arriba abajo — 73 de las
     * 75 tarjetas medidas en #40 —, y eso es lo que hacía que el defecto de la clave viviera
     * escondido: sin un catálogo de otro metal, nada choca.
     *
     * Las conmemorativas circulantes de Portugal (#157) rompen la premisa por primera vez: los
     * 2,50 y los 5 escudos son de cuproníquel, así que el metal ya es una componente de la clave
     * que separa tarjetas de verdad y no sólo en teoría. Fijarlo aquí sigue siendo fijar la
     * premisa, no el resultado.
     *
     * Los 2 € conmemorativos de España (#216) añaden el tercer caso y el primero que no es un
     * metal: una bimetálica no tiene metal dominante, así que declara `other`, que es lo que
     * `inferMetal` deduce también de la composición de sus piezas —«Bimetálica: centro de níquel
     * recubierto…»—. Que las dos partes coincidan es lo que mantiene la pieza y su lámina en la
     * misma tarjeta.
     */
    @Test
    fun `every catalog that is not a set declares its metal, two cupronickel and one bimetallic`() {
        val declared = catalogs.filterNot { it.isSet }.map { it.id to it.metal }
        assertEquals(64, declared.size)
        assertEquals(
            listOf(
                "espana-2-euros-conmemorativos" to Metal.Other,
                "portugal-2-50-escudos-cuproniquel" to Metal.Cupronickel,
                "portugal-5-escudos-cuproniquel" to Metal.Cupronickel,
            ),
            declared.filterNot { (_, metal) -> metal == Metal.Silver }.sortedBy { it.first },
        )
        // Los dos conjuntos no declaran variante física de ninguna clase (ADR 0012). El metal no
        // es lo que los parte —el venezolano de 1975 es plata .925 en sus dos miembros—, pero sin
        // esa exención tampoco tendrían dónde vivir: sus pesos no caben en una sola clave.
        assertEquals(listOf(null, null), catalogs.filter { it.isSet }.map { it.metal })
    }
}
