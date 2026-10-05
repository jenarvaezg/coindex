package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.CollectedItemEntity
import com.jenarvaezg.coindex.data.db.OwnGroupingEntity
import com.jenarvaezg.coindex.data.db.OwnGroupingMemberEntity
import com.jenarvaezg.coindex.data.db.TypeMetaEntity
import com.jenarvaezg.coindex.data.db.WishEntity
import com.jenarvaezg.coindex.data.ficha.FICHA_READING
import com.jenarvaezg.coindex.data.ficha.FichaReading
import com.jenarvaezg.coindex.data.ficha.readFichaBody
import com.jenarvaezg.coindex.data.ficha.thumbnails
import com.jenarvaezg.coindex.data.numista.CollectedItemDto
import com.jenarvaezg.coindex.data.numista.NumistaTypeDto
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.OwnGrouping
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.Wish
import com.jenarvaezg.coindex.domain.WishKey
import com.jenarvaezg.coindex.domain.gramsToOunces
import com.jenarvaezg.coindex.domain.inferFinish
import com.jenarvaezg.coindex.domain.inferMetal
import com.jenarvaezg.coindex.domain.silverFineness
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun CollectedItemEntity.toDomain(): CollectedItem = CollectedItem(
    id = id,
    quantity = quantity,
    typeId = typeId,
    title = title,
    issuerCode = issuerCode,
    issueYear = issueYear,
    gregorianYear = gregorianYear,
    grade = grade,
    price = price,
    forSwap = forSwap,
    collectionName = collectionName,
    issueId = issueIdFromRaw(raw),
)

private val lenientJson = Json { ignoreUnknownKeys = true }

/**
 * The Numista issue id, read from the stored response (`raw`, see [SyncService]) rather than a
 * column, so pieces already synced have it without a migration or an API call. A row with no issue,
 * or unreadable JSON, fills no member of an issue run but is still a piece everywhere else. Not
 * memoized: the collection is small next to the type cache, and this is one integer.
 */
internal fun issueIdFromRaw(raw: String): Int? = runCatching {
    lenientJson.parseToJsonElement(raw)
        .jsonObject["issue"]
        ?.jsonObject
        ?.get("id")
        ?.jsonPrimitive
        ?.intOrNull
}.getOrNull()

/**
 * The finish, the metal and the fineness are inferred here rather than stored, so a later fix to
 * the rules applies to types cached long ago. Everything else is a column read; the body is parsed
 * once, when the ficha arrives (#221).
 */
fun TypeMetaEntity.toDomain(): TypeMeta = TypeMeta(
    id = typeId,
    title = title,
    displayTitle = title,
    family = family,
    issuerCode = issuerCode,
    issuerName = issuerName,
    // A medal has no `min_year`, only the year it was issued, which answers the same question (#460).
    minYear = minYear ?: issuedYear,
    maxYear = maxYear ?: issuedYear,
    weightOz = weightGrams?.let(::gramsToOunces),
    finish = inferFinish(title, family, composition),
    metal = inferMetal(composition),
    // The silver floor counts fine silver only: a .835 coin is 16.5 % copper (ADR 0028 §8).
    fineness = silverFineness(composition),
    sizeMillimetres = sizeMillimetres,
    category = category,
    numistaUrl = numistaUrl,
    thicknessMillimetres = thicknessMillimetres,
    demonetized = demonetized,
    hands = hands.toNameList(),
    mints = mints.toNameList(),
)

/**
 * One name per line, as version 7 stores a list in a column. Numista names hold commas and quotes
 * but never a newline, and splitting avoids a JSON parse per row on every redraw (#221).
 */
private fun String?.toNameList(): List<String> =
    this?.lineSequence()?.filter(String::isNotBlank)?.toList().orEmpty()

/** The same list on the way in; null rather than empty when there are no names. */
internal fun List<String>.toNameColumn(): String? =
    filter(String::isNotBlank).takeIf { it.isNotEmpty() }?.joinToString("\n")

/**
 * The two faces of a type as pictures to ask for: the thumbnail column and the original. A row
 * cached before version 3 has no thumbnail yet and falls back to the original.
 */
fun TypeMetaEntity.toImages(): TypeImages = TypeImages(
    obverse = CoinPhoto(thumbnail = obverseThumbnailUrl, picture = obverseUrl),
    reverse = CoinPhoto(thumbnail = reverseThumbnailUrl, picture = reverseUrl),
)

/**
 * Maps a Numista type response onto the cache row, keeping the untouched body and reading it once
 * into its columns, stamped with the reading that wrote them (#221). Used by the sync and the ficha
 * refresh.
 */
fun typeMetaEntity(
    typeId: Int,
    dto: NumistaTypeDto,
    raw: String,
    fetchedAt: Long,
): TypeMetaEntity {
    val thumbnails = dto.thumbnails()
    return TypeMetaEntity(
        typeId = typeId,
        title = dto.title,
        family = dto.series,
        issuerCode = dto.issuer?.code,
        minYear = dto.minYear,
        maxYear = dto.maxYear,
        weightGrams = dto.weight,
        obverseUrl = dto.obverse?.picture ?: dto.obverse?.thumbnail,
        reverseUrl = dto.reverse?.picture ?: dto.reverse?.thumbnail,
        raw = raw,
        fetchedAt = fetchedAt,
        obverseThumbnailUrl = thumbnails.obverse,
        reverseThumbnailUrl = thumbnails.reverse,
    ).withReading(readFichaBody(raw))
}

/** The row as this version of the reading writes it. */
internal fun TypeMetaEntity.withReading(reading: FichaReading): TypeMetaEntity = copy(
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
    readVersion = FICHA_READING,
)

/** One own grouping with its memberships, stitched from the two flat lists Room observes. */
fun OwnGroupingEntity.toDomain(members: List<OwnGroupingMemberEntity>): OwnGrouping = OwnGrouping(
    id = id,
    name = name,
    typeIds = members.filter { it.groupingId == id }.map { it.typeId },
)

/**
 * One marked casilla, with the stored sentinel read back as «no issue» (ADR 0029 §1). The zero
 * exists only in the table, since a primary key can't hold null; both directions live here so they
 * can't drift.
 */
fun WishEntity.toDomain(): Wish = Wish(
    key = WishKey(typeId = typeId, year = year, issueId = issueId.takeIf { it != NO_ISSUE }),
    markedAt = markedAt,
)

fun Wish.toEntity(): WishEntity = WishEntity(
    typeId = key.typeId,
    year = key.year,
    issueId = key.storedIssueId(),
    markedAt = markedAt,
)

/**
 * The issue this key is stored under, sentinel included. Public because unmarking needs the three
 * columns from a key, without building a `Wish` with a made-up date.
 */
fun WishKey.storedIssueId(): Int = issueId ?: NO_ISSUE

/** The curated file declares no issue for this casilla. */
private const val NO_ISSUE = 0

/** Missing quantities default to one piece; an id-less or type-less item cannot be stored. */
fun CollectedItemDto.toEntity(raw: String, syncedAt: Long): CollectedItemEntity? {
    val itemId = id ?: return null
    val typeId = itemType?.id ?: return null
    return CollectedItemEntity(
        id = itemId,
        typeId = typeId,
        quantity = (quantity ?: 1).coerceAtLeast(1),
        title = itemType.title,
        issuerCode = itemType.issuer?.code,
        issueYear = issue?.year,
        gregorianYear = issue?.gregorianYear,
        grade = grade,
        price = price?.value,
        forSwap = forSwap,
        collectionName = collection?.name,
        raw = raw,
        syncedAt = syncedAt,
    )
}
