package com.jenarvaezg.coindex.ui.ficha

import com.jenarvaezg.coindex.data.prices.PriceBook
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.PieceValue
import com.jenarvaezg.coindex.domain.PriceLadder
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.ValueSource
import com.jenarvaezg.coindex.domain.fineSilverGrams
import com.jenarvaezg.coindex.domain.pieceLadder
import com.jenarvaezg.coindex.ui.eurosLabel
import com.jenarvaezg.coindex.ui.numistaStampLabel
import com.jenarvaezg.coindex.ui.spotStampLabel
import java.time.ZoneId

/** The rungs of every ladder on the screen, worded against one price book and one clock. */
internal class FichaPrices(
    private val typeId: Int,
    private val meta: TypeMeta?,
    private val book: PriceBook,
    private val settled: Boolean,
    private val nowMillis: Long,
    private val zone: ZoneId,
) {
    fun pieceRungs(item: CollectedItem): List<FichaRung> {
        if (!settled) return emptyList()
        val ladder = pieceLadder(item, meta, book.spot, book::of)
        return rungs(
            ladder,
            listOf(
                marketRung(ladder.market, item.issueId, unknown = FichaPageLabels.NO_ISSUE),
                Rung(ladder.paid, FichaPageLabels.PAID, FichaPageLabels.NOT_PAID, source = null),
                silverRung(ladder.silver),
            ),
        )
    }

    fun holeRungs(ladder: PriceLadder, issueId: Int?): List<FichaRung> {
        if (!settled) return emptyList()
        // A hole's issue comes from a listing, so an unknown one was never asked about.
        val market = marketRung(ladder.market, issueId, unknown = FichaPageLabels.NUMISTA_NOT_ASKED)
        return rungs(ladder, listOf(market, silverRung(ladder.silver)))
    }

    private fun marketRung(market: PieceValue?, issueId: Int?, unknown: String): Rung {
        val readAt = issueId?.let { book.readAt(typeId, it) }
        return Rung(
            value = market,
            label = market?.grade
                ?.let { FichaPageLabels.market(it, market.source == ValueSource.NeighbouringGrade) }
                ?: FichaPageLabels.MARKET,
            why = when {
                issueId == null -> unknown
                readAt == null -> FichaPageLabels.NUMISTA_NOT_ASKED
                else -> FichaPageLabels.NUMISTA_HAS_NO_PRICE
            },
            source = readAt?.let { numistaStampLabel(it, nowMillis, zone) },
        )
    }

    private fun silverRung(silver: PieceValue?): Rung {
        val fine = fineSilverGrams(meta)
        val spot = book.spot
        return Rung(
            value = silver,
            label = FichaPageLabels.SILVER,
            why = when {
                // An unknown metal is not «no silver».
                meta?.metal == null -> FichaPageLabels.NO_FINE_SILVER
                meta.metal != Metal.Silver -> FichaPageLabels.NO_SILVER
                fine == null -> FichaPageLabels.NO_FINE_SILVER
                else -> FichaPageLabels.NO_SPOT
            },
            source = if (fine != null && spot != null) {
                FichaPageLabels.fineSilverSource(fine, spotStampLabel(spot, nowMillis, zone))
            } else {
                null
            },
        )
    }

    private fun rungs(ladder: PriceLadder, rungs: List<Rung>): List<FichaRung> {
        val greatest = ladder.greatest
        val top = rungs.mapNotNull { it.value?.eur }.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
        return rungs.map { rung ->
            val value = rung.value
            FichaRung(
                label = rung.label,
                amount = value?.let { eurosLabel(it.eur) },
                why = rung.why.takeIf { value == null },
                source = rung.source.takeIf { value != null },
                greatest = value != null && value == greatest,
                share = ((value?.eur ?: 0.0) / top).toFloat(),
            )
        }
    }

    private class Rung(
        val value: PieceValue?,
        val label: String,
        val why: String,
        val source: String?,
    )
}
