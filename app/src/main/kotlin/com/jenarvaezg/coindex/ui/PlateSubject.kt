package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogAlbum
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.CollectionCatalogMemberStatus
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.domain.ProgrammeStanding
import com.jenarvaezg.coindex.domain.WishKey
import com.jenarvaezg.coindex.domain.coverage
import com.jenarvaezg.coindex.domain.firstOwnedIndex
import com.jenarvaezg.coindex.domain.wishKey

/**
 * What the three drawers of a plate look at: the screen, the exported sheet, the notebook. Built
 * once from [PlateResult.Available] and consumed whole (#218), as [PiecesSubject] is for the
 * collections without an issue list.
 *
 * No catalog or album reaches a drawer, only worded prose and pictures. Counting and lifting shared
 * facts happen once, in [plateSubject], so «Progreso» and the card's ratio are one number.
 */
data class PlateSubject(
    /** Names the export file and keys the picture the sheet is recorded into. */
    val catalogId: String,
    val title: String,
    val source: String,
    /** Which face a cell prints when it prints one (#227). The plate declares it, never the cell. */
    val printedSide: PrintedSide,
    /** The specification block, in the order all three drawers print it. */
    val entries: List<Pair<String, String>>,
    val cells: List<DrawnCell>,
    /**
     * The ratio printed over the title, where the stamp lands: `22/22` (ADR 0026 §3). Drawers that
     * print it drop the «Progreso» row ([plateEntriesBesideRatio]).
     */
    val ratio: String?,
    /**
     * Every issued member owned: the completion stamp (ADR 0026 §3). A state read from the
     * inventory, not an event, so a plate that stops being complete stops showing it.
     */
    val complete: Boolean,
    /**
     * The casilla the index card's coin flies to, or null. Uses
     * [CollectionCatalogAlbum.firstOwnedIndex], the rule that picks the card's photograph, so the
     * coin that takes off is the one that lands.
     */
    val landingCell: Int?,
    /**
     * The screen's wording of what the coins in these casillas are worth (#493, ADR 0026 §10).
     * Null before the market lands (ADR 0028 §7), when the plate holds nothing, and in an export
     * with money off (#228, ADR 0021 §13), so a drawer can't print an amount it wasn't given. The
     * printed page uses `plateAmountLabel` under its own «Valor» row.
     */
    val value: String? = null,
    /**
     * What closing this plate would cost (#493). Null on a complete plate, and past the threshold
     * of ADR 0028 §1 (`holesAreWithinReach`), where those prices are not asked for.
     */
    val cost: String? = null,
    /**
     * What entering costs, on a plate that isn't the collector's (ADR 0030 §6): its only money
     * figure. Null on the collector's plates and on a shelf-window plate never valued.
     */
    val entry: String? = null,
    /**
     * What a shelf-window plate says when it was valued and Numista had no price (ADR 0028 §4), so
     * it doesn't look untouched.
     */
    val entryNote: String? = null,
    /**
     * Whether this plate is the collector's own (ADR 0030 §1). A shelf-window plate offers «Tasar
     * esta lámina» instead of «Exportar la lámina» (#282); everything else is the same.
     */
    val mine: Boolean = true,
    /** Whether this plate has been valued, priced or not; picks the gesture's word. */
    val entryValued: Boolean = false,
    /**
     * Whether the header says the money is still coming (#519): true only when the market is the
     * reason [value] and [cost] are null.
     */
    val moneyWaiting: Boolean = false,
)

/**
 * One casilla as it is drawn, with everything it says resolved. Like [DrawnPiece]: what a cell says
 * depends on the whole plate ([plateCellFootnote]), so drawers get the answer, not the member.
 * [id] is the member's, the lazy grid's stable key.
 */
data class DrawnCell(
    val id: String,
    val label: String,
    /** The Numista type behind it, or null for an announced or unlisted casilla. */
    val numistaTypeId: Int?,
    val footnote: String?,
    /**
     * The year on the casilla's recessed tag, which on screen also opens the coin's ficha (#508). Unlike
     * [footnote], it stays when the whole plate shares one year: it is the handle.
     */
    val year: String?,
    val owned: Boolean,
    /** Only an issued member absent from the collection gets the catalog-design ghost. */
    val missing: Boolean,
    /**
     * The price stamped inside a hole whose price is on the phone, or null (#493). Past the
     * threshold of ADR 0028 §1 only marked holes have one (see
     * [com.jenarvaezg.coindex.ui.components.HoleStamp]).
     */
    val cost: String? = null,
    /**
     * The key this casilla is marked by, or null for an announced or unlisted member, which can't
     * be looked for (ADR 0029). Read off the member here so the mark and the price address the same
     * casilla.
     */
    val wishKey: WishKey? = null,
    /** Whether the collector marked this casilla: «lo busco» (ADR 0029 §2). */
    val wished: Boolean = false,
    /** What the piece sunk into the cardboard says, and what it therefore leaves to [printedName]. */
    val plaque: CellPlaque? = null,
)

/**
 * What the piece sunk into the cardboard says: the casilla's identity in its plate and its door to
 * the ficha (#302, #508). On a date run, the year; where every casilla shares the year, what tells
 * it from its sisters, such as «Estrella 66» (#511). A shared year is printed once, in the
 * specification.
 */
sealed interface CellPlaque {
    /** The year of a casilla that has one of its own. */
    data class Year(val year: String) : CellPlaque

    /** What distinguishes a casilla whose year does not. */
    data class Name(val name: String) : CellPlaque
}

/**
 * The plaque rule, shared by the subject and its tests. A casilla labelled with its year keeps the
 * year plaque even when the plate shares it: there is nothing else to show.
 */
internal fun plaqueOf(label: String, year: String?, yearIsCommon: Boolean): CellPlaque? = when {
    yearIsCommon && label != year -> CellPlaque.Name(label)
    year != null -> CellPlaque.Year(year)
    else -> null
}

/**
 * The name a casilla prints under its plaque, or null where the plaque already says it: a casilla
 * titled with its year, or one whose plaque is its name (#511). Null rather than empty, since no
 * space is reserved for a missing name (#473).
 */
val DrawnCell.printedName: String?
    get() = if (plaque is CellPlaque.Name) null else printedNameOf(label, year)

/** The rule itself, shared by the plate and the annex (ADR 0029), which both draw a year tag. */
internal fun printedNameOf(label: String, year: String?): String? = label.takeIf { it != year }

/** The one catalog photograph a resting plate shows and exports. */
fun TypeImages.printedPhoto(side: PrintedSide): CoinPhoto = when (side) {
    PrintedSide.Obverse -> obverse
    PrintedSide.Reverse -> reverse
}

/**
 * The plate of one catalog, worded once. Takes the whole resolution so the album, the catalog and
 * the programmes arrive together.
 */
fun plateSubject(
    plate: PlateResult.Available,
    money: PlateMoney = PlateMoney(),
    /** The casillas of this plate the collector marked, by key (ADR 0029). */
    wished: Set<WishKey> = emptySet(),
    /**
     * For the age every amount on a plate shows (ADR 0030 §4, #594). A parameter so a test can hold
     * it still.
     */
    nowMillis: Long = System.currentTimeMillis(),
): PlateSubject {
    val catalog = plate.catalog
    // From the album, not the catalog, so the heading is lifted out of the cells being drawn.
    val common = plateCommonFacts(plate.album.members.map { it.member })
    // The card's ratio (#218, ADR 0026 §3): the index, the header and the stamp share it.
    val coverage = plate.album.coverage()
    val cells = plate.album.members.map { albumMember ->
        val wishKey = albumMember.member.wishKey()
        // Welded once, for the foot of the casilla or its plaque (#511).
        val label = albumMember.member.label.weldUnits()
        val year = albumMember.member.year?.toString()
        DrawnCell(
            id = albumMember.member.id,
            label = label,
            numistaTypeId = albumMember.member.numistaTypeId,
            footnote = plateCellFootnote(albumMember.member, common),
            year = year,
            owned = albumMember.status is CollectionCatalogMemberStatus.Owned,
            missing = albumMember.status is CollectionCatalogMemberStatus.Missing,
            cost = money.holeCosts[albumMember.member.id]?.let(::holeCostLabel),
            wishKey = wishKey,
            // Only on an empty casilla: a wish whose coin arrived is dead (ADR 0029 §2). The mark
            // goes with the hole; the row stays for the day the coin leaves again.
            wished = albumMember.status is CollectionCatalogMemberStatus.Missing &&
                wishKey != null && wishKey in wished,
            plaque = plaqueOf(
                label = label,
                year = year,
                // As in `plateCellFootnote`: a year every casilla shares is in the specification.
                yearIsCommon = common.year != null,
            ),
        )
    }
    return PlateSubject(
        catalogId = catalog.id,
        title = catalog.name.weldUnits(),
        source = catalog.source,
        printedSide = catalog.printedSide,
        entries = plateEntries(catalog, plate.album, common, plate.programmes),
        cells = cells,
        ratio = coverage?.let { "${it.owned}/${it.issued}" },
        complete = coverage?.nothingMissing == true,
        landingCell = plate.album.firstOwnedIndex(),
        value = money.value?.let { plateValueLabel(it, nowMillis) },
        cost = money.cost?.let { plateCostLabel(it, nowMillis) },
        entry = money.entry?.let { showcaseEntryLabel(it, nowMillis) },
        entryNote = ShowcaseLabels.NOTHING_PRICED.takeIf { money.entryAsked && money.entry == null },
        // Asked, not priced, turns «Tasar esta lámina» into «Volver a tasar»: «no price» counts.
        entryValued = money.entryAsked,
        moneyWaiting = money.waiting,
        mine = plate.mine,
    )
}

/**
 * Facts every member of a catalog shares, which belong to the plate rather than its cells. The
 * type is never handed back to a cell: it heads the plate or isn't shown (see [plateCellFootnote]).
 */
private data class PlateCommonFacts(val numistaTypeId: Int?, val year: Int?)

private fun plateCommonFacts(members: List<CollectionCatalogMember>): PlateCommonFacts {
    // An announced member has neither type nor, often, a year. An unlisted member does have a
    // real year, so it participates here even though its absent type is absorbed by mapNotNull.
    val issued = members.filterNot { it.isAnnounced }
    return PlateCommonFacts(
        numistaTypeId = issued.mapNotNull { it.numistaTypeId }.distinct().singleOrNull(),
        year = issued.mapNotNull { it.year }.distinct().singleOrNull(),
    )
}

/**
 * What one cell has left to say under its title: the year, unless the title is the year or every
 * cell shares it.
 *
 * Never the Numista type (#88): on screen the title already links to Numista, and on paper an
 * identifier under each coin is noise. The type goes in the specification when the whole plate is
 * one type ([plateEntries]).
 */
private fun plateCellFootnote(member: CollectionCatalogMember, common: PlateCommonFacts): String? {
    val year = member.year ?: return null
    if (common.year != null || member.label == year.toString()) return null
    return year.toString()
}

/**
 * The plate's specification block, for the three drawers; whatever [plateCommonFacts] lifts out of
 * the cells lands here. Every count is the album's (#218), so the divisor is the one the card
 * divided by (`CollectionCatalogAlbum.issuedMembers`).
 */
private fun plateEntries(
    catalog: CollectionCatalog,
    album: CollectionCatalogAlbum,
    common: PlateCommonFacts,
    programmes: List<ProgrammeStanding>,
): List<Pair<String, String>> = buildList {
    // The divisor is what the app can measure (#48), which is exactly the issued members.
    add(PROGRESS_LABEL to "${album.ownedMembers()} / ${album.issuedMembers()} emisiones")
    val announced = album.announcedMembers()
    if (announced > 0) {
        add("" to if (announced == 1) "1 anunciada" else "$announced anunciadas")
    }
    val unlisted = album.unlistedMembers()
    if (unlisted > 0) {
        add(
            "" to
                if (unlisted == 1) "1 emisión no medible" else "$unlisted no medibles",
        )
    }
    // The second reading (ADR 0022), after the plate's own progress and never mixed into it:
    // its denominator counts coins no catalog of this project claims.
    programmes.forEach { standing ->
        add(
            "Programa" to "${standing.programme.shortName} · " +
                "${standing.progress.owned} de ${standing.progress.total}",
        )
    }
    addAll(variantEntries(catalog.weightMillioz, catalog.finish))
    common.numistaTypeId?.let { typeId -> add("Tipo" to "Numista $typeId") }
    common.year?.let { year -> add("Año" to year.toString()) }
    // The curated file's edition, not an age (#518).
    add("Catálogo" to catalogDateLabel(catalog.updatedAt))
}

/**
 * The specification without the «Progreso» row, for the screen and the exported sheet, which print
 * the ratio over the title (ADR 0026 §3, §5). The lines that come with it («1 anunciada»,
 * «2 no medibles») stay. The notebook has no such header and prints the entries whole.
 */
fun plateEntriesBesideRatio(entries: List<Pair<String, String>>): List<Pair<String, String>> =
    entries.filterNot { (label, _) -> label == PROGRESS_LABEL }

/** What the progress row is called in the specification, in the one place that has to match. */
private const val PROGRESS_LABEL = "Progreso"
