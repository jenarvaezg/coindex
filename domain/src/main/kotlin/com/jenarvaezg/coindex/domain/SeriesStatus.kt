package com.jenarvaezg.coindex.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Whether the series a catalog covers is still being issued (#28).
 *
 * Declared, never deduced: the last member year cannot tell a series that ended from a curation
 * that fell behind. Closing costs proof, since almost no series is ever declared over officially:
 * a closed catalog must carry a `closed_note`, while an open one claims only «N of N catalogued».
 */
@Serializable
enum class SeriesStatus {
    @SerialName("open")
    Open,

    @SerialName("closed")
    Closed,
}
