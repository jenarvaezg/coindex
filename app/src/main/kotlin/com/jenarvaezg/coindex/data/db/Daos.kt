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
}

@Dao
interface ProposalPreferenceDao {
    @Query("SELECT * FROM collection_proposal_preferences")
    fun observeAll(): Flow<List<ProposalPreferenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preference: ProposalPreferenceEntity)

    @Query(
        """
        DELETE FROM collection_proposal_preferences
        WHERE family = :family AND weightMillioz = :weightMillioz AND finishCode = :finishCode
        """,
    )
    suspend fun delete(family: String, weightMillioz: Int, finishCode: String)
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
