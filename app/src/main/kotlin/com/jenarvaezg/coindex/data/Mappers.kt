package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.CollectedItemEntity
import com.jenarvaezg.coindex.data.db.OwnGroupingEntity
import com.jenarvaezg.coindex.data.db.OwnGroupingMemberEntity
import com.jenarvaezg.coindex.data.db.ProposalPreferenceEntity
import com.jenarvaezg.coindex.data.db.TypeMetaEntity
import com.jenarvaezg.coindex.data.numista.CollectedItemDto
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionProposalKey
import com.jenarvaezg.coindex.domain.CollectionProposalPreference
import com.jenarvaezg.coindex.domain.OwnGrouping
import com.jenarvaezg.coindex.domain.ProposalDisposition
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.gramsToOunces
import com.jenarvaezg.coindex.domain.inferFinish
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
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
 * The finish is inferred here rather than stored, so a later fix to the inference rules
 * applies to types cached long ago without spending API budget again.
 */
fun TypeMetaEntity.toDomain(): TypeMeta = TypeMeta(
    id = typeId,
    title = title,
    displayTitle = title,
    family = family,
    issuerCode = issuerCode,
    issuerName = issuerNameFromRaw(raw),
    minYear = minYear,
    maxYear = maxYear,
    weightOz = weightGrams?.let(::gramsToOunces),
    finish = inferFinish(title, family),
)

/** One own grouping with its memberships, stitched from the two flat lists Room observes. */
fun OwnGroupingEntity.toDomain(members: List<OwnGroupingMemberEntity>): OwnGrouping = OwnGrouping(
    id = id,
    name = name,
    typeIds = members.filter { it.groupingId == id }.map { it.typeId },
)

/** A stored preference whose parts are no longer canonical is ignored, not guessed at. */
fun ProposalPreferenceEntity.toDomain(): CollectionProposalPreference? {
    val key = CollectionProposalKey.fromCanonicalParts(family, weightMillioz, finishCode)
        ?: return null
    val disposition = ProposalDisposition.fromCode(disposition) ?: return null
    return CollectionProposalPreference(key, disposition)
}

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
