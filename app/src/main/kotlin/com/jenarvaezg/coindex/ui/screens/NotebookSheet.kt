package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.jenarvaezg.coindex.ui.components.Silhouette
import com.jenarvaezg.coindex.ui.components.paperCoinFilter
import com.jenarvaezg.coindex.ui.print.PrintCell
import com.jenarvaezg.coindex.ui.print.PrintGeometry
import com.jenarvaezg.coindex.ui.print.PrintGrid
import com.jenarvaezg.coindex.ui.print.PrintPage
import com.jenarvaezg.coindex.ui.print.QR_QUIET_MODULES
import com.jenarvaezg.coindex.ui.print.numistaQr
import com.jenarvaezg.coindex.ui.print.qrModulesWithQuietZone
import com.jenarvaezg.coindex.ui.print.qrRuns
import com.jenarvaezg.coindex.ui.theme.Paper

/** The density that makes one dp a millimetre of paper: the layout is written in millimetres. */
val printDensity = Density(density = PrintGeometry.PX_PER_MM, fontScale = 1f)

/** Millimetres, as the unit the whole printed page is laid out in. */
private val Float.mm: Dp get() = Dp(this)

// Type sized in millimetres of paper rather than in phone dp: the sheet of a single plate scales
// the screen's typography by a factor (`scaledBy`), which works because that sheet's width is a
// multiple of a phone's. A4 is not, and paper diverges from the screen on purpose (#169) — 3 mm of
// serif is around 8,5 pt, which is what a printed album caption is set in.
private val PRINT_EYEBROW = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 2.6f.sp,
    letterSpacing = 0.35f.sp,
)
private val PRINT_TITLE = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 7f.sp,
    lineHeight = 8f.sp,
)
private val PRINT_SUBTITLE = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 3.6f.sp,
    lineHeight = 4.2f.sp,
)
private val PRINT_FACT_LABEL = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 2.2f.sp,
    letterSpacing = 0.25f.sp,
)
private val PRINT_FACT_VALUE = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 3.2f.sp,
    lineHeight = 3.6f.sp,
)
private val PRINT_STATE = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 2.3f.sp,
    letterSpacing = 0.25f.sp,
)
private val PRINT_CELL_TITLE = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 2.9f.sp,
    lineHeight = 3.3f.sp,
)
private val PRINT_FOOTNOTE = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = 2.3f.sp,
    letterSpacing = 0.1f.sp,
)

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
            .background(Paper.paper)
            // The margin is the page's and not each band's: inside it the three bands of the
            // geometry add up to exactly the printable height, which is what the page count was
            // computed against.
            .padding(geometry.marginMm.mm),
    ) {
        PageHeading(page)
        PageGrid(page, onImageSettled)
        PageFoot(page)
    }
}

/**
 * The heading, repeated on every page of a plate that spills over.
 *
 * Its height is fixed by [PrintGeometry.headingMm] and its overflow is clipped, which is what
 * keeps the arithmetic of the page count and the drawing of the page in step: a heading that grew
 * with a catalog's specification would push cells off a page the exporter had already counted.
 */
@Composable
private fun PageHeading(page: PrintPage) {
    val section = page.section
    val geometry = page.geometry
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(geometry.headingMm.mm)
            .clipToBounds(),
        verticalArrangement = Arrangement.spacedBy(1f.mm),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(section.eyebrow, style = PRINT_EYEBROW, color = Paper.rust)
            Spacer(modifier = Modifier.weight(1f))
            // Only where there is a break to explain: «página 1 de 1» is noise on paper.
            if (page.pagesInSection > 1) {
                Text(
                    "PÁGINA ${page.numberInSection} DE ${page.pagesInSection}",
                    style = PRINT_EYEBROW,
                    color = Paper.muted,
                )
            }
        }
        // Two lines, because the title on a plate is the catalog's editorial name and it says what
        // the list does and does not claim: «1 oz bullion anual (sin proof, burnished ni privy)»
        // truncated at one line is exactly the qualification a printed page cannot afford to lose.
        Text(section.title, style = PRINT_TITLE, maxLines = 2, overflow = TextOverflow.Ellipsis)
        section.subtitle?.let { subtitle ->
            Text(subtitle, style = PRINT_SUBTITLE, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        HorizontalDivider(thickness = 0.5f.mm, color = Paper.ink)
        // Flowed and clipped: what a plate says about itself grew with the catalogs that share a
        // type or a year, and the band is what it is.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5f.mm),
            verticalArrangement = Arrangement.spacedBy(1f.mm),
        ) {
            section.facts.forEach { (label, value) ->
                Column {
                    Text(label.uppercase(), style = PRINT_FACT_LABEL, color = Paper.muted)
                    Text(value, style = PRINT_FACT_VALUE, maxLines = 1)
                }
            }
        }
    }
}

/** The cells of this page, on the grid its plate's largest coin fixed. */
@Composable
private fun PageGrid(page: PrintPage, onImageSettled: (painted: Boolean) -> Unit) {
    val grid = page.grid
    val geometry = page.geometry
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(geometry.gridHeightMm.mm)
            .clipToBounds(),
        verticalArrangement = Arrangement.spacedBy(geometry.gutterMm.mm),
        // The block is centred and the rows inside it are not: what the grid leaves over is
        // margin, and margin on one side only reads as a page printed askew.
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        page.cells.chunked(grid.columns).forEach { row ->
            Row(
                modifier = Modifier.width(page.blockWidthMm.mm),
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
 * One coin at its real diameter, and what is written under it.
 *
 * The coin band is as tall as the **plate's** largest coin and the coin is drawn at **its own**
 * diameter inside it, which is how a plate whose issues changed size over the years keeps its rows
 * aligned without rescaling a single piece. A cell with no coin behind it — a hole — takes the same
 * diameter as the coin the collector does have, so an album page reads as a gap and not as a
 * shrunken coin.
 */
@Composable
private fun PrintedCell(
    cell: PrintCell,
    grid: PrintGrid,
    onImageSettled: (painted: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val geometry = grid.geometry
    val diameter = cell.diameterMm ?: grid.diameterMm
    Column(modifier = modifier.clipToBounds(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.fillMaxWidth().height(grid.diameterMm.mm),
            contentAlignment = Alignment.Center,
        ) {
            PrintedCoin(
                cell = cell,
                onImageSettled = onImageSettled,
                modifier = Modifier.size(diameter.mm),
            )
        }
        cell.state?.let { state ->
            Text(
                state.uppercase(),
                style = PRINT_STATE,
                color = if (cell.filled) Paper.rust else Paper.muted,
                maxLines = 1,
                modifier = Modifier.padding(top = 1f.mm),
            )
        }
        Text(
            cell.label,
            style = PRINT_CELL_TITLE,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
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
private fun NumistaCode(url: String?, sideMm: Float) {
    // Encoded once per URL rather than on every recomposition: a page of twelve cells is twelve
    // encodings, and the same type shows up on several pages of the notebook.
    val code = remember(url) { numistaQr(url) } ?: return
    Canvas(modifier = Modifier.size(sideMm.mm)) {
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
 * The reverse of one coin, printed round.
 *
 * Only the reverse reaches the paper (#169): two faces at 1:1 would halve the diameter, which is
 * the one thing a page measured with a ruler cannot do. A hole keeps the catalog design faded and
 * desaturated, exactly as the plate on screen does, and is ruled with a dashed circle so it reads
 * as an empty mount rather than as a badly printed coin.
 */
@Composable
private fun PrintedCoin(
    cell: PrintCell,
    onImageSettled: (painted: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val candidates = cell.reverse?.candidates.orEmpty()
    var attempt by remember(candidates) { mutableIntStateOf(0) }
    var painted by remember(candidates) { mutableStateOf(false) }
    val url = candidates.getOrNull(attempt)
    Box(modifier = modifier) {
        // The stand-in for a coin whose picture is not cached, and only for a coin the collector
        // has: a hole's stand-in is the dashed mount below, and a filled silhouette in a «me
        // falta» cell would make the two indistinguishable on a page with no photographs at all.
        if (cell.filled && !painted) {
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
                colorFilter = paperCoinFilter(missing = !cell.filled),
                modifier = Modifier
                    .matchParentSize()
                    // Clipped round, which the screen's cell does not do and this page must: at
                    // 1:1 the photograph's own background is a pale square exactly as wide as the
                    // coin, and a square around a round coin is what gives away that a printed
                    // plate is a screenshot. Numista crops its photographs to the coin, so the
                    // circle takes the corners and nothing else.
                    .clip(CircleShape)
                    .alpha(if (cell.filled) 1f else 0.45f),
            )
        }
        if (!cell.filled) {
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
 */
@Composable
private fun PageFoot(page: PrintPage) {
    val geometry = page.geometry
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(geometry.rulerMm.mm)
            .clipToBounds(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column {
            Ruler(geometry)
            Text(
                "${geometry.rulerBarMm.toInt()} MM · ESCALA 1:1",
                style = PRINT_FACT_LABEL,
                color = Paper.muted,
                modifier = Modifier.padding(top = 0.8f.mm),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            page.section.source,
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
