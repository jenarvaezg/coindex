package com.jenarvaezg.coindex.ui

import android.net.Uri
import com.jenarvaezg.coindex.domain.VariantKey

/** Every destination the notebook has. The masthead reads these to name the current screen. */
object Routes {
    /**
     * Shared by the pattern and the builder below: if they drifted, `NavHost` would match nothing
     * and the tap would silently go nowhere.
     */
    private const val DERIVED_COLLECTION_PATH = "derived-collection"

    const val INDEX = "index"

    /**
     * The second top-level hierarchy (ADR 0021 §1). Pieces no collection claims are its
     * «Sin colección» chip.
     */
    const val COINS = "coins"

    /**
     * «Las cifras», the third top-level hierarchy (ADR 0026 §8). Its grain is grams. It has no
     * shelf: the figure touched decides where it leads.
     */
    const val FIGURES = "figures"
    /**
     * «Explorar», the annex of ADR 0026 §8, entered only from the last row of the Colecciones list.
     * Not a root: no bar, left with «Volver». It holds the shelf window and a door to [WISHES].
     */
    const val EXPLORE = "explore"

    /**
     * «Lo que busco», a sibling annex of [EXPLORE] (#520): opened from the head of Colecciones and
     * from «Explorar». A screen of its own because it has its own export, the sheet taken to a fair.
     */
    const val WISHES = "wishes"
    /**
     * «Este teléfono», opened by the sewn edge's glyph: sync, the two queues and the data export
     * (#521). Credentials and notices hang off its foot (ADR 0026 §14).
     */
    const val PHONE = "phone"

    /**
     * The two credential fields, one screen down (ADR 0026 §14). Reached from the foot of [PHONE]
     * and from the two valuation states the credentials cause, so the symptom links to the cure.
     */
    const val CREDENTIALS = "credentials"
    const val NOTICES = "notices"
    const val PLATE = "plate/{catalogId}"
    const val DERIVED_COLLECTION =
        "$DERIVED_COLLECTION_PATH?family={family}&weight={weight}&finish={finish}&metal={metal}"
    const val OWN_GROUPING = "own-grouping/{groupingId}"

    fun plate(catalogId: String): String = "plate/$catalogId"

    fun ownGrouping(groupingId: Long): String = "own-grouping/$groupingId"

    /**
     * A derived collection is addressed by the same canonical parts its disposition is stored
     * under, as query parameters rather than path segments: a Numista family is arbitrary text
     * and may contain a slash.
     */
    fun derivedCollection(key: VariantKey): String =
        "$DERIVED_COLLECTION_PATH?family=${Uri.encode(key.family)}" +
            "&weight=${key.storedWeightMillioz()}" +
            "&finish=${key.finishCode()}" +
            "&metal=${key.metalCode()}"

    fun isPlate(route: String?): Boolean = route == PLATE

    fun isDerivedCollection(route: String?): Boolean = route == DERIVED_COLLECTION

    fun isOwnGrouping(route: String?): Boolean = route == OWN_GROUPING

    /**
     * The three hierarchies of the bottom bar. Everything else is reached through one of them, and
     * a root has nothing to pop, so it offers «Este teléfono» instead of «Volver».
     */
    fun isRoot(route: String?): Boolean = route == INDEX || route == COINS || route == FIGURES

    /**
     * The roots draw the sewn edge instead of the generic masthead, which would print COINDEX and
     * the way into «Este teléfono» twice (ADR 0026 §1).
     */
    fun ownsChrome(route: String?): Boolean = isRoot(route)

    /**
     * The two routes that open `PiecesScreen` (ADR 0021 §9): one addressed by a derived variant
     * key, the other by a box id.
     */
    fun isPieces(route: String?): Boolean = isDerivedCollection(route) || isOwnGrouping(route)
}

/**
 * The route to a card's destination. Untested because it encodes through `android.net.Uri`; the
 * choice itself is [destinationOf], which is.
 */
fun routeOf(destination: CardDestination): String = when (destination) {
    is CardDestination.Plate -> Routes.plate(destination.catalogId)
    is CardDestination.Pieces -> Routes.derivedCollection(destination.key)
    is CardDestination.Box -> Routes.ownGrouping(destination.boxId)
}

/**
 * The key a derived collection route carries, or null if it does not describe one. Rebuilt
 * through [VariantKey.fromCanonicalParts], so a hand-typed or truncated route is rejected rather
 * than guessed at.
 */
fun variantKeyFromRoute(
    family: String?,
    weight: String?,
    finish: String?,
    metal: String?,
): VariantKey? {
    val weightMillioz = weight?.toIntOrNull() ?: return null
    return VariantKey.fromCanonicalParts(
        family ?: return null,
        weightMillioz,
        finish ?: return null,
        metal ?: return null,
    )
}
