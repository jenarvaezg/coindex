package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.ApiCallDao
import com.jenarvaezg.coindex.data.db.ApiCallEntity
import com.jenarvaezg.coindex.data.db.CollectedItemDao
import com.jenarvaezg.coindex.data.db.CollectedItemEntity
import com.jenarvaezg.coindex.data.db.OwnGroupingDao
import com.jenarvaezg.coindex.data.db.OwnGroupingEntity
import com.jenarvaezg.coindex.data.db.OwnGroupingMemberEntity
import com.jenarvaezg.coindex.data.db.TypeMetaDao
import com.jenarvaezg.coindex.data.db.TypeMetaEntity
import com.jenarvaezg.coindex.data.db.TypeRawRow
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
    override suspend fun byId(typeId: Int): TypeMetaEntity? =
        rows.value.firstOrNull { it.typeId == typeId }
    override suspend fun overwrite(type: TypeMetaEntity) {
        rows.value = rows.value.filterNot { it.typeId == type.typeId } + type
    }
    private fun withoutThumbnails() = rows.value
        .filter { it.obverseThumbnailUrl == null && it.reverseThumbnailUrl == null }
    override suspend fun countWithoutThumbnails(): Int = withoutThumbnails().size
    override suspend fun rawWithoutThumbnails(): List<TypeRawRow> =
        withoutThumbnails().map { TypeRawRow(it.typeId, it.raw) }
    override suspend fun setThumbnails(typeId: Int, obverse: String?, reverse: String?) {
        rows.value = rows.value.map { row ->
            if (row.typeId == typeId) {
                row.copy(obverseThumbnailUrl = obverse, reverseThumbnailUrl = reverse)
            } else {
                row
            }
        }
    }
}

/**
 * The boxes the collector typed, in memory.
 *
 * The last of the four DAOs to get a stand-in (#217), and the only one with logic of its own to
 * stand in for: `create` and `removeMemberOrDelete` are `@Transaction` default methods, so what is
 * reimplemented here is the two queries they call and never the rule between them — a grouping over
 * nothing is deleted here because the interface's own body says so.
 */
class FakeOwnGroupingDao : OwnGroupingDao {
    val groupings = MutableStateFlow<List<OwnGroupingEntity>>(emptyList())
    val members = MutableStateFlow<List<OwnGroupingMemberEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<OwnGroupingEntity>> = groupings
    override fun observeMembers(): Flow<List<OwnGroupingMemberEntity>> = members
    override suspend fun insert(grouping: OwnGroupingEntity): Long {
        val id = nextId++
        groupings.value = groupings.value + grouping.copy(id = id)
        return id
    }
    override suspend fun addMembers(added: List<OwnGroupingMemberEntity>) {
        // `IGNORE` sobre la clave (groupingId, typeId): un tipo repetido no duplica fila, y eso
        // vale también dentro del mismo lote — `create(nombre, listOf(7, 7), now)` deja una.
        members.value = (members.value + added).distinct()
    }
    override suspend fun rename(id: Long, name: String, updatedAt: Long) {
        groupings.value = groupings.value.map { grouping ->
            if (grouping.id == id) grouping.copy(name = name, updatedAt = updatedAt) else grouping
        }
    }
    override suspend fun removeMember(groupingId: Long, typeId: Int) {
        members.value = members.value
            .filterNot { it.groupingId == groupingId && it.typeId == typeId }
    }
    override suspend fun delete(id: Long) {
        groupings.value = groupings.value.filterNot { it.id == id }
        members.value = members.value.filterNot { it.groupingId == id }
    }
    override suspend fun memberCount(groupingId: Long): Int =
        members.value.count { it.groupingId == groupingId }
    override suspend fun touch(id: Long, updatedAt: Long) {
        groupings.value = groupings.value.map { grouping ->
            if (grouping.id == id) grouping.copy(updatedAt = updatedAt) else grouping
        }
    }
}

class FakeApiCallDao : ApiCallDao {
    val calls = mutableListOf<ApiCallEntity>()

    override suspend fun record(call: ApiCallEntity) { calls += call }
    override suspend fun countSince(since: Long): Int = calls.count { it.calledAt >= since }
    override fun observeCountSince(since: Long): Flow<Int> =
        MutableStateFlow(calls.count { it.calledAt >= since })
}
