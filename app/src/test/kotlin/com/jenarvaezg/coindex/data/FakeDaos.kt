package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.ApiCallDao
import com.jenarvaezg.coindex.data.db.ApiCallEntity
import com.jenarvaezg.coindex.data.db.CollectedItemDao
import com.jenarvaezg.coindex.data.db.CollectedItemEntity
import com.jenarvaezg.coindex.data.db.OwnGroupingDao
import com.jenarvaezg.coindex.data.db.OwnGroupingEntity
import com.jenarvaezg.coindex.data.db.IssuePriceEntity
import com.jenarvaezg.coindex.data.db.IssuePriceReadEntity
import com.jenarvaezg.coindex.data.db.MetalSpotEntity
import com.jenarvaezg.coindex.data.db.OwnGroupingMemberEntity
import com.jenarvaezg.coindex.data.db.PriceDao
import com.jenarvaezg.coindex.data.db.TypeIssueEntity
import com.jenarvaezg.coindex.data.db.TypeIssueReadEntity
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
    private fun readBefore(version: Int) = rows.value.filter { it.readVersion < version }
    override suspend fun countReadBefore(version: Int): Int = readBefore(version).size
    override suspend fun rawReadBefore(version: Int, limit: Int): List<TypeRawRow> =
        readBefore(version).take(limit).map { TypeRawRow(it.typeId, it.raw) }
    override suspend fun setReading(
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
        version: Int,
    ) {
        rows.value = rows.value.map { row ->
            if (row.typeId == typeId) {
                row.copy(
                    issuerName = issuerName,
                    composition = composition,
                    sizeMillimetres = sizeMillimetres,
                    category = category,
                    numistaUrl = numistaUrl,
                    thicknessMillimetres = thicknessMillimetres,
                    demonetized = demonetized,
                    hands = hands,
                    mints = mints,
                    readVersion = version,
                )
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

/**
 * The prices and the spot, in memory.
 *
 * The three states of ADR 0028 §4 are visible from here and that is the point of the stand-in: [reads]
 * with `hasPrices = false` is «Numista answered and had none», and **no** row at all is «not asked yet».
 * A pass that failed must leave the second, not the first.
 */
class FakePriceDao : PriceDao {
    val prices = MutableStateFlow<List<IssuePriceEntity>>(emptyList())
    val reads = MutableStateFlow<List<IssuePriceReadEntity>>(emptyList())
    val spots = MutableStateFlow<List<MetalSpotEntity>>(emptyList())

    /** The listings of #452, which tell «listed and empty» from «never listed» the same way. */
    val typeIssueReads = MutableStateFlow<List<TypeIssueReadEntity>>(emptyList())
    val typeIssues = MutableStateFlow<List<TypeIssueEntity>>(emptyList())

    override fun observePrices(): Flow<List<IssuePriceEntity>> = prices
    override fun observeReads(): Flow<List<IssuePriceReadEntity>> = reads
    override fun observeSpot(symbol: String): Flow<MetalSpotEntity?> =
        MutableStateFlow(spots.value.firstOrNull { it.symbol == symbol })
    override suspend fun reads(): List<IssuePriceReadEntity> = reads.value
    override suspend fun spot(symbol: String): MetalSpotEntity? =
        spots.value.firstOrNull { it.symbol == symbol }
    override suspend fun putSpot(spot: MetalSpotEntity) {
        spots.value = spots.value.filterNot { it.symbol == spot.symbol } + spot
    }
    override suspend fun deletePrices(typeId: Int, issueId: Int) {
        prices.value = prices.value.filterNot { it.typeId == typeId && it.issueId == issueId }
    }
    override suspend fun insertPrices(prices: List<IssuePriceEntity>) {
        this.prices.value = this.prices.value + prices
    }
    override suspend fun insertRead(read: IssuePriceReadEntity) {
        reads.value = reads.value
            .filterNot { it.typeId == read.typeId && it.issueId == read.issueId } + read
    }
    override suspend fun typeIssueReads(): List<TypeIssueReadEntity> = typeIssueReads.value
    // Ordenado como lo ordena la consulta de verdad: la posición decide qué emisión tasa un hueco.
    override suspend fun typeIssues(): List<TypeIssueEntity> =
        typeIssues.value.sortedWith(compareBy({ it.typeId }, { it.position }))
    override suspend fun deleteTypeIssues(typeId: Int) {
        typeIssues.value = typeIssues.value.filterNot { it.typeId == typeId }
    }
    override suspend fun insertTypeIssues(issues: List<TypeIssueEntity>) {
        typeIssues.value = typeIssues.value + issues
    }
    override suspend fun insertTypeIssueRead(read: TypeIssueReadEntity) {
        typeIssueReads.value =
            typeIssueReads.value.filterNot { it.typeId == read.typeId } + read
    }
}
