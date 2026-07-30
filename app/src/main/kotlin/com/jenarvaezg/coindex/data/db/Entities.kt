package com.jenarvaezg.coindex.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Snapshot of the collector's Numista collection at the last sync.
 *
 * `raw` keeps every field of the API response — re-encoded, not byte-identical — so later
 * versions can read fields this one ignores without spending API budget again.
 */
@Entity(tableName = "collected_items")
data class CollectedItemEntity(
    @PrimaryKey val id: Long,
    val typeId: Int,
    val quantity: Int,
    val title: String?,
    val issuerCode: String?,
    val issueYear: Int?,
    val gregorianYear: Int?,
    val grade: String?,
    val price: Double?,
    val forSwap: Boolean?,
    val collectionName: String?,
    val raw: String,
    val syncedAt: Long,
)

/**
 * Permanent catalog cache. A type that has been downloaded is never requested again: catalog
 * data is essentially immutable and API calls are the project's scarcest resource.
 *
 * The finish is deliberately *not* stored: it is inferred from `title` and `family` on read,
 * so improving the inference rules fixes old rows without re-fetching anything.
 */
@Entity(tableName = "type_meta")
data class TypeMetaEntity(
    @PrimaryKey val typeId: Int,
    val title: String?,
    val family: String?,
    val issuerCode: String?,
    val minYear: Int?,
    val maxYear: Int?,
    val weightGrams: Double?,
    val obverseUrl: String?,
    val reverseUrl: String?,
    val raw: String,
    val fetchedAt: Long,
)

/** Durable intent about one proposal variant key. Absence means Available (ADR 0008). */
@Entity(
    tableName = "collection_proposal_preferences",
    primaryKeys = ["family", "weightMillioz", "finishCode"],
)
data class ProposalPreferenceEntity(
    val family: String,
    val weightMillioz: Int,
    val finishCode: String,
    val disposition: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/** One row per Numista API request actually sent. The basis of the monthly budget counter. */
@Entity(tableName = "api_call_log")
data class ApiCallEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val endpoint: String,
    val calledAt: Long,
)
