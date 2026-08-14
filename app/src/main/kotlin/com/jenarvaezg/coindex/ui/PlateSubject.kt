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
 * What the three drawers of a plate are looking at: the screen, the exported sheet, the notebook.
 *
 * **Built once from [PlateResult.Available] and consumed whole** (#218). The plate used to be taken
 * apart at the edge of the screen and its four pieces threaded by hand through four signatures of
 * six and eight parameters, which is how the same facts came to be recomputed three times over —
 * once in the body of a `LazyVerticalGrid`, with no `remember` under it — and how the exported
 * sheet ended up recomputing them again while the export was in flight. `PiecesSubject` had already answered this
 * for the collections without an issue list; this is the same answer for the ones with one.
 *
 * **Nothing here is a catalog and nothing here is an album.** What a drawer needs is prose and
 * pictures: a heading, a specification already worded, and cells that know what they say. The
 * counting and the lifting of shared facts happen once, in [plateSubject], and no drawer can do
 * either — which is what makes «Progreso» and the card's ratio one number instead of two.
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
     * The ratio the header prints, and what the stamp lands on: `22/22` (ADR 0026 §3).
     *
     * The short form and not «Progreso»'s sentence, because it is drawn as a figure over the title
     * and not as a row of the specification — see [plateEntriesBesideRatio], which is what takes it out
     * of the card so the same number is never printed twice.
     */
    val ratio: String?,
    /**
     * Every issued member owned, which is the whole of what the completion stamp is (ADR 0026 §3).
     *
     * **A state and not an event**: it is read from the inventory like the die-cut, so a plate that
     * stops being complete stops showing it, and nothing anywhere remembers that it once did.
     */
    val complete: Boolean,
    /**
     * The casilla the coin of the index card flies to, or null where no cell can receive it.
     *
     * [CollectionCatalogAlbum.firstOwnedIndex], which is **the very rule the card's photograph is
     * chosen by**: the coin that took off is the coin that lands, and one function is what keeps the
     * two from being picked apart. A plate the collector owns nothing of has no landing cell — and no
     * plate either, because `resolvePlate` needs evidence to open one.
     */
    val landingCell: Int?,
    /**
     * What the coins in these casillas are worth, or null when there is no money to say (ADR 0026 §10).
     *
     * Null in three different situations that all mean the same thing on the page — the market has not
     * landed yet (ADR 0028 §7), the plate holds nothing, or **this drawer is the export with the money
     * switched off** (#228, ADR 0021 §13). One nullable field rather than a flag per drawer, so a drawer
     * cannot print an amount it was not given.
     *
     * It is the **screen's** wording, with the name of the figure inside it (#493). The printed page
     * words the same amount as a row of its specification and takes it from `plateAmountLabel`
     * directly: its row is already titled «Valor», and a value that carried its own title would say
     * the word twice.
     */
    val value: String? = null,
    /**
     * What closing this plate would cost, or null when there is nothing to close (#493).
     *
     * Absent and not zero on a complete plate: without a hole there is no cost, no line and no stamp,
     * and no zero anybody has to word. Absent too over the threshold of ADR 0028 §1, where the prices
     * were never asked for — the same clause, read from `holesAreWithinReach` and not counted twice.
     */
    val cost: String? = null,
)

/**
 * One casilla of a plate as it is drawn, with everything it says already resolved.
 *
 * The parallel of [DrawnPiece], and for the same reason: what a cell has left to say under its own
 * title is decided by the whole plate — see [plateCellFootnote] — so a drawer that received the raw
 * member would have to ask the plate again, three times, with three chances to ask differently.
 *
 * [id] is the member's, and it is here because a lazy grid needs a stable key: the label of a cell
 * is what the collector reads, not a promise of uniqueness.
 */
data class DrawnCell(
    val id: String,
    val label: String,
    /** The Numista type behind it, or null for an announced or unlisted casilla. */
    val numistaTypeId: Int?,
    val footnote: String?,
    /**
     * The year on the casilla's recessed tag, which on screen is also what opens Numista (#337).
     *
     * Not the same thing as [footnote], which is what the *paper* has left to say under a coin and
     * therefore goes silent when the whole plate shares one year. The tag is not a note: it is the
     * handle, so it keeps the year in the twelve casillas of four catalogs where the footnote
     * drops it rather than leaving them with nothing to press.
     */
    val year: String?,
    val owned: Boolean,
    /** Only an issued member absent from the collection gets the catalog-design ghost. */
    val missing: Boolean,
    /**
     * What this casilla costs, stamped inside the hole, or null where nothing can be said (#493).
     *
     * Only ever a hole: a full casilla has no cost — it has a value, and that one is the header's. And
     * only ever a hole whose price is on the phone, which is why the plates over the threshold of ADR
     * 0028 §1 carry no stamps at all: nobody asked for those prices, so there is no «—» to draw. A
     * **marked** hole is the exception the mark buys, and it is one chip either way — see
     * [com.jenarvaezg.coindex.ui.components.HoleStamp].
     */
    val cost: String? = null,
    /**
     * The key this casilla is marked by, or null where it cannot be marked at all (ADR 0029).
     *
     * Null for an announced or unlisted member: a coin the app cannot name cannot be looked for, which
     * is the recorte ADR 0029 accepts. It is here and not derived by the screen because the mark and
     * the price have to address **the same casilla**: both are read off the member, once.
     */
    val wishKey: WishKey? = null,
    /** Whether the collector marked this casilla: «lo busco» (ADR 0029 §2). */
    val wished: Boolean = false,
)

/**
 * The name a casilla prints under its year, or null where its label is already that year.
 *
 * A casilla titled with its own year would otherwise print it twice, once on the tag sunk into the
 * cardboard and once as a gloss underneath. Null and not the empty string, because since #473 the
 * difference is whether the casilla prints a third thing at all: nothing is reserved for a name
 * that is not there, and what the casilla does not use falls into the gap between two rows.
 */
val DrawnCell.printedName: String?
    get() = printedNameOf(label, year)

/**
 * The rule itself, shared by the two surfaces that draw a casilla under a year.
 *
 * The plate is one and the annex of ADR 0029 is the other, and the annex is where it was measured
 * again: a date run labels its casillas with their year, so «1886» came out on the tag and again
 * underneath it. One function rather than the same `takeIf` twice — a rule about what not to print
 * twice, written twice, is the joke telling itself.
 */
internal fun printedNameOf(label: String, year: String?): String? = label.takeIf { it != year }

/** The one catalog photograph a resting plate shows and exports. */
fun TypeImages.printedPhoto(side: PrintedSide): CoinPhoto = when (side) {
    PrintedSide.Obverse -> obverse
    PrintedSide.Reverse -> reverse
}

/**
 * The plate of one catalog, worded once.
 *
 * Takes the resolution and not its pieces so that the pieces cannot arrive apart: an album belongs
 * to the catalog it was built from, and the programmes to both.
 */
fun plateSubject(
    plate: PlateResult.Available,
    money: PlateMoney = PlateMoney(),
    /** The casillas of this plate the collector marked, by key (ADR 0029). */
    wished: Set<WishKey> = emptySet(),
): PlateSubject {
    val catalog = plate.catalog
    // Off the album and not off the catalog, so the heading is lifted out of the very cells the
    // plate is about to draw: the album is the plate as this collector has it.
    val common = plateCommonFacts(plate.album.members.map { it.member })
    // The card's ratio itself (#218, ADR 0026 §3): what the index prints in rust, what the header
    // prints over the title, and what the stamp is read from are one measurement.
    val coverage = plate.album.coverage()
    val cells = plate.album.members.map { albumMember ->
        val wishKey = albumMember.member.wishKey()
        DrawnCell(
            id = albumMember.member.id,
            label = albumMember.member.label,
            numistaTypeId = albumMember.member.numistaTypeId,
            footnote = plateCellFootnote(albumMember.member, common),
            year = albumMember.member.year?.toString(),
            owned = albumMember.status is CollectionCatalogMemberStatus.Owned,
            missing = albumMember.status is CollectionCatalogMemberStatus.Missing,
            cost = money.holeCosts[albumMember.member.id]?.let(::holeCostLabel),
            wishKey = wishKey,
            // Only where the casilla is empty, and it is not a filter over the table: a wish whose
            // coin arrived is dead by ADR 0029 §2, and the album is where that is measured. So the
            // mark disappears with the hole it was made in, and the row stays for the day the coin
            // leaves again.
            wished = albumMember.status is CollectionCatalogMemberStatus.Missing &&
                wishKey != null && wishKey in wished,
        )
    }
    return PlateSubject(
        catalogId = catalog.id,
        title = catalog.name,
        source = catalog.source,
        printedSide = catalog.printedSide,
        entries = plateEntries(catalog, plate.album, common, plate.programmes),
        cells = cells,
        ratio = coverage?.let { "${it.owned}/${it.issued}" },
        complete = coverage?.nothingMissing == true,
        landingCell = plate.album.firstOwnedIndex(),
        value = money.value?.let(::plateValueLabel),
        cost = money.cost?.let(::plateCostLabel),
    )
}

/**
 * The facts every member of a catalog shares, which therefore belong to the plate and not to
 * its cells.
 *
 * An issue run repeats the year across its cells, so a footnote built from it said the same thing
 * in twenty-one cells at once. The type is here for the same reason but is never handed back to a
 * cell: it either heads the whole plate or it is not on the plate at all (see [plateCellFootnote]).
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
 * What one cell has left to say under its own title: the year, unless the title is already the
 * year or every cell shares it.
 *
 * Null when nothing is left, which is the common case of any catalog whose cells are titled with
 * their year.
 *
 * The Numista type is deliberately absent (issue #88). Lifting it only when every cell agreed left
 * it in the cells of the forty-three catalogs whose cells do not: twenty-one repetitions of one
 * identifier in the fuertes, and a hundred and twenty-one distinct ones — no norm, so no exception
 * to annotate — in the Russian personalities. A type identifier is not what a plate says under a
 * coin: on screen the cell title already links to its Numista page, and the exported sheet is a
 * picture the collection is shown with. Where the type does belong is the plate's own
 * specification, and only when the whole plate is that type — see [plateEntries].
 */
private fun plateCellFootnote(member: CollectionCatalogMember, common: PlateCommonFacts): String? {
    val year = member.year ?: return null
    if (common.year != null || member.label == year.toString()) return null
    return year.toString()
}

/**
 * The plate's specification block, said once for the three drawers.
 *
 * Whatever [plateCommonFacts] lifts out of the cells lands here, once.
 *
 * **Every count is the album's** (#218). The divisor the plate prints is the divisor the card
 * divided by — `CollectionCatalogAlbum.issuedMembers` — and the two prose lines next to it count
 * the same statuses rather than the catalog's flags a second time.
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
    add("Actualizado" to catalog.updatedAt)
}

/**
 * The specification with the progress row taken out, for the two drawers that print the ratio
 * themselves.
 *
 * The screen and the exported sheet both head the plate with `22/22` over the title, which is where
 * the stamp lands (ADR 0026 §3); leaving «Progreso · 22 / 22 emisiones» in the card underneath would
 * print the same number twice, and the frequency rule of §5 prices a word by how often it is printed.
 * The lines the progress *brought with it* — «1 anunciada», «2 no medibles» — stay: they are not the
 * ratio, and the figure over the title deliberately says nothing about them.
 *
 * The printed notebook does not call this: its page has no header of its own to raise the ratio into,
 * so [plateSubject]'s entries reach it whole.
 */
fun plateEntriesBesideRatio(entries: List<Pair<String, String>>): List<Pair<String, String>> =
    entries.filterNot { (label, _) -> label == PROGRESS_LABEL }

/** What the progress row is called in the specification, in the one place that has to match. */
private const val PROGRESS_LABEL = "Progreso"
