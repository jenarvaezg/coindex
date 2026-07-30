package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.CollectedItemEntity
import com.jenarvaezg.coindex.data.db.ProposalPreferenceEntity
import com.jenarvaezg.coindex.data.db.TypeMetaEntity
import com.jenarvaezg.coindex.data.numista.CollectedItemDto
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionProposalKey
import com.jenarvaezg.coindex.domain.CollectionProposalPreference
import com.jenarvaezg.coindex.domain.ProposalDisposition
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.gramsToOunces
import com.jenarvaezg.coindex.domain.inferFinish

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
)

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
    minYear = minYear,
    maxYear = maxYear,
    weightOz = weightGrams?.let(::gramsToOunces),
    finish = inferFinish(title, family),
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
