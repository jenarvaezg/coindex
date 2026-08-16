package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.prices.ValuationRefusal
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Every string «Explorar» prints (ADR 0030, ADR 0026 §6).
 *
 * The vocabulary of the shelf window is **four things**: «Explorar» over the screen, «entrar» for what
 * a plate that is not yours costs, «Tasar» for the gesture that asks its price, and «lo que te falta»
 * for what the shelf is of. Nothing here says «escaparate» — that is the word this project uses to
 * talk about the feature, not a word the collector is shown — and nothing here says «no coleccionas»
 * twice: the door of the index says it once, on the way in.
 */
object ShowcaseLabels {
    /**
     * The screen's own name, printed by the masthead (ADR 0026 §8).
     *
     * «Explorar» and not «Lo que busco», which was its name only while the shelf did not exist —
     * `WishLabels.DESTINATION` says so in as many words. The annex now holds two things and this is the
     * one word that covers both: a list of coins you are hunting and a shelf of plates you do not own
     * are both places to look rather than things you have.
     */
    const val DESTINATION: String = "Explorar"

    /**
     * What the shelf is of, under its heading, said once.
     *
     * It has to name the two populations because the grid mixes them — your plates with a marked
     * casilla, and the twenty you own nothing of — and a collector who saw their own Kookaburra in here
     * without being told would read it as a bug.
     */
    const val SENTENCE: String = "Las láminas donde te falta algo: las tuyas con casillas marcadas " +
        "y las que no coleccionas."

    /**
     * That browsing is free, and that a price is asked for by hand (ADR 0030 §2, §3).
     *
     * The one place the app promises the shelf costs nothing, and it is said **here** rather than on
     * each of twenty tiles: the frequency rule of ADR 0026 §5 prices a word by how often it is
     * printed, and what the tiles say is which plate they are.
     */
    const val FREE_SENTENCE: String = "Hojear no cuesta nada. Cada lámina se tasa cuando la abres."

    /** The search box of the shelf, which is the shelf invariant of ADR 0026 §8 clause 4. */
    const val SEARCH_PLACEHOLDER: String = "Buscar entre las láminas"

    /** What the shelf says with a search that matches nothing. */
    const val NO_MATCHES: String = "Ninguna lámina se llama así."

    /**
     * The empty shelf, which is a collection that has reached every curated catalog.
     *
     * Not «no hay nada»: what it says is what would have to happen for something to appear, which is
     * the same shape `WishLabels.EMPTY_EXPLANATION` has. Unreachable today — the father is 20 plates
     * from it and Jose 30 — and written because a screen with nothing on it is where a collector
     * decides the app is broken.
     */
    const val EMPTY: String = "No queda ninguna lámina curada sin una moneda tuya dentro."

    /**
     * The gesture that spends, with its ceiling in it (ADR 0030 §3).
     *
     * The spend is named **before** it is pressed, which is #282's rule and the one ADR 0028 §3 gained
     * when it gained a gesture. It is a ceiling and not an estimate: a hole whose curated file names
     * its issue costs one call and any other costs two, and rounding a spend down is the direction this
     * sentence must never err in.
     */
    const val VALUE_ACTION: String = "Tasar esta lámina"

    /**
     * The same gesture on a plate that already carries a price (ADR 0030 §4).
     *
     * It never disappears, because the price it asks for never expires: what would otherwise happen is
     * an amount from March with no way on earth to refresh it.
     */
    const val REVALUE_ACTION: String = "Volver a tasar"

    /** While the calls are in flight, in the words the ficha's own gesture uses. */
    const val VALUING: String = "Preguntando a Numista…"

    /**
     * What a tasación that asked for nothing says, and why (ADR 0028 §5).
     *
     * A plate valued last week has every price it can have: the pass's own thirty days are what decide
     * whether an issue is asked about again, and the gesture obeys them rather than buying the same
     * answer twice. Said in a snackbar because it is the answer to a press, not a state of the plate.
     */
    const val ALREADY_FRESH: String =
        "Esta lámina ya está tasada: sus precios son de hace menos de un mes."

    /** What a plate says when Numista had no price for a single one of its casillas. */
    const val NOTHING_PRICED: String = "Numista no da precio de ninguna de estas casillas."

    /**
     * The shelf ordered by money with no money anywhere on it (#513).
     *
     * It counts nothing, unlike [showcaseOrderNote]'s other form: with every plate at the end there is
     * no «las tasadas» to compare the figure against, and «20 láminas sin tasar, al final» describes an
     * order that placed none of them as though it had placed some.
     *
     * **And it says what the order has, not what the screen does.** «Este orden no cambia nada» was
     * written first and is false wherever the collector has marked a casilla: the default order leads
     * with the marked plates (`showcaseShelf`) and this one does not, so the grid moves — by dropping
     * their own plates to the end — while nothing on it has a price. What is always true is the thing
     * the collector needs: there is no amount for this order to sort by.
     */
    const val NOTHING_VALUED: String =
        "Todavía no hay ninguna lámina tasada: este orden no tiene precios con los que ordenar."
}

/**
 * What «por coste de entrar» could not place, under the two orders (#513).
 *
 * An order that sorts by an amount can only sort what has one, and ADR 0030 §8 clause 3 leaves the rest
 * behind it — which on a shelf that is born with no amount at all is a control that changes the screen
 * invisibly or not at all. This is the transparency Ajustes already prints under the pass (ADR 0028 §6):
 * one line, in the place the collector is looking, saying what the app did with what it had.
 *
 * **It counts the plates of the window and never the collector's own.** A plate of theirs carries no
 * `entryEur` and never will — entering a plate you already collect is not a thing that costs, so it has
 * no «Coste de entrar» and no gesture to ask for one (ADR 0030 §3, §6) — so counting it among «las que
 * faltan por tasar» would send the collector looking for a button that is not there.
 *
 * Null in the default order, because «por casillas» needs no datum that could be missing, and null when
 * there is nothing to warn about — every plate of the window valued, or a shelf with none on it at all.
 * It reads the shelf **as shown**: a search narrowed to three unvalued plates is three, not the twenty
 * behind the box.
 */
fun showcaseOrderNote(sort: ShowcaseSort, shelf: List<ShowcaseTile>): String? {
    if (sort != ShowcaseSort.ByEntryCost) return null
    val valued = shelf.count { it.entryEur != null }
    val unvalued = shelf.count { !it.mine && it.entryEur == null }
    return when {
        unvalued == 0 -> null
        valued == 0 -> ShowcaseLabels.NOTHING_VALUED
        else -> "${plural(unvalued, "lámina", "láminas")} sin tasar, al final: " +
            "este orden sólo coloca las tasadas."
    }
}

/**
 * Why the tasación did not happen, for the snackbar of the gesture (ADR 0028 §4).
 *
 * Its own sentence and not `valuationLabel`'s: that one is a **state** in the settings screen —
 * «esperan a que haya red» about prices arriving on their own — and this one is the answer to a press.
 * A collector who has just spent a gesture needs to know that nothing was spent and nothing was
 * written, which is the promise ADR 0025 makes: a refresh that fails is never worse than not asking.
 */
fun showcaseRefusalMessage(refusal: ValuationRefusal): String = "No se ha podido tasar: " + when (refusal) {
    ValuationRefusal.Syncing -> "espera a que termine el sincronizado."
    ValuationRefusal.BudgetExhausted -> "se acabó el presupuesto de llamadas de este mes."
    ValuationRefusal.Offline -> "no hay red."
    ValuationRefusal.NoApiKey -> "faltan las credenciales de Numista."
}

/**
 * The gesture and its spend: «Tasar esta lámina · 34 consultas» (ADR 0030 §3).
 *
 * The unit is **consultas** and not «llamadas», which is the word the marking mode already uses for the
 * same thing on the same subject (`WishLabels.MARK_HINT`): what the collector is told about is what the
 * app is going to ask Numista, and two words for one spend on two surfaces of one feature is the
 * vocabulary rule of ADR 0026 §6.
 *
 * **No figure at all when there is nothing left to ask**, which is a plate whose prices are all fresh:
 * «· 0 consultas» reads as a gesture that is broken rather than as one that has nothing to do, and what
 * happens when it is pressed is [ShowcaseLabels.ALREADY_FRESH].
 */
fun showcaseValueAction(calls: Int, valued: Boolean, valuing: Boolean): String = when {
    valuing -> ShowcaseLabels.VALUING
    else -> {
        val head = if (valued) ShowcaseLabels.REVALUE_ACTION else ShowcaseLabels.VALUE_ACTION
        if (calls > 0) "$head · ${queriesLabel(calls)}" else head
    }
}

/** The spend of the shelf window, in the unit the marking mode already spends in. */
fun queriesLabel(calls: Int): String = plural(calls, "consulta", "consultas")

/**
 * The one figure of a plate that is not yours: what entering costs, and when it was read.
 *
 * Three things in one line, and each of them is load-bearing: the **name**, because a figure of money
 * on a plate has to say which of the two it is (#493); the **provenance**, `en sin circular`, because a
 * hole is priced in `unc` and has no «lo que pagaste» (ADR 0028 §8); and the **date**, because this
 * amount never expires and nothing will ever refresh it (ADR 0030 §4). It is the oldest of its reads,
 * which is how a total with two ages is dated (#494).
 */
fun showcaseEntryLabel(
    cost: ShowcaseCost,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String = listOfNotNull(
    "${FiguresLabels.SHOWCASE_ENTRY_LABEL}: ${eurosLabel(cost.eur)}",
    FiguresLabels.HOLE_CRITERION,
    // **What the amount covers, whenever it is not the whole plate.** The figure is a floor — a casilla
    // Numista had no price for adds nothing, and a pass cut short by the budget leaves the rest unasked
    // (ADR 0028 §4) — and «412 €» over a plate of twelve where four were priced is the kind of total
    // that is not incomplete but false (§7). Silent when it covers every casilla, because then the
    // fraction says what the plate already said.
    coverageLabel(cost),
    valuedAgeLabel(cost.readAt, nowMillis, zone),
).joinToString(" · ")

/** «4 de 12 casillas», or null where the amount covers the whole plate. */
private fun coverageLabel(cost: ShowcaseCost): String? =
    "${cost.holes} de ${showcaseSlotsLabel(cost.slots)}".takeIf { cost.holes < cost.slots }

/**
 * How old a hand-asked price is, in the coarsest unit that is still true.
 *
 * The ficha's own wording (`fichaAgeLabel`) applied to the other thing this app brings and keeps: a
 * price. Calendar days and not elapsed milliseconds, so a tasación from last night reads «ayer» this
 * morning instead of «hace 11 horas» rounded to today.
 */
fun valuedAgeLabel(
    readAtMillis: Long,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val read = Instant.ofEpochMilli(readAtMillis).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    // A clock that has gone backwards is not a price from the future: it is today's.
    val days = ChronoUnit.DAYS.between(read, today).coerceAtLeast(0)
    return "tasada " + when {
        days == 0L -> "hoy"
        days == 1L -> "ayer"
        days < 30L -> "hace ${plural(days.toInt(), "día", "días")}"
        // **Past a month it says the day and not the age**, which is where this parts company with
        // `fichaAgeLabel`: a ficha can always be brought again, and this price cannot — nothing will ever
        // refresh it (ADR 0030 §4). «hace 8 meses» over an amount that is going to sit there for years is
        // the date §4 asks for, rounded away.
        else -> "el ${dayMonthYearLabel(read)}"
    }
}

/**
 * How many casillas a plate of the shelf window has, under its tile (ADR 0030 §8).
 *
 * What a tile of the window says where a card of the index says its fraction — and it is not `0/12`,
 * which is a fraction of a plate you are collecting and would read as a reproach on a plate you are
 * not. Twelve casillas is what the plate **is**.
 */
fun showcaseSlotsLabel(slots: Int): String = plural(slots, "casilla", "casillas")

/**
 * What one tile says once its plate has been valued: the amount and how old it is.
 *
 * Shorter than the plate's own line by one thing — the provenance — because a shelf of twenty tiles
 * would print «en sin circular» twenty times, which is the frequency ADR 0026 §5 prices. The date
 * stays: it is the difference between «entrar cuesta esto» and «costaba esto en marzo».
 */
fun showcaseTileCostLabel(
    cost: ShowcaseCost,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String = "${eurosLabel(cost.eur)} · ${valuedAgeLabel(cost.readAt, nowMillis, zone)}"

/**
 * How many casillas of a plate of the collector's they are looking for, under its tile.
 *
 * The tile of one of *their* plates in this shelf, which is what makes the shelf «lo que te falta» and
 * not «lo que no coleccionas»: the fraction is already on its card in the index, so what this tile adds
 * is the thing that put it here. Lower case like the chip in the hole, because it is the same note.
 */
fun showcaseWishedLabel(marks: Int): String = "$marks ${WishLabels.MARK_WORD}"

/**
 * The order the shelf is read in, for its folded line (ADR 0030 §8).
 *
 * Two and no more. «Por casillas» is the default and says nothing in the folded line — the same rule
 * `indexShelfSummary` follows for the sort the index would have used anyway — and «por coste de entrar»
 * is the one #282 chose, which can only sort what has been valued and leaves the rest behind it.
 *
 * **The order they are declared in is the order they are drawn in** (#513): `ExploreScreen` walks
 * `entries` rather than naming the two, so moving a constant here moves the chips on the screen.
 */
enum class ShowcaseSort(val label: String) {
    ByCasillas("Por casillas"),
    ByEntryCost("Por coste de entrar"),
}
