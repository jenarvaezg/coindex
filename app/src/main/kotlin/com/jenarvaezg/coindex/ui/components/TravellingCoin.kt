package com.jenarvaezg.coindex.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/**
 * The shared-element scopes of the navigation host, or null wherever there is no journey. Two
 * locals because the layout scope spans the whole `NavHost` while the visibility scope belongs to
 * one destination. Null by default, so anything composed outside the host (an exported sheet, a
 * test, the bench) draws the coin without flying it.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransition = compositionLocalOf<SharedTransitionScope?> { null }

val LocalNavAnimation = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Shared-content key for the index → casilla journey (ADR 0026 §3). */
fun travellingCatalogKey(catalogId: String): String = "coin-$catalogId"

/** Shared-content key for the Monedas → ficha journey (ADR 0026 §3, #370). */
fun travellingTypeKey(typeId: Int): String = "type-$typeId"

/**
 * Flies this coin between a collection's index card and its casilla on the plate (ADR 0026 §3). The
 * key is [catalogId] alone, since both ends resolve the first owned member in album order. It is
 * null where the coin must not fly: on `Pieces` and `Box` cards, which have no casilla (they carry
 * no ratio, ADR 0021 §3), and on every casilla but the landing one. Nor does it fly with
 * [LocalMotion] off.
 *
 * The overlay is clipped to a circle, or the element's rectangular bounds flash a square on
 * landing.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.travellingCoin(catalogId: String?): Modifier {
    if (catalogId == null) return this
    if (!LocalMotion.current) return this
    val layout = LocalSharedTransition.current ?: return this
    val destination = LocalNavAnimation.current ?: return this
    return with(layout) {
        this@travellingCoin.sharedElement(
            sharedContentState = rememberSharedContentState(key = travellingCatalogKey(catalogId)),
            animatedVisibilityScope = destination,
            clipInOverlayDuringTransition = OverlayClip(CircleShape),
        )
    }
}

/**
 * Flies this coin between its hole in Monedas and the top of its ficha sheet (#370). The ficha is a
 * sheet, not a destination, and `ModalBottomSheet` (a dialog window) can't host a shared element,
 * so visibility is caller-managed: [visible] is true on the end that currently owns the photograph.
 * With [LocalMotion] false neither end yields and both draw their photograph (#514).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.travellingTypeCoin(typeId: Int, visible: Boolean): Modifier {
    if (!LocalMotion.current) return this
    val layout = LocalSharedTransition.current ?: return this
    return with(layout) {
        this@travellingTypeCoin.sharedElementWithCallerManagedVisibility(
            sharedContentState = rememberSharedContentState(key = travellingTypeKey(typeId)),
            visible = visible,
            clipInOverlayDuringTransition = OverlayClip(CircleShape),
        )
    }
}
