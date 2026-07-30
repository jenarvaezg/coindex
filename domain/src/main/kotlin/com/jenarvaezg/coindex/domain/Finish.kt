package com.jenarvaezg.coindex.domain

import kotlinx.serialization.Serializable

/**
 * Physical finish of a coin type. Numista exposes no stable finish field, so this is
 * inferred from the type title with auditable rules (see [inferFinish]).
 *
 * The entry names double as the on-disk representation of curated catalogs, which were
 * written by the frozen Rust implementation using these exact spellings.
 */
@Serializable
enum class Finish {
    Bullion,
    Proof,
    Coloured,
    ProofColoured,
    Gilded,
    Antiqued,
}

/** Canonical code used to persist a finish, including the unknown case. */
fun finishCode(finish: Finish?): String = when (finish) {
    null -> "unknown"
    Finish.Bullion -> "bullion"
    Finish.Proof -> "proof"
    Finish.Coloured -> "coloured"
    Finish.ProofColoured -> "proof_coloured"
    Finish.Gilded -> "gilded"
    Finish.Antiqued -> "antiqued"
}

/**
 * Parses a canonical finish code.
 *
 * Returns a nested optional because "unknown" is a valid code for the absent finish:
 * `null` means the code itself was not recognised, `Optional(null)` means unknown finish.
 */
fun finishFromCode(code: String): FinishParse? = when (code) {
    "unknown" -> FinishParse(null)
    "bullion" -> FinishParse(Finish.Bullion)
    "proof" -> FinishParse(Finish.Proof)
    "coloured" -> FinishParse(Finish.Coloured)
    "proof_coloured" -> FinishParse(Finish.ProofColoured)
    "gilded" -> FinishParse(Finish.Gilded)
    "antiqued" -> FinishParse(Finish.Antiqued)
    else -> null
}

/** Result of [finishFromCode]; distinguishes "unknown finish" from "unparseable code". */
@JvmInline
value class FinishParse(val finish: Finish?)

/** Stable ordering key, mirroring the frozen Rust implementation's grouping order. */
internal fun finishOrder(finish: Finish?): Int = when (finish) {
    null -> 0
    Finish.Bullion -> 1
    Finish.Proof -> 2
    Finish.Coloured -> 3
    Finish.ProofColoured -> 4
    Finish.Gilded -> 5
    Finish.Antiqued -> 6
}
