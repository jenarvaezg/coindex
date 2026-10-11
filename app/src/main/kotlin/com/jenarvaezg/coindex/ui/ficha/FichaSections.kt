package com.jenarvaezg.coindex.ui.ficha

import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.domain.PrintedSide

/** One folded section: closed it says [summary] on one line (#643). */
sealed interface FichaSection {
    val title: String
    val summary: String

    /** What the screen remembers it open by: the title carries a count, which a sync can move. */
    val key: String get() = this::class.java.simpleName

    /** Whether it enters open. Only the pieces ever do, from a date run's casilla. */
    val open: Boolean get() = false
}

/**
 * The collector's pieces of the coin. With more than [MANY_ROWS] rows they are chosen by date:
 * [dates] is the grid, [selected] the date shown on entry.
 */
data class PiecesSection(
    override val title: String,
    override val summary: String,
    override val open: Boolean,
    val pieces: List<FichaPiece>,
    val dates: List<DateTile>,
    val selected: String?,
) : FichaSection

/** What filling the casilla would cost: Numista in `unc` and the metal (ADR 0028 §8). */
data class CostSection(
    override val title: String,
    override val summary: String,
    val rungs: List<FichaRung>,
) : FichaSection

/** The collections that claim the coin (#635), and the plate it came from in miniature. */
data class ClaimsSection(
    override val title: String,
    override val summary: String,
    val claims: List<FichaClaim>,
    val strip: FichaStrip?,
) : FichaSection

/** The cartouche: only the rows the ficha has. */
data class SpecSection(
    override val title: String,
    override val summary: String,
    val rows: List<Pair<String, String>>,
) : FichaSection

data class FacesSection(
    override val title: String,
    override val summary: String,
    val faces: List<FaceBlock>,
) : FichaSection

/** Numista's comments, kept as HTML so their links still open the browser. */
data class CommentsSection(
    override val title: String,
    override val summary: String,
    val html: String,
) : FichaSection

data class RelatedSection(
    override val title: String,
    override val summary: String,
    val rows: List<RelatedRow>,
) : FichaSection

/**
 * One collected row. [rungs] is empty until the market lands (ADR 0028 §7).
 *
 * @param dateKey the date tile it falls under.
 * @param here the piece of the casilla the screen was opened from.
 */
data class FichaPiece(
    val itemId: Long,
    val dateKey: String,
    val heading: String,
    val grade: String?,
    val quantity: String?,
    val here: Boolean,
    val facts: String?,
    val rungs: List<FichaRung>,
    val issueComment: String?,
    val note: String?,
    val photo: CoinPhoto?,
)

/**
 * One price of a piece or a hole, with its whole label and where it comes from; or, empty, why.
 *
 * @param share its length against the greatest rung, for the bar under it.
 */
data class FichaRung(
    val label: String,
    val amount: String?,
    val why: String?,
    val source: String?,
    val greatest: Boolean,
    val share: Float,
)

/** One date of the grid, with its grades and how many pieces it holds. */
data class DateTile(
    val key: String,
    val label: String,
    val grades: String,
    val quantity: String?,
    /** Some piece of the date has a note or a photograph. */
    val noted: Boolean,
    /** The date of the casilla the screen was opened from. */
    val casilla: Boolean,
)

/** The plate the screen was opened from, in miniature, on its own `printed_side`. */
data class FichaStrip(val cells: List<StripCell>, val count: String?, val side: PrintedSide)

data class StripCell(val typeId: Int?, val missing: Boolean, val here: Boolean)

/** A face or the edge, as Numista writes it. [photo] is the edge's, shown apart on request. */
data class FaceBlock(
    val heading: String,
    val lettering: String?,
    val unabridgedLegend: String?,
    val description: String?,
    val photo: CoinPhoto? = null,
)

data class RelatedRow(val typeId: Int, val country: String?, val title: String, val years: String?)
