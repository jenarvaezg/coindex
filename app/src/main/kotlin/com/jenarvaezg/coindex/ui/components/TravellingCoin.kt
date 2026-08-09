package com.jenarvaezg.coindex.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/**
 * The shared-element scopes of the navigation host, or null wherever there is no journey.
 *
 * Two locals and not one because they come from two different places: the layout is the whole
 * `NavHost` and lives for as long as the app does, while the visibility scope belongs to **one**
 * destination and is what tells Compose which of the two ends is arriving.
 *
 * Null by default so that anything composed outside the host — an exported sheet, a test, the
 * calibration bench — draws the same coin without a journey rather than crashing for lack of one.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransition = compositionLocalOf<SharedTransitionScope?> { null }

val LocalNavAnimation = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Flies this coin between the index card of a collection and **its** casilla on the plate.
 *
 * The fourth movement of ADR 0026 §3, and the one that says «es la misma moneda»: the photograph
 * takes off from the die-cut hole of the card and lands in the hole of the slot, with the grid
 * arriving behind it.
 *
 * [catalogId] is the whole key, and it is null wherever the coin must not fly:
 *
 * - **In `Pieces` and in `Box`** — 20 of the father's 69 cards — because on the other side there is
 *   no casilla of its own but inventory rows where `CoinSides` paints both faces at 150 dp. Which
 *   cards fly is visible before touching them: the ones that do not carry no ratio (ADR 0021 §3).
 * - **On any casilla but the landing one**, which the plate decides by the same rule the card's
 *   photograph was chosen by, so the coin that took off is the coin that lands.
 *
 * The catalog is enough of a key on its own: both ends resolve «the first owned member in album
 * order» from the same album, so a key that also carried the type would be two chances to disagree
 * about one coin.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.travellingCoin(catalogId: String?): Modifier {
    if (catalogId == null) return this
    val layout = LocalSharedTransition.current ?: return this
    val destination = LocalNavAnimation.current ?: return this
    return with(layout) {
        this@travellingCoin.sharedElement(
            sharedContentState = rememberSharedContentState(key = "coin-$catalogId"),
            animatedVisibilityScope = destination,
        )
    }
}
