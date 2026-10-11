package com.jenarvaezg.coindex.ui.ficha

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.ui.COIN_VIEW_ON_NUMISTA
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.components.AlbumHole
import com.jenarvaezg.coindex.ui.components.BackGlyph
import com.jenarvaezg.coindex.ui.components.ExternalLink
import com.jenarvaezg.coindex.ui.components.FichaBrought
import com.jenarvaezg.coindex.ui.components.FichaRefresh
import com.jenarvaezg.coindex.ui.components.HoleAbsence
import com.jenarvaezg.coindex.ui.components.dashedBorder
import com.jenarvaezg.coindex.ui.components.LocalMotion
import com.jenarvaezg.coindex.ui.components.travellingTypeCoin
import com.jenarvaezg.coindex.ui.printedPhoto
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * The coin's screen (#643, ADR 0026 §13): back, the coin at 300 dp, the title and the value, the
 * folded sections and the foot. Once the title scrolls off, a compact bar
 * keeps the coin and its value in view.
 *
 * @param side the face up on arrival; a missing photograph gives way to the other face.
 * @param back where the screen was opened from, the name its way back prints.
 */
@Composable
fun CoinFichaScreen(
    subject: CoinFichaSubject,
    images: Map<Int, TypeImages>,
    side: PrintedSide,
    back: String,
    ficha: FichaRefresh,
    onBack: () -> Unit,
    onOpenNumista: () -> Unit,
    onOpenType: (Int) -> Unit,
    onOpenClaim: (CardDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    // Where the title ends, in the scrolled column: the head's top plus the title's bottom in it.
    var headTop by remember { mutableIntStateOf(Int.MAX_VALUE / 2) }
    var titleBottom by remember { mutableIntStateOf(0) }
    var lightbox by remember { mutableStateOf<Lightbox?>(null) }
    val faces = remember(images[subject.typeId], side) { heroFaces(images[subject.typeId], side) }

    BackHandler(enabled = lightbox != null) { lightbox = null }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
            BackRow(back, onBack)
            Hero(subject, faces)
            Head(
                subject = subject,
                onTitlePlaced = { titleBottom = it },
                modifier = Modifier.onGloballyPositioned { head ->
                    headTop = head.positionInParent().y.toInt()
                },
            )
            Folds(
                subject = subject,
                images = images,
                onOpenType = onOpenType,
                onOpenClaim = onOpenClaim,
                onPhoto = { photo, caption -> lightbox = Lightbox(photo, caption) },
            )
            Foot(ficha, onOpenNumista)
        }
        CompactBar(
            visible = scroll.value > headTop + titleBottom,
            subject = subject,
            photo = faces.up,
            onBack = onBack,
        )
        lightbox?.let { shown -> LightboxView(shown, onClose = { lightbox = null }) }
    }
}

/** The faces of the hero hole, which side is up, and whether it can turn. */
internal data class HeroFaces(val upSide: PrintedSide, val up: CoinPhoto?, val other: CoinPhoto?)

/**
 * [side] up, unless it has no photograph and the other face does: a casilla keeps its face for the
 * ghost (#227), but a 300 dp hole showing an empty disc would hide the coin.
 */
internal fun heroFaces(images: TypeImages?, side: PrintedSide): HeroFaces {
    val up = images?.printedPhoto(side)
    val other = images?.printedPhoto(side.other)
    return if (up?.hasPicture != true && other?.hasPicture == true) {
        HeroFaces(side.other, other, null)
    } else {
        HeroFaces(side, up, other?.takeIf { it.hasPicture })
    }
}

@Composable
private fun BackRow(label: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onBack)
            .padding(horizontal = 20.dp),
    ) {
        BackGlyph()
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Hero(subject: CoinFichaSubject, faces: HeroFaces) {
    var turned by remember(subject.typeId, faces) { mutableStateOf(false) }
    val upSide = if (turned) faces.upSide.other else faces.upSide
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
    ) {
        AlbumHole(
            photo = faces.up,
            // A hole is drawn whole here: the screen is where the coin is looked at.
            absence = if (subject.owned) HoleAbsence.Filled else HoleAbsence.Wanted,
            backed = subject.backed,
            otherSide = faces.other,
            onTurned = { turned = it },
            sharp = true,
            modifier = Modifier.size(HERO).travellingTypeCoin(subject.typeId),
        )
        Text(
            FichaPageLabels.faceUp(upSide, turns = faces.other != null),
            style = MaterialTheme.typography.labelSmall,
            color = Paper.muted,
        )
    }
}

/** [onTitlePlaced] is told where the title ends inside the head, for the compact bar. */
@Composable
private fun Head(
    subject: CoinFichaSubject,
    onTitlePlaced: (bottom: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp),
    ) {
        Text(
            subject.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.onGloballyPositioned { title ->
                onTitlePlaced((title.positionInParent().y + title.size.height).toInt())
            },
        )
        Text(
            subject.identity,
            style = MaterialTheme.typography.labelLarge,
            color = Paper.muted,
            textAlign = TextAlign.Center,
        )
        if (subject.missing) MissingTag()
        listOfNotNull(subject.value, subject.cost).forEach { line ->
            Text(
                line,
                style = MaterialTheme.typography.labelLarge,
                color = Paper.rust,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** «Te falta», stamped like the hole it names: dashed, in rust. */
@Composable
private fun MissingTag() {
    Text(
        FichaPageLabels.MISSING,
        style = MaterialTheme.typography.labelMedium,
        color = Paper.rust,
        modifier = Modifier
            .padding(top = 4.dp)
            .dashedBorder(Paper.rust, 1.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun Foot(ficha: FichaRefresh, onOpenNumista: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 30.dp, bottom = 32.dp),
    ) {
        HorizontalDivider(color = Paper.paperDeep)
        FichaBrought(ficha, modifier = Modifier.padding(top = 14.dp))
        ExternalLink(
            text = COIN_VIEW_ON_NUMISTA,
            onClick = onOpenNumista,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/**
 * Back, the coin at 40 dp, the title and the value, once the head has scrolled off (#643). It
 * slides in from the top, and simply appears with the system's animations off (#514).
 */
@Composable
private fun CompactBar(
    visible: Boolean,
    subject: CoinFichaSubject,
    photo: CoinPhoto?,
    onBack: () -> Unit,
) {
    val moving = LocalMotion.current
    AnimatedVisibility(
        visible = visible,
        enter = compactBarEnter(moving),
        exit = compactBarExit(moving),
    ) {
        Column(modifier = Modifier.fillMaxWidth().background(Paper.paper)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onBack)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                BackGlyph()
                AlbumHole(
                    photo = photo,
                    absence = if (subject.owned) HoleAbsence.Filled else HoleAbsence.Wanted,
                    backed = subject.backed,
                    modifier = Modifier.size(40.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        subject.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    (subject.value ?: subject.cost)?.let { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.labelMedium,
                            color = Paper.rust,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            HorizontalDivider(color = Paper.line)
        }
    }
}

/** No transition at all with the system's animations off, not one of zero length (#514). */
internal fun compactBarEnter(moving: Boolean): EnterTransition =
    if (moving) slideInVertically { -it } + fadeIn() else EnterTransition.None

internal fun compactBarExit(moving: Boolean): ExitTransition =
    if (moving) slideOutVertically { -it } + fadeOut() else ExitTransition.None

/** A photograph to look at whole: the edge's from Numista, or the collector's own. */
private data class Lightbox(val photo: CoinPhoto, val caption: String)

@Composable
private fun LightboxView(shown: Lightbox, onClose: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(LIGHTBOX_SCRIM)
            .clickable(role = Role.Button, onClick = onClose)
            .padding(20.dp),
    ) {
        AsyncImage(
            model = shown.photo.picture ?: shown.photo.thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
        )
        Spacer(Modifier.height(12.dp))
        Text(shown.caption, style = MaterialTheme.typography.labelMedium, color = Paper.paper)
    }
}

/** Behind a photograph opened whole. */
private val LIGHTBOX_SCRIM = Color(0xE0141512)

/** The hero hole: big enough to read a legend, and the title still in the first fold. */
internal val HERO = 300.dp
