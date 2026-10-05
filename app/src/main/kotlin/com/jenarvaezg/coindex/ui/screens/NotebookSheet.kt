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

/** Makes one dp a millimetre of paper, so the page layout is written in millimetres. */
val printDensity = Density(density = PrintGeometry.PX_PER_MM, fontScale = 1f)

private val Float.mm: Dp get() = Dp(this)

// Type sized in millimetres of paper, not scaled from the screen's typography as the single-plate
// sheet does (#169): A4 isn't a multiple of a phone's width. 3 mm of serif is about 8,5 pt.
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
 * Cell titles shrink before they ellipsize, as on screen (#348, #412). Paper can't get a third
 * line like the screen did: [PrintGeometry.captionMm] feeds the page count.
 *
 * The 1,8 mm floor (about 5 pt) is the size at which no name the screen prints whole gets cut on
 * paper in the narrowest cell (28 mm); it is only reached when the alternative is an ellipsis.
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
// Denomination plus at most two theme lines, within the 16 mm caption, so it can't change the page
// count (#350).
private const val PRINT_CARTOUCHE_MM = 9.1f
private val PRINT_FOOTNOTE = TextStyle(
    fontFamily = BarlowCondensedFamily,
    fontSize = 2.3f.sp,
    fontFeatureSettings = "'tnum'",
)

/** Side of the tick box on a page without photographs (#231). */
private const val LIST_BOX_MM = 3.4f

/** Gap between items on one list line. */
private const val LIST_GAP_MM = 1.4f

/**
 * Fixed width of the state column on a list line, so the names line up. Fits «SIN EMITIR» and
 * «TENGO · ×9»; a two-digit quantity ellipsizes rather than shifting the name.
 */
private const val LIST_STATE_MM = 17f

/**
 * One page of the printed notebook, at A4 and 1:1.
 *
 * Not the exported single-plate sheet with other numbers: that is a bitmap as wide as its grid,
 * both faces, with a density that shrinks as the catalog grows. Here the page and the coin size are
 * fixed and what varies is how many fit (#169). The two diverge on purpose.
 */
@Composable
fun NotebookPageSheet(
    page: PrintPage,
    onImageSettled: (painted: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // From the export's configuration (#228).
    val geometry = page.geometry
    Column(
        // Painted here, or the recording comes out transparent and each viewer picks a colour.
        modifier = modifier
            .size(geometry.widthMm.mm, geometry.heightMm.mm)
            .paperSurface()
            // If a plate given its own folio ever overflowed it, clip rather than draw off the
            // page. Doesn't happen at shipped diameters.
            .clipToBounds()
            // One page margin; inside it plates and foot fit the printable height the packer used.
            .padding(geometry.marginMm.mm),
    ) {
        page.blocks.forEachIndexed { index, block ->
            // Between plates only; the folio's leftover space goes at its foot (#232).
            if (index > 0) Spacer(modifier = Modifier.height(geometry.blockGapMm.mm))
            PlateHeading(block)
            PlateGrid(block, onImageSettled)
        }
        Spacer(modifier = Modifier.weight(1f))
        PageFoot(page)
    }
}

/**
 * A plate's heading, or the thin name band on a folio it continues onto or shares.
 *
 * Its height comes from [PrintHeading] (#232), the same value the page count used, and overflow is
 * clipped, so the drawing can't push cells off a counted page. The block decides which band (#480):
 * a continuation page gets the thin one, since repeating the specification costs a row of coins.
 */
@Composable
private fun PlateHeading(block: PrintBlock) {
    val section = block.section
    val heading = block.heading
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
        // The stamp sits on this plate's heading, so a shared folio (#232) stamps each complete
        // plate, not the page (#371). The facts row below still prints the ratio.
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
        // A band without room for the facts drops them whole rather than clipping half. Where they
        // print, they flow and are clipped to the band.
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
 * The cells of one plate on one folio, on the grid its largest coin fixes. Only as tall as its rows
 * (#232), so a shared folio can pack the next plate below.
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
        // Block centred, rows left-aligned inside it: one-sided margin looks askew.
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
 * One coin at its diameter with its caption, or a [ListedCell] line when «fotos» is off (#231).
 *
 * The coin band is as tall as the plate's largest coin and each coin is drawn at its own diameter,
 * so rows stay aligned without rescaling. A hole takes the diameter of the coin it stands for.
 * With «ambas caras» (#230) both faces share one band and one caption.
 *
 * With «tamaño real» off (#233) every diameter is already scaled by
 * [PrintGeometry.printedDiameterMm], so coins keep their proportions; the caption then states the
 * real diameter, since there is no ruler.
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
    // Printed diameter: the real one, scaled when «tamaño real» is off (#233).
    val diameter = geometry.printedDiameterMm(cell.diameterMm ?: grid.diameterMm)
    Column(modifier = modifier.clipToBounds(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.fillMaxWidth().height(grid.printedDiameterMm.mm),
            contentAlignment = Alignment.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(geometry.gutterMm.mm)) {
                // No key needed: a cell's faces are fixed for the whole export.
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
        // QR beside the caption when the cell is wide enough, else under it (#478). Decided by
        // [PrintGeometry.qrBesideCaption], as the page count was, so drawing and count agree.
        if (geometry.qrBesideCaption(cell.diameterMm ?: grid.diameterMm)) {
            // Caption and code centred as a pair, not the code pinned to the cell edge: with
            // «ambas caras» the edge sits nearer the next cell's coin than this caption.
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
            // Right under the caption, not at the cell's foot: anchored to the foot, the caption's
            // slack fell between name and code. Codes in a row no longer line up, which is fine.
            if (geometry.qrMm > 0f) {
                Spacer(modifier = Modifier.height(geometry.qrGapMm.mm))
                NumistaCode(cell.numistaUrl, geometry.qrMm)
            }
        }
    }
}

/**
 * The text under a coin: state, name, distinguishing year and diameter. Shared by both QR
 * placements (#478).
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
    // The diameter when the page has no ruler (#233), on its own line: footnotes can be long
    // enough to ellipsize it away.
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
 * One member on one line, for the take-along list of #231: a box to tick in pencil, the state, the
 * name, then year and diameter at the right edge.
 *
 * The state is redundant with the box for «Tengo» and «Me falta» but not for «Sin ficha» or «Sin
 * emitir»; its column has a fixed width so the names line up. The diameter is a number because this
 * page has no ruler. With «QR de Numista» on, the code ends the line.
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
            // Shrinks before ellipsizing (#412).
            autoSize = PRINT_CELL_TITLE_AUTO_SIZE,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        cell.footnote?.let { footnote ->
            Text(footnote, style = PRINT_FOOTNOTE, color = Paper.muted, maxLines = 1)
        }
        // Always: a list page never has a ruler (the rule behind `printsDiameterLabel`).
        printedDiameterLabel(cell.diameterMm)?.let { diameter ->
            Text(diameter, style = PRINT_FOOTNOTE, color = Paper.muted, maxLines = 1)
        }
        if (geometry.qrMm > 0f) {
            NumistaCode(cell.numistaUrl, geometry.qrMm)
        }
    }
}

/**
 * The cell's state («Tengo», «Me falta», «Sin ficha»), or nothing for a sheet of pieces
 * (ADR 0021 §9). Coloured by ownership in both cell shapes.
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
 * The box the collector ticks in pencil, pre-ticked when the coin is owned. Drawn, not a glyph, so
 * it is crisp at any resolution and doesn't depend on the viewer's font substitution.
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
        // A check, not a fill, to match pencil ticks.
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
 * A QR code for the coin's Numista page (#234), drawn as rectangles: crisp at any zoom, and it
 * never goes through Coil, so it can't fail to load or slow the photo warm-up (#169).
 *
 * Without a URL the square is left blank; the space stays reserved because the page count assumed
 * it.
 */
@Composable
internal fun NumistaCode(url: String?, sideMm: Float, modifier: Modifier = Modifier) {
    // Encoded once per URL, not per recomposition.
    val code = remember(url) { numistaQr(url) } ?: return
    Canvas(modifier = modifier.size(sideMm.mm)) {
        // No paper grain behind a QR: an even opaque field under the light modules and quiet zone.
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
 * One face of one coin, printed round. Which face was decided by the plate (#227); «ambas caras»
 * (#230) means two calls. A hole shows the catalog design faded, under a dashed circle so it reads
 * as an empty mount rather than a bad print.
 *
 * [filled] is the cell's, not the face's: an owned coin missing one photo still gets the owned-coin
 * silhouette, not the empty mount.
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
        // Silhouette only for owned coins; a hole has the dashed mount, and the two must stay
        // distinguishable when no photo loads.
        if (filled && !painted) {
            Silhouette(Modifier.matchParentSize())
        }
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                // Thumbnail first, then the original (#67). Both outcomes report back so the
                // closing message can count missing photos.
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
                    // Clipped round, unlike on screen: at 1:1 the photo's pale square background
                    // shows. Numista crops photos to the coin, so only the corners go.
                    .clip(CircleShape)
                    .alpha(if (filled) 1f else 0.45f),
            )
        }
        if (!filled) {
            EmptyMount(Modifier.matchParentSize())
        }
    }
}

/** The dashed circle of an empty mount. */
@Composable
private fun EmptyMount(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // Via the draw scope's density, not `PX_PER_MM`, so it stays a millimetre at any
        // resolution.
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
 * The page foot: ruler on the left, source on the right, one strip per folio (#232).
 *
 * The ruler lets the collector check that a viewer's «fit to page» didn't rescale the 1:1 coins.
 * It is dropped when no coin is at 1:1 («fotos» off, #231; «tamaño real» off, #233), and captions
 * print diameters instead. The source always prints, so paper can be traced back; it can be plural
 * when two plates from different catalogs share a folio.
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

/** The ruler bar, divided in five, drawn rather than set in a font. */
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
        // Six marks, both ends included.
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
