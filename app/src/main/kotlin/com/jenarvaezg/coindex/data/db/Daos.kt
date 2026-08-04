package com.jenarvaezg.coindex.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(types: List<TypeMetaEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(type: TypeMetaEntity)

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
