package com.jenarvaezg.coindex.data.ficha

import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.domain.recordedText
import kotlinx.serialization.json.JsonObject

/**
 * What the coin's screen prints from a stored ficha beyond the cache's columns (#643). Read from
 * the body when the screen opens rather than stored as columns: one screen reads one ficha, so a
 * migration and a backfill over the whole cache would buy nothing.
 */
data class FichaDetails(
    /** `value.text`, as Numista writes it: «2 euros», «8 Reales». */
    val faceValue: String? = null,
    /** `value.currency.full_name`, with its period: «Bolívar (1879-2007)». */
    val currency: String? = null,
    val rulers: List<String> = emptyList(),
    /** `composition.text`, which the cache stores for the metal but no screen printed. */
    val composition: String? = null,
    val series: String? = null,
    /** `commemorated_topic`. */
    val topic: String? = null,
    val orientation: CoinOrientation? = null,
    val technique: String? = null,
    /** `demonetization.demonetization_date`, ISO, only when Numista gives one. */
    val demonetizedOn: String? = null,
    val references: List<FichaReference> = emptyList(),
    val obverse: FichaFace? = null,
    val reverse: FichaFace? = null,
    val edge: FichaEdge? = null,
    /** Numista's own HTML, for the screen to render with its links. */
    val comments: String? = null,
    val related: List<RelatedType> = emptyList(),
)

/** How the two faces sit when the coin turns on its vertical axis. */
enum class CoinOrientation { Medal, Coin, Variable }

data class FichaReference(val catalogue: String, val number: String)

data class FichaFace(
    val description: String? = null,
    /** The legend as struck, one line per line of Numista's field. */
    val lettering: String? = null,
    /** The legend with its abbreviations written out. */
    val unabridgedLegend: String? = null,
)

data class FichaEdge(
    val description: String? = null,
    val lettering: String? = null,
    val unabridgedLegend: String? = null,
    /** Numista's photograph of the edge, shown apart because each one is shot differently. */
    val photo: CoinPhoto? = null,
)

data class RelatedType(
    val typeId: Int,
    val title: String,
    val country: String?,
    val minYear: Int? = null,
    val maxYear: Int? = null,
)

/**
 * Reads one stored Numista body. Like [readFichaBody], each field is read on its own and a body
 * that isn't JSON gives empty details rather than throwing.
 */
fun readFichaDetails(raw: String): FichaDetails {
    val body = bodyOf(raw) ?: return FichaDetails()
    return FichaDetails(
        faceValue = body.plainAt("value", "text"),
        currency = body.objectAt("value")?.objectAt("currency")?.plainAt("full_name"),
        rulers = body.objectsAt("ruler").mapNotNull { it.plainAt("name") },
        composition = body.plainAt("composition", "text"),
        series = body.plainAt("series"),
        topic = body.plainAt("commemorated_topic"),
        orientation = when (body.plainAt("orientation")) {
            "medal" -> CoinOrientation.Medal
            "coin" -> CoinOrientation.Coin
            "variable" -> CoinOrientation.Variable
            else -> null
        },
        technique = body.plainAt("technique", "text"),
        demonetizedOn = body.plainAt("demonetization", "demonetization_date"),
        references = body.objectsAt("references").mapNotNull { reference ->
            val catalogue = reference.objectAt("catalogue")?.plainAt("code")
                ?: return@mapNotNull null
            val number = reference.plainAt("number") ?: return@mapNotNull null
            FichaReference(catalogue, number)
        },
        obverse = body.objectAt("obverse")?.face(),
        reverse = body.objectAt("reverse")?.face(),
        edge = body.objectAt("edge")?.edge(),
        comments = recordedText(body.stringAt("comments")?.let(::untranslated)),
        related = body.objectsAt("related_types").mapNotNull { related ->
            RelatedType(
                typeId = related.intAt("id") ?: return@mapNotNull null,
                title = related.plainAt("title") ?: return@mapNotNull null,
                country = related.objectAt("country")?.plainAt("name"),
                minYear = related.intAt("minYear"),
                maxYear = related.intAt("maxYear"),
            )
        },
    )
}

private fun JsonObject.face(): FichaFace? = FichaFace(
    description = plainAt("description"),
    lettering = plainAt("lettering"),
    unabridgedLegend = plainAt("unabridged_legend"),
).takeIf { it != FichaFace() }

private fun JsonObject.edge(): FichaEdge? {
    val picture = plainAt("picture")
    val thumbnail = plainAt("thumbnail")
    return FichaEdge(
        description = plainAt("description"),
        lettering = plainAt("lettering"),
        unabridgedLegend = plainAt("unabridged_legend"),
        photo = CoinPhoto(thumbnail = thumbnail, picture = picture)
            .takeIf { picture != null || thumbnail != null },
    ).takeIf { it != FichaEdge() }
}
