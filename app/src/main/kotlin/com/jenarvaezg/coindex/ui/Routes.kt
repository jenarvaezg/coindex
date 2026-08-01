package com.jenarvaezg.coindex.ui

import android.net.Uri
import com.jenarvaezg.coindex.domain.CollectionProposalKey

/** Every destination the notebook has. The masthead reads these to name the current screen. */
object Routes {
    const val INDEX = "index"
    const val UNCLASSIFIED = "unclassified"
    const val SETTINGS = "settings"
    const val PLATE = "plate/{catalogId}"
    const val PROPOSAL =
        "proposal?family={family}&weight={weight}&finish={finish}&metal={metal}"
    const val OWN_GROUPING = "own-grouping/{groupingId}"

    fun plate(catalogId: String): String = "plate/$catalogId"

    fun ownGrouping(groupingId: Long): String = "own-grouping/$groupingId"

    /**
     * A proposal is addressed by the same canonical parts that its disposition is stored under,
     * as query parameters rather than path segments: a Numista family is arbitrary text and may
     * contain a slash.
     */
    fun proposal(key: CollectionProposalKey): String =
        "proposal?family=${Uri.encode(key.family)}" +
            "&weight=${key.storedWeightMillioz()}" +
            "&finish=${key.finishCode()}" +
            "&metal=${key.metalCode()}"

    fun isPlate(route: String?): Boolean = route == PLATE

    fun isProposal(route: String?): Boolean = route == PROPOSAL

    fun isOwnGrouping(route: String?): Boolean = route == OWN_GROUPING
}

/**
 * The key a proposal route carries, or null if it does not describe one.
 *
 * Rebuilt through [CollectionProposalKey.fromCanonicalParts], so a hand-typed or truncated route
 * is rejected rather than guessed at, exactly as a stored disposition would be.
 */
fun proposalKeyFromRoute(
    family: String?,
    weight: String?,
    finish: String?,
    metal: String?,
): CollectionProposalKey? {
    val weightMillioz = weight?.toIntOrNull() ?: return null
    return CollectionProposalKey.fromCanonicalParts(
        family ?: return null,
        weightMillioz,
        finish ?: return null,
        metal ?: return null,
    )
}
