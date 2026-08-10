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
 * Permanent catalog cache. No sync and no seed ever asks for a type twice: catalog data is
 * essentially immutable and API calls are the project's scarcest resource.
 *
 * **The collector can** (#185, ADR 0025), one type at a time, from the card where the wrong data is
 * on screen — and that gesture is the only writer that overwrites a row here. `fetchedAt` is
 * therefore the day this phone got the ficha, not the day it was first cached, and it is what the
 * card prints as «ficha traída hace ocho meses».
 *
 * The finish is deliberately *not* stored: it is inferred from `title` and `family` on read,
 * so improving the inference rules fixes old rows without re-fetching anything.
 *
 * The thumbnail URLs arrived in version 3 and are the reason `raw` exists: every row already
 * held them, unread, so the whole cache could be filled in without a single API call.
 *
 * The five columns of version 6 arrived the same way, out of the body rather than out of the
 * network (#221). What they store is what Numista *wrote* — the issuer's name, the composition
 * prose, the diameter, the category, the short URL — and never what this app makes of it: the
 * metal is still inferred from [composition] on read, the class still from [category]. So a rule
 * improved tomorrow still fixes rows cached today, and only a better *reading of the body* needs a
 * pass, which is what `FICHA_READING` and [readVersion] are for.
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
     * Which reading of the body filled the five columns above; `0` means «none yet».
     *
     * The default is declared to Room and not only to Kotlin: SQLite cannot add a `NOT NULL`
     * column without one, so the exported schema and the `ALTER TABLE` of version 6 have to agree
     * on it letter by letter or the app throws on opening the collector's database.
     */
    @ColumnInfo(defaultValue = "0") val readVersion: Int = 0,
    /**
     * The four columns of version 7, and the same bargain again: read out of the body every row
     * already stores, so «Las cifras» opens whole on a phone that has never called Numista (ADR 0028
     * §7).
     *
     * [thicknessMillimetres] is Numista's `thickness`, missing in a third of the types, which is why
     * the stack is the one figure the app gives extrapolated. [demonetized] is
     * `demonetization.is_demonetized`, and null is «Numista does not say» and not «still money».
     * [hands] and [mints] are the names of `engravers`/`designers` of both faces and of `mints`, one
     * per line: a delimited string and not JSON, because a mapper that ran on sixteen hundred rows per
     * redraw is exactly what version 6 was built to stop (#221).
     */
    val thicknessMillimetres: Double? = null,
    val demonetized: Boolean? = null,
    val hands: String? = null,
    val mints: String? = null,
)

/** The stored ficha of one type, for reading fields the columns never captured. */
data class TypeRawRow(val typeId: Int, val raw: String)

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

/**
 * That one issue's prices were read, and whether Numista had any (ADR 0028 §4).
 *
 * **This table is what makes three states out of two**, and it is the reason there are two of them
 * rather than one. A price row exists per grade; an issue Numista answered for and had no price for
 * has no grade to be keyed on, and without a row of its own those 19 issues of his 223 would be asked
 * for again on every pass, for ever. A pass that **failed** writes neither, so «not asked yet» and
 * «asked and empty» stay different questions.
 *
 * [readAt] is also the only clock: a price expires 30 days after the issue was read, not per grade,
 * because one call brought every grade at the same instant. And **expiry is not deletion** — the row
 * stays and is shown with this date until a newer read replaces it.
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
 * The last spot this phone read for one metal, in euros per troy ounce, and when.
 *
 * One row per symbol and **no history**: a table of daily spots is how wealth management would arrive
 * without anybody deciding it, and that stays outside (ADR 0026 §10, ADR 0028). What is kept is the
 * last reading and its date, because the date is what stops the number reading as a quotation.
 *
 * It is not seeded in the APK either: a seeded spot would only buy the silver floor of a piece opened
 * with no network, and the silver floor alone is precisely the figure the page refuses to show.
 */
@Entity(tableName = "metal_spot")
data class MetalSpotEntity(
    @PrimaryKey val symbol: String,
    val eurPerTroyOunce: Double,
    val readAt: Long,
)
