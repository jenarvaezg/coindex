package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.domain.curedFamilyLabels
import com.jenarvaezg.coindex.domain.familyLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Los rótulos curados de `Family.kt` (#581) contra la caché sembrada y los ficheros curados: una
 * corrección muere cuando Numista arregla la serie o cuando un fichero curado la reclama. Si falla,
 * se vuelve a leer el rótulo en la fuente y se corrige la tabla, no el test.
 */
class CuredFamiliesTest {
    private val json = Json { ignoreUnknownKeys = true }

    /** Se lee de la ficha entera, como la lee la app. */
    private val series: Set<String> =
        json.parseToJsonElement(TypeCacheFile.read()).jsonObject.values.mapNotNull { ficha ->
            ficha.jsonObject["series"]?.jsonPrimitive?.contentOrNull
        }.toSet()

    /** Contra la caché que se envía, no contra numista.com: es lo que lee el móvil (ADR 0025). */
    @Test
    fun `every correction still names a series the cache serves`() {
        assertEquals(emptySet(), curedFamilyLabels().keys - series)
    }

    /**
     * Si un catálogo o una agrupación reclama la familia, su `short_name` gana en
     * [com.jenarvaezg.coindex.domain.CollectionTitles] y la entrada queda muerta (alias del #22).
     */
    @Test
    fun `no correction is shadowed by a curated file`() {
        val claimed = buildSet {
            SHIPPED_CURATION.catalogs.forEach { add(it.key().family) }
            SHIPPED_CURATION.groupings.forEach { add(it.family) }
        }

        assertEquals(emptySet(), curedFamilyLabels().keys intersect claimed)
    }

    /**
     * La validación de arranque sólo exige `short_name` único entre ficheros; un rótulo curado no
     * pasa por ella, y el desempate por peso de `CollectionTitles.of` (#565) no debe tapar choques.
     */
    @Test
    fun `no cured name collides with a curated one`() {
        val curated = buildSet {
            SHIPPED_CURATION.catalogs.forEach { add(it.shortName) }
            SHIPPED_CURATION.groupings.forEach { add(it.shortName) }
        }

        assertEquals(emptySet(), curedFamilyLabels().values.toSet() intersect curated)
    }

    /** La clave conserva la cadena cruda, así que curar un rótulo renombra sin mover monedas. */
    @Test
    fun `a cured family is painted and never keyed`() {
        curedFamilyLabels().forEach { (raw, cured) ->
            assertEquals(cured, familyLabel(raw))
        }
        assertEquals("Gothic Horror", familyLabel("Gothic Horror"))
        assertEquals("DC Comics", familyLabel("DC Comics"))
    }
}
