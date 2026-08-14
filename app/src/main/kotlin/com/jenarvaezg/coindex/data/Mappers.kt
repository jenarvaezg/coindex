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
 * The Numista issue id, read from the stored response rather than from a column.
 *
 * `raw` keeps the untouched JSON element of each row (see [SyncService]), so every piece already
 * synced carries its issue id without a migration and without spending API budget again — the
 * same bargain that lets the finish be inferred on read instead of stored.
 *
 * A row with no issue, or one whose JSON cannot be read, has no issue: it fills no member of an
 * issue run, and it is still a piece everywhere else.
 *
 * Read on every pass and not memoized, unlike the ficha's five fields once were: a collection is a
 * couple of hundred rows against the cache's sixteen hundred, and this is one integer rather than
 * five parses.
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
 * The finish and the metal are inferred here rather than stored, so a later fix to the
 * inference rules applies to types cached long ago without spending API budget again.
 *
 * Everything else is a column read. The issuer's name, the composition prose, the diameter, the
 * category and the URL used to be parsed out of `raw` on every pass — sixteen hundred rows, five
 * parses each, every time the index redrew — behind a memo keyed on `(typeId, fetchedAt)` that
 * never evicted anything and that `MappersTest` had to know about to write two assertions in a row
 * (#221). Since version 6 the body is read once, when the ficha arrives, and this is a mapper
 * again.
 */
fun TypeMetaEntity.toDomain(): TypeMeta = TypeMeta(
    id = typeId,
    title = title,
    displayTitle = title,
    family = family,
    issuerCode = issuerCode,
    issuerName = issuerName,
    // A medal has no `min_year`; what it has is the year it was issued, and for the one question
    // the app asks — «what year is this piece from» — they are the same answer (#460).
    minYear = minYear ?: issuedYear,
    maxYear = maxYear ?: issuedYear,
    weightOz = weightGrams?.let(::gramsToOunces),
    finish = inferFinish(title, family),
    metal = inferMetal(composition),
    // The third rule read off the composition prose, beside the metal and for the same reason: the
    // silver floor of a piece is its *fine* silver, and a .835 coin is 16,5 % copper (ADR 0028 §8).
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
 * One name per line, which is how version 7 stores a list in a column.
 *
 * Split and not parsed: Numista names hold commas and quotes but never a newline, and a JSON array per
 * row would put a parse back on a mapper that runs over sixteen hundred rows every redraw (#221).
 */
private fun String?.toNameList(): List<String> =
    this?.lineSequence()?.filter(String::isNotBlank)?.toList().orEmpty()

/** The same list on the way in. Null and not the empty string when there is nothing to say. */
internal fun List<String>.toNameColumn(): String? =
    filter(String::isNotBlank).takeIf { it.isNotEmpty() }?.joinToString("\n")

/**
 * The two faces of a type as pictures to ask for.
 *
 * `obverseUrl` is the original and the thumbnail column is the small one; a row cached before
 * version 3 has no thumbnail yet, and falls back to the single URL it does have.
 */
fun TypeMetaEntity.toImages(): TypeImages = TypeImages(
    obverse = CoinPhoto(thumbnail = obverseThumbnailUrl, picture = obverseUrl),
    reverse = CoinPhoto(thumbnail = reverseThumbnailUrl, picture = reverseUrl),
)

/**
 * Maps a Numista type response onto the cache row, keeping the untouched body.
 *
 * Here and not in `data.seed`, which is where it used to live: the sync and the refresh both spend
 * the collector's real API budget through it, and neither of them has anything to do with the
 * snapshot shipped in the assets (#221).
 *
 * The body is read **once, right here**, into the five columns of version 6 — so a ficha is parsed
 * on the way in rather than on every pass over the cache, and a row always knows which reading
 * wrote it.
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
 * One marked casilla, with the sentinel read back as the absence it stands for (ADR 0029 §1).
 *
 * The zero lives in the table and nowhere else: SQLite cannot hold a null in a primary key, and a
 * `WishKey` that carried a zero would have to be compared against a curated member that carries a
 * null. Both directions are here, together, so the two halves of the sentinel cannot drift.
 */
fun WishEntity.toDomain(): Wish = Wish(
    key = WishKey(typeId = typeId, year = year, issueId = issueId.takeIf { it != NO_ISSUE }),
    markedAt = markedAt,
)

fun Wish.toEntity(): WishEntity = WishEntity(
    typeId = key.typeId,
    year = key.year,
    issueId = key.issueId ?: NO_ISSUE,
    markedAt = markedAt,
)

/** «The curated file declares no issue for this casilla», in the one column that cannot say null. */
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
