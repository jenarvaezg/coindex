package com.jenarvaezg.coindex.ui.screens

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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.ui.COIN_IN_ONE_COLLECTION
import com.jenarvaezg.coindex.ui.COIN_IN_SEVERAL_COLLECTIONS
import com.jenarvaezg.coindex.ui.COIN_VIEW_ON_NUMISTA
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.CoinValue
import com.jenarvaezg.coindex.ui.coinFichaIdentity
import com.jenarvaezg.coindex.ui.coinValueLabel
import com.jenarvaezg.coindex.ui.components.AlbumHole
import com.jenarvaezg.coindex.ui.components.HoleAbsence
import com.jenarvaezg.coindex.ui.components.ExternalLink
import com.jenarvaezg.coindex.ui.components.FichaBrought
import com.jenarvaezg.coindex.ui.components.FichaRefresh
import com.jenarvaezg.coindex.ui.components.LinkText
import com.jenarvaezg.coindex.ui.components.LocalMotion
import com.jenarvaezg.coindex.ui.components.travellingTypeCoin
import com.jenarvaezg.coindex.ui.shelf.CoinClaim
import com.jenarvaezg.coindex.ui.shelf.CoinRow
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * What a screen needs to open a coin sheet (#508), bundled so the three surfaces that draw casillas
 * word and link a coin the same way. What varies per surface (face up, current collection, flight)
 * stays a parameter of [CoinSheetOverlay].
 *
 * @param coin the type's row; null only when the collection changed under an open sheet.
 * @param value null on a hole, where nothing is owned.
 */
class CoinSheetSurface(
    val coin: (typeId: Int) -> CoinRow?,
    val ficha: (typeId: Int) -> FichaRefresh,
    val value: (typeId: Int) -> CoinValue?,
    val onOpenNumista: (typeId: Int) -> Unit,
    val onOpenClaim: (CardDestination) -> Unit,
)

/**
 * The sheet of one coin, openable from any surface (#508). A casilla's year tag opens it instead of
 * leaving for Numista directly; leaving is the sheet's labelled «Ver en Numista» (ADR 0026 §3).
 *
 * Place it last inside a Box that fills the screen. It is an overlay rather than a
 * `ModalBottomSheet` because a dialog window cannot host a shared element (#370).
 *
 * @param typeId the open coin, or null when none is.
 * @param faces the photograph that rests up and the one behind it, matching what the surface was
 *   showing: a casilla follows its catalog's `printed_side` (ADR 0020), an album cell
 *   reverse-first.
 * @param here the collection the collector is in, whose claim is not drawn as a link.
 * @param travelling whether the coin flies between the sheet and the surface (ADR 0026 §3). False
 *   on a casilla: its hole is already one end of the index's transition, and one photograph cannot
 *   be two shared elements.
 */
@Composable
fun CoinSheetOverlay(
    typeId: Int?,
    surface: CoinSheetSurface,
    faces: (Int) -> Pair<CoinPhoto?, CoinPhoto?>,
    onDismiss: () -> Unit,
    here: CardDestination? = null,
    travelling: Boolean = false,
) {
    // Kept across the dismiss so the exit animation still has a coin to draw, and saveable so a
    // sheet restored after process death draws on its first frame.
    var exiting by rememberSaveable { mutableStateOf<Int?>(null) }
    SideEffect {
        if (typeId != null) exiting = typeId
    }

    BackHandler(enabled = typeId != null, onBack = onDismiss)

    val moving = LocalMotion.current
    AnimatedVisibility(
        visible = typeId != null,
        enter = sheetEnter(moving),
        exit = sheetExit(moving),
    ) {
        val open = exiting ?: return@AnimatedVisibility
        // Once per coin: building the row walks the inventory and the index, and the sheet
        // recomposes on every frame of its entrance.
        val row = remember(open, surface) { surface.coin(open) } ?: return@AnimatedVisibility
        val (photo, otherSide) = faces(open)
        CoinSheet(
            row = row,
            photo = photo,
            otherSide = otherSide,
            travelling = travelling,
            // Ownership follows the open coin, not `exiting`: otherwise both ends claim the
            // photograph during the exit and the return transition pops (#370).
            ownsCoin = typeId == open,
            ficha = surface.ficha(open),
            value = surface.value(open),
            doors = row.claims.filterNot { it.destination == here },
            onDismiss = onDismiss,
            // Both ways out close the sheet first, so coming back doesn't land on it still open.
            onOpenNumista = {
                onDismiss()
                surface.onOpenNumista(open)
            },
            onOpenClaim = { destination ->
                onDismiss()
                surface.onOpenClaim(destination)
            },
        )
    }
}

/**
 * The sheet slides up from the bottom, or appears without animation when [LocalMotion] is off
 * (#514). Extracted so tests can check the pair without an emulator.
 */
internal fun sheetEnter(moving: Boolean): EnterTransition =
    if (moving) fadeIn() + slideInVertically { it } else EnterTransition.None

internal fun sheetExit(moving: Boolean): ExitTransition =
    if (moving) fadeOut() + slideOutVertically { it } else ExitTransition.None

@Composable
private fun CoinSheet(
    row: CoinRow,
    photo: CoinPhoto?,
    otherSide: CoinPhoto?,
    travelling: Boolean,
    ownsCoin: Boolean,
    ficha: FichaRefresh,
    value: CoinValue?,
    doors: List<CoinClaim>,
    onDismiss: () -> Unit,
    onOpenNumista: () -> Unit,
    onOpenClaim: (CardDestination) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(
                    role = Role.Button,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss,
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Paper.paper, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .navigationBarsPadding()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
        ) {
            CoinFicha(
                row = row,
                photo = photo,
                otherSide = otherSide,
                travelling = travelling,
                ownsCoin = ownsCoin,
                ficha = ficha,
                value = value,
                doors = doors,
                onOpenNumista = onOpenNumista,
                onOpenClaim = onOpenClaim,
            )
        }
    }
}

/**
 * The sheet's content: exact identity, value, ficha upkeep and the collections that claim the coin.
 *
 * The 104 dp hole at the top is the landing of ADR 0026 §3's second transition (#370), drawn like
 * the cell it left: cardboard only when a collection claims the type, and the design's ghost when
 * no piece is owned (#508).
 */
@Composable
private fun CoinFicha(
    row: CoinRow,
    photo: CoinPhoto?,
    otherSide: CoinPhoto?,
    travelling: Boolean,
    ownsCoin: Boolean,
    ficha: FichaRefresh,
    value: CoinValue?,
    doors: List<CoinClaim>,
    onOpenNumista: () -> Unit,
    onOpenClaim: (CardDestination) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
    ) {
        val hole = Modifier.padding(top = 20.dp, bottom = 12.dp).size(104.dp)
        AlbumHole(
            photo = photo,
            absence = if (row.quantity == 0) HoleAbsence.Missing else HoleAbsence.Filled,
            backed = row.claims.isNotEmpty(),
            otherSide = otherSide,
            modifier = if (travelling) {
                hole.travellingTypeCoin(row.typeId, visible = ownsCoin)
            } else {
                hole
            },
        )
        Text(
            row.rawTitle,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            coinFichaIdentity(row),
            style = MaterialTheme.typography.labelLarge,
            color = Paper.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        // The value always says where it comes from (#316).
        value?.let { reading ->
            Text(
                coinValueLabel(reading),
                style = MaterialTheme.typography.labelLarge,
                color = Paper.rust,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp).fillMaxWidth(),
            )
        }
        FichaBrought(ficha, modifier = Modifier.padding(top = 8.dp).fillMaxWidth())
        ExternalLink(
            text = COIN_VIEW_ON_NUMISTA,
            onClick = onOpenNumista,
            modifier = Modifier.padding(top = 2.dp).fillMaxWidth(),
        )
        if (doors.isNotEmpty()) {
            Text(
                if (doors.size == 1) COIN_IN_ONE_COLLECTION else COIN_IN_SEVERAL_COLLECTIONS,
                style = MaterialTheme.typography.labelLarge,
                color = Paper.muted,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
            )
            doors.forEach { claim ->
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
}
