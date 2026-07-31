package com.jenarvaezg.coindex.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Whether the series a catalog covers is still being issued (#28).
 *
 * Declared, never deduced: the last year of the members cannot tell a series that ended from a
 * curation that fell behind, and a threshold over it would turn that silence into a claim.
 *
 * Closing is what costs proof. Almost no series is ever declared over officially, so a closed
 * catalog must carry a `closed_note` saying what sustains the closure, while an open one claims
 * only «N of N catalogued» and promises nothing about being up to date.
 */
@Serializable
enum class SeriesStatus {
    @SerialName("open")
    Open,

    @SerialName("closed")
    Closed,
}
