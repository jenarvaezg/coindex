package com.jenarvaezg.coindex.ui

import android.net.Uri
import com.jenarvaezg.coindex.domain.VariantKey

/** Every destination the notebook has. The masthead reads these to name the current screen. */
object Routes {
    /**
     * Shared by the pattern and the builder below, which are matched against each other by
     * `NavHost` and would fail silently — a tap that navigates nowhere — if they ever drifted.
     */
    private const val DERIVED_COLLECTION_PATH = "derived-collection"

    const val INDEX = "index"

    /**
     * The other hierarchy of the top level (ADR 0021 §1), not a view inside a collection.
     *
     * It replaces `unclassified`, which was a screen for the pieces no collection claimed: those
     * pieces are now the «Sin colección» chip of this one, reached from where they already live.
     */
    const val COINS = "coins"
    const val SETTINGS = "settings"
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
     * The two destinations of the bottom bar, which are the two hierarchies and nothing else.
     *
     * Everything else in the app is reached *through* one of them, so this is also the answer to
     * «does the masthead offer Ajustes or «Volver»?»: a root has nothing underneath to pop.
     */
    fun isRoot(route: String?): Boolean = route == INDEX || route == COINS

    /**
     * The two routes that open `PiecesScreen`.
     *
     * They stay two because they address different subjects — a variant key the inventory derives,
     * a box id the collector's own table holds — but they arrive at one screen (ADR 0021 §9), and
     * everything that speaks about the destination rather than the subject asks this.
     */
    fun isPieces(route: String?): Boolean = isDerivedCollection(route) || isOwnGrouping(route)
}

/**
 * The route that reaches a card's one destination.
 *
 * Untested for the same reason `Routes.derivedCollection` is: it encodes through `android.net.Uri`.
 * What is worth testing is the choice, and that is [destinationOf], which knows nothing about
 * routes.
 */
fun routeOf(destination: CardDestination): String = when (destination) {
    is CardDestination.Plate -> Routes.plate(destination.catalogId)
    is CardDestination.Pieces -> Routes.derivedCollection(destination.key)
    is CardDestination.Box -> Routes.ownGrouping(destination.boxId)
}

/**
 * The key a derived collection route carries, or null if it does not describe one.
 *
 * Rebuilt through [VariantKey.fromCanonicalParts], so a hand-typed or truncated route is
 * rejected rather than guessed at, exactly as a stored disposition would be.
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
