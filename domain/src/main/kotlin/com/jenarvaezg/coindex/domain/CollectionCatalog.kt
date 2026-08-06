package com.jenarvaezg.coindex.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A curated, sourced reference list of official members for one exact variant key.
 *
 * For an `issued` member, `schema_version` 1 identifies it by a unique Numista type, optionally
 * refined with one or more Numista issues when a type page covers several physical variants.
 * `schema_version` 2 is a date run (ADR 0009): issued members repeat one type across years, and
 * a member is owned only when the piece also records that year. It may refine that year with
 * `numista_issue_ids` when the type page mixes finishes (ADR 0019). `schema_version` 3 is a set
 * issued as a set (ADR 0012): its members span physical variants, so it declares no weight, no
 * finish and no metal, and its key carries an absent weight.
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
    /**
     * The card-sized name of the collection (#22). `name` defines the editorial scope and runs to
     * 200 characters, so it belongs on the plate and never fitted an index card; before this field
     * the card showed `family`, which is the **grouping key** of a variant and not a name at all —
     * and the six hardcoded aliases it needed were the scar.
     *
     * Required, unique across every curated file, and a prefix of [name] so it cannot drift from
     * it. The uniqueness is what does the work: it forces «Silver Britannia ¼ oz» where cutting
     * [name] mechanically would have produced three identical cards.
     */
    @SerialName("short_name") val shortName: String,
    /**
     * The issuer of a member that does not name its own, which is every member of 59 of the 60
     * shipped catalogs.
     *
     * It is **a default and not a claim about the list** (#170). Equilibrium is struck for Tokelau
     * and for Niue in alternate years — Numista's own series 3245 heads itself «Emisores: Niue,
     * Tokelau» — so a single code here could only be read as the issuer of the whole catalog by
     * printing «Tokelau» over the two coins that say Niue on them. Required all the same: a
     * catalog whose every member had to repeat its country would spend eight lines saying what one
     * says, and [CollectionCatalogMember.issuerCode] is the exception, not the rule.
     */
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
    /**
     * What draws the boundary when Numista is not what draws it, in prose with a URL when there
     * is one. Optional and allowed in both statuses, which is the whole point: `closed_note` is
     * forbidden while a series is open (#28), so before this field an open catalog that existed
     * because its mint said so had nowhere in the file to say it (#53, ADR 0020). It is the same
     * pair the members got in #48, one level up, and it proves nothing on its own — the curator
     * writes it, the validator only refuses it blank.
     */
    @SerialName("source_note") val sourceNote: String? = null,
    /**
     * Calendar years inside the member span where the mint issued nothing for this variant.
     * Structured so `scripts/stale-catalogs.py` can drop them from interior gaps; they are never
     * members and never touch the plate denominator. Declaring any year requires [noIssueNote]
     * with the proof (#130, #131).
     */
    @SerialName("no_issue_years") val noIssueYears: List<Int> = emptyList(),
    /** What sustains [noIssueYears], in prose with a URL when there is one. */
    @SerialName("no_issue_note") val noIssueNote: String? = null,
    /**
     * Which face the notebook prints when the page prints one, declared by the curator (#227).
     *
     * See [PrintedSide] for the criterion and for why it cannot be inferred. Two things about it are
     * this file's business. It is **of the plate and never of a member**: if one coin wants a face
     * its sisters do not, either the whole plate changes or it is borne, and the reason goes in
     * [sourceNote] — a hard rule on purpose, so the debt stays visible in the curated file instead
     * of becoming one more precedence to validate, test and explain. And even in the catalogs of
     * heterogeneous members — `spain-face-value-18g`, the 1983 Portuguese set — the pattern is
     * uniform inside the plate: the obverse is the constant and the reverse is what changes.
     *
     * Absent is [PrintedSide.Reverse], so no shipped file had to be touched to add it.
     */
    @SerialName("printed_side") val printedSide: PrintedSide = PrintedSide.Reverse,
    @SerialName("updated_at") val updatedAt: String,
    val members: List<CollectionCatalogMember>,
) {
    fun key(): VariantKey =
        VariantKey(family, weightMillioz, finish, metal)

    /** Who struck one member: its own issuer where it declares one, and the catalog's where not. */
    fun issuerCodeOf(member: CollectionCatalogMember): String = member.issuerCode ?: issuerCode

    /**
     * Every issuer the members of this catalog were struck for, in the order they appear.
     *
     * One in 59 of the 60 shipped catalogs and two in Equilibrium (#170), which is why nothing may
     * read [issuerCode] as *the* issuer of a collection: a card that names a country has to name
     * one this returns, and a catalog that spans two names none of them by itself.
     */
    fun issuerCodes(): Set<String> = members.mapTo(LinkedHashSet()) { issuerCodeOf(it) }

    val isDateRun: Boolean get() = schemaVersion == 2

    /** A set issued as a set: the set is the collectible unit, not any one physical variant. */
    val isSet: Boolean get() = schemaVersion == 3

    /** Members are Numista issues of one type rather than years of it (ADR 0014). */
    val isIssueRun: Boolean get() = schemaVersion == 5

    /**
     * Whether a collected item satisfies one member.
     *
     * Schema 1 matches by type and, when declared, Numista issue; a date run also requires the
     * year recorded on the piece, so an undated piece never fills a year. An issue run matches
     * by Numista issue and **ignores the year entirely**: its members share one, which is exactly
     * why they are keyed on the issue. A piece recorded without an issue fills no member of it.
     *
     * An announced or unlisted member has no type, so nothing ever fills it, and its
     * `design_type_id` is
     * **never** consulted (#31): the Seymour Panther announced in 2 oz bullion cites its proof
     * cousin, and the father owns proof pieces of that series — matching on it would fill a
     * bullion slot with a coin that does not exist in bullion.
     */
    fun memberMatches(member: CollectionCatalogMember, item: CollectedItem): Boolean {
        val typeId = member.numistaTypeId ?: return false
        if (item.quantity <= 0 || item.typeId != typeId) return false
        if (member.numistaIssueIds.isNotEmpty() && item.issueId !in member.numistaIssueIds) {
            return false
        }
        return when {
            isIssueRun -> member.numistaIssueIds.isNotEmpty()
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
     * Whether the collector owns at least one official identity of this catalog. Evidence
     * ignores the date in date runs so a plate stays reachable while its years are still
     * missing, but an issue qualifier remains part of the identity.
     *
     * An announced member contributes nothing — it has no type — and its `design_type_id` is
     * ignored here for the same reason [memberMatches] ignores it.
     */
    fun isEvidencedBy(items: List<CollectedItem>): Boolean {
        return items.any { item ->
            item.quantity > 0 && members.any { member ->
                member.numistaTypeId == item.typeId &&
                    (member.numistaIssueIds.isEmpty() || item.issueId in member.numistaIssueIds)
            }
        }
    }

    fun validate(): CollectionCatalogValidationError? {
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

@Serializable
data class CollectionCatalogMember(
    val id: String,
    val label: String,
    /**
     * The year on the coin. Required when issued or unlisted and optional only when announced,
     * whose source may name the design without a date — writing in a year the mint has not
     * announced would be claiming more than the source says.
     */
    val year: Int? = null,
    /** Required only when issued (see [MemberStatus]). */
    @SerialName("numista_type_id") val numistaTypeId: Int? = null,
    /**
     * The Numista issues this member stands for: optional in schema 1 and in a date run, and
     * required in an issue run (ADR 0014, ADR 0019).
     *
     * A list rather than one id because a slot can hold several varieties of the same issue and
     * the collector counts them as one: the 1969 star of the 100 pesetas exists with a curved
     * and a straight nine, and owning either fills the 1969. On a date run the list keeps a
     * proof or burnished row of the same type and year from filling the bullion slot.
     */
    @SerialName("numista_issue_ids") val numistaIssueIds: List<Int> = emptyList(),
    /** Issued unless the file says otherwise, so no shipped catalog had to be touched (#31). */
    val status: MemberStatus = MemberStatus.Issued,
    /** Proof for a non-issued status. HTTPS, any host, required unless [status] is issued. */
    val source: String? = null,
    /** What [source] proves, in prose so the claim outlives the link. */
    @SerialName("source_note") val sourceNote: String? = null,
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
    /**
     * Who struck this member, when it is not who struck the rest of the catalog (#170).
     *
     * Absent means the catalog's own [CollectionCatalog.issuerCode], so no shipped file had to be
     * touched. It is here and not only in the header because the issuer is a fact about a coin: a
     * mint that alternates countries within one series — Pressburg strikes Equilibrium for Tokelau
     * and for Niue — leaves a list that no single code describes, and splitting the catalog would
     * split a series the mint did not, which ADR 0020 already refused to make a gatekeeper.
     *
     * It changes nothing about matching or counting: the piece is found by its type, and the
     * country is what gets printed over it.
     */
    @SerialName("issuer_code") val issuerCode: String? = null,
) {
    val isIssued: Boolean get() = status == MemberStatus.Issued

    /** Struck and sold, but with no publicly verifiable Numista type. */
    val isUnlisted: Boolean get() = status == MemberStatus.Unlisted

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
