package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.ui.components.Silhouette
import com.jenarvaezg.coindex.ui.components.paperCoinFilter
import com.jenarvaezg.coindex.ui.print.PrintBlock
import com.jenarvaezg.coindex.ui.print.PrintCell
import com.jenarvaezg.coindex.ui.print.PrintGeometry
import com.jenarvaezg.coindex.ui.print.PrintGrid
import com.jenarvaezg.coindex.ui.print.PrintHeading
import com.jenarvaezg.coindex.ui.print.PrintPage
import com.jenarvaezg.coindex.ui.print.PrintedCompletionStamp
import com.jenarvaezg.coindex.ui.print.QR_QUIET_MODULES
import com.jenarvaezg.coindex.ui.print.notebookSourceLabel
import com.jenarvaezg.coindex.ui.print.numistaQr
import com.jenarvaezg.coindex.ui.print.printedDiameterLabel
import com.jenarvaezg.coindex.ui.print.printedPageOfSection
import com.jenarvaezg.coindex.ui.print.printedRulerLabel
import com.jenarvaezg.coindex.ui.print.qrModulesWithQuietZone
import com.jenarvaezg.coindex.ui.print.qrRuns
import com.jenarvaezg.coindex.ui.components.paperSurface
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.theme.BarlowCondensedFamily
import com.jenarvaezg.coindex.ui.theme.BitterFamily

/** The density that makes one dp a millimetre of paper: the layout is written in millimetres. */
val printDensity = Density(density = PrintGeometry.PX_PER_MM, fontScale = 1f)

/** Millimetres, as the unit the whole printed page is laid out in. */
private val Float.mm: Dp get() = Dp(this)

// Type sized in millimetres of paper rather than in phone dp: the sheet of a single plate scales
// the screen's typography by a factor (`scaledBy`), which works because that sheet's width is a
// multiple of a phone's. A4 is not, and paper diverges from the screen on purpose (#169) — 3 mm of
// serif is around 8,5 pt, which is what a printed album caption is set in.
private val PRINT_EYEBROW = TextStyle(
    fontFamily = BarlowCondensedFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 2.6f.sp,
    fontFeatureSettings = "'smcp', 'tnum'",
)
private val PRINT_TITLE = TextStyle(
    fontFamily = BitterFamily,
    fontSize = 7f.sp,
    lineHeight = 8f.sp,
)
private val PRINT_SUBTITLE = TextStyle(
    fontFamily = BitterFamily,
    fontSize = 3.6f.sp,
    lineHeight = 4.2f.sp,
)
private val PRINT_FACT_LABEL = TextStyle(
    fontFamily = BarlowCondensedFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 2.2f.sp,
    fontFeatureSettings = "'smcp', 'tnum'",
)
private val PRINT_FACT_VALUE = TextStyle(
    fontFamily = BitterFamily,
    fontSize = 3.2f.sp,
    lineHeight = 3.6f.sp,
)
private val PRINT_STATE = TextStyle(
    fontFamily = BarlowCondensedFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 2.3f.sp,
    fontFeatureSettings = "'smcp', 'tnum'",
)
private val PRINT_CELL_TITLE = TextStyle(
    fontFamily = BitterFamily,
    fontSize = 2.9f.sp,
    lineHeight = 3.3f.sp,
)

/**
 * Bitter shrinks before the paper cuts, which is the ladder of #348 brought to the notebook (#412).
 *
 * **The screen was given a third line and the paper cannot be**: the sixteen millimetres of
 * [PrintGeometry.captionMm] are what the page count was computed from, so a taller caption is a
 * longer notebook. What the paper has instead is resolution: 2,9 mm of serif is a comfortable
 * caption at 300 dpi and there is room underneath it, while 13 sp was already the screen's floor.
 *
 * Measured over the 1.082 named members of `data/` in the narrowest cell there is — 28 mm, the floor
 * a 2 euros of 25,75 mm falls back to, and the very coin whose names the report quoted: at the fixed
 * 2,9 mm the paper cut 57 of them, and it cut **40 that the screen prints whole**. Down to 1,8 mm it
 * cuts none of those, which is the property this exists for — no name is legible on the glass and an
 * ellipsis on the page. Only 7 names ever reach the bottom of the ladder, and the last one to give in
 * is «Saint Trinity Seraphim-Diveyevsky Monastery», whose hyphenated 19-letter word cannot break.
 *
 * The floor is small — 1,8 mm is about 5 pt — and it is only ever reached where the alternative is
 * not reading the name at all. The footnote and the state of this same caption are set at 2,3 mm.
 */
private val PRINT_CELL_TITLE_AUTO_SIZE = TextAutoSize.StepBased(
    minFontSize = 1.8f.sp,
    maxFontSize = 2.9f.sp,
    stepSize = 0.1f.sp,
)
private val PRINT_CELL_THEME = TextStyle(
    fontFamily = BitterFamily,
    fontSize = 2.5f.sp,
    lineHeight = 2.9f.sp,
)
// Denomination plus the worst-case two theme lines. The rest of the 16 mm caption stays untouched,
// so fixing alignment cannot change the page count (#350).
private const val PRINT_CARTOUCHE_MM = 9.1f
private val PRINT_FOOTNOTE = TextStyle(
    fontFamily = BarlowCondensedFamily,
    fontSize = 2.3f.sp,
    fontFeatureSettings = "'tnum'",
)

/** The side of the box that gets ticked on a page with no photographs (#231). */
private const val LIST_BOX_MM = 3.4f

/** Between two things on one line of the list: enough to separate, not enough to be a column rule. */
private const val LIST_GAP_MM = 1.4f

/**
 * The column the state takes on a line of the list, so the names below each other line up.
 *
 * Sized for the longest thing `plateMemberStateLabel` says — «SIN EMITIR», and «TENGO · ×9» just
 * under it. A quantity in double figures ellipsizes rather than pushing the name out of line: it is
 * the one case where the count is better read in the app than in the column, and a checklist whose
 * left edge moves row by row is not read at all.
 */
private const val LIST_STATE_MM = 17f

/**
 * One page of the printed notebook, at A4 and at 1:1.
 *
 * It is **not** the exported sheet with different numbers. That sheet is a bitmap as wide as its
 * grid needs, both faces of every coin, and a density that shrinks as the catalog grows; here the
 * page is given, the coin's size is given, and what varies is how many of them fit (#169). The two
 * renderings diverge on purpose and neither one is the other's fallback: the PNG of a single plate
 * is the gesture the collector makes every day and does not change by a pixel.
 */
@Composable
fun NotebookPageSheet(
    page: PrintPage,
    onImageSettled: (painted: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The millimetres this page was counted with, which since #228 come from the configuration the
    // export was started under and no longer from a constant.
    val geometry = page.geometry
    Column(
        // The paper is painted inside whatever the caller wraps the page in, or the recording
        // comes out transparent and every viewer fills it with a colour of its own.
        modifier = modifier
            .size(geometry.widthMm.mm, geometry.heightMm.mm)
            .paperSurface()
            // Nothing is drawn outside the paper, which is the guarantee the packer makes when it
            // gives a plate a folio to itself «even where a single row would overflow»: at shipped
            // diameters that never happens, and if it ever did what came off the bottom would be
            // clipped rather than drawn past the edge of a page the exporter had already counted.
            .clipToBounds()
            // The margin is the page's and not each band's: inside it the plates and the strip at
            // the foot add up to at most the printable height, which is what the packer counted
            // against.
            .padding(geometry.marginMm.mm),
    ) {
        page.blocks.forEachIndexed { index, block ->
            // The seam between two plates, and never above the first: what the folio has left over
            // belongs at its foot and not between the plates on it (#232).
            if (index > 0) Spacer(modifier = Modifier.height(geometry.blockGapMm.mm))
            PlateHeading(block)
            PlateGrid(block, onImageSettled)
        }
        Spacer(modifier = Modifier.weight(1f))
        PageFoot(page)
    }
}

/**
 * One plate's heading, repeated on every folio it spills onto.
 *
 * Its height is fixed by [PrintGeometry.headingMm] and its overflow is clipped, which is what
 * keeps the arithmetic of the page count and the drawing of the page in step: a heading that grew
 * with a catalog's specification would push cells off a page the exporter had already counted.
 *
 * **What it holds is [PrintHeading] and not this function's opinion** (#232). The band is forty
 * millimetres of masthead, twenty-eight of a name with no specification (#231) or fourteen of a name
 * band when the folio is shared — and the height and the contents come from the same value precisely
 * so that a subtitle cannot be drawn into millimetres nobody reserved.
 */
@Composable
private fun PlateHeading(block: PrintBlock) {
    val section = block.section
    val heading = block.geometry.heading
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(heading.millimetres.mm)
            .clipToBounds(),
        verticalArrangement = Arrangement.spacedBy(1f.mm),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(section.eyebrow, style = PRINT_EYEBROW, color = Paper.rust)
            Spacer(modifier = Modifier.weight(1f))
            if (block.pagesInSection > 1) {
                Text(
                    printedPageOfSection(block.numberInSection, block.pagesInSection),
                    style = PRINT_EYEBROW,
                    color = Paper.muted,
                )
            }
        }
        // Title and stamp share the row the screen already uses: the caucho lands on this plate's
        // own heading, so a shared folio (#232) stamps each complete plate and not the page (#371).
        // The Progress row below stays whole; the ratio inside the ink is the celebration rather
        // than a replacement for that labelled specification.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                section.title,
                style = PRINT_TITLE,
                maxLines = heading.titleLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (section.complete) {
                section.ratio?.let { ratio ->
                    PrintedCompletionStamp(heading = heading, ratio = ratio)
                }
            }
        }
        section.subtitle?.takeIf { heading.subtitle }?.let { subtitle ->
            Text(subtitle, style = PRINT_SUBTITLE, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        HorizontalDivider(thickness = 0.5f.mm, color = Paper.ink)
        // A band without room for the specification drops it whole rather than clipping it: the
        // list of #231 already says «Tengo» or «Me falta» on every one of its rows, so it *is* the
        // coverage the block would have summarised, and a shared folio spends those millimetres on
        // the plate underneath. Half a fact under a rule is worse than none. Where it is printed it
        // is flowed and clipped — what a plate says about itself grew with the catalogs that share
        // a type or a year, and the band is what it is.
        if (heading.facts) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5f.mm),
                verticalArrangement = Arrangement.spacedBy(1f.mm),
            ) {
                section.facts.forEach { (label, value) ->
                    Column {
                        Text(label, style = PRINT_FACT_LABEL, color = Paper.muted)
                        Text(value, style = PRINT_FACT_VALUE, maxLines = 1)
                    }
                }
            }
        }
    }
}

/**
 * The cells of one plate on one folio, on the grid its largest coin fixed.
 *
 * It is as tall as the rows it holds and no taller (#232), where before it took the whole band under
 * the heading: on a shared folio what is left over is the room the next plate is packed into, and on
 * a folio of one plate the two are the same page — the leftover simply falls above the foot, where
 * it always did.
 */
@Composable
private fun PlateGrid(block: PrintBlock, onImageSettled: (painted: Boolean) -> Unit) {
    val grid = block.grid
    val geometry = block.geometry
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(block.cellsHeightMm.mm)
            .clipToBounds(),
        verticalArrangement = Arrangement.spacedBy(geometry.gutterMm.mm),
        // The block is centred and the rows inside it are not: what the grid leaves over is
        // margin, and margin on one side only reads as a page printed askew.
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        block.cells.chunked(grid.columns).forEach { row ->
            Row(
                modifier = Modifier.width(block.blockWidthMm.mm),
                horizontalArrangement = Arrangement.spacedBy(geometry.gutterMm.mm),
            ) {
                row.forEach { cell ->
                    PrintedCell(
                        cell = cell,
                        grid = grid,
                        onImageSettled = onImageSettled,
                        modifier = Modifier
                            .width(grid.cellWidthMm.mm)
                            .height(grid.cellHeightMm.mm),
                    )
                }
            }
        }
    }
}

/**
 * One coin at its real diameter, and what is written under it — or one line, with no coin at all.
 *
 * The coin band is as tall as the **plate's** largest coin and the coin is drawn at **its own**
 * diameter inside it, which is how a plate whose issues changed size over the years keeps its rows
 * aligned without rescaling a single piece. A cell with no coin behind it — a hole — takes the same
 * diameter as the coin the collector does have, so an album page reads as a gap and not as a
 * shrunken coin.
 *
 * With «ambas caras» on (#230) the band holds the two of them side by side, each at that same
 * diameter and separated by the gutter, under **one** caption: the cell is a coin and not two. With
 * «fotos» off (#231) there is no band and the cell is a [ListedCell]: the two shapes are decided by
 * the geometry the page count was computed from, so the brush cannot disagree with the arithmetic.
 *
 * With «tamaño real» off (#233) every diameter here is the printed one — the fraction is applied by
 * [PrintGeometry.printedDiameterMm] and by nothing in this file — so the band, the circle and the hole
 * all shrink together and a plate whose issues changed size still keeps its proportions. What does not
 * shrink is the caption, which then has to say the **real** measure in words: there is no ruler under a
 * page like this to lay a coin against.
 */
@Composable
private fun PrintedCell(
    cell: PrintCell,
    grid: PrintGrid,
    onImageSettled: (painted: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val geometry = grid.geometry
    if (!geometry.printsCoins) {
        ListedCell(cell = cell, geometry = geometry, modifier = modifier)
        return
    }
    // The real diameter is what the caption may print as a number and what the band is measured from;
    // what the circle comes out at is that fraction of it, which is «tamaño real» (#233).
    val diameter = geometry.printedDiameterMm(cell.diameterMm ?: grid.diameterMm)
    Column(modifier = modifier.clipToBounds(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.fillMaxWidth().height(grid.printedDiameterMm.mm),
            contentAlignment = Alignment.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(geometry.gutterMm.mm)) {
                // Keyed by nothing: the faces of a cell are a fixed pair for the whole export, so
                // the obverse's slot is the obverse's for as long as this page exists.
                cell.faces.forEach { face ->
                    PrintedCoin(
                        face = face,
                        filled = cell.filled,
                        onImageSettled = onImageSettled,
                        modifier = Modifier.size(diameter.mm),
                    )
                }
            }
        }
        // The code goes **beside** the caption where the cell has the width for it and under the name
        // where it has not (#478). Which of the two is not this drawing's opinion: it is
        // [PrintGeometry.qrBesideCaption], the same question the page count asked before any of this
        // was composed — a cell drawn one way and counted the other is a row falling off a folio.
        if (geometry.qrBesideCaption(cell.diameterMm ?: grid.diameterMm)) {
            // The words and the code travel together and the pair is centred, rather than the code
            // being pinned to the cell's edge: with «ambas caras» a cell is two coins wide and its
            // edge is three millimetres from the **next** cell's coin, so a code anchored there ends
            // up nearer its neighbour than its own caption. Grouped, it sits two millimetres from the
            // name it belongs to, and the name gives up half the code's band of centring.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(
                    modifier = Modifier
                        .width((grid.cellWidthMm - 2 * (geometry.qrMm + geometry.qrGapMm)).mm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CellCaption(cell = cell, geometry = geometry)
                }
                Spacer(modifier = Modifier.width(geometry.qrGapMm.mm))
                NumistaCode(url = cell.numistaUrl, sideMm = geometry.qrMm)
            }
        } else {
            CellCaption(cell = cell, geometry = geometry)
            // Under the year and against it, not at the foot of the cell.
            //
            // Both were printed. Anchored to the foot the codes line up across a row, and every one of
            // them sits a finger's width from the caption it belongs to — because the words take some ten
            // of the sixteen millimetres the caption budgets for them, and the slack fell between the
            // name and the code. Against the caption the slack falls at the bottom of the cell instead,
            // where a gutter already is, and «bajo el nombre» is what the page actually shows. The price
            // is that a two-line title lowers its own code by a line; the codes of one plate are not read
            // as a row.
            if (geometry.qrMm > 0f) {
                Spacer(modifier = Modifier.height(geometry.qrGapMm.mm))
                NumistaCode(cell.numistaUrl, geometry.qrMm)
            }
        }
    }
}

/**
 * What is written under a coin: its state, its name, the year that tells it apart and its diameter.
 *
 * A composable of its own since #478, because the code can now sit beside it or under it and the words
 * are the same words either way — the two arrangements differ in where twelve millimetres of symbol go
 * and in nothing else.
 */
@Composable
private fun ColumnScope.CellCaption(cell: PrintCell, geometry: PrintGeometry) {
    CellState(cell, modifier = Modifier.padding(top = 1f.mm))
    if (cell.name != null) {
        Column(
            modifier = Modifier.fillMaxWidth().height(PRINT_CARTOUCHE_MM.mm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                cell.name.denomination,
                style = PRINT_CELL_TITLE,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 1f.sp,
                    maxFontSize = 2.9f.sp,
                    stepSize = 0.1f.sp,
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Visible,
            )
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                cell.name.theme?.let { theme ->
                    Text(
                        theme,
                        style = PRINT_CELL_THEME,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    } else {
        Text(
            cell.label,
            style = PRINT_CELL_TITLE,
            textAlign = TextAlign.Center,
            autoSize = PRINT_CELL_TITLE_AUTO_SIZE,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    // The year remains outside the cartouche; #337 owns its separate rendering change.
    cell.footnote?.let { footnote ->
        Text(
            footnote,
            style = PRINT_FOOTNOTE,
            color = Paper.muted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    // And under it the diameter, on a page with no ruler to lay a coin against (#233). A line of
    // its own and not the end of the one above: the cells of a collection with no issue list fill
    // that one with «1977 · Numista 681», and the ellipsis ate the millimetres.
    if (geometry.printsDiameterLabel) {
        printedDiameterLabel(cell.diameterMm)?.let { measure ->
            Text(
                measure,
                style = PRINT_FOOTNOTE,
                color = Paper.muted,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/**
 * One member on one line: a box to tick in pencil, the state, the name, the year and the diameter.
 *
 * This is the notebook you take with you (#231). It is not the album page with its pictures turned
 * off — it is the other thing a catalog can be, and every part of it earns its place on a line eighty
 * millimetres wide:
 *
 * - **The box first, and it is what the pencil is for.** Ticked for what the collector has, empty for
 *   what they do not, and an empty one at a fair is a box you fill in before the app ever hears about
 *   it. It is the only mark on the page meant to be made by hand.
 * - **The state beside it, in a column of its own width.** It is redundant with the box for a plate
 *   —«Tengo», «Me falta»— and it is not for «Sin ficha» or «Sin emitir», which no tick can say. The
 *   column is fixed so the names line up down the page: a checklist read at a glance is a checklist
 *   whose left edge does not move.
 * - **The name takes what is left**, and the year and the diameter are pushed to the right edge,
 *   where a column of numbers is scanned. The diameter is a *number* because this page has no ruler
 *   to hold a coin against (`printedDiameterLabel`).
 *
 * The code, where «QR de Numista» is on, closes the line rather than sitting under it: the row is
 * already a left-to-right reading, and there is nothing above it to stack it beneath.
 */
@Composable
private fun ListedCell(cell: PrintCell, geometry: PrintGeometry, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clipToBounds(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LIST_GAP_MM.mm),
    ) {
        TickBox(ticked = cell.filled, modifier = Modifier.size(LIST_BOX_MM.mm))
        CellState(cell, modifier = Modifier.width(LIST_STATE_MM.mm))
        Text(
            cell.label,
            style = PRINT_CELL_TITLE,
            // One line and the same ladder, which on a line eighty millimetres wide reaches further
            // than it does in a cell: the name gives up type before it gives up its tail (#412).
            autoSize = PRINT_CELL_TITLE_AUTO_SIZE,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        cell.footnote?.let { footnote ->
            Text(footnote, style = PRINT_FOOTNOTE, color = Paper.muted, maxLines = 1)
        }
        // Unconditional, and it is the same rule the caption of a coin asks `printsDiameterLabel` for:
        // a page of lines never carries a ruler, so the answer here is yes by construction.
        printedDiameterLabel(cell.diameterMm)?.let { diameter ->
            Text(diameter, style = PRINT_FOOTNOTE, color = Paper.muted, maxLines = 1)
        }
        if (geometry.qrMm > 0f) {
            NumistaCode(cell.numistaUrl, geometry.qrMm)
        }
    }
}

/**
 * What the cell says about itself — «Tengo», «Me falta», «Sin ficha» — or nothing where it says none.
 *
 * Shared by the two shapes of cell because it is the same claim about the same coin: a sheet of
 * pieces has no state at all (ADR 0021 §9), and what a plate's state is coloured by is whether the
 * collector owns it, on the page of coins and on the page of lines alike. Only where it sits differs,
 * which is what [modifier] carries.
 */
@Composable
private fun CellState(cell: PrintCell, modifier: Modifier = Modifier) {
    val state = cell.state ?: return
    Text(
        state,
        style = PRINT_STATE,
        color = if (cell.filled) Paper.rust else Paper.muted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * The box the collector marks: ruled empty, and ticked where the coin is already theirs.
 *
 * Drawn rather than set in a font, for the same reason the ruler and the empty mount are: it goes
 * into the PDF as commands, it is crisp at any printer's resolution, and it is a millimetre of paper
 * and not a glyph whose size depends on which font the viewer substitutes.
 */
@Composable
private fun TickBox(ticked: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 0.3f.mm.toPx()
        drawRect(
            color = Paper.ink,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(width = stroke),
        )
        if (!ticked) return@Canvas
        // A check and not a fill: a solid box beside «TENGO» is a blot, and the collector's own
        // pencil marks are going to be checks too.
        val tick = 0.45f.mm.toPx()
        drawLine(
            color = Paper.rust,
            start = Offset(size.width * 0.22f, size.height * 0.52f),
            end = Offset(size.width * 0.44f, size.height * 0.76f),
            strokeWidth = tick,
        )
        drawLine(
            color = Paper.rust,
            start = Offset(size.width * 0.44f, size.height * 0.76f),
            end = Offset(size.width * 0.80f, size.height * 0.24f),
            strokeWidth = tick,
        )
    }
}

/**
 * The Numista page of one coin, as modules drawn on the page (#234).
 *
 * **Rectangles and not a bitmap.** The code goes into the PDF as drawing commands, so it is crisp at
 * any zoom and at any printer's resolution — and it never touches Coil, which means it is not a
 * photograph that can fail to arrive, cannot leave a hole in a page, and costs nothing in the
 * warm-up of #169. It is the one thing on a printed cell that is guaranteed to be there.
 *
 * A cell with no URL —a member no Numista type backs, an unlisted one— leaves the square blank rather
 * than drawing a code that leads nowhere. The square is reserved either way: the caption is a
 * constant of the layout, and it is what the page count was computed against.
 */
@Composable
internal fun NumistaCode(url: String?, sideMm: Float, modifier: Modifier = Modifier) {
    // Encoded once per URL rather than on every recomposition: a page of twelve cells is twelve
    // encodings, and the same type shows up on several pages of the notebook.
    val code = remember(url) { numistaQr(url) } ?: return
    Canvas(modifier = modifier.size(sideMm.mm)) {
        // PaperGrain belongs to the sheet, never to a QR. Paint one opaque, perfectly even field
        // behind both the light modules and the four-module quiet zone before laying down the ink.
        drawRect(color = Paper.paper)
        val module = size.minDimension / code.qrModulesWithQuietZone
        val quiet = module * QR_QUIET_MODULES
        for (row in 0 until code.height) {
            code.qrRuns(row).forEach { run ->
                drawRect(
                    color = Paper.ink,
                    topLeft = Offset(quiet + run.first * module, quiet + row * module),
                    size = Size((run.last - run.first + 1) * module, module),
                )
            }
        }
    }
}

/**
 * One face of one coin, printed round.
 *
 * By default it is one face and one only (#169): the album page is the side you look at, and a
 * second picture at 1:1 is paid for in width — which is what «ambas caras» decides to do (#230), not
 * something this drawing can settle on its own. Which face that one is was decided before it got
 * here too, by the plate that declared it (#227). A hole keeps the catalog design faded
 * and desaturated, exactly as the plate on screen does, and is ruled with a dashed circle so it
 * reads as an empty mount rather than as a badly printed coin.
 *
 * [filled] is the **cell's** and not the face's: a coin the collector owns whose obverse nobody
 * photographed is still owned, so that slot gets the silhouette of a coin that is there and not the
 * dashed mount of one that is missing.
 */
@Composable
private fun PrintedCoin(
    face: CoinPhoto,
    filled: Boolean,
    onImageSettled: (painted: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val candidates = face.candidates
    var attempt by remember(candidates) { mutableIntStateOf(0) }
    var painted by remember(candidates) { mutableStateOf(false) }
    val url = candidates.getOrNull(attempt)
    Box(modifier = modifier) {
        // The stand-in for a coin whose picture is not cached, and only for a coin the collector
        // has: a hole's stand-in is the dashed mount below, and a filled silhouette in a «me
        // falta» cell would make the two indistinguishable on a page with no photographs at all.
        if (filled && !painted) {
            Silhouette(Modifier.matchParentSize())
        }
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                // The thumbnail first and the original behind it, as everywhere else (#67); both
                // outcomes report back, because the notebook has to know how many cells it froze
                // empty before it says the export went well.
                onState = { state ->
                    when (state) {
                        is AsyncImagePainter.State.Success -> {
                            painted = true
                            onImageSettled(true)
                        }
                        is AsyncImagePainter.State.Error ->
                            if (attempt < candidates.lastIndex) {
                                attempt += 1
                            } else {
                                onImageSettled(false)
                            }
                        else -> Unit
                    }
                },
                colorFilter = paperCoinFilter(missing = !filled),
                modifier = Modifier
                    .matchParentSize()
                    // Clipped round, which the screen's cell does not do and this page must: at
                    // 1:1 the photograph's own background is a pale square exactly as wide as the
                    // coin, and a square around a round coin is what gives away that a printed
                    // plate is a screenshot. Numista crops its photographs to the coin, so the
                    // circle takes the corners and nothing else.
                    .clip(CircleShape)
                    .alpha(if (filled) 1f else 0.45f),
            )
        }
        if (!filled) {
            EmptyMount(Modifier.matchParentSize())
        }
    }
}

/** The dashed circle of a mount with nothing in it, drawn in the guide's own hand. */
@Composable
private fun EmptyMount(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // Through the draw scope's own density rather than by multiplying by `PX_PER_MM`: the
        // millimetre is the unit of the page either way, and this one stays a millimetre if the
        // page is ever recorded at another resolution.
        val stroke = 0.4f.mm.toPx()
        drawCircle(
            color = Paper.hairline,
            radius = (size.minDimension - stroke) / 2f,
            style = Stroke(
                width = stroke,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(1.6f.mm.toPx(), 1.2f.mm.toPx()),
                ),
            ),
        )
    }
}

/**
 * The foot: the ruler on the left, the source on the right.
 *
 * The ruler is the only thing on the page that is about the page itself. A PDF viewer's «fit to
 * page» silently rescales everything, and a plate whose whole claim is that a one-ounce coin
 * measures forty millimetres has to be falsifiable with the ruler in the collector's drawer.
 *
 * On a page with no coin at 1:1 there is nothing to falsify, so the ruler goes and the strip narrows to
 * the source alone — with «fotos» off because no coin is drawn (#231), and with «tamaño real» off
 * because the one that is drawn is not the size it claims (#233). The source stays whatever the page
 * prints, because the paper outlives the app and a list that does not say where it came from cannot be
 * checked later either. What replaces the bar is every caption printing its diameter as a number.
 *
 * **One strip per folio and not per plate** (#232), which is why the source can be a plural: two
 * plates sharing a page can come from two catalogs, and one of them going unnamed would attribute
 * its coins to the other.
 */
@Composable
private fun PageFoot(page: PrintPage) {
    val geometry = page.geometry
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(geometry.footMm.mm)
            .clipToBounds(),
        verticalAlignment = Alignment.Bottom,
    ) {
        if (geometry.rulerBarMm > 0f) {
            Column {
                Ruler(geometry)
                Text(
                    printedRulerLabel(geometry.rulerBarMm.toInt()),
                    style = PRINT_FACT_LABEL,
                    color = Paper.muted,
                    modifier = Modifier.padding(top = 0.8f.mm),
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            notebookSourceLabel(page.sources),
            style = PRINT_FOOTNOTE,
            color = Paper.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A 50 mm bar ticked every 10 mm, drawn rather than measured from any font. */
@Composable
private fun Ruler(geometry: PrintGeometry) {
    Canvas(
        modifier = Modifier
            .width(geometry.rulerBarMm.mm)
            .height(3f.mm),
    ) {
        val stroke = 0.3f.mm.toPx()
        val baseline = size.height - stroke / 2f
        drawLine(
            color = Paper.ink,
            start = Offset(0f, baseline),
            end = Offset(size.width, baseline),
            strokeWidth = stroke,
        )
        // Six marks for five centimetres, the ends included: a bar with no ends is not a ruler.
        for (tick in 0..5) {
            val x = (size.width - stroke) * tick / 5f + stroke / 2f
            val long = tick % 5 == 0
            drawLine(
                color = Paper.ink,
                start = Offset(x, baseline),
                end = Offset(x, if (long) 0f else size.height / 2f),
                strokeWidth = stroke,
            )
        }
    }
}
