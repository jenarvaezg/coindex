package com.jenarvaezg.coindex.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
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
 *
 * The thumbnail URLs arrived in version 3 and are the reason `raw` exists: every row already
 * held them, unread, so the whole cache could be filled in without a single API call.
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
    val obverseThumbnailUrl: String? = null,
    val reverseThumbnailUrl: String? = null,
)

/** The stored ficha of one type, for reading fields the columns never captured. */
data class TypeRawRow(val typeId: Int, val raw: String)

/**
 * Durable intent about one variant key. Absence means Available (ADR 0008).
 *
 * The whole key is the primary key, so widening the key rewrites this table: `metalCode` joined
 * it in version 4 (ADR 0018) and the rows that could not be given a metal were dropped rather
 * than guessed at — a stored key that no longer matches any card is dormant either way.
 */
@Entity(
    tableName = "collection_proposal_preferences",
    primaryKeys = ["family", "weightMillioz", "finishCode", "metalCode"],
)
data class DerivedCollectionPreferenceEntity(
    val family: String,
    val weightMillioz: Int,
    val finishCode: String,
    val metalCode: String,
    val disposition: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * A grouping the collector made themselves (ADR 0013): a heading and the types under it.
 *
 * It is the collector's own organization, not a claim about the catalog, so it lives only on
 * this device and never travels with the app.
 */
@Entity(tableName = "own_groupings")
data class OwnGroupingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * One type under one of those headings.
 *
 * By type rather than by collected row: row ids come from Numista and are replaced wholesale on
 * every sync, so a grouping keyed on them would quietly empty itself.
 */
@Entity(
    tableName = "own_grouping_members",
    primaryKeys = ["groupingId", "typeId"],
    foreignKeys = [
        ForeignKey(
            entity = OwnGroupingEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupingId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class OwnGroupingMemberEntity(
    val groupingId: Long,
    val typeId: Int,
)

/** One row per Numista API request actually sent. The basis of the monthly budget counter. */
@Entity(tableName = "api_call_log")
data class ApiCallEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val endpoint: String,
    val calledAt: Long,
)
