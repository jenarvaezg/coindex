package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMemberStatus
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.buildCollectionCatalogAlbum
import com.jenarvaezg.coindex.domain.cardCountry
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

    /**
     * A piece no casilla claims: inked ring, no sunk cardboard, and it does not grow the
     * denominator («Francia 9»).
     */
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

/**
 * The country axis of the notebook (ADR 0026 §9 / atlas-315).
 *
 * Each measurable member of every evidenced catalog becomes a cell in **the member's** country
 * (#170), not the catalog header's. Loose pieces join their country's block with no «sueltas»
 * band. Order is [countryAxisOrder] — the same spirit as `indexOrder()`: reveals, does not
 * reproach.
 */
fun countryAxis(
    state: CollectionState,
    catalogs: List<CollectionCatalog>,
    claimedRowIds: Set<Long> = claimsOf(state).rowIds,
    /**
     * Catalog ids that survive the shelf's filters, or null to keep every evidenced catalog.
     *
     * The weight / estado / serie chips still narrow the sheet when the axis is not «por lámina»:
     * a slot belongs to a catalog, and a catalog that the shelf hid does not paint.
     */
    keptCatalogIds: Set<String>? = null,
    /** Loose row ids that survive the shelf, or null to keep every unclaimed piece. */
    keptLooseIds: Set<Long>? = null,
): CountryAxisModel {
    val byCountry = linkedMapOf<String, MutableList<CountryAxisCell>>()
    val catalogsToDraw = catalogs.filter { catalog ->
        catalog.id in state.evidencedCatalogIds &&
            (keptCatalogIds == null || catalog.id in keptCatalogIds)
    }

    for (catalog in catalogsToDraw) {
        val album = buildCollectionCatalogAlbum(catalog, state.items)
        for (albumMember in album.members) {
            val status = albumMember.status
            val owned = status is CollectionCatalogMemberStatus.Owned
            val missing = status is CollectionCatalogMemberStatus.Missing
            if (!owned && !missing) continue
            val member = albumMember.member
            val country = memberCountry(catalog, member, state.typeMeta) ?: continue
            val quantity = (status as? CollectionCatalogMemberStatus.Owned)
                ?.items
                ?.sumOf { it.quantity }
                ?: 0
            byCountry.getOrPut(country) { mutableListOf() }.add(
                CountryAxisCell.Slot(
                    catalogId = catalog.id,
                    memberId = member.id,
                    typeId = member.numistaTypeId,
                    owned = owned,
                    quantity = quantity.coerceAtLeast(if (owned) 1 else 0),
                ),
            )
        }
    }

    for (piece in state.items) {
        if (piece.quantity <= 0 || piece.id in claimedRowIds) continue
        if (keptLooseIds != null && piece.id !in keptLooseIds) continue
        val meta = state.typeMeta[piece.typeId]
        val country = meta?.country ?: continue
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

/** Member country: the override on the member, else the catalog's, cured (ADR 0023, #170). */
internal fun memberCountry(
    catalog: CollectionCatalog,
    member: com.jenarvaezg.coindex.domain.CollectionCatalogMember,
    typeMeta: Map<Int, TypeMeta>,
): String? {
    val code = catalog.issuerCodeOf(member)
    // The member's own ficha first; any sibling of the same issuer next — a hole whose type has
    // not reached the phone still paints when another coin of that country already did.
    val numistaName = member.numistaTypeId?.let { typeMeta[it]?.issuerName }
        ?: typeMeta.values.firstOrNull { it.issuerCode == code }?.issuerName
    return cardCountry(code, numistaName)
}

/**
 * Same spirit as `indexOrder()`: has ratio ↓, ratio ↓, denominator ↓, name ↑.
 *
 * Opens on Italia 2/2 rather than Rusia 3/280 — reveals, does not reproach.
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
