package com.jenarvaezg.coindex.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Whether a catalog member is issued with a published Numista type, issued without one, or
 * named by the issuer but not yet struck (#31, #48).
 *
 * A field of the member and not a `schema_version` of its own: «announced» is a property of one
 * member and **composes** with every way of identifying one, so a date run can hold an announced
 * member exactly as a plain catalog can. A version would multiply instead.
 *
 * Every status other than `issued` costs proof here, the way closing does in [SeriesStatus]: it
 * forbids a `numista_type_id` and requires `source` plus `source_note`, because a third party's URL
 * rots and the claim has to survive it. `unlisted` means the coin was struck and sold but has no
 * publicly verifiable Numista type. A submitted type awaiting a referee is still `unlisted`:
 * unpublished ids can be deleted and therefore are never written into a curated catalog (#38).
 */
@Serializable
enum class MemberStatus {
    @SerialName("issued")
    Issued,

    @SerialName("unlisted")
    Unlisted,

    @SerialName("announced")
    Announced,
}
