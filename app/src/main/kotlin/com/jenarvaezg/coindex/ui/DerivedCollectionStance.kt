package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.VariantKey

/**
 * Where one collection stands with the collector: the three blocks of the index, and the
 * three sets of actions a card can offer.
 *
 * It is not a fourth disposition. Available is the absence of one (ADR 0008); this enum only
 * names what the screens already show.
 */
enum class DerivedCollectionStance { Followed, Available, Ignored }

fun CollectionState.stanceFor(key: VariantKey): DerivedCollectionStance = when {
    key in followedKeys -> DerivedCollectionStance.Followed
    derivedCollections.ignored.any { it.key() == key } -> DerivedCollectionStance.Ignored
    else -> DerivedCollectionStance.Available
}
