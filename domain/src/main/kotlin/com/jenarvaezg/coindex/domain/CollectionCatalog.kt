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
 * so it declares no weight, no finish and no metal, and its key carries an absent weight.
 * `schema_version` 5 identifies members by Numista issue (ADR 0014), for the types whose
 * members share a year and differ by a variety of it.
 *
 * Every version declares a [SeriesStatus]: coverage is what a catalog is for, and an open
 * series has no complete coverage to claim.
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
    /**
     * The dominant metal of the variant (#40, ADR 0018). Required of everything but a set, and
     * a statement about **the collection**, not about each member: a curator who puts seven
     * silver coins and one of cupronickel in the same list is curating, not making a mistake.
     */
    val metal: Metal? = null,
    /**
     * Whether the series is still being issued. Required in every schema version: a catalog
     * that keeps quiet about it claims completeness by omission (#28).
     */
    @SerialName("series_status") val seriesStatus: SeriesStatus,
    /** What sustains the closure, in prose with a URL when there is one. Only when closed. */
    @SerialName("closed_note") val closedNote: String? = null,
    val source: String,
    @SerialName("updated_at") val updatedAt: String,
    val members: List<CollectionCatalogMember>,
) {
    fun key(): CollectionProposalKey =
        CollectionProposalKey(family, weightMillioz, finish, metal)

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
     *
     * An announced member has no type, so nothing ever fills it, and its `design_type_id` is
     * **never** consulted (#31): the Seymour Panther announced in 2 oz bullion cites its proof
     * cousin, and the father owns proof pieces of that series — matching on it would fill a
     * bullion slot with a coin that does not exist in bullion.
     */
    fun memberMatches(member: CollectionCatalogMember, item: CollectedItem): Boolean {
        val typeId = member.numistaTypeId ?: return false
        if (item.quantity <= 0 || item.typeId != typeId) return false
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
     *
     * An announced member contributes nothing — it has no type — and its `design_type_id` is
     * ignored here for the same reason [memberMatches] ignores it.
     */
    fun isEvidencedBy(items: List<CollectedItem>): Boolean {
        val memberTypeIds = members.mapNotNullTo(mutableSetOf()) { it.numistaTypeId }
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
        if (isSet && (weightMillioz != null || finish != null || metal != null)) {
            return CollectionCatalogValidationError.SetDeclaresVariant
        }
        if (!isSet && weightMillioz == null) {
            return CollectionCatalogValidationError.MissingWeight
        }
        // The metal is part of the key, so a catalog that keeps quiet about it would collide
        // with the one curated next over the same family, weight and finish (#40).
        if (!isSet && metal == null) {
            return CollectionCatalogValidationError.MissingMetal
        }
        val canonical = CollectionProposalKey.fromCanonicalParts(
            family,
            weightMillioz ?: SPANNING_VARIANTS_WEIGHT,
            finishCode(finish),
            metalCode(metal),
        )
        if (canonical != key()) {
            return CollectionCatalogValidationError.InvalidVariantKey
        }
        // A series URL when one proposed the list, and a type page when nothing did: the series
        // only proposes and the catalog is what affirms coverage (#43), so a boundary that lives
        // outside Numista is not a lesser catalog (#33). The 10 gulden of Beatrix are five in the
        // Handboek and no series at all in Numista; requiring one would have forced a fake.
        if (!isNumistaSeriesSource(source) && !isNumistaTypeSource(source)) {
            return CollectionCatalogValidationError.InvalidSource
        }
        if (members.isEmpty()) {
            return CollectionCatalogValidationError.EmptyMembers
        }
        // Closing costs proof and opening costs none, so the note is required exactly one way.
        when (seriesStatus) {
            SeriesStatus.Closed -> if (closedNote.isNullOrBlank()) {
                return CollectionCatalogValidationError.ClosedWithoutNote
            }
            SeriesStatus.Open -> if (closedNote != null) {
                return CollectionCatalogValidationError.OpenWithClosedNote
            }
        }

        val memberIds = mutableSetOf<String>()
        val typeIds = mutableSetOf<Int>()
        val dateSlots = mutableSetOf<Pair<Int, Int?>>()
        val issueIds = mutableSetOf<Int>()
        for (member in members) {
            if (!isSlug(member.id)) {
                return CollectionCatalogValidationError.InvalidId("member", member.id)
            }
            if (!memberIds.add(member.id)) {
                return CollectionCatalogValidationError.DuplicateMemberId(member.id)
            }
            blankField("member[${member.id}].label", member.label)?.let { return it }
            if (member.variantNote != null && member.variantNote.isBlank()) {
                return CollectionCatalogValidationError.BlankVariantNote(member.id)
            }
            validateMemberStatus(member)?.let { return it }
            // Only an issued member of an issue run may name issues, and every one of them must
            // name at least one: a member with none could never be owned.
            if (isIssueRun && !member.isAnnounced) {
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
            val typeId = member.numistaTypeId
            when {
                // An announced member holds no type, so there is no slot of any kind to collide.
                typeId == null -> Unit
                // An issue run repeats its type across issues, as a date run does across years.
                isIssueRun -> Unit
                !isDateRun -> if (!typeIds.add(typeId)) {
                    return CollectionCatalogValidationError.DuplicateNumistaTypeId(typeId)
                }
                !dateSlots.add(typeId to member.year) ->
                    return CollectionCatalogValidationError.DuplicateMemberYear(
                        typeId,
                        member.year,
                    )
            }
        }
        return null
    }
}

/**
 * The symmetry of [MemberStatus], required in both directions so nothing is implicit: an
 * absent `numista_type_id` never *means* announced, the file has to say so.
 */
private fun validateMemberStatus(
    member: CollectionCatalogMember,
): CollectionCatalogValidationError? {
    if (!member.isAnnounced) {
        if (member.numistaTypeId == null || member.numistaTypeId <= 0) {
            return CollectionCatalogValidationError.InvalidNumistaTypeId
        }
        // A struck coin has a verifiable year on its Numista page; only an announced one may
        // omit it.
        if (member.year == null) {
            return CollectionCatalogValidationError.IssuedWithoutYear(member.id)
        }
        if (member.announcedSource != null || member.announcedNote != null) {
            return CollectionCatalogValidationError.IssuedWithAnnouncement(member.id)
        }
        if (member.designTypeId != null) {
            return CollectionCatalogValidationError.IssuedWithDesignType(member.id)
        }
        return null
    }
    if (member.numistaTypeId != null) {
        return CollectionCatalogValidationError.AnnouncedWithNumistaType(member.id)
    }
    // The host is not constrained: numista.com cannot hold an unstruck coin by definition, and
    // the mint publishes the programme rather than the absence of one release from it.
    if (member.announcedSource == null || !isHttpsUrl(member.announcedSource)) {
        return CollectionCatalogValidationError.AnnouncedWithoutSource(member.id)
    }
    if (member.announcedNote.isNullOrBlank()) {
        return CollectionCatalogValidationError.AnnouncedWithoutNote(member.id)
    }
    if (member.designTypeId != null && member.designTypeId <= 0) {
        return CollectionCatalogValidationError.InvalidDesignTypeId
    }
    return null
}

@Serializable
data class CollectionCatalogMember(
    val id: String,
    val label: String,
    /**
     * The year on the coin. Required of an issued member and optional only in an announced one,
     * whose source may name the design without a date — writing in a year the mint has not
     * announced would be claiming more than the source says.
     */
    val year: Int? = null,
    /** Required of an issued member and forbidden in an announced one (see [MemberStatus]). */
    @SerialName("numista_type_id") val numistaTypeId: Int? = null,
    /**
     * The Numista issues this member stands for, in an issue run (ADR 0014).
     *
     * A list rather than one id because a slot can hold several varieties of the same issue and
     * the collector counts them as one: the 1969 star of the 100 pesetas exists with a curved
     * and a straight nine, and owning either fills the 1969.
     */
    @SerialName("numista_issue_ids") val numistaIssueIds: List<Int> = emptyList(),
    /** Issued unless the file says otherwise, so no shipped catalog had to be touched (#31). */
    val status: MemberStatus = MemberStatus.Issued,
    /** Where the announcement was read. HTTPS, any host, and required when announced. */
    @SerialName("announced_source") val announcedSource: String? = null,
    /** What the source said, in prose, so the claim outlives the link. Required when announced. */
    @SerialName("announced_note") val announcedNote: String? = null,
    /**
     * The Numista type of the **same design in another variant**, when one exists: the anchor
     * that keeps an unstruck member verifiable, and the picture the plate can put in its cell.
     *
     * Never used for matching — see [CollectionCatalog.memberMatches].
     */
    @SerialName("design_type_id") val designTypeId: Int? = null,
    /**
     * Why this member departs from the variant the catalog declares, in prose.
     *
     * By ADR 0016 the catalog is authoritative about its members' variant, so the deviation
     * changes nothing about how the piece is keyed or counted. What it buys is the right to say
     * so out loud: the suite cross-checks each member's metal against its Numista ficha, and a
     * member carrying this note is exempt — the same bargain `closed_note` makes when a catalog
     * closes a series. It never silences anything by itself; a note with no deviation is inert.
     */
    @SerialName("variant_note") val variantNote: String? = null,
) {
    /** Named by the issuer and not yet struck, so no piece can fill it. */
    val isAnnounced: Boolean get() = status == MemberStatus.Announced
}

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
        "a set catalog spans physical variants, so it cannot declare a weight, finish or metal",
    )

    data object MissingWeight : CollectionCatalogValidationError(
        "collection catalog must declare `weight_millioz` unless it is a set",
    )

    data object MissingMetal : CollectionCatalogValidationError(
        "collection catalog must declare `metal` unless it is a set",
    )

    data class BlankVariantNote(val memberId: String) : CollectionCatalogValidationError(
        "member `$memberId` carries an empty `variant_note`: say what deviates, or drop it",
    )

    data object InvalidSource : CollectionCatalogValidationError(
        "collection catalog source must be an HTTPS Numista series or type URL",
    )

    data object EmptyMembers : CollectionCatalogValidationError(
        "collection catalog must contain at least one member",
    )

    data object ClosedWithoutNote : CollectionCatalogValidationError(
        "a closed collection catalog must say what sustains the closure in `closed_note`",
    )

    data object OpenWithClosedNote : CollectionCatalogValidationError(
        "an open collection catalog cannot carry a `closed_note`",
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

    data class DuplicateMemberYear(val typeId: Int, val year: Int?) :
        CollectionCatalogValidationError(
            "date-run member for type `$typeId` and year `$year` is duplicated",
        )

    data class IssuedWithoutYear(val memberId: String) : CollectionCatalogValidationError(
        "issued member `$memberId` must declare the year on the coin",
    )

    data class IssuedWithAnnouncement(val memberId: String) : CollectionCatalogValidationError(
        "issued member `$memberId` cannot carry `announced_source` or `announced_note`",
    )

    data class IssuedWithDesignType(val memberId: String) : CollectionCatalogValidationError(
        "issued member `$memberId` already names its own type, so `design_type_id` is redundant",
    )

    data class AnnouncedWithNumistaType(val memberId: String) : CollectionCatalogValidationError(
        "announced member `$memberId` cannot name a `numista_type_id`: it has not been struck",
    )

    data class AnnouncedWithoutSource(val memberId: String) : CollectionCatalogValidationError(
        "announced member `$memberId` must cite an HTTPS `announced_source`",
    )

    data class AnnouncedWithoutNote(val memberId: String) : CollectionCatalogValidationError(
        "announced member `$memberId` must say in `announced_note` what the source said",
    )

    data object InvalidDesignTypeId : CollectionCatalogValidationError(
        "Numista design type id must be greater than zero",
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

/** Enough to reject a note pasted into the URL field; the host is deliberately not policed. */
private fun isHttpsUrl(value: String): Boolean =
    value.startsWith("https://") && value.length > "https://".length && value.none { it == ' ' }

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
