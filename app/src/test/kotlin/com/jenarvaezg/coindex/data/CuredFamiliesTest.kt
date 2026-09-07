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
 * La cura de #581 medida sobre lo que de verdad se publica: la caché sembrada y los ficheros curados.
 *
 * Es el `CardCountriesTest` de la otra línea que escribe Numista, y por la misma razón: la tabla de
 * `Family.kt` es una lista de correcciones sobre prosa de tercero, y una lista así se podre de dos
 * maneras. Por arriba, cuando Numista reescribe una serie y la corrección apunta a nada — que no es
 * hipotético: la `Charlemagme` que el #581 dio por errata de la fuente ya estaba corregida en
 * numista.com el 7 de septiembre de 2026, y esta clase es lo único que lo habría dicho. Y por abajo,
 * cuando la serie acaba curada en un fichero y la entrada queda muerta, como los seis alias del #22.
 *
 * **Rojo aquí no significa «cambia el test».** Significa que hay un rótulo que volver a leer en la
 * fuente, y se arregla tocando la tabla.
 */
class CuredFamiliesTest {
    private val json = Json { ignoreUnknownKeys = true }

    /** La serie por tipo, leída del mismo sitio del que la lee la app: la ficha entera. */
    private val series: Set<String> =
        json.parseToJsonElement(TypeCacheFile.read()).jsonObject.values.mapNotNull { ficha ->
            ficha.jsonObject["series"]?.jsonPrimitive?.contentOrNull
        }.toSet()

    /**
     * Ninguna corrección apunta a una serie que ya no exista.
     *
     * Una entrada podrida es verde para siempre y no la nota nadie: la tarjeta vuelve a rotularse en
     * inglés sin que nada se queje, y el comentario de la tabla miente sobre lo que la app pinta.
     * Se mide contra la caché que se envía y no contra numista.com, porque la caché es lo que el
     * móvil lee (ADR 0025) y una ficha corregida arriba llega aquí cuando alguien la vuelve a pedir.
     */
    @Test
    fun `every correction still names a series the cache serves`() {
        assertEquals(emptySet(), curedFamilyLabels().keys - series)
    }

    /**
     * Ninguna corrección la tapa un fichero curado.
     *
     * Es la muerte que tuvieron los seis alias del #22: en cuanto un catálogo o una agrupación
     * reclama la familia, su `short_name` gana en [com.jenarvaezg.coindex.domain.CollectionTitles] y
     * la entrada no la lee nadie. La cura es para lo que nadie ha curado todavía.
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
     * Ningún nombre curado se llama como algo que ya se llama así.
     *
     * El `short_name` es único entre los ficheros y eso se valida al arrancar, pero una familia
     * curada rotula una tarjeta del mismo índice sin pasar por esa validación. Dos tarjetas con el
     * mismo nombre las desempata `CollectionTitles.of` por el peso (#565), y apoyarse en el
     * desempate para un nombre que escribimos nosotros sería usar la red de otro accidente.
     */
    @Test
    fun `no cured name collides with a curated one`() {
        val curated = buildSet {
            SHIPPED_CURATION.catalogs.forEach { add(it.shortName) }
            SHIPPED_CURATION.groupings.forEach { add(it.shortName) }
        }

        assertEquals(emptySet(), curedFamilyLabels().values.toSet() intersect curated)
    }

    /**
     * La cura llega a la línea que pinta, y no a la clave.
     *
     * `familyLabel` es el único sitio donde una familia se convierte en texto de pantalla, y el
     * nombre de cada tarjeta se resuelve una vez para todo el índice, así que la lámina, la
     * exportación y el cuaderno no pueden discrepar de la tarjeta. Lo que esto fija es la otra mitad:
     * la clave sigue llevando la cadena cruda, que es lo que hace que curar un rótulo renombre una
     * tarjeta sin mover ni una moneda de sitio.
     */
    @Test
    fun `a cured family is painted and never keyed`() {
        curedFamilyLabels().forEach { (raw, cured) ->
            assertEquals(cured, familyLabel(raw))
        }
        assertEquals("Gothic Horror", familyLabel("Gothic Horror"))
        assertEquals("DC Comics", familyLabel("DC Comics"))
    }
}
