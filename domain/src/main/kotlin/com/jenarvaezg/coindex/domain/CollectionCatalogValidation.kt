package com.jenarvaezg.coindex.domain

/**
 * Typed validation of a [CollectionCatalog] (ADR 0027).
 *
 * The sealed error surface is intentional: the curator — not the seed loader that only reads
 * `.message` — is the consumer, and each case names the field and the condition that failed.
 */

fun CollectionCatalog.validate(): CollectionCatalogValidationError? {
    if (schemaVersion !in 1..3 && schemaVersion != 5) {
        return CollectionCatalogValidationError.UnsupportedSchemaVersion(schemaVersion)
    }
    if (!isSlug(id)) {
        return CollectionCatalogValidationError.InvalidId("catalog", id)
    }
    blankField("catalog.name", name)?.let { return it }
    blankField("catalog.short_name", shortName)?.let { return it }
    // A card name that is not a prefix of the editorial one is a second source of truth.
    if (!name.startsWith(shortName)) {
        return CollectionCatalogValidationError.ShortNameNotPrefix(shortName)
    }
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
    val canonical = VariantKey.fromCanonicalParts(
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
    if (sourceNote != null && sourceNote.isBlank()) {
        return CollectionCatalogValidationError.BlankSourceNote
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
    // Silenced calendar gaps are the same bargain: years without a note are unsigned claims,
    // and a note without years is orphan prose. They must sit inside the member year span and
    // never collide with a slot that already exists.
    if (noIssueYears.isEmpty()) {
        if (noIssueNote != null) {
            return CollectionCatalogValidationError.NoIssueNoteWithoutYears
        }
    } else {
        if (noIssueNote == null) {
            return CollectionCatalogValidationError.NoIssueYearsWithoutNote
        }
        if (noIssueNote.isBlank()) {
            return CollectionCatalogValidationError.BlankNoIssueNote
        }
        val memberYears = members.mapNotNull { it.year }.toSet()
        val firstYear = memberYears.minOrNull()
        val lastYear = memberYears.maxOrNull()
        val seenNoIssue = mutableSetOf<Int>()
        for (year in noIssueYears) {
            if (!seenNoIssue.add(year)) {
                return CollectionCatalogValidationError.DuplicateNoIssueYear(year)
            }
            if (year in memberYears) {
                return CollectionCatalogValidationError.NoIssueYearConflictsWithMember(year)
            }
            if (firstYear == null || lastYear == null || year !in firstYear..lastYear) {
                return CollectionCatalogValidationError.NoIssueYearOutsideSpan(year)
            }
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
        if (member.issuerCode != null && member.issuerCode.isBlank()) {
            return CollectionCatalogValidationError.BlankMemberIssuerCode(member.id)
        }
        validateMemberStatus(member)?.let { return it }
        // Schema 1 and date runs may refine an issued type with issues. Schema 5 requires
        // that refinement; its members would otherwise be indistinguishable. Sets and
        // non-issued members retain their established identities.
        val issuesAllowed = member.isIssued && (schemaVersion == 1 || isDateRun || isIssueRun)
        if (isIssueRun && member.isIssued && member.numistaIssueIds.isEmpty()) {
            return CollectionCatalogValidationError.MemberWithoutIssue(member.id)
        }
        if (issuesAllowed) {
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
            // A non-issued member holds no type, so there is no slot of any kind to collide.
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
    // The default has to default for somebody. A header naming a country every member
    // overrides is the very lie #170 came from, one indirection further in.
    if (issuerCode !in issuerCodes()) {
        return CollectionCatalogValidationError.UnusedIssuerCode(issuerCode)
    }
    return null
}

/**
 * The symmetry of [MemberStatus], required in both directions so nothing is implicit: an
 * absent `numista_type_id` never *means* announced, the file has to say so.
 */
private fun validateMemberStatus(
    member: CollectionCatalogMember,
): CollectionCatalogValidationError? = when (member.status) {
    MemberStatus.Issued -> when {
        member.numistaTypeId == null || member.numistaTypeId <= 0 ->
            CollectionCatalogValidationError.InvalidNumistaTypeId
        member.year == null -> CollectionCatalogValidationError.IssuedWithoutYear(member.id)
        member.source != null || member.sourceNote != null ->
            CollectionCatalogValidationError.IssuedWithSource(member.id)
        member.designTypeId != null ->
            CollectionCatalogValidationError.IssuedWithDesignType(member.id)
        else -> null
    }
    MemberStatus.Unlisted -> when {
        member.numistaTypeId != null ->
            CollectionCatalogValidationError.UnlistedWithNumistaType(member.id)
        member.year == null -> CollectionCatalogValidationError.UnlistedWithoutYear(member.id)
        else -> validateStatusProof(member)
    }
    MemberStatus.Announced -> when {
        member.numistaTypeId != null ->
            CollectionCatalogValidationError.AnnouncedWithNumistaType(member.id)
        else -> validateStatusProof(member)
    }
}

private fun validateStatusProof(
    member: CollectionCatalogMember,
): CollectionCatalogValidationError? {
    // The host is not constrained: Numista cannot sustain either an unstruck coin or the claim
    // that a struck one has no published type. The issuer or a third party supplies that proof.
    if (member.source == null || !isHttpsUrl(member.source)) {
        return CollectionCatalogValidationError.MemberWithoutSource(member.id)
    }
    if (member.sourceNote.isNullOrBlank()) {
        return CollectionCatalogValidationError.MemberWithoutSourceNote(member.id)
    }
    if (member.designTypeId != null && member.designTypeId <= 0) {
        return CollectionCatalogValidationError.InvalidDesignTypeId
    }
    return null
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

    data class ShortNameNotPrefix(val value: String) : CollectionCatalogValidationError(
        "`short_name` `$value` must be a prefix of `name`",
    )

    data object InvalidVariantKey : CollectionCatalogValidationError(
        "collection catalog has an invalid variant key",
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

    data class BlankMemberIssuerCode(val memberId: String) : CollectionCatalogValidationError(
        "member `$memberId` carries an empty `issuer_code`: name the issuer, or drop it",
    )

    data class UnusedIssuerCode(val value: String) : CollectionCatalogValidationError(
        "catalog `issuer_code` `$value` is the issuer of no member: it defaults for nobody",
    )

    data object InvalidSource : CollectionCatalogValidationError(
        "collection catalog source must be an HTTPS Numista series or type URL",
    )

    data object BlankSourceNote : CollectionCatalogValidationError(
        "collection catalog carries an empty `source_note`: say what draws the boundary, or drop it",
    )

    data object NoIssueYearsWithoutNote : CollectionCatalogValidationError(
        "a catalog that declares `no_issue_years` must say in `no_issue_note` what sustains them",
    )

    data object BlankNoIssueNote : CollectionCatalogValidationError(
        "collection catalog carries an empty `no_issue_note`: say why those years have no slot, or drop them",
    )

    data object NoIssueNoteWithoutYears : CollectionCatalogValidationError(
        "a catalog that carries `no_issue_note` must declare the years in `no_issue_years`",
    )

    data class DuplicateNoIssueYear(val year: Int) : CollectionCatalogValidationError(
        "`no_issue_years` repeats `$year`",
    )

    data class NoIssueYearConflictsWithMember(val year: Int) : CollectionCatalogValidationError(
        "`no_issue_years` claims `$year` had no issue, but a member already occupies that year",
    )

    data class NoIssueYearOutsideSpan(val year: Int) : CollectionCatalogValidationError(
        "`no_issue_years` claims `$year`, which falls outside the span of member years",
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

    data class IssuedWithSource(val memberId: String) : CollectionCatalogValidationError(
        "issued member `$memberId` cannot carry `source` or `source_note`",
    )

    data class IssuedWithDesignType(val memberId: String) : CollectionCatalogValidationError(
        "issued member `$memberId` already names its own type, so `design_type_id` is redundant",
    )

    data class AnnouncedWithNumistaType(val memberId: String) : CollectionCatalogValidationError(
        "announced member `$memberId` cannot name a `numista_type_id`: it has not been struck",
    )

    data class UnlistedWithNumistaType(val memberId: String) : CollectionCatalogValidationError(
        "unlisted member `$memberId` cannot name a `numista_type_id`: no published type exists",
    )

    data class UnlistedWithoutYear(val memberId: String) : CollectionCatalogValidationError(
        "unlisted member `$memberId` must declare the year on the coin",
    )

    data class MemberWithoutSource(val memberId: String) : CollectionCatalogValidationError(
        "member `$memberId` whose status is not `issued` must cite an HTTPS `source`",
    )

    data class MemberWithoutSourceNote(val memberId: String) : CollectionCatalogValidationError(
        "member `$memberId` whose status is not `issued` must say in `source_note` what the source proves",
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
        "member `$memberId` names Numista issues, which only an issued schema-1 member or issue run may do",
    )
}

private fun blankField(field: String, value: String): CollectionCatalogValidationError? =
    if (value.isBlank()) CollectionCatalogValidationError.BlankField(field) else null

/** Enough to reject a note pasted into the URL field; the host is deliberately not policed. */
private fun isHttpsUrl(value: String): Boolean =
    value.startsWith("https://") && value.length > "https://".length && value.none { it == ' ' }

private const val SERIES_PREFIX = "https://en.numista.com/catalogue/series.php?id="

private fun isNumistaSeriesSource(source: String): Boolean {
    if (!source.startsWith(SERIES_PREFIX)) return false
    val id = source.removePrefix(SERIES_PREFIX)
    return id.isNotEmpty() && id.all(::isAsciiDigit)
}

