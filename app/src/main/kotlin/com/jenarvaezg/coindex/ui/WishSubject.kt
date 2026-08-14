package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.domain.WishKey
import com.jenarvaezg.coindex.domain.WishedSlot

/**
 * What the annex draws, worded once for its two drawers: the screen and the exported list.
 *
 * The parallel of [PlateSubject] and [PiecesSubject], and for the same reason (#218): a drawer that
 * received the resolved slots would have to ask the catalogue again for every name, every face and
 * every price — three times over, once per drawer, with three chances to ask differently.
 *
 * **What the marks cost a month is deliberately not here.** ADR 0029 §5 names two places for that
 * figure and the annex is neither: it is said **in the gesture**, where the collector is deciding to
 * spend it, and **in Ajustes**, where the budget already lives. On this screen it was a third printing
 * of the same number over a list that is being used rather than budgeted — the frequency rule of
 * ADR 0026 §5 — and a decision the ticket asked to be written and not re-taken.
 */
data class WishSubject(
    val rows: List<DrawnWish>,
    /**
     * «7 casillas en 5 láminas», counted here so the screen and the paper cannot disagree.
     *
     * Null on an empty list, and it is the same clause the door keeps: a number that says nothing is
     * not shown (ADR 0028 §1). The screen is reachable empty by exactly one route — «Quitar» on the
     * last row — and there «0 casillas en 0 láminas» over «no queda ninguna casilla marcada» is the
     * same fact twice, which is what the frequency rule of ADR 0026 §5 prices.
     */
    val census: String?,
)

/**
 * One marked casilla as it is drawn: the coin, the plate it belongs to, and what it would cost.
 *
 * [id] is the plate's member id qualified by its catalog, because a lazy grid needs a stable key and
 * two catalogs can name the same coin — the label is what the collector reads, not a promise of
 * uniqueness. [key] is what the gestures address: [WishKey] is what the table is keyed by, so
 * «Quitar» removes the row that was marked and never «the one that looks like it».
 */
data class DrawnWish(
    val id: String,
    val key: WishKey,
    val typeId: Int,
    val label: String,
    /** The year on the recessed tag, which is also what opens Numista — as on a plate (#302). */
    val year: String?,
    /**
     * Which plate this casilla is a slot of, in the card-sized name (#22).
     *
     * The one thing a row has that a casilla on its own plate does not need: the list crosses plates,
     * and at a fair «1966» means nothing without «100 Pesetas de Franco» over it.
     */
    val plate: String,
    /** Which face to draw, which is the plate's declaration and never this row's (#227). */
    val printedSide: PrintedSide,
    /** What filling this casilla would cost, or null where no price is on the phone (#493). */
    val cost: String?,
)

/**
 * The name a row prints under its year, by the plate's own rule (see [printedNameOf]).
 *
 * It is the plate's rule and not a second one because these are the plate's casillas: a date run
 * labels each of them with its year, so a row that printed both would say «1886» on the tag sunk into
 * the cardboard and «1886» again underneath — which is what the emulator showed the first time this
 * screen was drawn.
 */
val DrawnWish.printedName: String?
    get() = printedNameOf(label, year)

/**
 * The marked casillas, worded once.
 *
 * @param costs what each casilla would cost, by key, out of the prices already on the phone. Empty is
 *   a phone whose pass has not landed yet, and then no row carries a price — the same absence a plate
 *   over the threshold shows, and never a «—».
 */
fun wishSubject(
    slots: List<WishedSlot>,
    costs: Map<WishKey, Double> = emptyMap(),
): WishSubject = WishSubject(
    rows = slots.map { slot ->
        DrawnWish(
            id = "${slot.catalog.id}/${slot.member.id}",
            key = slot.key,
            typeId = slot.typeId,
            label = slot.member.label,
            year = slot.member.year?.toString(),
            plate = slot.catalog.shortName,
            printedSide = slot.catalog.printedSide,
            cost = costs[slot.key]?.let(::holeCostLabel),
        )
    },
    census = slots
        .takeIf { it.isNotEmpty() }
        ?.let { marked ->
            wishCensusLabel(slots = marked.size, plates = marked.distinctBy { it.catalog.id }.size)
        },
)
