package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.prices.ValuationRefusal
import com.jenarvaezg.coindex.data.prices.ValuationStatus
import com.jenarvaezg.coindex.domain.Ladder
import com.jenarvaezg.coindex.domain.LadderUnit
import com.jenarvaezg.coindex.domain.Ladders
import com.jenarvaezg.coindex.domain.MarginFigure
import com.jenarvaezg.coindex.domain.PaidComparison
import com.jenarvaezg.coindex.domain.Referent
import com.jenarvaezg.coindex.domain.SilverSpot
import com.jenarvaezg.coindex.domain.ValueSource
import com.jenarvaezg.coindex.domain.place
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Every word «Las cifras» prints, and the one number the bottom bar prints for it. */
class FiguresLabelsTest {
    /**
     * Weight, never money: an amount in a permanent bar moves on its own and shows the collection's
     * worth to anyone glancing at the phone (#316).
     */
    @Test
    fun `the cell of the bottom bar counts weight`() {
        assertEquals("6,91 kg", figuresCellCount(6_907.4))
        assertEquals("0,00 kg", figuresCellCount(0.0))
        // Absence, not zero, while the snapshot is still being read (#418).
        assertEquals("—", figuresCellCount(null))
    }

    /** The Spanish decimal comma, whatever language the phone is in. */
    @Test
    fun `the numbers are written with a Spanish comma`() {
        assertEquals("191,0 oz finas", fineOuncesLabel(190.98))
        assertEquals("0,35 m² · 5,6 folios A4", squareMetresLabel(0.352, 5.643))
        assertEquals("16.841 €", eurosLabel(16_840.6))
        assertEquals("86 %", percentLabel(0.8607))
    }

    @Test
    fun `only an extrapolated magnitude says «unos»`() {
        assertEquals("unos 94 cm", ladderAmountLabel(LadderUnit.Centimetres, 94.0, true))
        assertEquals("94 cm", ladderAmountLabel(LadderUnit.Centimetres, 94.0, false))
        assertEquals("15,22 m", ladderAmountLabel(LadderUnit.Metres, 15.22, false))
    }

    /** The comparison is the figure, not a decoration (`docs/ux/cifras-326.md`). */
    @Test
    fun `the ladder says what has been passed and what is a hand's breadth away`() {
        assertEquals(
            "más que un gato y a 310 g de una bola de bolos",
            ladderComparison(reading(Ladders.weight, 6.95)),
        )
        // The gap uses the smallest unit that keeps it whole: «310 g», not «0,31 kg».
        assertEquals(
            "más que un autobús y a 3,50 m de un camión",
            ladderComparison(reading(Ladders.row, 13.0)),
        )
        assertEquals(
            "más que una encimera y a 6 cm de un pomo",
            ladderComparison(reading(Ladders.stack, 94.0)),
        )
    }

    @Test
    fun `the ends of a ladder are said as ends`() {
        assertEquals("todavía por debajo de un ladrillo", ladderComparison(reading(Ladders.weight, 0.5)))
        assertEquals(
            "por encima de un labrador, que era el último referente",
            ladderComparison(reading(Ladders.weight, 40.0)),
        )
    }

    /** Coverage, never progress (ADR 0028 §7); a complete total needs no sentence. */
    @Test
    fun `the total says its coverage and only when it has one`() {
        assertEquals("el valor de 570 de tus 574 piezas", coverageLabel(570, 574))
        assertNull(coverageLabel(574, 574))
    }

    /**
     * The ounces are the silver above them in bullion's unit, so they read as a conversion rather
     * than a second figure (#398).
     */
    @Test
    fun `the matter says its census and the silver says it is a conversion`() {
        assertEquals("580 piezas de 35 emisores", matterCensusLabel(580, 35))
        assertEquals("que son 196,4 oz finas de plata pura", fineSilverSentence(196.42))
    }

    /** An old reading shows its age instead of vanishing; the price makes it checkable (#398). */
    @Test
    fun `the stamp carries the price of the silver and when it was read`() {
        val read = spot(day = 8, hour = 11, minute = 52)

        assertEquals(
            "plata: 55,23 €/oz · hoy 11:52",
            spotStampLabel(read, millis(day = 8, hour = 23, minute = 10), MADRID),
        )
        assertEquals(
            "plata: 55,23 €/oz · ayer 11:52",
            spotStampLabel(read, millis(day = 9, hour = 8, minute = 0), MADRID),
        )
        // Past yesterday the hour gives way to the age.
        assertEquals(
            "plata: 55,23 €/oz · hace 40 días",
            spotStampLabel(read, millis(day = 48, hour = 8, minute = 0), MADRID),
        )
    }

    /**
     * The spot is read daily and a catalogue price lives ninety days (ADR 0028 §5, #561), so each
     * clock gets its own clause (#594); the catalogue's dates the oldest read in the total (#494).
     */
    @Test
    fun `the stamp of the total names each of its two clocks`() {
        val now = millis(day = 8, hour = 23, minute = 10)
        val read = spot(day = 8, hour = 11, minute = 52)

        assertEquals(
            "plata: 55,23 €/oz · hoy 11:52 · Numista: hace 12 días",
            moneyStampLabel(read, millis(day = -4, hour = 9, minute = 0), now, MADRID),
        )
        // Past a month the date replaces the age, as in `valuedAgeLabel`. «Numista», because a
        // plate already labels the curated file's date «Catálogo» (#518).
        assertEquals(
            "plata: 55,23 €/oz · hoy 11:52 · Numista: el 21 jun 2026",
            moneyStampLabel(read, millis(day = -40, hour = 9, minute = 0), now, MADRID),
        )
        // With no catalogue price the clause is absent, not «sin fecha».
        assertEquals(
            spotStampLabel(read, now, MADRID),
            moneyStampLabel(read, catalogReadAt = null, nowMillis = now, zone = MADRID),
        )
    }

    /** Nine hours across midnight is «ayer»; counting 24-hour blocks would call it «hoy». */
    @Test
    fun `the days are calendar days and not elapsed ones`() {
        assertEquals(
            "plata: 55,23 €/oz · ayer 23:00",
            spotStampLabel(
                spot(day = 8, hour = 23, minute = 0),
                millis(day = 9, hour = 8, minute = 0),
                MADRID,
            ),
        )
    }

    /** A pinned zone, so the hour the stamp prints is the same one wherever the suite runs. */
    private val MADRID = ZoneId.of("Europe/Madrid")

    private val AUGUST_8 = ZonedDateTime.of(2026, 8, 8, 0, 0, 0, 0, MADRID)

    /** A wall clock in August 2026, where `day` counts on past the end of the month. */
    private fun millis(day: Int, hour: Int, minute: Int): Long = AUGUST_8
        .plusDays((day - 8).toLong())
        .withHour(hour)
        .withMinute(minute)
        .toInstant()
        .toEpochMilli()

    private fun spot(day: Int, hour: Int, minute: Int) = SilverSpot(55.23, millis(day, hour, minute))

    /**
     * Prices still arriving and a spent budget must read differently: only the first is worth
     * waiting for (ADR 0028 §6).
     */
    @Test
    fun `the settings line says which silence it is`() {
        val falling = ValuationStatus(wanted = 223, missing = 83)

        assertTrue(valuationLabel(falling).endsWith("Se traen solos con la app abierta."))
        // The pass's own allowance, not the phone's (#605): the pass stops short of the monthly cap
        // so the inventory keeps its calls.
        assertTrue(
            valuationLabel(falling.copy(held = ValuationRefusal.BudgetExhausted))
                .contains("consultas que los precios tienen"),
        )
        assertTrue(
            valuationLabel(falling.copy(held = ValuationRefusal.Syncing))
                .contains("sincronizado"),
        )
        assertEquals(
            "Los precios de las 223 emisiones están al día.",
            valuationLabel(ValuationStatus(wanted = 223, missing = 0)),
        )
        assertTrue(valuationLabel(ValuationStatus()).contains("Todavía no hay emisiones"))
    }

    /** The budget is news only while something is still missing (#421). */
    @Test
    fun `settled prices do not mention a spent budget`() {
        assertEquals(
            "Los precios de las 231 emisiones están al día.",
            valuationLabel(
                ValuationStatus(
                    wanted = 231,
                    missing = 0,
                    held = ValuationRefusal.BudgetExhausted,
                ),
            ),
        )
    }

    /** A number with no provenance can't be checked (#316). */
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
        // Pieces with different origins leave the origin unsaid; the piece count stays.
        assertEquals("540 € · 2 piezas", coinValueLabel(CoinValue(540.0, 2, null, null)))
    }

    /**
     * The plate's total carries its criterion, as the ficha carries its origin (#408). It is
     * unnamed because it is the paper's reading, whose row is already titled «Valor» (#493).
     */
    @Test
    fun `a plate's value line names the criterion`() {
        assertEquals(
            "4.116 € · al mayor de tres precios",
            plateAmountLabel(PlateValue(4_116.0, pieces = 12)),
        )
    }

    /** The names carry the hierarchy (#493): near closing, the cost can be the larger amount. */
    @Test
    fun `the two figures of a header are named, and neither borrows the other's criterion`() {
        val now = millis(day = 8, hour = 11, minute = 0)
        // No catalogue read behind either amount, so neither line carries a date.
        val value = plateValueLabel(PlateValue(4_116.0, pieces = 12), now, MADRID)
        val cost = plateCostLabel(PlateCost(84.0, holes = 2), now, MADRID)

        assertEquals("Valor actual: 4.116 € · al mayor de tres precios", value)
        assertEquals("Coste de cerrar: 84 € · en sin circular", cost)
        // A hole has no «lo que pagaste», so it has two prices, not three (ADR 0028 §8).
        assertTrue(FiguresLabels.MONEY_CRITERION !in cost)
    }

    /**
     * The two figures are made of different reads, and a marked casilla is repriced whatever the
     * plate (ADR 0029 §4), so their ages can differ (#594). Each dates its oldest read (#494).
     */
    @Test
    fun `each figure of a header carries the age of its own price`() {
        val now = millis(day = 8, hour = 11, minute = 0)

        assertEquals(
            "Valor actual: 4.116 € · al mayor de tres precios · Numista: el 21 jun 2026",
            plateValueLabel(
                PlateValue(4_116.0, pieces = 12, catalogReadAt = millis(day = -40, hour = 9, minute = 0)),
                now,
                MADRID,
            ),
        )
        assertEquals(
            "Coste de cerrar: 84 € · en sin circular · Numista: hace 3 días",
            plateCostLabel(
                PlateCost(84.0, holes = 2, catalogReadAt = millis(day = 5, hour = 9, minute = 0)),
                now,
                MADRID,
            ),
        )
    }

    /**
     * The header already says the criterion; ten holes repeating it is the cost ADR 0026 §5 counts.
     */
    @Test
    fun `the stamp of a hole says its amount and repeats no criterion`() {
        assertEquals("12 €", holeCostLabel(12.0))
    }

    @Test
    fun `every referent is named`() {
        Referent.entries.forEach { referent ->
            assertTrue(
                FiguresLabels.referent(referent).isNotBlank(),
                "el referente $referent no tiene nombre",
            )
        }
    }

    @Test
    fun `every ladder says what it measures`() {
        assertEquals(
            listOf("todas juntas pesan", "una al lado de otra llegan a", "una encima de otra levantan"),
            Ladders.all.map { FiguresLabels.ladderStatement(it.kind) },
        )
    }

    /** The value share follows the money section (ADR 0028 §4): absent, never «0 % del valor». */
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
     * On screen the coin drawn beside it is the measure; paper, with no coin to compare against,
     * keeps the tenth (`printedDiameterLabel`).
     */
    @Test
    fun `a diameter on screen is whole millimetres`() {
        assertEquals("38 mm", screenDiameterLabel(38.61))
        assertEquals("40 mm", screenDiameterLabel(40.9))
    }

    /** The four «al margen» sentences live with the rest of «Las cifras»' copy (ADR 0026 §6). */
    @Test
    fun `the margin says the four things the ficha already knew`() {
        // The denominator is the whole collection, not the types Numista answered for.
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

    @Test
    fun `a margin figure over an empty collection says zero`() {
        assertEquals(
            "0 % ya no son dinero en ninguna parte",
            demonetizedSentence(MarginFigure(pieces = 0, outOf = 0)),
        )
    }

    /** «o casi» covers `au`, which is about uncirculated and not «sin circular» on its ficha. */
    @Test
    fun `the margin says how much of the collection has not circulated`() {
        assertEquals(
            "40 % están sin circular o casi",
            uncirculatedSentence(MarginFigure(pieces = 227, outOf = 572)),
        )
    }

    /**
     * Counted over the pieces with a declared price, never as a share of the collection: the rest
     * had no price written down, which doesn't mean they weren't bought (#491).
     */
    @Test
    fun `what was paid says over how many pieces it was declared`() {
        assertEquals(
            "De las 91 piezas cuyo precio anotaste, pagaste 1.234 €. Hoy valen 2.345 €.",
            paidAgainstTodayLabel(PaidComparison(paid = 1_234.0, today = 2_345.0, pieces = 91)),
        )
    }

    /** «De la 1 pieza» is not Spanish. */
    @Test
    fun `a single declared price is said in the singular`() {
        assertEquals(
            "De la única pieza cuyo precio anotaste, pagaste 30 €. Hoy vale 40 €.",
            paidAgainstTodayLabel(PaidComparison(paid = 30.0, today = 40.0, pieces = 1)),
        )
    }
}

private fun reading(ladder: Ladder, amount: Double) = LadderReading(
    ladder = ladder,
    amount = amount,
    placement = ladder.place(amount),
    approximate = false,
)
