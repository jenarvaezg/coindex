package com.jenarvaezg.coindex.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoinNameTest {
    @Test
    fun `the denomination and the best available theme name a coin`() {
        val examples = mapOf(
            "1 Dollar - Elizabeth II (2nd portrait, Confederation)" to
                CoinName("1 Dollar", "Confederation"),
            "8 Reales - Charles IV" to CoinName("8 Reales", "Charles IV"),
            "12 Euros - Juan Carlos I (Christopher Columbus)" to
                CoinName("12 Euros", "Christopher Columbus"),
            "5 Pounds - Elizabeth II (Red Dragon of Wales; 2 oz Fine Silver)" to
                CoinName("5 Pounds", "Red Dragon of Wales"),
            "1 Dollar \"Morgan Dollar\"" to CoinName("1 Dollar", "Morgan Dollar"),
            "Medal - 1200 Jahre Münzgeschichte (Euro)" to
                CoinName("Medal", "1200 Jahre Münzgeschichte"),
            "10 Pesos (First silver extraction from Pueblo Viejo Mine)" to
                CoinName("10 Pesos", "First silver extraction from Pueblo Viejo Mine"),
            "1 Dollar - Elizabeth II (4th Portrait - Koala - Silver Bullion Coin)" to
                CoinName("1 Dollar", "Koala"),
            "1 Dollar \"American Silver Eagle\" New Reverse (Bullion Coin)" to
                CoinName("1 Dollar", "American Silver Eagle"),
            "2 Pounds - Elizabeth II (4th portrait; 1 oz Fine Silver (.958))" to
                CoinName("2 Pounds", "Elizabeth II"),
        )

        examples.forEach { (title, expected) ->
            assertEquals(expected, coinName(title), title)
        }
    }

    @Test
    fun `material finish and portrait tails do not become themes`() {
        val examples = listOf(
            "1 Dollar - Elizabeth II (2nd portrait; 1oz Fine Silver)",
            "50 Pence - Elizabeth II (1/4 oz Fine Silver)",
            "1 Dollar - Elizabeth II (Bullion Coinage)",
            "1 Dollar - Elizabeth II (Silver Proof)",
            "1 Dollar - Elizabeth II (Proof)",
        )

        assertEquals(
            listOf("Elizabeth II", "Elizabeth II", "Elizabeth II", "Elizabeth II", "Elizabeth II"),
            examples.map { coinName(it).theme },
        )
    }

    @Test
    fun `the target corpus keeps denominations compact and only themes need multiline space`() {
        val names = listOf(
            "1 Dollar - Elizabeth II (2nd portrait, Confederation)",
            "8 Reales - Charles IV",
            "12 Euros - Juan Carlos I (Christopher Columbus)",
            "5 Pounds - Elizabeth II (Red Dragon of Wales; 2 oz Fine Silver)",
            "1 Dollar \"Morgan Dollar\"",
            "Medal - 1200 Jahre Münzgeschichte (Euro)",
        ).map(::coinName)

        assertTrue(names.all { it.denomination.length <= 15 })
        assertTrue(names.any { (it.theme?.length ?: 0) > 20 })
    }
}
