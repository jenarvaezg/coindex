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
 * The one coin sheet a screen can open, as it is handed it (#508).
 *
 * A value rather than five parameters, for the reason [FichaRefresh] is one: it travels through three
 * screens to reach the three surfaces that draw casillas, and handed apart they could disagree — a
 * lámina wording a coin one way and Monedas another, or one of them leaving to a different address
 * for the same ficha. What varies per surface is **not** in here and stays a parameter of the overlay:
 * which face rests up, which collection the collector is already standing in, and whether the coin
 * flies. Those are three facts about the surface, not about the coin.
 *
 * @param coin the row of a type, holding whatever the collector has of it — nothing, on a hole. Null
 *   only where the collection changed under an open sheet, and then there is nothing left to draw.
 * @param value what the coin is worth, which on a hole is null by construction: nothing owned,
 *   nothing worth anything.
 * @param onOpenNumista the sheet's «Ver en Numista» under its drawn arrow — the one label of any of
 *   these screens that reaches a browser.
 * @param onOpenClaim where the collections that claim this coin are.
 */
class CoinSheetSurface(
    val coin: (typeId: Int) -> CoinRow?,
    val ficha: (typeId: Int) -> FichaRefresh,
    val value: (typeId: Int) -> CoinValue?,
    val onOpenNumista: (typeId: Int) -> Unit,
    val onOpenClaim: (CardDestination) -> Unit,
)

/**
 * The sheet of one coin, as any surface of the app can open it (#508).
 *
 * It was Monedas' own until the audit of 14 August 2026: a casilla of a lámina had two invisible
 * targets, and the year's sunken tag **left the app** — no arrow on it, Numista behind Cloudflare,
 * and three unintended trips to Chrome in one session. The tag now opens this, which is the same
 * sheet with the same «Ver en Numista» under its drawn arrow and the same «Actualizar la ficha ·
 * 1 consulta»: leaving is still one tap away, and it is behind a label that says so (ADR 0026 §3,
 * amended).
 *
 * Placed **last inside a Box that fills the screen**: it is an overlay and not a destination, because
 * `ModalBottomSheet` is a dialog window and cannot host a shared element (#370).
 *
 * @param typeId the coin that is open, or null when none is. The one input that says what to draw.
 * @param faces which photograph rests up and which waits behind it. A parameter and not a rule of its
 *   own: a casilla obeys its catalog's `printed_side` (ADR 0020) and an album cell obeys
 *   reverse-first, and the sheet must open on the face the surface behind it was showing.
 * @param here the collection the collector is already standing in, whose claim is therefore not drawn
 *   as a door: a link from the plate of «1 Bolívar» back to the plate of «1 Bolívar» is a door onto
 *   the sheet you are reading.
 * @param travelling whether the coin flies between this sheet and the surface behind it (ADR 0026 §3).
 *   True in Monedas, where the cell yields its photograph on opening. False on a casilla: the hole of a
 *   plate is already one end of the index's journey, and one photograph cannot be two shared elements.
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
    // Kept across the dismiss so `AnimatedVisibility` still has a coin to draw while the sheet exits,
    // and saved with it: restored after a process death the sheet has to draw on its first frame.
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
        // Read once per coin and not once per recomposition: building the row walks the inventory and
        // the index, and the sheet recomposes for the whole of its own entrance.
        val row = remember(open, surface) { surface.coin(open) } ?: return@AnimatedVisibility
        val (photo, otherSide) = faces(open)
        CoinSheet(
            row = row,
            photo = photo,
            otherSide = otherSide,
            travelling = travelling,
            // The sheet yields on dismiss the same way the cell yielded on open (#370): `exiting`
            // keeps the hole composed through the exit, but ownership follows the open coin, or both
            // ends claim the photograph and the return pops.
            ownsCoin = typeId == open,
            ficha = surface.ficha(open),
            value = surface.value(open),
            doors = row.claims.filterNot { it.destination == here },
            onDismiss = onDismiss,
            // Both ways out close the sheet first: coming back from Numista or from a collection onto
            // an overlay nobody asked to still be there is the sheet outliving the gesture.
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
 * The sheet coming up from the foot of the screen, and none at all where the system asked for quiet.
 *
 * [LocalMotion] and not a duration of its own: at zero the app does not animate faster, it does not
 * animate (#514). Named so the pair can be read — and defended — without an emulator between them.
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
 * Exact identity and upkeep live inside the coin instead of being repeated under every hole.
 *
 * The die-cut at the top is the landing of ADR 0026 §3's second journey (#370): same 104 dp hole as
 * the cell it left, cardboard only when a collection claims the type — the form «En ninguna
 * colección» already used in the grid — and the **ghost of the design** where the collector owns no
 * piece at all, which is the casilla of a lámina that opened it (#508).
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
            // A coin no piece of which is in the collection is drawn as what it is: a hole with the
            // catalog design behind it, exactly as the casilla the sheet was opened from.
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
        // The value with its origin said, because a number with no provenance in an app with two
        // users is a number nobody can check (#316).
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
