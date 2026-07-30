package com.jenarvaezg.coindex.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A curated, sourced reference list of official members for one exact proposal variant key.
 *
 * `schema_version` 1 identifies members by a unique Numista type; owning the type is owning
 * the member. `schema_version` 2 is a date run (ADR 0009): members repeat one type across
 * years, and a member is owned only when the piece also records that year.
 * `schema_version` 3 is a set issued as a set (ADR 0012): its members span physical variants,
 * so it declares no weight and no finish and its key carries an absent weight.
 * `schema_version` 5 identifies members by Numista issue (ADR 0014), for the types whose
 * members share a year and differ by a variety of it.
 */
@Serializable
data class CollectionCatalog(
    @SerialName("schema_version") val schemaVersion: Int,
    val id: String,
    val name: String,
    @SerialName("issuer_code") val issuerCode: String,
    val family: String,
    @SerialName("weight_millioz") val weightMillioz: Int? = null,
    val finish: Finish? = null,
    val source: String,
    @SerialName("updated_at") val updatedAt: String,
    val members: List<CollectionCatalogMember>,
) {
    fun key(): CollectionProposalKey = CollectionProposalKey(family, weightMillioz, finish)

    val isDateRun: Boolean get() = schemaVersion == 2

    /** A set issued as a set: the set is the collectible unit, not any one physical variant. */
    val isSet: Boolean get() = schemaVersion == 3

    /** Members are Numista issues of one type rather than years of it (ADR 0014). */
    val isIssueRun: Boolean get() = schemaVersion == 5

    /**
     * Whether a collected item satisfies one member.
     *
     * Schema 1 matches by type alone; a date run also requires the year recorded on the piece,
     * so an undated piece never fills a year. An issue run matches by Numista issue and
     * **ignores the year entirely**: its members share one, which is exactly why they are keyed
     * on the issue. A piece recorded without an issue fills no member of it.
     */
    fun memberMatches(member: CollectionCatalogMember, item: CollectedItem): Boolean {
        if (item.quantity <= 0 || item.typeId != member.numistaTypeId) return false
        return when {
            isIssueRun -> item.issueId != null && item.issueId in member.numistaIssueIds
            isDateRun -> item.recordedYear == member.year
            else -> true
        }
    }

    /**
     * What to call one owned piece, when this catalog is what tells its rows apart.
     *
     * Only an issue run has something to add: «Estrella 67» where the row itself can only say
     * 1966, like its five siblings.
     */
    fun emissionLabelFor(item: CollectedItem): String? {
        if (!isIssueRun) return null
        return members.firstOrNull { member -> memberMatches(member, item) }?.label
    }

    /**
     * Whether the collector owns at least one official type of this catalog. Evidence is by
     * type even in date runs, so a plate stays reachable while its years are still missing.
     */
    fun isEvidencedBy(items: List<CollectedItem>): Boolean {
        val memberTypeIds = members.mapTo(mutableSetOf()) { it.numistaTypeId }
        return items.any { item -> item.quantity > 0 && item.typeId in memberTypeIds }
    }

    fun validate(): CollectionCatalogValidationError? {
        if (schemaVersion !in 1..3 && schemaVersion != 5) {
            return CollectionCatalogValidationError.UnsupportedSchemaVersion(schemaVersion)
        }
        if (!isSlug(id)) {
            return CollectionCatalogValidationError.InvalidId("catalog", id)
        }
        blankField("catalog.name", name)?.let { return it }
        blankField("catalog.issuer_code", issuerCode)?.let { return it }
        blankField("catalog.updated_at", updatedAt)?.let { return it }
        // A set declares no physical variant; anything else must declare exactly one weight.
        if (isSet && (weightMillioz != null || finish != null)) {
            return CollectionCatalogValidationError.SetDeclaresVariant
        }
        if (!isSet && weightMillioz == null) {
            return CollectionCatalogValidationError.MissingWeight
        }
        val canonical = CollectionProposalKey.fromCanonicalParts(
            family,
            weightMillioz ?: SPANNING_VARIANTS_WEIGHT,
            finishCode(finish),
        )
        if (canonical != key()) {
            return CollectionCatalogValidationError.InvalidVariantKey
        }
        val sourceIsValid = if (schemaVersion == 1) {
            isNumistaSeriesSource(source)
        } else {
            isNumistaSeriesSource(source) || isNumistaTypeSource(source)
        }
        if (!sourceIsValid) {
            return CollectionCatalogValidationError.InvalidSource
        }
        if (members.isEmpty()) {
            return CollectionCatalogValidationError.EmptyMembers
        }

        val memberIds = mutableSetOf<String>()
        val typeIds = mutableSetOf<Int>()
        val dateSlots = mutableSetOf<Pair<Int, Int>>()
        val issueIds = mutableSetOf<Int>()
        for (member in members) {
            if (!isSlug(member.id)) {
                return CollectionCatalogValidationError.InvalidId("member", member.id)
            }
            if (!memberIds.add(member.id)) {
                return CollectionCatalogValidationError.DuplicateMemberId(member.id)
            }
            blankField("member[${member.id}].label", member.label)?.let { return it }
            if (member.numistaTypeId <= 0) {
                return CollectionCatalogValidationError.InvalidNumistaTypeId
            }
            // Only an issue run may name issues, and every one of its members must name at
            // least one: a member with none could never be owned.
            if (isIssueRun) {
                if (member.numistaIssueIds.isEmpty()) {
                    return CollectionCatalogValidationError.MemberWithoutIssue(member.id)
                }
                for (issueId in member.numistaIssueIds) {
                    if (issueId <= 0) {
                        return CollectionCatalogValidationError.InvalidNumistaIssueId
                    }
                    // One issue in two slots would let a single piece fill both.
                    if (!issueIds.add(issueId)) {
                        return CollectionCatalogValidationError.DuplicateNumistaIssueId(issueId)
                    }
                }
            } else if (member.numistaIssueIds.isNotEmpty()) {
                return CollectionCatalogValidationError.IssuesOutsideIssueRun(member.id)
            }
            when {
                // An issue run repeats its type across issues, as a date run does across years.
                isIssueRun -> Unit
                !isDateRun -> if (!typeIds.add(member.numistaTypeId)) {
                    return CollectionCatalogValidationError.DuplicateNumistaTypeId(
                        member.numistaTypeId,
                    )
                }
                !dateSlots.add(member.numistaTypeId to member.year) ->
                    return CollectionCatalogValidationError.DuplicateMemberYear(
                        member.numistaTypeId,
                        member.year,
                    )
            }
        }
        return null
    }
}

@Serializable
data class CollectionCatalogMember(
    val id: String,
    val label: String,
    val year: Int,
    @SerialName("numista_type_id") val numistaTypeId: Int,
    /**
     * The Numista issues this member stands for, in an issue run (ADR 0014).
     *
     * A list rather than one id because a slot can hold several varieties of the same issue and
     * the collector counts them as one: the 1969 star of the 100 pesetas exists with a curved
     * and a straight nine, and owning either fills the 1969.
     */
    @SerialName("numista_issue_ids") val numistaIssueIds: List<Int> = emptyList(),
)

sealed class CollectionCatalogValidationError(val message: String) {
    data class UnsupportedSchemaVersion(val version: Int) : CollectionCatalogValidationError(
        "collection catalog schema version `$version` is not supported",
    )

    data class InvalidId(val kind: String, val value: String) : CollectionCatalogValidationError(
        "$kind id `$value` must be a nonempty lowercase slug",
    )

    data class BlankField(val field: String) : CollectionCatalogValidationError(
        "required field `$field` cannot be blank",
    )

    data object InvalidVariantKey : CollectionCatalogValidationError(
        "collection catalog has an invalid proposal variant key",
    )

    data object SetDeclaresVariant : CollectionCatalogValidationError(
        "a set catalog spans physical variants, so it cannot declare a weight or a finish",
    )

    data object MissingWeight : CollectionCatalogValidationError(
        "collection catalog must declare `weight_millioz` unless it is a set",
    )

    data object InvalidSource : CollectionCatalogValidationError(
        "collection catalog source must be an HTTPS Numista series URL " +
            "(or a Numista type URL for date runs)",
    )

    data object EmptyMembers : CollectionCatalogValidationError(
        "collection catalog must contain at least one member",
    )

    data class DuplicateMemberId(val memberId: String) : CollectionCatalogValidationError(
        "member id `$memberId` is duplicated",
    )

    data object InvalidNumistaTypeId : CollectionCatalogValidationError(
        "Numista type id must be greater than zero",
    )

    data class DuplicateNumistaTypeId(val typeId: Int) : CollectionCatalogValidationError(
        "Numista type id `$typeId` is assigned more than once in the collection catalog",
    )

    data class DuplicateMemberYear(val typeId: Int, val year: Int) :
        CollectionCatalogValidationError(
            "date-run member for type `$typeId` and year `$year` is duplicated",
        )

    data class MemberWithoutIssue(val memberId: String) : CollectionCatalogValidationError(
        "issue-run member `$memberId` names no Numista issue, so it could never be owned",
    )

    data object InvalidNumistaIssueId : CollectionCatalogValidationError(
        "Numista issue id must be greater than zero",
    )

    data class DuplicateNumistaIssueId(val issueId: Int) : CollectionCatalogValidationError(
        "Numista issue `$issueId` is assigned to more than one member",
    )

    data class IssuesOutsideIssueRun(val memberId: String) : CollectionCatalogValidationError(
        "member `$memberId` names Numista issues, which only an issue run may do",
    )
}

private fun blankField(field: String, value: String): CollectionCatalogValidationError? =
    if (value.isBlank()) CollectionCatalogValidationError.BlankField(field) else null

internal fun isSlug(value: String): Boolean =
    value.isNotEmpty() &&
        value.split('-').all { segment ->
            segment.isNotEmpty() && segment.all { character ->
                character in 'a'..'z' || character in '0'..'9'
            }
        }

internal fun isAsciiDigit(character: Char): Boolean = character in '0'..'9'

private const val SERIES_PREFIX = "https://en.numista.com/catalogue/series.php?id="
private const val TYPE_PREFIX = "https://en.numista.com/catalogue/pieces"

private fun isNumistaSeriesSource(source: String): Boolean {
    if (!source.startsWith(SERIES_PREFIX)) return false
    val id = source.removePrefix(SERIES_PREFIX)
    return id.isNotEmpty() && id.all(::isAsciiDigit)
}

internal fun isNumistaTypeSource(source: String): Boolean {
    if (!source.startsWith(TYPE_PREFIX) || !source.endsWith(".html")) return false
    val id = source.removePrefix(TYPE_PREFIX).removeSuffix(".html")
    return id.isNotEmpty() && id.all(::isAsciiDigit)
}
