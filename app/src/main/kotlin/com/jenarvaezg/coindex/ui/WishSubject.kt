package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.domain.WishKey
import com.jenarvaezg.coindex.domain.WishedSlot

/**
 * What the annex draws, worded once for the screen and the exported list (#218), like
 * [PlateSubject] and [PiecesSubject].
 *
 * The marks' monthly cost is deliberately not here: ADR 0029 §5 prints it in the marking gesture
 * and on «Este teléfono», where the budget lives, and a third place would repeat it
 * (ADR 0026 §5).
 */
data class WishSubject(
    val rows: List<DrawnWish>,
    /**
     * «7 casillas en 5 láminas», counted once for the screen and the paper. Null on an empty list,
     * where it would repeat the empty explanation (ADR 0026 §5).
     */
    val census: String?,
)

/**
 * One marked casilla as drawn: the coin, its plate, and what it would cost. [id] is the member id
 * qualified by catalog, a stable key for the lazy grid since two catalogs can share a coin. [key]
 * is what the gestures address, so «Quitar» removes exactly the row that was marked.
 */
data class DrawnWish(
    val id: String,
    val key: WishKey,
    val typeId: Int,
    val label: String,
    /** The year on the recessed tag, which also opens the coin's sheet, as on a plate (#508). */
    val year: String?,
    /** The plate's short name (#22): the list crosses plates, and a bare «1966» means nothing. */
    val plate: String,
    /** Which face to draw, as the plate declares it (#227). */
    val printedSide: PrintedSide,
    /** What filling this casilla would cost, or null where no price is on the phone (#493). */
    val cost: String?,
)

/**
 * The name a row prints under its year, by the plate's rule ([printedNameOf]): a date run labels
 * its casillas with their year, which would otherwise print twice.
 */
val DrawnWish.printedName: String?
    get() = printedNameOf(label, year)

/**
 * The marked casillas, worded once.
 *
 * @param costs what each casilla would cost, by key, from the prices on the phone. Empty until the
 *   pass lands; rows then carry no price, never a «—».
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
            label = slot.member.label.weldUnits(),
            year = slot.member.year?.toString(),
            plate = slot.catalog.shortName.weldUnits(),
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
