package com.jenarvaezg.coindex.data.numista

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response shapes for the Numista endpoints the app uses.
 *
 * Every field is optional on purpose: the catalog is filled in by volunteers and is full of
 * holes. Unknown fields are ignored (see [NumistaClient]); the untouched body is persisted
 * alongside the parsed values.
 */
@Serializable
data class OAuthTokenResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("user_id") val userId: Long? = null,
)

@Serializable
data class CollectedItemsResponse(
    @SerialName("item_count") val itemCount: Long? = null,
    val items: List<CollectedItemDto>? = null,
)

@Serializable
data class CollectedItemDto(
    val id: Long? = null,
    val quantity: Int? = null,
    @SerialName("type") val itemType: ItemTypeDto? = null,
    val issue: IssueDto? = null,
    val grade: String? = null,
    val price: PriceDto? = null,
    @SerialName("for_swap") val forSwap: Boolean? = null,
    val collection: CollectionDto? = null,
)

@Serializable
data class ItemTypeDto(
    val id: Int? = null,
    val title: String? = null,
    val issuer: IssuerDto? = null,
)

@Serializable
data class IssuerDto(val code: String? = null, val name: String? = null)

@Serializable
data class IssueDto(
    /**
     * The issue's id, which `/issues/{id}/prices` is addressed by. Parsed for the issues listed by
     * `/types/{id}/issues`; an owned piece's issue is read from its stored body instead
     * (`Mappers.issueIdFromRaw`, ADR 0028).
     */
    val id: Int? = null,
    val year: Int? = null,
    @SerialName("gregorian_year") val gregorianYear: Int? = null,
)

/**
 * Numista's estimated prices for one issue, one per grade, in the currency asked for. One call
 * answers every grade, so the cost is one call per issue however pieces are graded (ADR 0028 §1).
 * An empty list means Numista has no price: a datum to store, not a failure to retry (ADR 0028 §4).
 */
@Serializable
data class IssuePricesResponse(
    val currency: String? = null,
    val prices: List<IssuePriceDto>? = null,
)

@Serializable
data class IssuePriceDto(val grade: String? = null, val price: Double? = null)

@Serializable
data class PriceDto(val value: Double? = null, val currency: String? = null)

@Serializable
data class CollectionDto(val id: Long? = null, val name: String? = null)

@Serializable
data class NumistaTypeDto(
    val id: Int? = null,
    val url: String? = null,
    val title: String? = null,
    val issuer: IssuerDto? = null,
    @SerialName("min_year") val minYear: Int? = null,
    @SerialName("max_year") val maxYear: Int? = null,
    val weight: Double? = null,
    /** Raw Numista family. The domain never sees an editorial alias. */
    val series: String? = null,
    val obverse: CoinSideDto? = null,
    val reverse: CoinSideDto? = null,
)

@Serializable
data class CoinSideDto(
    val picture: String? = null,
    val thumbnail: String? = null,
)
