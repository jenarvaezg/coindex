package com.jenarvaezg.coindex.data

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
 * Las correcciones de país de `CardCountry.kt` (#180) contra la caché sembrada y los ficheros
 * curados: una corrección muere cuando Numista cambia la etiqueta, y un emisor nuevo puede traer
 * otra que no sea un país. Si falla, se añade o se borra una línea de la tabla, no se toca el test.
 */
class CardCountriesTest {
    private val json = Json { ignoreUnknownKeys = true }

    /** El emisor por tipo, leído de la ficha entera como lo lee la app. */
    private val issuers: List<Pair<String?, String?>> =
        json.parseToJsonElement(TypeCacheFile.read()).jsonObject.values.map { ficha ->
            val issuer = ficha.jsonObject["issuer"]?.jsonObject
            val code = issuer?.get("code")?.jsonPrimitive?.contentOrNull
            code to issuer?.get("name")?.jsonPrimitive?.contentOrNull
        }

    /**
     * Los vicios de `readsAsACountry`: paréntesis de vigencia («Haití (1804-presente)»), nombre
     * invertido («China, República Popular») y pasar del techo de 40 caracteres del `short_name`
     * que va bajo el eyebrow (ADR 0021 §4).
     */
    @Test
    fun `no issuer the seeded cache serves reads as a Numista label`() {
        val dirty = issuers
            .mapNotNull { (code, name) -> cardCountry(code, name) }
            .filterNot(::readsAsACountry)
            .distinct()

        assertEquals(emptyList(), dirty)
    }

    /** Si Numista renombra un código o su último tipo sale de la caché, la corrección sobra. */
    @Test
    fun `every correction still has a ficha behind it`() {
        val present = issuers.mapNotNull { (code, _) -> code }.toSet()

        assertEquals(emptySet(), curedIssuerCodes() - present)
    }

    /**
     * Un código declarado sin nombre detrás deja el eyebrow desnudo (ADR 0021 §9). Se mide el
     * emisor de cada casilla porque un catálogo puede abarcar más de uno (Equilibrium, #170).
     */
    @Test
    fun `every issuer a curated file declares is a country`() {
        val namesByCode = issuers.mapNotNull { (code, name) ->
            code?.let { name?.let { code to name } }
        }.toMap()
        val declared = buildSet {
            SHIPPED_CURATION.catalogs.forEach { addAll(it.issuerCodes()) }
            SHIPPED_CURATION.groupings.forEach { add(it.issuerCode) }
            SHIPPED_CURATION.programmes.forEach { add(it.issuerCode) }
        }

        val unlabelled = declared.filterNot { code ->
            cardCountry(code, namesByCode[code])?.let(::readsAsACountry) ?: false
        }

        assertEquals(emptyList(), unlabelled)
    }
}
