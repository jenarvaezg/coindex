package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.ApiCallDao
import com.jenarvaezg.coindex.data.db.ApiCallEntity
import com.jenarvaezg.coindex.data.db.CollectedItemDao
import com.jenarvaezg.coindex.data.db.CollectedItemEntity
import com.jenarvaezg.coindex.data.db.TypeMetaDao
import com.jenarvaezg.coindex.data.db.TypeMetaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory stand-ins so the sync and budget rules can be tested without Android. */
class FakeCollectedItemDao : CollectedItemDao {
    val rows = MutableStateFlow<List<CollectedItemEntity>>(emptyList())

    override fun observeAll(): Flow<List<CollectedItemEntity>> = rows
    override suspend fun loadAll(): List<CollectedItemEntity> = rows.value
    override suspend fun deleteAll() { rows.value = emptyList() }
    override suspend fun insertAll(items: List<CollectedItemEntity>) {
        rows.value = rows.value + items
    }
    override suspend fun replaceAll(items: List<CollectedItemEntity>) { rows.value = items }
}

class FakeTypeMetaDao : TypeMetaDao {
    val rows = MutableStateFlow<List<TypeMetaEntity>>(emptyList())

    override fun observeAll(): Flow<List<TypeMetaEntity>> = rows
    override suspend fun cachedTypeIds(): List<Int> = rows.value.map { it.typeId }
    override suspend fun count(): Int = rows.value.size
    override suspend fun insertIfAbsent(types: List<TypeMetaEntity>) {
        types.forEach { insertIfAbsent(it) }
    }
    override suspend fun insertIfAbsent(type: TypeMetaEntity) {
        if (rows.value.none { it.typeId == type.typeId }) rows.value = rows.value + type
    }
}

class FakeApiCallDao : ApiCallDao {
    val calls = mutableListOf<ApiCallEntity>()

    override suspend fun record(call: ApiCallEntity) { calls += call }
    override suspend fun countSince(since: Long): Int = calls.count { it.calledAt >= since }
    override fun observeCountSince(since: Long): Flow<Int> =
        MutableStateFlow(calls.count { it.calledAt >= since })
}
