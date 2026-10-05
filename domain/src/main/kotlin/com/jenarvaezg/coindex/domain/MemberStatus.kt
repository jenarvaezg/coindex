package com.jenarvaezg.coindex.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Whether a catalog member is issued with a published Numista type (`issued`), issued without one
 * (`unlisted`), or named by the issuer but not yet struck (`announced`) (#31, #48).
 *
 * A field of the member rather than a `schema_version`, so it composes with every way of
 * identifying members: a date run can hold an announced member.
 *
 * Any status other than `issued` forbids `numista_type_id` and requires `source` plus
 * `source_note`, so the claim survives the link rotting. A type submitted to Numista and awaiting a
 * referee is still `unlisted`: unpublished ids can be deleted, so they never go into a curated file
 * (#38).
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
