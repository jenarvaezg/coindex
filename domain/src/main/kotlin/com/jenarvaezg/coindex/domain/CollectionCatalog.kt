package com.jenarvaezg.coindex.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A curated, sourced reference list of official members for one exact variant key.
 *
 * `schema_version` says how an issued member is identified:
 * - 1: by a unique Numista type, optionally refined by Numista issues when a type page covers
 *   several physical variants.
 * - 2: a date run (ADR 0009). Members repeat one type across years, and a member is owned only
 *   when the piece records that year; `numista_issue_ids` may refine a year when the type page
 *   mixes finishes (ADR 0019).
 * - 3: a set issued as a set (ADR 0012). Members span physical variants, so it declares no
 *   weight, finish or metal, and its key carries an absent weight.
 * - 5: an issue run (ADR 0014). Members are Numista issues of one type that share a year and
 *   differ by a variety.
 *
 * Every version declares a [SeriesStatus], since an open series cannot claim complete coverage.
 */
@Serializable
data class CollectionCatalog(
    @SerialName("schema_version") val schemaVersion: Int,
    val id: String,
    val name: String,
    /**
     * The name on the index card (#22); [name] is the editorial scope and goes on the plate.
     * Required, unique across catalogs and groupings, and a prefix of [name], so it cannot drift
     * from it. The uniqueness forces «Silver Britannia ¼ oz» where a mechanical cut of [name]
     * would give three identical cards.
     */
    @SerialName("short_name") val shortName: String,
    /**
     * The issuer of every member that does not declare its own
     * [CollectionCatalogMember.issuerCode]. A default, not the issuer of the whole catalog (#170):
     * Equilibrium alternates Tokelau and Niue. Required, and it must be the issuer of at least one
     * member.
     */
    @SerialName("issuer_code") val issuerCode: String,
    val family: String,
    @SerialName("weight_millioz") val weightMillioz: Int? = null,
    val finish: Finish? = null,
    /**
     * The dominant metal of the variant (#40, ADR 0018), required unless the catalog is a set. It
     * describes the collection, not each member: a list may hold a coin of another metal.
     */
    val metal: Metal? = null,
    /**
     * Whether the series is still being issued. Required in every schema version, so no catalog
     * claims completeness by omission (#28).
     */
    @SerialName("series_status") val seriesStatus: SeriesStatus,
    /** What sustains the closure, in prose with a URL when there is one. Only when closed. */
    @SerialName("closed_note") val closedNote: String? = null,
    val source: String,
    /**
     * What draws the boundary when Numista does not, in prose with a URL when there is one (#53,
     * ADR 0020). Optional in both statuses, unlike `closed_note`. The validator only rejects it
     * blank.
     */
    @SerialName("source_note") val sourceNote: String? = null,
    /**
     * Calendar years inside the member span where the mint issued nothing for this variant, so
     * `scripts/stale-catalogs.py` can drop them from interior gaps. Never members, never in the
     * plate denominator, never a year a member holds. Requires [noIssueNote] (#130, #131).
     */
    @SerialName("no_issue_years") val noIssueYears: List<Int> = emptyList(),
    /** What sustains [noIssueYears], in prose with a URL when there is one. */
    @SerialName("no_issue_note") val noIssueNote: String? = null,
    /**
     * Which face the notebook prints when the page prints one (#227); see [PrintedSide]. It
     * belongs to the plate and never to a member, on purpose: a coin that wants another face is
     * either borne, with the reason in [sourceNote], or the whole plate changes. Absent means
     * [PrintedSide.Reverse].
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
     * Every issuer the members were struck for, in member order. A card that names a country names
     * one of these, never [issuerCode] as the issuer of the whole collection (#170).
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
     * Schema 1 matches by type and, when declared, Numista issue. A date run also requires the
     * year recorded on the piece, so an undated piece never fills a year. An issue run matches by
     * Numista issue and ignores the year, which its members share; a piece recorded without an
     * issue fills none of them.
     *
     * An announced or unlisted member has no type, so nothing fills it, and its `design_type_id`
     * is never consulted (#31): it may cite a proof cousin the collector owns, which would fill a
     * bullion slot with a coin never struck in bullion.
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
     * The member label that tells one owned piece apart, such as «Estrella 67» where the row only
     * says 1966. Only an issue run has one.
     */
    fun emissionLabelFor(item: CollectedItem): String? {
        if (!isIssueRun) return null
        return members.firstOrNull { member -> memberMatches(member, item) }?.label
    }

    /**
     * Whether the collector owns at least one official identity of this catalog. The date is
     * ignored, so a date run stays reachable while its years are missing, but an issue qualifier
     * still counts. A member without a type contributes nothing, as in [memberMatches].
     */
    fun isEvidencedBy(items: List<CollectedItem>): Boolean {
        return items.any { item ->
            item.quantity > 0 && members.any { member ->
                member.numistaTypeId == item.typeId &&
                    (member.numistaIssueIds.isEmpty() || item.issueId in member.numistaIssueIds)
            }
        }
    }
}

@Serializable
data class CollectionCatalogMember(
    val id: String,
    val label: String,
    /**
     * The year on the coin. Required when issued or unlisted; optional when announced, since the
     * source may name the design without a date.
     */
    val year: Int? = null,
    /** Required when issued and forbidden otherwise (see [MemberStatus]). */
    @SerialName("numista_type_id") val numistaTypeId: Int? = null,
    /**
     * The Numista issues this member stands for: optional in schema 1 and in a date run, required
     * in an issue run, and only on issued members (ADR 0014, ADR 0019). No issue may sit in two
     * members.
     *
     * A list because one slot can hold several varieties the collector counts as one: the 1969
     * star of the 100 pesetas with a curved or a straight nine. On a date run it keeps a proof or
     * burnished row of the same type and year out of the bullion slot.
     */
    @SerialName("numista_issue_ids") val numistaIssueIds: List<Int> = emptyList(),
    /** Absent means issued (#31). */
    val status: MemberStatus = MemberStatus.Issued,
    /** Proof for a non-issued status: HTTPS, any host. Required unless issued, forbidden if so. */
    val source: String? = null,
    /** What [source] proves, in prose so the claim outlives the link. */
    @SerialName("source_note") val sourceNote: String? = null,
    /**
     * The Numista type of the same design in another variant, when one exists: it keeps an
     * unstruck member verifiable and gives the plate a picture for its cell. Forbidden on issued
     * members and never used for matching (see [CollectionCatalog.memberMatches]).
     */
    @SerialName("design_type_id") val designTypeId: Int? = null,
    /**
     * Why this member departs from the variant the catalog declares, in prose. The catalog stays
     * authoritative about the variant (ADR 0016), so keying and counting do not change; the note
     * only exempts the member from the suite's metal cross-check against its Numista ficha.
     */
    @SerialName("variant_note") val variantNote: String? = null,
    /**
     * Who struck this member, when it is not the catalog's [CollectionCatalog.issuerCode] (#170):
     * Pressburg strikes Equilibrium for both Tokelau and Niue, and splitting the catalog would
     * split a series the mint did not (ADR 0020). Absent means the catalog's code. It changes only
     * the country printed, not matching or counting.
     */
    @SerialName("issuer_code") val issuerCode: String? = null,
) {
    val isIssued: Boolean get() = status == MemberStatus.Issued

    /** Struck and sold, but with no publicly verifiable Numista type. */
    val isUnlisted: Boolean get() = status == MemberStatus.Unlisted

    /** Named by the issuer and not yet struck, so no piece can fill it. */
    val isAnnounced: Boolean get() = status == MemberStatus.Announced
}

internal fun isSlug(value: String): Boolean =
    value.isNotEmpty() &&
        value.split('-').all { segment ->
            segment.isNotEmpty() && segment.all { character ->
                character in 'a'..'z' || character in '0'..'9'
            }
        }

internal fun isAsciiDigit(character: Char): Boolean = character in '0'..'9'

private const val TYPE_PREFIX = "https://en.numista.com/catalogue/pieces"

internal fun isNumistaTypeSource(source: String): Boolean {
    if (!source.startsWith(TYPE_PREFIX) || !source.endsWith(".html")) return false
    val id = source.removePrefix(TYPE_PREFIX).removeSuffix(".html")
    return id.isNotEmpty() && id.all(::isAsciiDigit)
}
