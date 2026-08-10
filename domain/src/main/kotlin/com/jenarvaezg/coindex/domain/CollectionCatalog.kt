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
