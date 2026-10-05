package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.CoinClaims
import java.text.Collator
import java.util.Locale

/**
 * One cell of the country axis: a measurable slot, or a loose piece that lives in its country's
 * block with no cardboard behind it (ADR 0026 §9).
 */
sealed interface CountryAxisCell {
    val typeId: Int?
    val quantity: Int

    /** A catalog member — owned or missing — that contributes to the country's ratio. */
    data class Slot(
        val catalogId: String,
        val memberId: String,
        override val typeId: Int?,
        val owned: Boolean,
        override val quantity: Int,
    ) : CountryAxisCell

    /** A piece no casilla claims: no cardboard behind it, and not in the denominator. */
    data class Loose(
        val itemId: Long,
        override val typeId: Int,
        override val quantity: Int,
    ) : CountryAxisCell
}

/**
 * One country on the country axis, already ordered by [countryAxisOrder].
 *
 * [issued] is null when the block holds only loose pieces: the label then carries a count and no
 * denominator. Blocks with one or two loose coins and no slots go in the compact tail.
 */
data class CountryAxisBlock(
    val country: String,
    val owned: Int,
    val issued: Int?,
    val cells: List<CountryAxisCell>,
) {
    val compact: Boolean get() = issued == null && cells.size <= 2

    val label: String
        get() = if (issued != null) "$owned/$issued" else owned.toString()
}

data class CountryAxisModel(
    val blocks: List<CountryAxisBlock>,
    val ownedSlots: Int,
    val totalSlots: Int,
) {
    val body: List<CountryAxisBlock> get() = blocks.filterNot { it.compact }
    val tail: List<CountryAxisBlock> get() = blocks.filter { it.compact }
}

/** What a country block paints, and how many absences stay behind the fold (#417). */
data class CountryAxisFold(
    val cells: List<CountryAxisCell>,
    /**
     * Absences behind the fold, the number the fold label shows whether folded or open. Zero means
     * no fold at all.
     */
    val foldable: Int,
)

/**
 * A country's coins first, then one row of absences, and the rest behind «… y faltan N»
 * (`docs/ux/pliegue-417.md`).
 *
 * Coins come first, so the block no longer shows where a coin falls in its series; the plate does
 * (#473). One row of absences always shows what the series looks like. The fold appears only when
 * it hides more than one row's worth. Loose pieces count as coins, not absences (§9).
 *
 * @param columns holes per row, from the block's actual width.
 * @param expanded whether the collector opened this country's fold.
 */
fun CountryAxisBlock.fold(columns: Int, expanded: Boolean = false): CountryAxisFold {
    val missing = cells.filter { it is CountryAxisCell.Slot && !it.owned }
    val present = cells.filterNot { it is CountryAxisCell.Slot && !it.owned }
    // With no measured width, paint everything rather than hide it all.
    val foldable = if (columns <= 0) 0 else (missing.size - columns).coerceAtLeast(0)
    if (expanded || foldable == 0) {
        return CountryAxisFold(cells = present + missing, foldable = foldable)
    }
    return CountryAxisFold(cells = present + missing.take(columns), foldable = foldable)
}

/**
 * The notebook's country axis (ADR 0026 §9, atlas-315).
 *
 * Each measurable casilla is a cell in its member's country, not the catalog's (#170); loose pieces
 * join their country's block. Ordered by [countryAxisOrder]. It only groups `state.slots` (#538),
 * so the axis and the plate can't disagree about a hole.
 */
fun countryAxis(
    state: CollectionState,
    /**
     * Tells loose pieces from placed ones. Defaults to the assembly's (#540), shared with Monedas
     * and the notebook's loose-coin lámina; tests pass an empty one for an all-loose axis.
     */
    claims: CoinClaims = state.claims,
    /**
     * Catalog ids that survive the shelf's filters, or null for every evidenced catalog. Filters
     * still apply off the plate axis: a hidden catalog's slots don't paint.
     */
    keptCatalogIds: Set<String>? = null,
    /** Loose row ids that survive the shelf, or null to keep every unclaimed piece. */
    keptLooseIds: Set<Long>? = null,
    /**
     * With the país chip on, only that country's cells paint (#415), even when a kept plate spans
     * other countries.
     */
    keptCountry: String? = null,
): CountryAxisModel {
    val byCountry = linkedMapOf<String, MutableList<CountryAxisCell>>()

    for (slot in state.slots) {
        if (keptCatalogIds != null && slot.catalogId !in keptCatalogIds) continue
        // A casilla with no known country has no block to go in.
        val country = slot.country ?: continue
        if (keptCountry != null && country != keptCountry) continue
        byCountry.getOrPut(country) { mutableListOf() }.add(
            CountryAxisCell.Slot(
                catalogId = slot.catalogId,
                memberId = slot.memberId,
                typeId = slot.typeId,
                owned = slot.owned,
                quantity = slot.quantity,
            ),
        )
    }

    for (piece in state.items) {
        if (piece.quantity <= 0 || claims.claimed(piece)) continue
        if (keptLooseIds != null && piece.id !in keptLooseIds) continue
        val meta = state.typeMeta[piece.typeId]
        val country = meta?.country ?: continue
        if (keptCountry != null && country != keptCountry) continue
        byCountry.getOrPut(country) { mutableListOf() }.add(
            CountryAxisCell.Loose(
                itemId = piece.id,
                typeId = piece.typeId,
                quantity = piece.quantity,
            ),
        )
    }

    val blocks = byCountry.map { (country, cells) ->
        val slots = cells.filterIsInstance<CountryAxisCell.Slot>()
        val loose = cells.filterIsInstance<CountryAxisCell.Loose>()
        val issued = slots.size.takeIf { it > 0 }
        // Ratio counts casillas; a loose-only block counts pieces («Francia 9»).
        val owned = if (issued != null) {
            slots.count { it.owned }
        } else {
            loose.sumOf { it.quantity }
        }
        CountryAxisBlock(
            country = country,
            owned = owned,
            issued = issued,
            cells = cells,
        )
    }.sortedWith(countryAxisOrder())

    return CountryAxisModel(
        blocks = blocks,
        ownedSlots = blocks.sumOf { block -> block.cells.filterIsInstance<CountryAxisCell.Slot>().count { it.owned } },
        totalSlots = blocks.sumOf { block -> block.issued ?: 0 },
    )
}

/**
 * Like `indexOrder()`: has ratio ↓, ratio ↓, denominator ↓, name ↑. Opens on the most complete
 * countries rather than the largest gaps.
 */
internal fun countryAxisOrder(): Comparator<CountryAxisBlock> {
    val names = Collator.getInstance(Locale.forLanguageTag("es"))
    return compareByDescending<CountryAxisBlock> { it.issued != null }
        .thenByDescending { block ->
            val issued = block.issued ?: return@thenByDescending 0.0
            block.owned.coerceAtMost(issued).toDouble() / issued
        }
        .thenByDescending { it.issued ?: 0 }
        .thenBy { block -> names.getCollationKey(block.country) }
}
