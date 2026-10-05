package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * El eyebrow dice un país, y Numista escribe entidades emisoras con su vigencia, como «Federación
 * de Rusia (1991-presente)» (#180). La cura es una tabla del curador: ninguna regla sobre la prosa
 * de Numista da el nombre del país.
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

    /** La tabla sólo corrige: un país que Numista ya nombra bien se rotula sin tocar código. */
    @Test
    fun `everything Numista already says as a country is said by Numista`() {
        assertEquals("Venezuela", cardCountry("venezuela", "Venezuela"))
        assertEquals("Unión Soviética", cardCountry("ancienne_urss", "Unión Soviética"))
        assertEquals("Imperio austríaco", cardCountry("autriche-habsbourg", "Imperio austríaco"))
        assertEquals("Sudáfrica", cardCountry("afrique_du_sud", "Sudáfrica"))
    }

    /** El fichero rotula su país aunque ninguna ficha del emisor esté en caché (ADR 0021 §9). */
    @Test
    fun `a curated code answers with no ficha behind it`() {
        assertEquals("Rusia", cardCountry("russie", null))
        assertNull(cardCountry("venezuela", null))
        assertNull(cardCountry(null, null))
    }

    @Test
    fun `a ficha with no issuer code is printed as it came`() {
        assertEquals("Venezuela", cardCountry(null, "Venezuela"))
    }
}
