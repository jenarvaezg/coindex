package com.jenarvaezg.coindex.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.jenarvaezg.coindex.data.ficha.FichaReading
import com.jenarvaezg.coindex.data.toNameColumn
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectedItemDao {
    @Query("SELECT * FROM collected_items ORDER BY id")
    fun observeAll(): Flow<List<CollectedItemEntity>>

    @Query("SELECT * FROM collected_items ORDER BY id")
    suspend fun loadAll(): List<CollectedItemEntity>

    @Query("DELETE FROM collected_items")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CollectedItemEntity>)

    /**
     * Replaces the snapshot atomically. A sync that fetched nothing must never be able to
     * wipe the previous snapshot, so callers pass a non-empty list.
     */
    @Transaction
    suspend fun replaceAll(items: List<CollectedItemEntity>) {
        deleteAll()
        insertAll(items)
    }
}

@Dao
interface TypeMetaDao {
    @Query("SELECT * FROM type_meta")
    fun observeAll(): Flow<List<TypeMetaEntity>>

    @Query("SELECT typeId FROM type_meta")
    suspend fun cachedTypeIds(): List<Int>

    @Query("SELECT COUNT(*) FROM type_meta")
    suspend fun count(): Int

    @Query("SELECT * FROM type_meta WHERE typeId = :typeId")
    suspend fun byId(typeId: Int): TypeMetaEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(types: List<TypeMetaEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(type: TypeMetaEntity)

    /**
     * Writes a ficha over the one already cached. The **only** write that does (#185, ADR 0025):
     * the seed and the sync both ignore conflicts on purpose, because neither of them was asked
     * for the ficha it is holding — the collector was, one type at a time.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun overwrite(type: TypeMetaEntity)

    // Neither face, rather than the obverse alone: a ficha that only has a reverse thumbnail
    // would otherwise be read, written back with a null obverse, and read again for ever.
    @Query(
        "SELECT COUNT(*) FROM type_meta " +
            "WHERE obverseThumbnailUrl IS NULL AND reverseThumbnailUrl IS NULL",
    )
    suspend fun countWithoutThumbnails(): Int

    @Query(
        "SELECT typeId, raw FROM type_meta " +
            "WHERE obverseThumbnailUrl IS NULL AND reverseThumbnailUrl IS NULL",
    )
    suspend fun rawWithoutThumbnails(): List<TypeRawRow>

    @Query(
        "UPDATE type_meta SET obverseThumbnailUrl = :obverse, reverseThumbnailUrl = :reverse " +
            "WHERE typeId = :typeId",
    )
    suspend fun setThumbnails(typeId: Int, obverse: String?, reverse: String?)

    // By version and not by «is the column null», which is what the thumbnails had to settle for:
    // a ficha with no composition at all would otherwise be read again on every single start.
    @Query("SELECT COUNT(*) FROM type_meta WHERE readVersion < :version")
    suspend fun countReadBefore(version: Int): Int

    @Query("SELECT typeId, raw FROM type_meta WHERE readVersion < :version LIMIT :limit")
    suspend fun rawReadBefore(version: Int, limit: Int): List<TypeRawRow>

    /**
     * Writes what a batch of bodies said, as one unit.
     *
     * One transaction and not one per row: the first launch after version 6 reads the whole cache,
     * and a couple of thousand auto-committed `UPDATE`s is a couple of thousand `fsync`s in front
     * of a collector waiting for their index to draw.
     */
    @Transaction
    suspend fun setReadings(readings: Map<Int, FichaReading>, version: Int) {
        readings.forEach { (typeId, reading) ->
            setReading(
                typeId = typeId,
                issuerName = reading.issuerName,
                composition = reading.composition,
                sizeMillimetres = reading.sizeMillimetres,
                category = reading.category,
                numistaUrl = reading.numistaUrl,
                thicknessMillimetres = reading.thicknessMillimetres,
                demonetized = reading.demonetized,
                hands = reading.hands.toNameColumn(),
                mints = reading.mints.toNameColumn(),
                issuedYear = reading.issuedYear,
                version = version,
            )
        }
    }

    /**
     * Writes what one body said into its columns.
     *
     * A targeted `UPDATE` and not [overwrite]: this is the same ficha read again, not a new one,
     * and the row's `fetchedAt` still means the day this phone got it (#185, ADR 0025).
     */
    @Query(
        "UPDATE type_meta SET issuerName = :issuerName, composition = :composition, " +
            "sizeMillimetres = :sizeMillimetres, category = :category, " +
            "numistaUrl = :numistaUrl, thicknessMillimetres = :thicknessMillimetres, " +
            "demonetized = :demonetized, hands = :hands, mints = :mints, " +
            "issuedYear = :issuedYear, readVersion = :version WHERE typeId = :typeId",
    )
    suspend fun setReading(
        typeId: Int,
        issuerName: String?,
        composition: String?,
        sizeMillimetres: Double?,
        category: String?,
        numistaUrl: String?,
        thicknessMillimetres: Double?,
        demonetized: Boolean?,
        hands: String?,
        mints: String?,
        issuedYear: Int?,
        version: Int,
    )
}

/**
 * The collector's own groupings.
 *
 * Headings and memberships are observed as two flat lists and stitched together in the
 * repository: a `@Relation` would need a wrapper type whose only purpose is to be unwrapped
 * again one layer up.
 */
@Dao
interface OwnGroupingDao {
    // By id and not by name: the order of the index is one comparator over every card
    // (ADR 0021 §6), so a box has no ordering of its own to bring — this only keeps the read
    // deterministic.
    @Query("SELECT * FROM own_groupings ORDER BY id")
    fun observeAll(): Flow<List<OwnGroupingEntity>>

    @Query("SELECT * FROM own_grouping_members")
    fun observeMembers(): Flow<List<OwnGroupingMemberEntity>>

    @Insert
    suspend fun insert(grouping: OwnGroupingEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMembers(members: List<OwnGroupingMemberEntity>)

    @Query("UPDATE own_groupings SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun rename(id: Long, name: String, updatedAt: Long)

    @Query("DELETE FROM own_grouping_members WHERE groupingId = :groupingId AND typeId = :typeId")
    suspend fun removeMember(groupingId: Long, typeId: Int)

    @Query("DELETE FROM own_groupings WHERE id = :id")
    suspend fun delete(id: Long)

    /** Creates a grouping and its first types as one unit: an empty grouping is not a thing. */
    @Transaction
    suspend fun create(name: String, typeIds: List<Int>, now: Long): Long {
        val id = insert(OwnGroupingEntity(name = name, createdAt = now, updatedAt = now))
        addMembers(typeIds.map { typeId -> OwnGroupingMemberEntity(id, typeId) })
        return id
    }

    /**
     * Drops one type, and the grouping with it when that type was the last one: a heading over
     * nothing would be a card the collector cannot open and cannot get rid of.
     */
    @Transaction
    suspend fun removeMemberOrDelete(groupingId: Long, typeId: Int, now: Long) {
        removeMember(groupingId, typeId)
        if (memberCount(groupingId) == 0) {
            delete(groupingId)
        } else {
            touch(groupingId, now)
        }
    }

    @Query("SELECT COUNT(*) FROM own_grouping_members WHERE groupingId = :groupingId")
    suspend fun memberCount(groupingId: Long): Int

    @Query("UPDATE own_groupings SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touch(id: Long, updatedAt: Long)
}

@Dao
interface ApiCallDao {
    @Insert
    suspend fun record(call: ApiCallEntity)

    @Query("SELECT COUNT(*) FROM api_call_log WHERE calledAt >= :since")
    suspend fun countSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM api_call_log WHERE calledAt >= :since")
    fun observeCountSince(since: Long): Flow<Int>
}

/**
 * The catalog prices and the spot: everything money on this phone is made of (ADR 0028).
 *
 * One DAO over three tables, because they are one subject and are always read together — a total needs
 * the prices, the reads that say which issues are answered for, and the spot that buys the silver floor
 * — and a valuation assembled from three DAOs is three things a caller could forget one of.
 */
@Dao
interface PriceDao {
    @Query("SELECT * FROM issue_prices")
    fun observePrices(): Flow<List<IssuePriceEntity>>

    @Query("SELECT * FROM issue_price_reads")
    fun observeReads(): Flow<List<IssuePriceReadEntity>>

    @Query("SELECT * FROM metal_spot WHERE symbol = :symbol")
    fun observeSpot(symbol: String): Flow<MetalSpotEntity?>

    @Query("SELECT * FROM issue_price_reads")
    suspend fun reads(): List<IssuePriceReadEntity>

    @Query("SELECT * FROM type_issue_reads")
    suspend fun typeIssueReads(): List<TypeIssueReadEntity>

    /** In the order Numista listed them, which is what decides the issue a hole is priced by. */
    @Query("SELECT * FROM type_issues ORDER BY typeId, position")
    suspend fun typeIssues(): List<TypeIssueEntity>

    /**
     * The same two readings the pass makes, observed for the screens (#493).
     *
     * Until the plate's header had a cost of closing in it, which issue a casilla stands for was a
     * question only the pass ever asked, and it asked it once per pass. The header asks it of the 111
     * holes of 121 whose curated file does not name their issue — so it has to arrive the way a price
     * does, and change under a screen that is already open while the pass fills the table in.
     */
    @Query("SELECT * FROM type_issue_reads")
    fun observeTypeIssueReads(): Flow<List<TypeIssueReadEntity>>

    @Query("SELECT * FROM type_issues ORDER BY typeId, position")
    fun observeTypeIssues(): Flow<List<TypeIssueEntity>>

    @Query("SELECT * FROM metal_spot WHERE symbol = :symbol")
    suspend fun spot(symbol: String): MetalSpotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putSpot(spot: MetalSpotEntity)

    /**
     * Writes what one issue answered, as one unit.
     *
     * The old grades are deleted first and not merged over: a grade Numista has stopped pricing would
     * otherwise survive for ever under a fresh [IssuePriceReadEntity.readAt], which is a price with a
     * date that is not its own.
     *
     * Called only for an answer that arrived. A failure writes nothing at all, so being cut off between
     * the delete and the insert is the one thing this transaction is for.
     */
    @Transaction
    suspend fun putIssue(read: IssuePriceReadEntity, prices: List<IssuePriceEntity>) {
        deletePrices(read.typeId, read.issueId)
        insertPrices(prices)
        insertRead(read)
    }

    @Query("DELETE FROM issue_prices WHERE typeId = :typeId AND issueId = :issueId")
    suspend fun deletePrices(typeId: Int, issueId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrices(prices: List<IssuePriceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRead(read: IssuePriceReadEntity)

    /**
     * Writes what one type's listing answered, as one unit (#452).
     *
     * The same shape as [putIssue] and for the same reasons: the old rows go first, so an issue
     * Numista has withdrawn does not survive under a fresh read, and the mark is written last, so
     * being cut off leaves the type unlisted rather than listed-and-empty.
     */
    @Transaction
    suspend fun putListing(read: TypeIssueReadEntity, issues: List<TypeIssueEntity>) {
        deleteTypeIssues(read.typeId)
        insertTypeIssues(issues)
        insertTypeIssueRead(read)
    }

    @Query("DELETE FROM type_issues WHERE typeId = :typeId")
    suspend fun deleteTypeIssues(typeId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTypeIssues(issues: List<TypeIssueEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTypeIssueRead(read: TypeIssueReadEntity)
}
