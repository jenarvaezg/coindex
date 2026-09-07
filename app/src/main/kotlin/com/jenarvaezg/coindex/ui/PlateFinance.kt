package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.data.prices.PriceBook
import com.jenarvaezg.coindex.data.prices.showcaseCallCount
import com.jenarvaezg.coindex.domain.ShowcasePlate
import com.jenarvaezg.coindex.domain.WishKey

/**
 * The money of a plate and what tasar it would cost, decided once for the screen that draws it.
 *
 * **Which of the three régimes a plate is under is one decision** (ADR 0030 §3): a plate of the shelf
 * window is priced as a cost of entering, one of the collector's says what it holds and what closing
 * it would cost (ADR 0028), and while the market is still arriving neither says anything at all (ADR
 * 0028 §7). The three used to be a `when` inside two lambdas written in the body of the root
 * composable, which is where they went wrong twice over: nobody could test the choice without a
 * device, and a lambda literal is a **new object on every recomposition** — so the plate's own
 * `remember`, which keys its subject on the reading it was handed, never hit once and re-walked the
 * album on every frame of the entrance.
 *
 * So this is a class and not a pair of functions: the screen keys on the instance, and the root builds
 * one instance per change of the readings behind it. Nothing here is a composable and nothing here
 * touches Android — the three branches, the gesture's ceiling and what pressing it does are all
 * answerable in a JVM test.
 *
 * **What is in flight is not in here**, which is the same rule read once more: whether this plate's
 * tasación is running flips twice per press and moves no amount at all, so holding it would rebuild
 * the very object the album walk is remembered on. The screen is handed that bit apart.
 *
 * @param showcase the shelf window as the root crossed it once (ADR 0030 §1), which is what says
 *   whether **this** plate is one of the twenty. The resolution's own `mine` is not asked: a plate the
 *   window does not hold has no cost of entering to print, whatever anything else says about it.
 * @param state the inventory and the fichas, for the weight and the pieces an amount is made of.
 * @param book the whole price book and not its readings, so the header and the casillas cannot
 *   disagree about *when* (ADR 0028, #536).
 * @param settled whether the market has finished arriving, which gates the collector's own plate and
 *   never the window's: those prices arrive by a gesture of their own, so waiting for the market of a
 *   collection this plate has no coin in would leave the amount off a plate that was just valued.
 * @param waiting whether that absence is worth saying on the plate (#519). The two answers of the pass
 *   and not the pass itself, which is what keeps this object still while one runs: `ValuationStatus`
 *   carries a count that moves every twenty-five issues, and rebuilding the reading on it would rebuild
 *   the very object the album walk is remembered on.
 * @param wished the casillas the collector marked, which carry a price whatever the plate's shape
 *   (ADR 0029 §4).
 * @param nowMillis now, for the age of a hand-asked price (ADR 0030 §4) and for the calls the gesture
 *   would spend. Read once per arrival of a price, never per frame.
 * @param onValue starts the pass over one plate's holes, by catalog id: the unit the collector chose.
 * @param onMessage says out loud what a press bought nothing, which is [press]'s other half.
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
     * Everything this plate's header says about money, and the price inside each of its empty casillas.
     *
     * The three readings arrive together or none of them does (#493): they are one walk of the same
     * album, and a drawer holding this empty cannot print any of the three.
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
     * What tasar this plate would spend, which is the ceiling the gesture prints **before** it is
     * pressed (ADR 0030 §3, #282).
     *
     * Zero on a plate of the collector's, and that is how the screen knows it has no gesture at all: no
     * pass would ever ask for those holes by this route. Zero too on a plate of the window whose prices
     * are all fresh — what a press then answers is [press]'s.
     */
    fun calls(resolved: PlateResult.Available): Int =
        window(resolved)?.let { showcaseCallCount(it, book, nowMillis) } ?: 0

    /**
     * What pressing «Tasar esta lámina» does: one pass over **this** plate's holes and nothing else.
     *
     * Nothing to ask is answered here and not by a pass that would ask for nothing: the gesture never
     * buys the same answer twice (ADR 0028 §5), and a press that did nothing silently is a button the
     * collector reads as broken.
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
