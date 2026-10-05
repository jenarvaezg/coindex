package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.data.prices.PriceBook
import com.jenarvaezg.coindex.data.prices.showcaseCallCount
import com.jenarvaezg.coindex.domain.ShowcasePlate
import com.jenarvaezg.coindex.domain.WishKey

/**
 * A plate's money and what tasar it would cost, decided once for the screen that draws it.
 *
 * One decision picks the régime (ADR 0030 §3): a shelf-window plate shows a cost of entering, a
 * collector's plate its value and cost of closing (ADR 0028), and until the market lands neither
 * shows anything (ADR 0028 §7). A class rather than lambdas in the root composable so the choice
 * is testable on the JVM and the screen can key its `remember` on a stable instance; the root
 * builds a new one only when its readings change.
 *
 * Whether a tasación is running is not in here: it flips twice per press, and holding it would
 * rebuild the object the album walk is remembered on. The screen gets that bit separately.
 *
 * @param showcase the shelf window (ADR 0030 §1), which alone decides whether this plate has a
 *   cost of entering.
 * @param state the inventory and the fichas, for the weight and the pieces an amount is made of.
 * @param book the whole price book, so the header and the casillas agree on when (ADR 0028, #536).
 * @param settled whether the market has finished arriving. Gates the collector's plates only: a
 *   shelf-window plate is priced by its own gesture.
 * @param waiting whether that absence is said on the plate (#519). A boolean rather than the
 *   `ValuationStatus`, whose count moves during a pass and would rebuild this object.
 * @param wished the casillas the collector marked, priced whatever the plate's shape
 *   (ADR 0029 §4).
 * @param nowMillis for the age of a hand-asked price (ADR 0030 §4) and the calls the gesture would
 *   spend. Read once per arrival of a price, never per frame.
 * @param onValue starts the pass over one plate's holes, by catalog id.
 * @param onMessage reports a press that had nothing to ask (see [press]).
 */
class PlateFinance(
    private val showcase: List<ShowcasePlate>,
    private val state: CollectionState,
    private val book: PriceBook,
    private val settled: Boolean,
    private val waiting: Boolean,
    private val wished: Set<WishKey>,
    private val nowMillis: Long,
    private val onValue: (catalogId: String) -> Unit,
    private val onMessage: (UiNotice) -> Unit,
) {
    /**
     * Everything this plate's header says about money, and the price inside each empty casilla. The
     * readings arrive together or not at all (#493).
     */
    fun money(resolved: PlateResult.Available): PlateMoney {
        val window = window(resolved)
        return when {
            window != null -> showcaseMoney(window, state, book)
            !settled -> PlateMoney(waiting = waiting)
            else -> plateMoney(resolved.album, state, book, wished)
        }
    }

    /**
     * What tasar this plate would spend, printed on the gesture before it is pressed (ADR 0030 §3,
     * #282). Zero on a collector's plate, which has no gesture, and on a shelf-window plate whose
     * prices are all fresh.
     */
    fun calls(resolved: PlateResult.Available): Int =
        window(resolved)?.let { showcaseCallCount(it, book, nowMillis) } ?: 0

    /**
     * Pressing «Tasar esta lámina»: one pass over this plate's holes. With nothing to ask it says
     * so instead (ADR 0028 §5), so the button never silently does nothing.
     */
    fun press(resolved: PlateResult.Available) {
        if (calls(resolved) > 0) {
            onValue(resolved.catalog.id)
        } else {
            onMessage(UiNotice(ShowcaseLabels.ALREADY_FRESH))
        }
    }

    private fun window(resolved: PlateResult.Available): ShowcasePlate? =
        showcase.firstOrNull { it.catalog.id == resolved.catalog.id }
}
