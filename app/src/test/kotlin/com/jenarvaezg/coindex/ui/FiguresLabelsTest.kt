package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.prices.ValuationRefusal
import com.jenarvaezg.coindex.data.prices.ValuationStatus
import com.jenarvaezg.coindex.domain.Ladder
import com.jenarvaezg.coindex.domain.LadderUnit
import com.jenarvaezg.coindex.domain.Ladders
import com.jenarvaezg.coindex.domain.MarginFigure
import com.jenarvaezg.coindex.domain.Referent
import com.jenarvaezg.coindex.domain.ValueSource
import com.jenarvaezg.coindex.domain.place
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Every word «Las cifras» prints, and the one number the bottom bar prints for it. */
class FiguresLabelsTest {
    /**
     * The third cell counts **grams and never money**.
     *
     * An amount in a permanent bar is a pocket ticker: it changes on its own, with nobody touching
     * anything, and puts the collector's estate in front of whoever glances at the phone. The weight only
     * moves when a coin arrives (#316).
     */
    @Test
    fun `the cell of the bottom bar counts weight`() {
        assertEquals("6,91 kg", figuresCellCount(6_907.4))
        assertEquals("0,00 kg", figuresCellCount(0.0))
    }

    /** The Spanish decimal comma, whatever language the phone is in. */
    @Test
    fun `the numbers are written with a Spanish comma`() {
        assertEquals("191,0 oz finas", fineOuncesLabel(190.98))
        assertEquals("0,35 m² · 5,6 folios A4", squareMetresLabel(0.352, 5.643))
        assertEquals("16.841 €", eurosLabel(16_840.6))
        assertEquals("86 %", percentLabel(0.8607))
    }

    /** The stack is the one figure that carries «unos», and only while it is extrapolated. */
    @Test
    fun `only an extrapolated magnitude says «unos»`() {
        assertEquals("unos 94 cm", ladderAmountLabel(LadderUnit.Centimetres, 94.0, true))
        assertEquals("94 cm", ladderAmountLabel(LadderUnit.Centimetres, 94.0, false))
        assertEquals("15,22 m", ladderAmountLabel(LadderUnit.Metres, 15.22, false))
    }

    /**
     * The sentence the ladder exists for: what has just been passed and what is within reach.
     *
     * «Más que un gato y a 310 g de una bola de bolos» **is** the figure — the comparison does not decorate
     * it (`docs/ux/cifras-326.md`).
     */
    @Test
    fun `the ladder says what has been passed and what is a hand's breadth away`() {
        assertEquals(
            "más que un gato y a 310 g de una bola de bolos",
            ladderComparison(reading(Ladders.weight, 6.95)),
        )
        // The gap is read in the smallest unit that keeps it whole: «a 0,31 kg» is a dashboard's number.
        assertEquals(
            "más que un autobús y a 3,50 m de un camión",
            ladderComparison(reading(Ladders.row, 13.0)),
        )
        assertEquals(
            "más que una encimera y a 6 cm de un pomo",
            ladderComparison(reading(Ladders.stack, 94.0)),
        )
    }

    /** Under the first rung nothing has been passed, and over the last there is nothing left to reach. */
    @Test
    fun `the ends of a ladder are said as ends`() {
        assertEquals("todavía por debajo de un ladrillo", ladderComparison(reading(Ladders.weight, 0.5)))
        assertEquals(
            "por encima de un labrador, que era el último referente",
            ladderComparison(reading(Ladders.weight, 40.0)),
        )
    }

    /**
     * Coverage yes, progress no (ADR 0028 §7).
     *
     * «El valor de N de tus 574 piezas» is said; «llevo 140 de 223» is not. Complete, the sentence has
     * nothing in it, so there is none.
     */
    @Test
    fun `the total says its coverage and only when it has one`() {
        assertEquals("el valor de 570 de tus 574 piezas", coverageLabel(570, 574))
        assertNull(coverageLabel(574, 574))
    }

    /**
     * Every number brought from outside carries the date it was read, and an expired one still does.
     *
     * It is what stops the total reading as a quotation, and «caducar no es borrar» read out loud: a phone
     * with no network for a month says «plata de hace 40 días» rather than emptying itself.
     */
    @Test
    fun `the silver is dated, however old it is`() {
        val now = 1_754_600_000_000L
        val day = 24L * 60 * 60 * 1_000

        assertEquals("plata de hoy", readAtLabel(now, now))
        assertEquals("plata de ayer", readAtLabel(now - day, now))
        assertEquals("plata de hace 40 días", readAtLabel(now - 40 * day, now))
    }

    /**
     * The one line the pass is allowed, and it has to tell the two silences apart.
     *
     * «Faltan y están cayendo» and «faltan porque se acabó el presupuesto» look identical from outside, and
     * only one of the two is worth waiting for (ADR 0028 §6).
     */
    @Test
    fun `the settings line says which silence it is`() {
        val falling = ValuationStatus(wanted = 223, missing = 83)

        assertTrue(valuationLabel(falling).endsWith("Se traen solos con la app abierta."))
        assertTrue(
            valuationLabel(falling.copy(held = ValuationRefusal.BudgetExhausted))
                .contains("presupuesto de llamadas"),
        )
        assertTrue(
            valuationLabel(falling.copy(held = ValuationRefusal.Syncing))
                .contains("sincronizado"),
        )
        assertTrue(
            valuationLabel(ValuationStatus(wanted = 223, missing = 0))
                .contains("están en este teléfono"),
        )
        assertTrue(valuationLabel(ValuationStatus()).contains("Todavía no hay emisiones"))
    }

    /** The origin of a value, because a number with no provenance is one nobody can check (#316). */
    @Test
    fun `a value says where it came from`() {
        assertEquals(
            "precio de catálogo en unc",
            FiguresLabels.valueOrigin(ValueSource.Market, "unc"),
        )
        assertEquals(
            "precio de catálogo en vg, el grado vecino",
            FiguresLabels.valueOrigin(ValueSource.NeighbouringGrade, "vg"),
        )
        assertEquals("su plata", FiguresLabels.valueOrigin(ValueSource.Silver, null))
        assertEquals("lo que pagaste", FiguresLabels.valueOrigin(ValueSource.Paid, null))
    }

    /** A coin's ficha: the amount, how many pieces it covers when it is more than one, and the origin. */
    @Test
    fun `a coin's value line carries its pieces only when there are several`() {
        assertEquals(
            "40 € · precio de catálogo en unc",
            coinValueLabel(CoinValue(40.0, 1, ValueSource.Market, "unc")),
        )
        assertEquals(
            "80 € · 2 piezas · precio de catálogo en unc",
            coinValueLabel(CoinValue(80.0, 2, ValueSource.Market, "unc")),
        )
        // Pieces that disagree about their origin leave **the origin** unsaid rather than print the wrong
        // one for half of them. How many pieces the total covers is not an origin, so it stays.
        assertEquals("540 € · 2 piezas", coinValueLabel(CoinValue(540.0, 2, null, null)))
    }

    /** Every referent has a name, or a rung would be a drawing with a blank under it. */
    @Test
    fun `every referent is named`() {
        Referent.entries.forEach { referent ->
            assertTrue(
                FiguresLabels.referent(referent).isNotBlank(),
                "el referente $referent no tiene nombre",
            )
        }
    }

    /** And every ladder says what it measures, which is the figure's own sentence and not furniture. */
    @Test
    fun `every ladder says what it measures`() {
        assertEquals(
            listOf("todas juntas pesan", "una al lado de otra llegan a", "una encima de otra levantan"),
            Ladders.all.map { FiguresLabels.ladderStatement(it.kind) },
        )
    }

    /**
     * The portrait's four shares, and the money one is absent rather than zero.
     *
     * The share of the value is money, so it goes exactly where the money section goes (ADR 0028
     * §4): a country with no valued piece says three shares and not «0 % del valor», which would be
     * a figure about a total the page is not showing.
     */
    @Test
    fun `a country's shares drop the money clause instead of printing it empty`() {
        fun venezuela(valueShare: Double?) = CountryPortrait(
            country = "Venezuela",
            pieces = 302,
            pieceShare = 0.62,
            massShare = 0.48,
            silverShare = 0.51,
            valueShare = valueShare,
        )

        assertEquals(
            "62 % de tus piezas · 48 % del peso · 51 % de la plata · 39 % del valor",
            portraitSharesLabel(venezuela(0.39)),
        )
        assertEquals(
            "62 % de tus piezas · 48 % del peso · 51 % de la plata",
            portraitSharesLabel(venezuela(null)),
        )
    }

    /**
     * On screen the coin beside the number is the measure, so the number is whole millimetres.
     *
     * The printed page keeps the tenth (`printedDiameterLabel`) because it has no coin to compare
     * against, and on a scaled page no ruler either.
     */
    @Test
    fun `a diameter on screen is whole millimetres`() {
        assertEquals("38 mm", screenDiameterLabel(38.61))
        assertEquals("40 mm", screenDiameterLabel(40.9))
    }

    /**
     * The four sentences «al margen», which moved out of the screen they were written in.
     *
     * They are the reason the page reads as a field guide rather than a dashboard, and they were the
     * one exception to this file's promise to hold every string «Las cifras» prints (ADR 0026 §6).
     */
    @Test
    fun `the margin says the four things the ficha already knew`() {
        // The denominator is the whole collection and not the types Numista answered for: a
        // percentage over a moving denominator is a figure nobody can check.
        assertEquals(
            "75 % ya no son dinero en ninguna parte",
            demonetizedSentence(MarginFigure(pieces = 429, outOf = 572)),
        )
        assertEquals(
            "246 las grabó la misma mano: Tomás Francisco Prieto",
            sameHandSentence(MarginFigure(246, 572, "Tomás Francisco Prieto")),
        )
        assertEquals(
            "58 salieron de Casa de la Moneda de México, de 14 cecas distintas",
            mintSentence(MarginFigure(58, 572, "Casa de la Moneda de México"), distinctMints = 14),
        )
        assertEquals(
            "31 llevan la fecha de 1977",
            commonestYearSentence(MarginFigure(31, 572, "1977")),
        )
    }

    /** A figure counted over nothing is 0 % and not a division by zero. */
    @Test
    fun `a margin figure over an empty collection says zero`() {
        assertEquals(
            "0 % ya no son dinero en ninguna parte",
            demonetizedSentence(MarginFigure(pieces = 0, outOf = 0)),
        )
    }
}

private fun reading(ladder: Ladder, amount: Double) = LadderReading(
    ladder = ladder,
    amount = amount,
    placement = ladder.place(amount),
    approximate = false,
)
