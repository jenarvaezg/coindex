package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * El eyebrow dice un país, y Numista no escribe países (#180).
 *
 * Numista escribe **entidades emisoras** con su periodo de vigencia: «Federación de Rusia
 * (1991-presente)» es un dato correcto suyo y una etiqueta equivocada nuestra en la línea de
 * identidad de una tarjeta. La cura es una tabla del curador, no una heurística sobre prosa de
 * tercero: cortar por el primer paréntesis daría «Federación de Rusia», que sigue sin ser el nombre
 * del país, y desinvertir por la coma daría «Imperio Romano» con la mayúscula de Numista dentro.
 */
class CardCountryTest {
    @Test
    fun `a vigency parenthesis is not part of the country's name`() {
        assertEquals("Rusia", cardCountry("russie", "Federación de Rusia (1991-presente)"))
        assertEquals("Haití", cardCountry("haiti", "Haití (1804-presente)"))
        assertEquals(
            "República Dominicana",
            cardCountry("republique_dominicaine", "Dominicana, República (1844-presente)"),
        )
    }

    @Test
    fun `an inverted name is not read aloud inverted`() {
        assertEquals("China", cardCountry("chine", "China, República Popular"))
        assertEquals("Alemania", cardCountry("allemagne", "Alemania, República Federal de"))
        assertEquals("Imperio romano", cardCountry("rome", "Romano, Imperio (27 a. C. - 395 d. C.)"))
        assertEquals("Imperio ruso", cardCountry("russia-empire", "Ruso, Imperio (1547-1917)"))
    }

    /**
     * Los 31 códigos que ya salen limpios los sigue diciendo la caché.
     *
     * La tabla es una lista de correcciones y no un catálogo de países: mantener «Venezuela» en
     * código sería duplicar en Kotlin lo que la ficha ya dice bien, y el día que el coleccionista
     * compre una moneda de un país nuevo la tarjeta lo rotula sin que nadie toque el código.
     */
    @Test
    fun `everything Numista already says as a country is said by Numista`() {
        assertEquals("Venezuela", cardCountry("venezuela", "Venezuela"))
        assertEquals("Unión Soviética", cardCountry("ancienne_urss", "Unión Soviética"))
        assertEquals("Imperio austríaco", cardCountry("autriche-habsbourg", "Imperio austríaco"))
        assertEquals("Sudáfrica", cardCountry("afrique_du_sud", "Sudáfrica"))
    }

    /**
     * La tabla responde por el código, así que un fichero curado puede rotular su país sin que
     * ninguna ficha de ese emisor esté en la caché.
     *
     * Es lo que el §9 del ADR 0021 buscaba al hacer que hablara el fichero: la tarjeta de «50
     * gourdes de plata de Haití» declara `haiti`, y si el día del sync la ficha aún no ha llegado
     * el país lo sabe el fichero igual.
     */
    @Test
    fun `a curated code answers with no ficha behind it`() {
        assertEquals("Rusia", cardCountry("russie", null))
        assertNull(cardCountry("venezuela", null))
        assertNull(cardCountry(null, null))
    }

    /** Sin código no hay tabla que consultar, y lo que la ficha diga es lo único que hay. */
    @Test
    fun `a ficha with no issuer code is printed as it came`() {
        assertEquals("Venezuela", cardCountry(null, "Venezuela"))
    }
}
