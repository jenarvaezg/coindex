package com.jenarvaezg.coindex.data.ficha

import com.jenarvaezg.coindex.data.photos.CoinPhoto
import java.time.LocalDate

/**
 * What one collected row says about its piece beyond the inventory's columns, for the coin's screen
 * (#643). Read from `collected_items.raw` when the screen opens, like [FichaDetails].
 */
data class PieceReading(
    val mintLetter: String? = null,
    /** `acquisition_date`. */
    val acquiredOn: LocalDate? = null,
    /** The mintage of the piece's issue. */
    val mintage: Long? = null,
    /** Numista's comment on the issue, such as «normal date; overdate variety exists». */
    val issueComment: String? = null,
    /** The collector's own note, which only this phone shows. */
    val privateComment: String? = null,
    /** The collector's photographs; one may be a certificate rather than the coin. */
    val pictures: List<CoinPhoto> = emptyList(),
)

fun readPieceBody(raw: String): PieceReading {
    val row = bodyOf(raw) ?: return PieceReading()
    val issue = row.objectAt("issue")
    return PieceReading(
        mintLetter = issue?.plainAt("mint_letter"),
        acquiredOn = row.dateAt("acquisition_date"),
        mintage = issue?.longAt("mintage")?.takeIf { it > 0 },
        issueComment = issue?.plainAt("comment"),
        privateComment = row.plainAt("private_comment"),
        pictures = row.objectsAt("pictures").mapNotNull { picture ->
            val url = picture.plainAt("url")
            val thumbnail = picture.plainAt("thumbnail_url")
            CoinPhoto(thumbnail = thumbnail, picture = url)
                .takeIf { url != null || thumbnail != null }
        },
    )
}
