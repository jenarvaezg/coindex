package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.CollectedItemEntity
import com.jenarvaezg.coindex.data.db.OwnGroupingEntity
import com.jenarvaezg.coindex.data.db.OwnGroupingMemberEntity
import com.jenarvaezg.coindex.data.db.TypeMetaEntity
import com.jenarvaezg.coindex.data.numista.CollectedItemDto
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.OwnGrouping
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.gramsToOunces
import com.jenarvaezg.coindex.domain.inferFinish
import com.jenarvaezg.coindex.domain.inferMetal
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
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
 * Numista's name for the issuer, read from the stored response like the issue id above.
 *
 * Only the code is a column, and a code is `australie` even in a Spanish catalogue: the name is
 * the only issuer string a card can print. Reading it from `raw` gives it to every type already
 * cached — including the 608 seeded from assets — without a migration or an API call.
 */
internal fun issuerNameFromRaw(raw: String): String? = runCatching {
    lenientJson.parseToJsonElement(raw)
        .jsonObject["issuer"]
        ?.jsonObject
        ?.get("name")
        ?.jsonPrimitive
        ?.contentOrNull
        ?.takeIf(String::isNotBlank)
}.getOrNull()

/**
 * Numista's `composition.text`, read from the stored response for the same bargain: the metal of
 * every type already cached — the 723 seeded among them — without a migration or an API call.
 *
 * A column would have frozen the reading. The rules that turn this prose into a [Metal] are
 * inference, like the finish, so keeping the prose is what lets a better rule fix old rows.
 */
internal fun compositionFromRaw(raw: String): String? = runCatching {
    lenientJson.parseToJsonElement(raw)
        .jsonObject["composition"]
        ?.jsonObject
        ?.get("text")
        ?.jsonPrimitive
        ?.contentOrNull
        ?.takeIf(String::isNotBlank)
}.getOrNull()

/**
 * Numista's `size`, the coin's diameter in millimetres, read from the stored response.
 *
 * Read rather than stored for the third time and for the same bargain (`issuer.name`,
 * `composition.text`): it covers 100 % of the seeded cache, so the printed notebook of #169 gets a
 * real diameter for every type already on the phone without a migration or an API call. A column
 * would have bought nothing — unlike the finish and the metal there is no inference here to
 * improve, the field is already a number in millimetres.
 */
internal fun sizeFromRaw(raw: String): Double? = runCatching {
    lenientJson.parseToJsonElement(raw)
        .jsonObject["size"]
        ?.jsonPrimitive
        ?.doubleOrNull
        ?.takeIf { it > 0.0 }
}.getOrNull()

/**
 * Numista's `category` — `coin` or `exonumia` — read from the stored response for the fourth time on
 * the same bargain.
 *
 * It covers 100 % of the seeded cache, so the class chip of Coins (ADR 0021 §1) works on every type
 * already on the phone without a migration or an API call. The prose is what is read, not the split:
 * `objectClassOf` is a rule, and a rule stored as a column could never be improved.
 */
internal fun categoryFromRaw(raw: String): String? = runCatching {
    lenientJson.parseToJsonElement(raw)
        .jsonObject["category"]
        ?.jsonPrimitive
        ?.contentOrNull
        ?.takeIf(String::isNotBlank)
}.getOrNull()

/**
 * Numista's short URL for the type, read from the stored response on the same bargain as the four
 * above — and covering 100 % of the seeded cache, so the QR of the printed notebook (#234) works on
 * every type already on the phone without a migration or an API call.
 *
 * Read and never built. `https://es.numista.com/$typeId` would produce the same string today for every
 * seeded type, and it would be **our** guess about Numista's host and about which language the
 * ficha was asked in — printed onto paper, where nobody can correct it. What is read is what the
 * collector's own request came back with.
 */
internal fun urlFromRaw(raw: String): String? = runCatching {
    lenientJson.parseToJsonElement(raw)
        .jsonObject["url"]
        ?.jsonPrimitive
        ?.contentOrNull
        ?.takeIf(String::isNotBlank)
}.getOrNull()

/**
 * Issuer names already read, kept for as long as the process lives.
 *
 * Every emission of the collection re-maps every cached type — 608 of them after the seed, each
 * carrying its whole Numista response — so parsing on each pass would put a tenth of a second
 * between a sync landing and the index redrawing. The key carries `fetchedAt`, which is what makes a
 * refreshed ficha read again instead of answering from the memo (#185): the refresh stamps the row
 * with the moment it was brought, so its old parse is unreachable rather than stale.
 * The empty string stands in for «no issuer», which a [ConcurrentHashMap] cannot hold as null.
 */
private val issuerNames = ConcurrentHashMap<Pair<Int, Long>, String>()

/** The composition prose, cached on the same terms and for the same reason as [issuerNames]. */
private val compositions = ConcurrentHashMap<Pair<Int, Long>, String>()

/** Diameters already read, on the same terms. A map cannot hold null, so [NO_SIZE] stands in. */
private val sizes = ConcurrentHashMap<Pair<Int, Long>, Double>()

/** Categories already read, on the same terms as [issuerNames]. */
private val categories = ConcurrentHashMap<Pair<Int, Long>, String>()

/** Short URLs already read, on the same terms. The empty string stands in for «no URL». */
private val urls = ConcurrentHashMap<Pair<Int, Long>, String>()

/** «Nobody recorded a diameter», as a value a [ConcurrentHashMap] can hold. */
private const val NO_SIZE = -1.0

/**
 * The finish and the metal are inferred here rather than stored, so a later fix to the
 * inference rules applies to types cached long ago without spending API budget again.
 */
fun TypeMetaEntity.toDomain(): TypeMeta = TypeMeta(
    id = typeId,
    title = title,
    displayTitle = title,
    family = family,
    issuerCode = issuerCode,
    issuerName = issuerNames
        .getOrPut(typeId to fetchedAt) { issuerNameFromRaw(raw).orEmpty() }
        .ifEmpty { null },
    minYear = minYear,
    maxYear = maxYear,
    weightOz = weightGrams?.let(::gramsToOunces),
    finish = inferFinish(title, family),
    // Only the parse is cached: the rules run on every read, so improving them fixes old rows.
    metal = inferMetal(
        compositions
            .getOrPut(typeId to fetchedAt) { compositionFromRaw(raw).orEmpty() }
            .ifEmpty { null },
    ),
    sizeMillimetres = sizes
        .getOrPut(typeId to fetchedAt) { sizeFromRaw(raw) ?: NO_SIZE }
        .takeIf { it > 0.0 },
    category = categories
        .getOrPut(typeId to fetchedAt) { categoryFromRaw(raw).orEmpty() }
        .ifEmpty { null },
    numistaUrl = urls
        .getOrPut(typeId to fetchedAt) { urlFromRaw(raw).orEmpty() }
        .ifEmpty { null },
)

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

/** One own grouping with its memberships, stitched from the two flat lists Room observes. */
fun OwnGroupingEntity.toDomain(members: List<OwnGroupingMemberEntity>): OwnGrouping = OwnGrouping(
    id = id,
    name = name,
    typeIds = members.filter { it.groupingId == id }.map { it.typeId },
)

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
