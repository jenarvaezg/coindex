package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.CollectionProposalKey

/**
 * Where one proposal stands with the collector: the three blocks of the index, and the three
 * sets of actions a proposal can offer.
 *
 * It is not a fourth disposition. Available is the absence of one (ADR 0008); this enum only
 * names what the screens already show.
 */
enum class ProposalStance { Followed, Available, Ignored }

fun CollectionState.stanceFor(key: CollectionProposalKey): ProposalStance = when {
    key in followedKeys -> ProposalStance.Followed
    proposals.ignored.any { it.key() == key } -> ProposalStance.Ignored
    else -> ProposalStance.Available
}
