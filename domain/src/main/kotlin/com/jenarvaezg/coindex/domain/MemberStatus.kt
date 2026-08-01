package com.jenarvaezg.coindex.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Whether a catalog member is a coin that was struck or one the issuer has named and not yet
 * issued (#31).
 *
 * A field of the member and not a `schema_version` of its own: «announced» is a property of one
 * member and **composes** with every way of identifying one, so a date run can hold an announced
 * member exactly as a plain catalog can. A version would multiply instead.
 *
 * Announced is what costs proof here, the way closing does in [SeriesStatus]: it forbids a
 * `numista_type_id` — Numista catalogues struck coins — and requires an `announced_source` with
 * a note in prose, because a third party's URL rots and the claim has to survive it.
 */
@Serializable
enum class MemberStatus {
    @SerialName("issued")
    Issued,

    @SerialName("announced")
    Announced,
}
