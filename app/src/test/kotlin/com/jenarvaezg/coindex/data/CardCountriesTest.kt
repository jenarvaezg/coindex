package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.domain.CatalogSeeds
import com.jenarvaezg.coindex.domain.GroupingSeeds
import com.jenarvaezg.coindex.domain.ProgrammeSeeds
import com.jenarvaezg.coindex.domain.cardCountry
import com.jenarvaezg.coindex.domain.curedIssuerCodes
import com.jenarvaezg.coindex.domain.readsAsACountry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * La cura de #180 medida sobre lo que de verdad se publica: la caché sembrada y los ficheros curados.
 *
 * La tabla de `CardCountry.kt` es una lista de correcciones sobre prosa de tercero, y una lista así se
 * podre de dos maneras: por arriba, cuando Numista cambia una etiqueta y la corrección apunta a nada;
 * y por abajo, cuando llega una moneda de un emisor nuevo cuya etiqueta tampoco es un país. Las dos
 * se notan aquí y en ningún otro sitio.
 *
 * **Rojo aquí no significa «cambia el test».** Significa que hay un país nuevo que rotular, y se
 * arregla añadiendo una línea a la tabla o borrando la que sobra.
 */
class CardCountriesTest {
    private val json = Json { ignoreUnknownKeys = true }

    /** El emisor por tipo, leído del mismo sitio del que lo lee la app: la ficha entera. */
    private val issuers: List<Pair<String?, String?>> =
        json.parseToJsonElement(TypeCacheFile.read()).jsonObject.values.map { ficha ->
            val issuer = ficha.jsonObject["issuer"]?.jsonObject
            val code = issuer?.get("code")?.jsonPrimitive?.contentOrNull
            code to issuer?.get("name")?.jsonPrimitive?.contentOrNull
        }

    /**
     * Ninguna tarjeta rotula una entidad emisora con su vigencia.
     *
     * Los tres vicios los dice `readsAsACountry`: el paréntesis de vigencia —«Haití (1804-presente)»—,
     * el nombre invertido de índice —«China, República Popular»— y el largo, medido contra los 40
     * caracteres de techo del `short_name` que el eyebrow lleva debajo. Los dos primeros son
     * exactamente lo que el §4 del ADR 0021 no quiere en una línea de identidad, y la caché tiene hoy
     * nueve códigos con uno de ellos.
     */
    @Test
    fun `no issuer the seeded cache serves reads as a Numista label`() {
        val dirty = issuers
            .mapNotNull { (code, name) -> cardCountry(code, name) }
            .filterNot(::readsAsACountry)
            .distinct()

        assertEquals(emptyList(), dirty)
    }

    /**
     * Ninguna corrección apunta a un código que ya no exista.
     *
     * Una entrada podrida es verde para siempre y no la nota nadie: si Numista renombra un código o
     * el último tipo de un emisor sale de la caché, la línea sobra y su comentario miente sobre lo que
     * la app pinta.
     */
    @Test
    fun `every correction still has a ficha behind it`() {
        val present = issuers.mapNotNull { (code, _) -> code }.toSet()

        assertEquals(emptySet(), curedIssuerCodes() - present)
    }

    /**
     * Todo `issuer_code` que declara un fichero curado rotula un país, y lo rotula limpio.
     *
     * Es la otra mitad del §9 del ADR 0021: el fichero habla, así que un código declarado sin nombre
     * detrás es una tarjeta con el eyebrow desnudo teniendo la curación hecha. Se mide sobre las tres
     * familias de ficheros porque las tres declaran emisor, y sobre el emisor de cada casilla porque
     * un catálogo puede abarcar más de uno (Equilibrium, #170).
     */
    @Test
    fun `every issuer a curated file declares is a country`() {
        val namesByCode = issuers.mapNotNull { (code, name) ->
            code?.let { name?.let { code to name } }
        }.toMap()
        val declared = buildSet {
            CatalogSeeds.parseAll(CatalogFiles.all()).forEach { addAll(it.issuerCodes()) }
            GroupingSeeds.parseAll(GroupingFiles.all()).forEach { add(it.issuerCode) }
            ProgrammeSeeds.parseAll(ProgrammeFiles.all()).forEach { add(it.issuerCode) }
        }

        val unlabelled = declared.filterNot { code ->
            cardCountry(code, namesByCode[code])?.let(::readsAsACountry) ?: false
        }

        assertEquals(emptyList(), unlabelled)
    }
}
