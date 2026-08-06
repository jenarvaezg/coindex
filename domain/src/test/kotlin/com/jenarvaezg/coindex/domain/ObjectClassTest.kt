package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The two-value split behind the class chip of Coins (ADR 0021 §1).
 *
 * It reads Numista's coarse `category` and **not** the `type` prose of [objectClassDeviations]: the
 * two answer different questions, and the difference is measurable on the shipped data — by
 * `category` there is exonumia inside curated catalogs, and by the five-class net of the curator
 * there is none. Confusing them would either lose the members the chip exists to reach, or turn a
 * curator's warning into a filter.
 */
class ObjectClassTest {
    @Test
    fun `exonumia is Numista's own category and nothing else`() {
        assertEquals(ObjectClass.Exonumia, objectClassOf("exonumia"))
        assertEquals(ObjectClass.Coin, objectClassOf("coin"))
    }

    @Test
    fun `the five-class net of the curator is not this split`() {
        // «Medallas» is a `type`, not a `category`: a member carrying it is caught by
        // objectClassDeviations, and whether it is exonumia is a different field.
        thingsThatAreNotMoney().forEach { notMoney ->
            assertEquals(ObjectClass.Coin, objectClassOf(notMoney))
        }
    }

    @Test
    fun `a ficha nobody categorised is a coin, which is a default and not a claim`() {
        assertEquals(ObjectClass.Coin, objectClassOf(null))
        assertEquals(ObjectClass.Coin, objectClassOf(""))
        assertEquals(ObjectClass.Coin, objectClassOf("banknote"))
    }
}
