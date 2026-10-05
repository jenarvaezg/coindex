package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.domain.objectClassDeviations
import com.jenarvaezg.coindex.domain.objectClassOf
import com.jenarvaezg.coindex.domain.thingsThatAreNotMoney
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * La red de #89, los catálogos publicados contra la caché sembrada: no hay estado de miembro para
 * lo que no es moneda, sólo este aviso. Si falla, no se saca la casilla: se mira, y un ensayo que
 * entra a propósito se declara en su `variant_note`, que el cruce respeta.
 */
class CatalogObjectClassTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val catalogs: List<CollectionCatalog> = SHIPPED_CURATION.catalogs

    /** La clase de objeto por tipo, leída de la ficha entera como la lee la app. */
    private val objectClasses: Map<Int, String?> =
        json.parseToJsonElement(TypeCacheFile.read()).jsonObject.entries.associate { (id, ficha) ->
            id.toInt() to ficha.jsonObject["type"]?.jsonPrimitive?.contentOrNull
        }

    /** La categoría gruesa de Numista, que es la que lee la chip de clase de Monedas (ADR 0021 §1). */
    private val categories: Map<Int, String?> =
        json.parseToJsonElement(TypeCacheFile.read()).jsonObject.entries.associate { (id, ficha) ->
            id.toInt() to ficha.jsonObject["category"]?.jsonPrimitive?.contentOrNull
        }

    @Test
    fun `no shipped member is a struck thing that is not money`() {
        assertEquals(emptyList(), objectClassDeviations(catalogs, objectClasses))
    }

    /**
     * Las medallas son filtro y no sección (ADR 0021 §1) porque la mayor parte de la exonumia
     * sembrada son miembros de láminas, como los ECU y euros de la FNMT (#258), y una sección las
     * arrancaría de ellas. Si falla, el reparto se dio la vuelta y hay que releer §1.
     */
    @Test
    fun `most curated exonumia is why the class is a chip and not a section`() {
        val curatedTypeIds = catalogs
            .flatMap { catalog -> catalog.members.mapNotNull { it.numistaTypeId } }
            .toSet()
        val exonumia = categories
            .filterValues { objectClassOf(it) == ObjectClass.Exonumia }
            .keys

        val curated = (exonumia intersect curatedTypeIds).size
        assertTrue(
            curated * 2 > exonumia.size,
            "la exonumia sembrada ya no vive sobre todo en láminas: $curated de ${exonumia.size}",
        )
    }

    /**
     * La red compara cadenas literales en español, porque la app y `scripts/seed-type-cache.py`
     * piden `lang=es`; si Numista cambiara el idioma o la redacción, el cruce seguiría en verde sin
     * comprobar nada. Las cinco clases están hoy en la caché: si una deja de aparecer, hay que
     * mirar por qué antes de fiarse del cruce.
     */
    @Test
    fun `the seeded cache still speaks the vocabulary this net reads`() {
        val present = objectClasses.values.filterNotNull().toSet()

        assertEquals(
            setOf(
                "Monedas de ensayo",
                "Monedas de fantasía",
                "Medallas",
                "Medallas conmemorativas",
                "Medallones de colección",
            ),
            thingsThatAreNotMoney(),
        )
        assertEquals(emptySet(), thingsThatAreNotMoney() - present)
    }
}
