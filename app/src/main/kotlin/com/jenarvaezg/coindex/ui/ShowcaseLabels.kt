package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.prices.ValuationRefusal
import java.time.ZoneId

/**
 * Every string «Explorar» prints (ADR 0030, ADR 0026 §6). The shelf window's vocabulary is
 * «Explorar», «entrar» (what a plate not yours costs), «Tasar» (the gesture that asks its price)
 * and «lo que te falta». «Escaparate» is the project's word for the feature, never shown.
 */
object ShowcaseLabels {
    /**
     * The screen's name, printed by the masthead (ADR 0026 §8). It covers both rooms of the annex:
     * the coins being looked for and the shelf of plates not owned.
     */
    const val DESTINATION: String = "Explorar"

    /**
     * What the shelf holds, said once under its heading. It names both populations the grid mixes,
     * so the collector's own plates in here don't look like a bug.
     */
    const val SENTENCE: String = "Las láminas donde te falta algo: las tuyas con casillas marcadas " +
        "y las que no coleccionas."

    /**
     * That browsing is free and a price is asked for by hand (ADR 0030 §2, §3); said once here,
     * not on every tile (ADR 0026 §5).
     */
    const val FREE_SENTENCE: String = "Hojear no cuesta nada. Cada lámina se tasa cuando la abres."

    /** The shelf's search box (ADR 0026 §8, clause 4). */
    const val SEARCH_PLACEHOLDER: String = "Buscar entre las láminas"

    /** What the shelf says with a search that matches nothing. */
    const val NO_MATCHES: String = "Ninguna lámina se llama así."

    /**
     * The empty shelf: every curated catalog holds a coin of the collector's. Like
     * `WishLabels.EMPTY_EXPLANATION`, it says why it is empty rather than «no hay nada».
     */
    const val EMPTY: String = "No queda ninguna lámina curada sin una moneda tuya dentro."

    /**
     * The gesture that spends, shown with its ceiling before it is pressed (ADR 0030 §3, #282). A
     * ceiling, not an estimate: a hole costs one call if its curated file names the issue and two
     * otherwise, and the spend must never be understated.
     */
    const val VALUE_ACTION: String = "Tasar esta lámina"

    /**
     * The same gesture on a plate that already has a price (ADR 0030 §4). Never hidden: that price
     * never expires, so this is the only way to refresh it.
     */
    const val REVALUE_ACTION: String = "Volver a tasar"

    /** While the calls are in flight, in the words the ficha's own gesture uses. */
    const val VALUING: String = "Preguntando a Numista…"

    /**
     * The snackbar when a tasación had nothing to ask (ADR 0028 §5): the pass's price lifetime
     * decides whether an issue is asked again, and the gesture follows it.
     *
     * It quotes that lifetime (`PRICE_LIFETIME_MILLIS`, #561) and must change with it. The plate's
     * header gives the exact date (`valuedAgeLabel`), so this line states the rule, not the age.
     */
    const val ALREADY_FRESH: String =
        "Esta lámina ya está tasada: sus precios son de hace menos de tres meses."

    /** What a plate says when Numista had no price for a single one of its casillas. */
    const val NOTHING_PRICED: String = "Numista no da precio de ninguna de estas casillas."

    /**
     * The money order on a shelf with no price anywhere (#513). Counts nothing, unlike
     * [showcaseOrderNote]'s other form. It says what the order lacks rather than that nothing
     * changes: the grid does move, since the default order leads with the marked plates
     * (`showcaseShelf`) and this one doesn't.
     */
    const val NOTHING_VALUED: String =
        "Todavía no hay ninguna lámina tasada: este orden no tiene precios con los que ordenar."
}

/**
 * What «por coste de entrar» couldn't place (#513). Sorting by amount leaves the plates without one
 * at the end (ADR 0030 §8, clause 3), and this line says so where the collector is looking, as
 * «Este teléfono» does for the pass (ADR 0028 §6).
 *
 * Counts shelf-window plates only: the collector's own have no cost of entering and no gesture to
 * ask for one (ADR 0030 §3, §6). Null in the default order and when nothing is unvalued. Counts the
 * shelf as shown, after any search.
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
 * Why a tasación didn't happen, for the gesture's snackbar (ADR 0028 §4). Worded as the answer to a
 * press, unlike [valuationLabel]'s states on «Este teléfono». A failed refresh is never worse than
 * not asking (ADR 0025).
 */
fun showcaseRefusalMessage(refusal: ValuationRefusal): String = "No se ha podido tasar: " + when (refusal) {
    ValuationRefusal.Syncing -> "espera a que termine el sincronizado."
    ValuationRefusal.BudgetExhausted -> "se acabó el presupuesto de consultas de este mes."
    ValuationRefusal.Offline -> "no hay red."
    ValuationRefusal.NoApiKey -> "faltan las credenciales de Numista."
    // Not reworded; [NUMISTA_IS_REFUSING] says why.
    ValuationRefusal.Rejected -> NUMISTA_IS_REFUSING
}

/**
 * The gesture and its spend: «Tasar esta lámina · 34 consultas» (ADR 0030 §3), in [queriesLabel]'s
 * unit (#516). No figure when there is nothing to ask: «· 0 consultas» would read as broken, and a
 * press gets [ShowcaseLabels.ALREADY_FRESH].
 */
fun showcaseValueAction(calls: Int, valued: Boolean, valuing: Boolean): String = when {
    valuing -> ShowcaseLabels.VALUING
    else -> {
        val head = if (valued) ShowcaseLabels.REVALUE_ACTION else ShowcaseLabels.VALUE_ACTION
        if (calls > 0) "$head · ${queriesLabel(calls)}" else head
    }
}

/**
 * The money figure of a plate that isn't yours, with its name (#493), its criterion (holes are
 * priced in `unc`, ADR 0028 §8) and its date, since it never expires (ADR 0030 §4). The date is the
 * oldest read (#494).
 */
fun showcaseEntryLabel(
    cost: ShowcaseCost,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String = listOfNotNull(
    "${FiguresLabels.SHOWCASE_ENTRY_LABEL}: ${eurosLabel(cost.eur)}",
    FiguresLabels.HOLE_CRITERION,
    // What the amount covers when it isn't the whole plate: unpriced or unasked holes add nothing
    // (ADR 0028 §4), and a partial total read as whole would be false (§7).
    coverageLabel(cost),
    valuedAgeLabel(cost.readAt, nowMillis, zone),
).joinToString(" · ")

/** «4 de 12 casillas», or null where the amount covers the whole plate. */
private fun coverageLabel(cost: ShowcaseCost): String? =
    "${cost.holes} de ${showcaseSlotsLabel(cost.slots)}".takeIf { cost.holes < cost.slots }

/**
 * How old a hand-asked price is: [priceAgeLabel] after «tasada», tying it to the gesture (#594).
 * Totals of the collection name the source instead.
 */
fun valuedAgeLabel(
    readAtMillis: Long,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String = "tasada ${priceAgeLabel(readAtMillis, nowMillis, zone)}"

/**
 * How many casillas a shelf-window plate has, under its tile (ADR 0030 §8), where an index card
 * shows a fraction: `0/12` would read as a reproach on a plate nobody is collecting.
 */
fun showcaseSlotsLabel(slots: Int): String = plural(slots, "casilla", "casillas")

/**
 * A valued tile's amount and age. No criterion, which would repeat on every tile (ADR 0026 §5); the
 * date stays, since the price never expires.
 */
fun showcaseTileCostLabel(
    cost: ShowcaseCost,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String = "${eurosLabel(cost.eur)} · ${valuedAgeLabel(cost.readAt, nowMillis, zone)}"

/**
 * The index row that opens the shelf: «Y otras 20 láminas que no coleccionas» (#520). It sits at
 * the foot, where ADR 0026 §8 clause 3 puts an annex's door; the marks have their own row at the
 * head.
 *
 * Counts the shelf window only, not the collector's marked plates (ADR 0030 §8, clause 5). Null at
 * zero. The singular is spelt out because `plural` would give «otra 1 lámina». The arrow is drawn,
 * not typed (#298).
 */
fun showcaseDoorLabel(plates: Int): String? = when {
    plates <= 0 -> null
    plates == 1 -> "Y otra lámina que no coleccionas"
    else -> "Y otras $plates láminas que no coleccionas"
}

/**
 * How many casillas of one of the collector's plates are marked, under its tile: what put the plate
 * on this shelf. Lower case, like the chip in the hole.
 */
fun showcaseWishedLabel(marks: Int): String = "$marks ${WishLabels.MARK_WORD}"

/**
 * The shelf's orders, for its folded line (ADR 0030 §8). «Por casillas» is the default and isn't
 * mentioned in the folded line, as in `indexShelfSummary`; «por coste de entrar» (#282) only sorts
 * valued plates.
 *
 * Declaration order is drawing order (#513): `ExploreScreen` walks `entries`.
 */
enum class ShowcaseSort(val label: String) {
    ByCasillas("Por casillas"),
    ByEntryCost("Por coste de entrar"),
}
