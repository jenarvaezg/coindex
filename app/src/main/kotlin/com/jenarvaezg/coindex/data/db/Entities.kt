package com.jenarvaezg.coindex.data.db

import androidx.room.ColumnInfo
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
 * Permanent catalog cache. No sync asks for a type twice: catalog data barely changes and API calls
 * are the scarcest resource. A row is overwritten only by the collector asking for the ficha again
 * (#185, ADR 0025) or by the seed of a newer APK (#606, ADR 0033), so `fetchedAt` is when this phone
 * got the ficha, which the card prints («ficha traída hace…»).
 *
 * Columns store what Numista wrote, never what the app makes of it: the finish is inferred from
 * `title` and `family`, the metal from [composition] and the class from [category] on read, so a
 * better rule fixes old rows without fetching anything. Columns added later (thumbnails in v3, five
 * more in v6, #221) were filled from `raw`; only a better reading of the body needs a pass, which is
 * what `FICHA_READING` and [readVersion] are for.
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
    val issuerName: String? = null,
    val composition: String? = null,
    val sizeMillimetres: Double? = null,
    val category: String? = null,
    val numistaUrl: String? = null,
    /**
     * Which reading of the body filled the five columns above; `0` means none yet. The default is
     * declared to Room too: SQLite can't add a `NOT NULL` column without one, and the exported
     * schema must match the `ALTER TABLE` of version 6 exactly or opening the database throws.
     */
    @ColumnInfo(defaultValue = "0") val readVersion: Int = 0,
    /**
     * Version 7, also read from `raw`, so «Las cifras» is complete on a phone that never called
     * Numista (ADR 0028 §7). [thicknessMillimetres] is Numista's `thickness`, often missing, so the
     * stack height is extrapolated. [demonetized] is `demonetization.is_demonetized`; null means
     * Numista doesn't say, not «still money». [hands] (engravers and designers of both faces) and
     * [mints] are names one per line rather than JSON, so nothing is parsed per redraw (#221).
     */
    val thicknessMillimetres: Double? = null,
    val demonetized: Boolean? = null,
    val hands: String? = null,
    val mints: String? = null,
    /**
     * The year a medal was issued, from `issue_terms.issue_date` (#460). Kept apart from [minYear];
     * `TypeMetaEntity.toDomain` decides which one a card prints.
     */
    val issuedYear: Int? = null,
)

/** The stored ficha of one type, for reading fields the columns never captured. */
data class TypeRawRow(val typeId: Int, val raw: String)

/**
 * A grouping the collector made themselves (ADR 0021 §11): a heading and the types under it.
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

/**
 * An empty casilla the collector marked: «lo busco» (ADR 0029). Like [OwnGroupingMemberEntity], it
 * lives only on this device, and none of its columns is a Numista collection row id, so it survives
 * the sync that replaces `collected_items` wholesale.
 *
 * Its own table rather than a column of the inventory, so nothing that counts pieces, grams, money
 * or coverage can see it (ADR 0029 §3). Keyed on the casilla (`WishKey`), not the type, since a date
 * run or an issue run repeats one type across slots. [issueId] is 0 when the curated file declares
 * no issue, because a primary key can't hold null; `toDomain` reads it back as none. Wishes never
 * expire: [markedAt] only orders the list, and «alive» is derived from the inventory (ADR 0029 §2).
 */
@Entity(tableName = "wishes", primaryKeys = ["typeId", "year", "issueId"])
data class WishEntity(
    val typeId: Int,
    val year: Int,
    val issueId: Int,
    val markedAt: Long,
)

/** One row per Numista API request actually sent. The basis of the monthly budget counter. */
@Entity(tableName = "api_call_log")
data class ApiCallEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val endpoint: String,
    val calledAt: Long,
)

/**
 * That one issue's prices were read, and whether Numista had any (ADR 0028 §4). Price rows are per
 * grade, so an issue answered with no prices needs this row or it would be asked on every pass; a
 * failed read writes neither, so «not asked» and «asked and empty» stay apart.
 *
 * [readAt] is the only clock: one call brings every grade, so they expire together. Expired rows
 * stay and are shown with this date until a newer read replaces them.
 */
@Entity(tableName = "issue_price_reads", primaryKeys = ["typeId", "issueId"])
data class IssuePriceReadEntity(
    val typeId: Int,
    val issueId: Int,
    val readAt: Long,
    val hasPrices: Boolean,
)

/** Numista's estimated price for one issue in one grade, in euros. */
@Entity(tableName = "issue_prices", primaryKeys = ["typeId", "issueId", "grade"])
data class IssuePriceEntity(
    val typeId: Int,
    val issueId: Int,
    val grade: String,
    val eur: Double,
)

/**
 * That this phone has listed the issues of one type, and when (#452). Like [IssuePriceReadEntity],
 * it tells «listed, and no issue matches this hole's year» from «not asked yet»; a failed listing
 * writes neither this nor [TypeIssueEntity]. [readAt] expires after `LISTING_LIFETIME_MILLIS`.
 */
@Entity(tableName = "type_issue_reads")
data class TypeIssueReadEntity(
    @PrimaryKey val typeId: Int,
    val readAt: Long,
)

/**
 * One issue of one type, as `/types/{id}/issues` listed it, with only the fields a hole is matched
 * on. Both years are kept because the curated file may name either: a Moroccan dirham's Hijri 1316
 * is its `year` and 1899 its `gregorianYear`. [position] is Numista's order: a year can have several
 * issues and a hole is priced by the first match, so reading the listing back in another order would
 * miss the price already held.
 */
@Entity(tableName = "type_issues", primaryKeys = ["typeId", "issueId"])
data class TypeIssueEntity(
    val typeId: Int,
    val issueId: Int,
    val position: Int,
    val year: Int?,
    val gregorianYear: Int?,
)

/**
 * The last spot this phone read for one metal, in euros per troy ounce, and when. No history: daily
 * spots would turn the app into wealth tracking (ADR 0026 §10, ADR 0028), and the date keeps the
 * number from reading as a quotation. Not seeded: offline it would only give the silver floor, which
 * the page never shows alone.
 */
@Entity(tableName = "metal_spot")
data class MetalSpotEntity(
    @PrimaryKey val symbol: String,
    val eurPerTroyOunce: Double,
    val readAt: Long,
)
