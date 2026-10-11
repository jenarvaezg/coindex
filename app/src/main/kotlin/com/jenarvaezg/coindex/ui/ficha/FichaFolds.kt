package com.jenarvaezg.coindex.ui.ficha

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.ui.COIN_CLAIM_HERE_SUFFIX
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.components.AlbumHole
import com.jenarvaezg.coindex.ui.components.ExternalLink
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.HoleAbsence
import com.jenarvaezg.coindex.ui.components.LinkText
import com.jenarvaezg.coindex.ui.printedPhoto
import com.jenarvaezg.coindex.ui.theme.Paper

/** The sections, each closed on one line until tapped (#643). Opening one moves nothing else. */
@Composable
internal fun Folds(
    subject: CoinFichaSubject,
    images: Map<Int, TypeImages>,
    onOpenType: (Int) -> Unit,
    onOpenClaim: (CardDestination) -> Unit,
    /** Opens a photograph whole, with the caption that says whose it is. */
    onPhoto: (CoinPhoto, String) -> Unit,
) {
    var open by rememberSaveable(subject.typeId) {
        mutableStateOf(subject.sections.filter { it.open }.map { it.title })
    }
    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
        HorizontalDivider(color = Paper.line)
        subject.sections.forEach { section ->
            val isOpen = section.title in open
            Fold(
                section = section,
                open = isOpen,
                onToggle = { open = if (isOpen) open - section.title else open + section.title },
            ) {
                when (section) {
                    is PiecesSection -> PiecesBody(section) { photo ->
                        onPhoto(photo, FichaPageLabels.PIECE_PHOTO_CAPTION)
                    }
                    is CostSection -> Ladder(section.rungs)
                    is ClaimsSection -> ClaimsBody(section, images, onOpenClaim)
                    is SpecSection -> SpecBody(section.rows)
                    is FacesSection -> FacesBody(section.faces) { photo ->
                        onPhoto(photo, FichaPageLabels.EDGE_PHOTO_CAPTION)
                    }
                    is CommentsSection -> FichaProse(section.html)
                    is RelatedSection -> RelatedBody(section.rows, onOpenType)
                }
            }
        }
    }
}

@Composable
private fun Fold(
    section: FichaSection,
    open: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle)
                .padding(vertical = 12.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(section.title, style = MaterialTheme.typography.titleMedium)
                if (section.summary.isNotEmpty()) {
                    Text(
                        section.summary,
                        style = MaterialTheme.typography.labelLarge,
                        color = Paper.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Chevron(open, modifier = Modifier.padding(start = 12.dp))
        }
        if (open) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                content = content,
            )
        }
        HorizontalDivider(color = Paper.line)
    }
}

/** Drawn, like the app's other glyphs: neither typeface has one (#298). */
@Composable
private fun Chevron(open: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(12.dp).rotate(if (open) 180f else 0f)) {
        val stroke = 1.5.dp.toPx()
        val left = Offset(size.width * 0.12f, size.height * 0.32f)
        val tip = Offset(size.width * 0.5f, size.height * 0.70f)
        val right = Offset(size.width * 0.88f, size.height * 0.32f)
        drawLine(Paper.ink, left, tip, stroke)
        drawLine(Paper.ink, tip, right, stroke)
    }
}

@Composable
private fun PiecesBody(section: PiecesSection, onPhoto: (CoinPhoto) -> Unit) {
    var selected by rememberSaveable(section.dates) { mutableStateOf(section.selected) }
    if (section.dates.isNotEmpty()) {
        DateGrid(section.dates, selected, onSelect = { selected = it })
    }
    val shown = if (section.dates.isEmpty()) {
        section.pieces
    } else {
        section.pieces.filter { it.dateKey == selected }
    }
    shown.forEachIndexed { index, piece ->
        if (index > 0) HorizontalDivider(color = Paper.paperDeep)
        PieceBlock(piece, onPhoto)
    }
}

@Composable
private fun DateGrid(dates: List<DateTile>, selected: String?, onSelect: (String) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val tile = (maxWidth - DATE_GAP * (DATES_PER_ROW - 1)) / DATES_PER_ROW
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(DATE_GAP),
            verticalArrangement = Arrangement.spacedBy(DATE_GAP),
            maxItemsInEachRow = DATES_PER_ROW,
        ) {
            dates.forEach { date ->
                DateTileView(
                    date = date,
                    selected = date.key == selected,
                    onClick = { onSelect(date.key) },
                    modifier = Modifier.width(tile),
                )
            }
        }
    }
}

@Composable
private fun DateTileView(
    date: DateTile,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = if (selected) Paper.paper else Paper.ink
    Box(
        modifier = modifier
            .background(if (selected) Paper.ink else Paper.card)
            .border(1.dp, if (selected) Paper.ink else Paper.line)
            .then(if (date.casilla) Modifier.padding(3.dp).dashedOutline(Paper.rust) else Modifier)
            .clickable(role = Role.Tab, onClick = onClick),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp, horizontal = 2.dp),
        ) {
            Text(date.label, style = MaterialTheme.typography.bodyMedium, color = ink)
            Text(
                date.grades,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Paper.paperDeep else Paper.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        date.quantity?.let { quantity ->
            Text(
                quantity,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Paper.paperDeep else Paper.rust,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 3.dp),
            )
        }
        if (date.noted) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .size(5.dp)
                    .background(Paper.moss, CircleShape),
            )
        }
    }
}

@Composable
private fun PieceBlock(piece: FichaPiece, onPhoto: (CoinPhoto) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(piece.heading, style = MaterialTheme.typography.titleMedium)
                piece.grade?.let { grade ->
                    Text(
                        grade,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .border(1.dp, Paper.line)
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
                piece.quantity?.let {
                    Text(it, style = MaterialTheme.typography.labelLarge, color = Paper.muted)
                }
                if (piece.here) {
                    Text(
                        FichaPageLabels.THIS_CASILLA,
                        style = MaterialTheme.typography.labelMedium,
                        color = Paper.rust,
                    )
                }
            }
            piece.photo?.let { photo ->
                AsyncImage(
                    model = photo.thumbnail ?: photo.picture,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 40.dp, height = 56.dp)
                        .border(1.dp, Paper.line)
                        .clickable(role = Role.Button) { onPhoto(photo) },
                )
            }
        }
        piece.facts?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        if (piece.rungs.isNotEmpty()) Ladder(piece.rungs)
        piece.issueComment?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = Paper.muted)
        }
        piece.note?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = Paper.muted)
        }
    }
}

/** The prices in a ladder: the whole label, the amount, its bar and where it comes from. */
@Composable
private fun Ladder(rungs: List<FichaRung>) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
        rungs.forEach { rung -> RungView(rung) }
    }
}

@Composable
private fun RungView(rung: FichaRung) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                rung.label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (rung.amount == null) Paper.muted else Paper.ink,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (rung.greatest) {
                Text(
                    FichaPageLabels.GREATEST,
                    style = MaterialTheme.typography.labelSmall,
                    color = Paper.rust,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .border(1.dp, Paper.rust)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                if (rung.amount != null) {
                    Text(
                        rung.amount,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                        ),
                        color = if (rung.greatest) Paper.rust else Paper.ink,
                    )
                } else {
                    Text(
                        rung.why.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Paper.muted,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
        RungBar(rung)
        rung.source?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = Paper.muted)
        }
    }
}

@Composable
private fun RungBar(rung: FichaRung) {
    val bar = Modifier.fillMaxWidth().height(7.dp)
    if (rung.amount == null) {
        Box(modifier = bar.dashedOutline(Paper.hairline))
        return
    }
    val colour = if (rung.greatest) Paper.rust else Paper.line
    Box(modifier = bar.border(1.dp, colour)) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(rung.share.coerceIn(0.04f, 1f))
                .background(if (rung.greatest) Paper.rust else Paper.paperDeep),
        )
    }
}

@Composable
private fun ClaimsBody(
    section: ClaimsSection,
    images: Map<Int, TypeImages>,
    onOpenClaim: (CardDestination) -> Unit,
) {
    Column {
        section.claims.forEach { (claim, here) ->
            if (here) {
                ClaimHere(claim.name)
            } else {
                LinkText(
                    text = claim.name,
                    style = MaterialTheme.typography.bodyLarge,
                    onClick = { onOpenClaim(claim.destination) },
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    section.strip?.let { strip -> StripView(strip, images) }
}

/** [LinkText]'s padding without the link; a long name is cut before the suffix (#635). */
@Composable
private fun ClaimHere(name: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            COIN_CLAIM_HERE_SUFFIX,
            style = MaterialTheme.typography.bodyLarge,
            color = Paper.muted,
            maxLines = 1,
        )
    }
}

/** The plate the screen came from, small, with this coin's casilla ringed. */
@Composable
private fun StripView(strip: FichaStrip, images: Map<Int, TypeImages>) {
    val hole = if (strip.cells.size <= FEW_CASILLAS) 56.dp else 32.dp
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        strip.cells.forEach { cell ->
            AlbumHole(
                photo = cell.typeId?.let { images[it]?.printedPhoto(strip.side) },
                absence = if (cell.missing) HoleAbsence.Missing else HoleAbsence.Filled,
                modifier = Modifier
                    .size(hole)
                    .then(
                        if (cell.here) Modifier.border(2.dp, Paper.rust, CircleShape) else Modifier,
                    ),
            )
        }
    }
    strip.count?.let {
        Text(it, style = MaterialTheme.typography.labelMedium, color = Paper.rust)
    }
}

/** The cartouche: label on the left, the value set to the right edge. */
@Composable
private fun SpecBody(rows: List<Pair<String, String>>) {
    FieldCard(modifier = Modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) {
                HorizontalDivider(
                    color = Paper.paperDeep,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Paper.muted,
                    modifier = Modifier.padding(top = 3.dp, end = 16.dp),
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Each face and the edge as Numista writes them: legend, legend written out, description. */
@Composable
private fun FacesBody(faces: List<FaceBlock>, onPhoto: (CoinPhoto) -> Unit) {
    faces.forEachIndexed { index, face ->
        if (index > 0) HorizontalDivider(color = Paper.paperDeep)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(face.heading, style = MaterialTheme.typography.labelMedium, color = Paper.muted)
            face.lettering?.let { legend ->
                LetteringText(
                    text = legend,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        letterSpacing = 0.12.em,
                        color = Paper.ink,
                    ),
                )
            }
            face.unabridgedLegend?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = Paper.muted)
            }
            face.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            // Apart, never as a texture: each contributor shoots the edge differently.
            face.photo?.let { photo ->
                LinkText(
                    text = FichaPageLabels.EDGE_PHOTO,
                    style = MaterialTheme.typography.bodyMedium,
                    onClick = { onPhoto(photo) },
                )
            }
        }
    }
}

@Composable
private fun RelatedBody(rows: List<RelatedRow>, onOpenType: (Int) -> Unit) {
    rows.forEachIndexed { index, row ->
        if (index > 0) HorizontalDivider(color = Paper.paperDeep)
        Column {
            row.country?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = Paper.muted)
            }
            ExternalLink(
                text = row.title,
                onClick = { onOpenType(row.typeId) },
                style = MaterialTheme.typography.bodyLarge,
            )
            row.years?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = Paper.muted)
            }
        }
    }
}

private const val DATES_PER_ROW = 7
private val DATE_GAP = 5.dp

/** Up to this many casillas, the miniature is big enough to tell the coins apart. */
private const val FEW_CASILLAS = 6

/** Behind a photograph opened whole. */
internal val LIGHTBOX_SCRIM = Color(0xE0141512)

/** A rounded dashed rule around the content, the mark of something absent (#633). */
internal fun Modifier.dashedOutline(color: Color): Modifier = drawBehind {
    val stroke = 1.5.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = Stroke(
            width = stroke,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
        ),
    )
}
